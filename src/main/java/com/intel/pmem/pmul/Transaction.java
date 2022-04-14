/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;
import static jdk.incubator.foreign.ValueLayout.*;

/*
                                        Committed
 State transitions are: New -> Active  /
                                       \
                                        Aborted
*/
class Transaction {
    private static ThreadLocal<Transaction> tlTransaction = new ThreadLocal<>();
    private LowLevelHeap heap;
    private State state; 
    private int depth;
    private long poolAddress;

    static final MethodHandle pmemobjTxBegin;
    static final MethodHandle pmemobjTxEnd;
    static final MethodHandle pmemobjTxCommit;
    static final MethodHandle pmemobjTxAbort;
    static final MethodHandle pmemobjTxAddRangeDirect;
    static final MethodHandle pmemobjTxErrno;

    static final int TX_FLAG;
    static {
        System.loadLibrary("pmemobj");

        TX_FLAG = nativeTxFlag();
        CLinker linker = CLinker.systemCLinker();
        pmemobjTxBegin = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_begin").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
        pmemobjTxEnd = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_end").get(), FunctionDescriptor.of(JAVA_INT));
        pmemobjTxCommit = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_commit").get(), FunctionDescriptor.ofVoid());
        pmemobjTxAbort = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_abort").get(), FunctionDescriptor.ofVoid(JAVA_INT));
        pmemobjTxAddRangeDirect = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_add_range_direct").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));
        pmemobjTxErrno = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_errno").get(), FunctionDescriptor.of(JAVA_INT));
    }

    private enum State {New, Active, Committed, Aborted}

    Transaction(LowLevelHeap heap) {
        this.heap = heap;
        this.poolAddress = heap.poolAddress();
        state = State.New;
    }

    static void run(LowLevelHeap heap, Runnable body) {
        run(heap, () -> {body.run(); return (Void)null;});
    }

    static <T> T run(LowLevelHeap heap, Supplier<T> body) {
        Transaction transaction = tlTransaction.get();
        if (transaction == null || transaction.state != State.Active) {
            tlTransaction.set(transaction = new Transaction(heap));
        }
        return transaction.run(body);
    }

    void run(Runnable body) {
        run(() -> {body.run(); return (Void)null;});
    }

    private <T> T run(Supplier<T> body) {
        if (state == State.New) {
            int err = startTransaction(poolAddress);
            if (err != 0) throw new TransactionException("Failed to start transaction.");
            state = State.Active;
        }
        checkActive();
        depth++;
        T result = null;
        try {
            result = body.get();
        }
        catch (InternalTransactionException e) {
            if (state == State.Active) {
               state = State.Aborted; 
                if (e.getCause() != null) {
                    try{ throw e.getCause(); }
                    catch (RuntimeException r) { throw e; }
                    catch (OutOfMemoryError o) { throw o; }
                    catch (Throwable throwable) { throw new RuntimeException(throwable.getMessage()); }
                }
            }
            throw e;
        }
        catch (Throwable t) {
            if (state == State.Active) { 
                abortTransaction(); 
                state = State.Aborted;
            }
            throw t;
        }
        finally {
            if (state == Transaction.State.Active && depth == 1) {
                state = State.Committed;
                commitTransaction();
                int err = endTransaction();            
                if (err != 0) throw new TransactionException("Failed to end transaction.");
            }
            if (state == Transaction.State.Aborted && depth == 1) {
                // error code will be non zero to reflect the cause of the aborted transaction
                int err = endTransaction();            
            }
            depth--;
        }
        return result;
    }

    static void addToTransaction(MemorySegment segment, long offset, long byteCount) {
        Transaction tx = tlTransaction.get();
        if (tx == null) throw new TransactionException("No transaction active");
        tx.checkActive();
        int err = addToTransaction(segment.asSlice(offset, byteCount));
        if (err != 0) throw new InternalTransactionException("Failed to add byte range to transaction: " + segment + ", byteCount: " + byteCount);
    }

    private void checkActive() {
        if (state != State.Active) throw new IllegalStateException("Transaction not Active");
    }
    
    int startTransaction(long poolAddress) {
        int ret = 0;
        try {
            ret = (int)pmemobjTxBegin.invokeExact((Addressable)MemoryAddress.ofLong(poolAddress), (Addressable)MemoryAddress.NULL, TX_FLAG); 
            if (ret != 0) pmemobjTxEnd.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return ret;
    }

    static void commitTransaction(){
        try{
            pmemobjTxCommit.invokeExact();
        } catch(Throwable t) {
            int errno;
            try{
                errno = (int)pmemobjTxErrno.invoke();
            } catch(Throwable tt) {
                throw new RuntimeException(tt.getMessage());
            }
            throw new RuntimeException("Error code "+errno+" "+t.getMessage());
        }
    }

    static int endTransaction(){
        int ret = 0;
        try{
            ret = (int)pmemobjTxEnd.invokeExact();
        } catch(Throwable t) {
            throw new RuntimeException("error code is "+ret+" "+t.getMessage());
        }
        return ret;
    }

    static void abortTransaction(){
        try{
            pmemobjTxAbort.invokeExact(0);
        } catch(Throwable t) {
            int errno;
            try{
                errno = (int)pmemobjTxErrno.invoke();
            } catch(Throwable tt) {
                throw new RuntimeException(tt.getMessage());
            }
            throw new RuntimeException("Error code "+errno+" "+t.getMessage());
        }
    }
    
    static int addToTransaction(MemorySegment segment){
        int ret = 0;
        try (ResourceScope scope = ResourceScope.newConfinedScope()){
            scope.keepAlive(segment.scope());
            ret = (int)pmemobjTxAddRangeDirect.invokeExact((Addressable)segment, segment.byteSize());
        } catch(Throwable t) {
            throw new RuntimeException("Error code "+ret+" "+t.getMessage());
        }
        return ret;
    }

    private static native int nativeTxFlag ();
}
