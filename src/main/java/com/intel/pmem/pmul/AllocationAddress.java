/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import jdk.incubator.foreign.MemoryAddress;

/**
 * An allocation address models a reference to a memory location.
 * An allocaton address is obtained by calling {@link com.intel.pmem.pmul.Allocation#address}
 */
public class AllocationAddress {
	private final MemoryAddress address;
    private final HighLevelHeap heap;

	AllocationAddress(Allocation allocation) {
		this.address = allocation.segment().address();
        this.heap = allocation.heap();
	}

	AllocationAddress(MemoryAddress address, HighLevelHeap heap) {
		this.address = address;
        this.heap = heap;
	}

    MemoryAddress address() {
        return address;
    }
    
    HighLevelHeap heap() {
        return heap;
    }

    @Override
    /**
     *{@inheritdoc}
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof AllocationAddress)) return false;
        return address.equals(((AllocationAddress)obj).address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return heap.lowLevelHeap.path + " AllocationAddress( " + address + ")";
    }
}
