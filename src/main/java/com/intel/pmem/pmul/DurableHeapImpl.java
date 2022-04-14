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

class DurableHeapImpl extends HighLevelHeap implements Heap {

    public DurableHeapImpl(Path path, long size) throws IOException {
        super(Heap.Kind.DURABLE, PersistentHeap.create(path, size, Heap.Kind.DURABLE), true); 
    }

    public DurableHeapImpl(Path path) throws IOException {
        super(Heap.Kind.DURABLE, PersistentHeap.create(path, Heap.Kind.DURABLE), true); 
    }

    public DurableHeapImpl(LowLevelHeap lowLevelHeap) throws IOException {
        super(Heap.Kind.DURABLE, lowLevelHeap, false); 
    }

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope) {
        return super.createAllocation(lowLevelHeap.allocateSegment(byteSize, false, scope));
    }

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        return initializeAllocation(byteSize, scope, initializer);
    }

    private Allocation initializeAllocation(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        Allocation allocation = allocate(byteSize, scope);
        initializeAllocation(allocation, initializer);
        lowLevelHeap.flush(allocation.segment(), 0, allocation.segment().byteSize());
        return allocation;
    }

    private Allocation initializeAllocation(Allocation allocation, Consumer<Allocation> initializer) {
        beginInFlightSegment(allocation.segment());
        initializer.accept(allocation);
        endInFlightSegment();
        return allocation;
    }

    @Override
    public void free(AllocationAddress address) {
        lowLevelHeap.freeSegment(address.address(), false);
    }

    @Override
    public void execute(Runnable body) {
        body.run();
    }        

    @Override
    public <T> T execute(Supplier<T> body) {
        T ans = null;
        ans = body.get();
        return ans;
    }        

   @Override
    public void execute(Allocation allocation, Runnable body) {
        beginInFlightSegment(allocation.segment());
        body.run();
        endInFlightSegment();
        lowLevelHeap.flush(allocation.segment(), 0, allocation.segment().byteSize());
    }

    @Override
    public <T> T execute(Allocation allocation, Supplier<T> body) {
        T ans = body.get();
        lowLevelHeap.flush(allocation.segment(), 0, allocation.segment().byteSize());
        return ans;
    }

    @Override
    public void copy(Allocation srcAllocation, Allocation dstAllocation) {
        dstAllocation.segment().copyFrom(srcAllocation.segment());
        if (!segmentIsInFlight(dstAllocation.segment())) lowLevelHeap.flush(dstAllocation.segment());
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, long srcOffset, Allocation dstAllocation, long dstOffset, long length) {
        MemorySegment.copy(srcAllocation.segment(), srcOffset, dstAllocation.segment(), dstOffset, length);
        if (!segmentIsInFlight(dstAllocation.segment())) lowLevelHeap.flush(dstAllocation.segment(), dstOffset, length);
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        MemorySegment.copy(srcAllocation.segment(), srcElementLayout, srcOffset, dstAllocation.segment(), dstElementLayout, dstOffset, elementCount);
        if (!segmentIsInFlight(dstAllocation.segment())) lowLevelHeap.flush(dstAllocation.segment(), dstOffset, dstElementLayout.byteSize() * elementCount);
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment) {
        try {
            body.accept(segment);
            if (!segmentIsInFlight(segment)) lowLevelHeap.flush(segment, (long)byteOffsetHandle.invokeExact(), elementSize);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index) {
        try {
            body.accept(segment);
            if (!segmentIsInFlight(segment)) lowLevelHeap.flush(segment, (long)byteOffsetHandle.invokeExact(index), elementSize);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index1, long index2) {
        try {
            body.accept(segment);
            if (!segmentIsInFlight(segment)) lowLevelHeap.flush(segment, (long)byteOffsetHandle.invokeExact(index1, index2), elementSize);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, Object... args) {
        try {
            MemorySegment segment= ((Allocation)args[0]).segment(); 
            body.accept(segment);
            if (!segmentIsInFlight(segment)) lowLevelHeap.flush(segment, (long)byteOffsetHandle.invokeWithArguments(Arrays.copyOfRange(args, 1, args.length - 1)), elementSize);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    void setAtOffset(Consumer<MemorySegment> body, long elementSize, MemorySegment segment, long offset) {
        try {
            body.accept(segment);
            if (!segmentIsInFlight(segment)) lowLevelHeap.flush(segment, offset, elementSize);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
