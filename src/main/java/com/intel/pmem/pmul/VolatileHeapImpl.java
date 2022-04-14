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
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;

class VolatileHeapImpl extends HighLevelHeap implements Heap {
	private Allocation rootAllocation;

	public VolatileHeapImpl(Path path, long size) throws IOException {
        super(Heap.Kind.VOLATILE, VolatileHeap.create(path, HighLevelHeap.Kind.VOLATILE, size), true);
	}

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope) {
        return super.createAllocation(lowLevelHeap.allocateSegment(byteSize, false, scope)); 
    }

    @Override
    public Allocation allocate(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        return initializeAllocation(byteSize, scope, initializer);
    }

    protected Allocation initializeAllocation(long byteSize, ResourceScope scope, Consumer<Allocation> initializer) {
        Allocation allocation = allocate(byteSize, scope);
        initializer.accept(allocation);
        return allocation;
    }

    @Override
    public void free(AllocationAddress address) {
        lowLevelHeap.freeSegment(address.address(), false);
    }
    
    @Override
    public void setRoot(Allocation allocation) {
        rootAllocation = allocation;
    }

    @Override
    public Allocation getRoot(ResourceScope scope) {
        return rootAllocation;
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
        body.run();
    }

    @Override
    public <T> T execute(Allocation allocation, Supplier<T> body) {
        T ans = body.get();
        return ans;
    }

    @Override
    public void copy(Allocation srcAllocation, Allocation dstAllocation) {
        dstAllocation.segment().copyFrom(srcAllocation.segment());
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, long srcOffset, Allocation dstAllocation, long dstOffset, long length) {
        MemorySegment.copy(srcAllocation.segment(), srcOffset, dstAllocation.segment(), dstOffset, length);
    }

    @Override
    public void copyToHeap(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        MemorySegment.copy(srcAllocation.segment(), srcElementLayout, srcOffset, dstAllocation.segment(), dstElementLayout, dstOffset, elementCount);
    }

    @Override
    public void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment) {
        try {
            body.accept(segment);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index) {
        try {
            body.accept(segment);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, MemorySegment segment, long index1, long index2) {
        try {
            body.accept(segment);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void set(Consumer<MemorySegment> body, MethodHandle byteOffsetHandle, long elementSize, Object... args) {
        try {
            body.accept(((Allocation)args[0]).segment());
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
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
