/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul.examples.intarray;

import com.intel.pmem.pmul.Accessor;
import com.intel.pmem.pmul.Allocation;
import com.intel.pmem.pmul.AllocationAddress;
import com.intel.pmem.pmul.Heap;
import java.util.Iterator;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;

public class IntArray {
    private static final int HEADER_SIZE = 8;
        Allocation arrayAllocation;
        Allocation headerAllocation;
        static final MemoryLayout headerLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("size"), 
            ValueLayout.ADDRESS.withName("elementsSegment"));
        MemoryLayout arrayLayout;
        Accessor elementsAccessor;
        Accessor headerSizeAccessor;
        Accessor headerElementsSegmentAccessor;
        ResourceScope arrayScope;
        Heap heap;

    // Reconstructor
    public IntArray(Heap heap, Allocation headerAllocation) {
        this.heap = heap;
        this.headerAllocation = headerAllocation;
        arrayScope = ResourceScope.newSharedScope();
        headerSizeAccessor = Accessor.of(headerLayout, PathElement.groupElement("size"));
        long size = (long)headerSizeAccessor.get(headerAllocation);
        arrayLayout = MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_INT);
        headerElementsSegmentAccessor = Accessor.of(headerLayout, PathElement.groupElement("elementsSegment"));
        AllocationAddress arrayAllocationAddress = headerElementsSegmentAccessor.getReference(headerAllocation);
        arrayAllocation = Allocation.ofAddress(arrayAllocationAddress, arrayLayout.byteSize(), ResourceScope.newSharedScope());
        assert arrayAllocation != null;
        elementsAccessor = Accessor.of(arrayLayout, PathElement.sequenceElement());
        arrayScope.addCloseAction(()-> {
            heap.free(arrayAllocation.address());
            heap.free(headerAllocation.address());
        });
    }

    public IntArray(Heap heap, long size) {
        this.heap = heap;
        ResourceScope arrayScope = ResourceScope.newImplicitScope();
        arrayLayout = MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_INT);

        elementsAccessor = Accessor.of(arrayLayout, PathElement.sequenceElement());
        headerSizeAccessor = Accessor.of(headerLayout, PathElement.groupElement("size"));
        headerElementsSegmentAccessor = Accessor.of(headerLayout, PathElement.groupElement("elementsSegment"));

        arrayAllocation = heap.allocate(arrayLayout, arrayScope);
        headerAllocation = heap.allocate(headerLayout, arrayScope, (allocation) -> {
            headerSizeAccessor.set(allocation, size);
            headerElementsSegmentAccessor.setReference(allocation, arrayAllocation.address());
        });
        arrayScope.addCloseAction(()-> {
            heap.free(arrayAllocation.address());
            heap.free(headerAllocation.address());
        });
    }

    public void set(long index, int value) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        elementsAccessor.set(arrayAllocation, index, value);
    }

    public int get(long index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (int)elementsAccessor.get(arrayAllocation, index);
    }

    public long size() {
        return (long)headerSizeAccessor.get(headerAllocation);
    }

    public Allocation getAllocation() {
        return headerAllocation;
    }

    public void free() {
        arrayScope.close();
    }
}
