/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;

class TransactionalHeapImpl extends HighLevelHeap implements Heap {

    public TransactionalHeapImpl(Path path, long size) throws IOException {
        super(Heap.Kind.TRANSACTIONAL, PersistentHeap.create(path, size, Heap.Kind.TRANSACTIONAL), true);
    }

    public TransactionalHeapImpl(Path path) throws IOException {
        super(Heap.Kind.TRANSACTIONAL, PersistentHeap.create(path, Heap.Kind.TRANSACTIONAL), true);
    }

    public TransactionalHeapImpl(LowLevelHeap lowLevelHeap) throws IOException {
        super(Heap.Kind.TRANSACTIONAL, lowLevelHeap, false);
    }

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope) {
        return super.createAllocation(lowLevelHeap.allocateSegment(byteSize, true, scope));
    }

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        return initializeAllocation(byteSize, scope, initializer);
    }

    private Allocation initializeAllocation(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        return Transaction.run(lowLevelHeap, () -> {
            return initializeAllocation(allocate(byteSize, scope), initializer);
        });
    }

    private Allocation initializeAllocation(Allocation allocation, Consumer<Allocation> initializer) {
        beginInFlightSegment(allocation.segment());
        initializer.accept(allocation);
        endInFlightSegment();
        return allocation;
    }
	
    @Override
    public void free(AllocationAddress address) {
        lowLevelHeap.freeSegment(address.address(), true);
    }
    
    @Override
    public void execute(Runnable body) {
        Transaction.run(lowLevelHeap, () -> {
            body.run();
        });
    }        

    @Override
    public <T> T execute(Supplier<T> body) {
        T ans = null;
        ans = Transaction.run(lowLevelHeap, () -> {
            return body.get();
        });
        return ans;
    }        

    @Override
    public void execute(Allocation allocation, Runnable body) {
        Transaction.run(lowLevelHeap, () -> {
            Transaction.addToTransaction(allocation.segment(), 0, allocation.segment().byteSize());
            beginInFlightSegment(allocation.segment());
            body.run();
            endInFlightSegment();
        });
    }

    @Override
    public <T> T execute(Allocation allocation, Supplier<T> body) {
        return Transaction.run(lowLevelHeap, () -> {
            Transaction.addToTransaction(allocation.segment(), 0, allocation.segment().byteSize());
            T ans = body.get();
            return ans;
        });
    }

    @Override
    public void copy(Allocation srcAllocation, Allocation dstAllocation) {
        Transaction.run(lowLevelHeap, () -> {
            MemorySegment dstSegment = dstAllocation.segment();
            if (!segmentIsInFlight(dstSegment)) Transaction.addToTransaction(dstSegment, 0, dstSegment.byteSize());
            dstSegment.copyFrom(srcAllocation.segment());
        });
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, long srcOffset, Allocation dstAllocation, long dstOffset, long length) {
        Transaction.run(lowLevelHeap, () -> {
            MemorySegment dstSegment = dstAllocation.segment();
            if (!segmentIsInFlight(dstSegment)) Transaction.addToTransaction(dstSegment, dstOffset, length);
            MemorySegment.copy(srcAllocation.segment(), srcOffset, dstSegment, dstOffset, length);
        });
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        Transaction.run(lowLevelHeap, () -> {
            MemorySegment dstSegment = dstAllocation.segment();
            if (!segmentIsInFlight(dstSegment)) Transaction.addToTransaction(dstSegment, dstOffset, dstElementLayout.byteSize() * elementCount);
            MemorySegment.copy(srcAllocation.segment(), srcElementLayout, srcOffset, dstSegment, dstElementLayout, dstOffset, elementCount);
        });
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment) {
        long byteOffset;
        try {
            byteOffset = (long)byteOffsetHandle.invokeExact();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        Transaction.run(lowLevelHeap, () -> {
            if (!segmentIsInFlight(segment)) Transaction.addToTransaction(segment, byteOffset, elementSize);
            body.accept(segment);
        });
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index) {
        long byteOffset;
        try {
            byteOffset = (long)byteOffsetHandle.invokeExact(index);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        Transaction.run(lowLevelHeap, () -> {
            if (!segmentIsInFlight(segment)) Transaction.addToTransaction(segment, byteOffset, elementSize);
            body.accept(segment);
        });
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index1, long index2) {
        long byteOffset;
        try {
            byteOffset = (long)byteOffsetHandle.invokeExact(index1, index2);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        Transaction.run(lowLevelHeap, () -> {
            if (!segmentIsInFlight(segment)) Transaction.addToTransaction(segment, byteOffset, elementSize);
            body.accept(segment);
        });
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, Object... args) {
        long byteOffset;
        try {
            byteOffset = (long)byteOffsetHandle.invokeWithArguments(Arrays.copyOfRange(args, 1, args.length - 1));
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        MemorySegment segment = ((Allocation)args[0]).segment();
        Transaction.run(lowLevelHeap, () -> {
            if (!segmentIsInFlight(segment)) Transaction.addToTransaction(segment, byteOffset, elementSize);
            body.accept(segment);
        });
    }

    @Override
    void setAtOffset(Consumer<MemorySegment> body, long elementSize, MemorySegment segment, long offset) {
        Transaction.run(lowLevelHeap, () -> {
            if (!segmentIsInFlight(segment)) Transaction.addToTransaction(segment, offset, elementSize);
            body.accept(segment);
        });
    }
}
