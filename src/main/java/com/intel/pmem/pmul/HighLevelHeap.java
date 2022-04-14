/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.lang.invoke.MethodHandle;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static jdk.incubator.foreign.ValueLayout.*;

abstract class HighLevelHeap implements Heap {
    private static final ThreadLocal<Stack<MemorySegment>> inFlightSegments; 
    protected final LowLevelHeap lowLevelHeap;
    private Metadata metadata;
    private Heap.Kind kind;

    static {
        inFlightSegments = ThreadLocal.withInitial(() -> {
            var s = new Stack<MemorySegment>(); 
            s.push(null); 
            return s;
        });  
    }

    HighLevelHeap(Heap.Kind kind, LowLevelHeap lowLevelHeap, boolean create) {
        this.kind = kind;
        this.lowLevelHeap = lowLevelHeap;
        metadata = new Metadata(new Allocation(this, lowLevelHeap.metadata.getSegment()));
    }

    static class Metadata {
        static final MemoryLayout layout = MemoryLayout.structLayout(
            JAVA_INT.withName("kind"),
            JAVA_INT.withName("version"),
            ADDRESS.withName("user_root"),
            JAVA_LONG.withName("user_root_size")
        );
        static final Accessor KIND = Accessor.of(layout, PathElement.groupElement("kind"));
        static final Accessor USER_ROOT = Accessor.of(layout, PathElement.groupElement("user_root"));
        static final Accessor USER_ROOT_SIZE = Accessor.of(layout, PathElement.groupElement("user_root_size"));
        private final Allocation metadata;

        private Metadata(Allocation allocation) {
            metadata = allocation;
        }

        public Allocation getAllocation() {return metadata;}
        public Allocation getUserRoot(ResourceScope scope) {
            long size = (long)USER_ROOT_SIZE.get(metadata); 
            MemoryAddress address = (MemoryAddress)USER_ROOT.get(metadata); 
            if (size == 0 || address.equals(MemoryAddress.NULL)) return null;
            return (Allocation)metadata.heap().createAllocation(address, size, scope);
        }
        public void setKind(int kind) {KIND.set(metadata, kind);}
        public int getKind() {return (int)KIND.get(metadata);}
        public void setUserRoot(Allocation allocation) {
            USER_ROOT_SIZE.set(metadata, allocation == null ? 0 : allocation.byteSize());
            USER_ROOT.set(metadata, allocation == null ? MemoryAddress.NULL : allocation.heap().segmentAddress(allocation.segment()));
        }
    }

    public Kind getKind() {
        int value = metadata.getKind();
        Kind retVal;
        switch (value) {
            case 1:
                retVal = Kind.VOLATILE; 
                break;
            case 2:
                retVal = Kind.DURABLE;
                break;
            case 3:
                retVal = Kind.TRANSACTIONAL;
                break;
            default:
                throw new HeapException("Unsupported Heap Kind " + value);
        }
        return retVal;
    }

    @Override
    public void setRoot(Allocation allocation) {
        metadata.setUserRoot(allocation);
    }

    @Override
    public Allocation getRoot(ResourceScope scope) {
        return metadata.getUserRoot(scope);
    }

    @Override
    public long size() { 
        return lowLevelHeap.size();
    }

    void close() {
        lowLevelHeap.close();
    }

    // Methods from Heap interface 
	@Override public abstract Allocation allocate(long byteSize, ResourceScope scope);			
    @Override public abstract Allocation allocate(long byteSize, ResourceScope scope, Consumer<Allocation> initializer);
	@Override public abstract void free(AllocationAddress address);

    // Methods to service public static Accessor methods
    public abstract void copy(Allocation srcAllocation, Allocation dstAllocation);
    public abstract void copyToHeap(Allocation srcAllocation, long srcOffset, Allocation dstAllocation, long dstOffset, long length);
    public abstract void copyToHeap(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount);
    public abstract void execute(Runnable body);
    public abstract <T> T execute(Supplier<T> body);
    public abstract void execute(Allocation allocation, Runnable body);
    public abstract <T> T execute(Allocation allocation, Supplier<T> body);
 
    // Methods to service generated Accessor set method. Each each heap kind will execute 
    // appropriate preamble, if any, then the body supplied, then appropriate postamble, if any. 
    // Overloaded with number of long index parameters to improve performance by maintaining 
    // sharp types for write call stack.
    abstract void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment);
    abstract void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index);
    abstract void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index1, long index2);
    abstract void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, Object... args);
    abstract void setAtOffset(Consumer<MemorySegment> body, long elementSize, MemorySegment segment, long offset);

    public MemoryAddress segmentAddress(MemorySegment segment) { 
        return lowLevelHeap.transformAddress(segment.address());
    }

    MemoryAddress transformAddress(AllocationAddress address) { 
        return lowLevelHeap.transformAddress(address.address());
    }

    AllocationAddress reformAddress(MemoryAddress address) { 
        return new AllocationAddress(lowLevelHeap.reformAddress(address), this);
    }

    Allocation createAllocation(MemorySegment segment) {
        Allocation allocation = new Allocation(this, segment);
        return allocation;
    }

    Allocation createAllocation(MemoryAddress address, long byteSize, ResourceScope scope) {
        address = lowLevelHeap.reformAddress(address);
        MemorySegment segment = MemorySegment.ofAddress(address, byteSize, scope);
        return new Allocation(this, segment);
    }

    Allocation createAllocation(AllocationAddress allocationAddress, long byteSize, ResourceScope scope) {
        MemoryAddress address = allocationAddress.address();
        MemorySegment segment = MemorySegment.ofAddress(address, byteSize, scope);
        return new Allocation(this, segment);
    }

    // 4 methods used to track in-flight allocations in support of optimized initialization
    boolean segmentIsInFlight(MemorySegment segment) {
        return segmentContainsSlice(inFlightSegments.get().peek(), segment);
    }

    void beginInFlightSegment(MemorySegment segment) {
        inFlightSegments.get().push(segment);
    }

    void endInFlightSegment() {
        inFlightSegments.get().pop();
    }

    boolean segmentContainsSlice(MemorySegment segment, MemorySegment slice) {
        if (segment == null || slice == null) return false;
        long segmentAddr = segment.address().toRawLongValue(); 
        long sliceAddr = slice.address().toRawLongValue(); 
        return segmentAddr <= sliceAddr && segmentAddr + segment.byteSize() >= sliceAddr + slice.byteSize();
    }
}
