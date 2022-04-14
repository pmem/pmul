/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.util.function.Supplier;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;
import static jdk.incubator.foreign.ValueLayout.*;

/**
 * Instances of this class represent memory allocated from a high-level Heap.
 * An Allocation is similar to a {@code MemorySegment} in that it is value based and has a similar API.
 * An Allocaton is passed to Accessor methods to specify the memory to which an access
 * operation applies.    
 */
public class Allocation {
	private static long counter = 0;
	private final HighLevelHeap heap;
	private final MemorySegment segment;
    private final AllocationAddress address;

	/**
	 * Creates an Allocation object that wraps the supplied MemorySegment. No new
	 * memory is allocated.  Useful for copying from the Java heap to a Heap.
	 * @param segment the segment to which the Allocation will refer
	 * @return the allocation
	 */
	private static Allocation ofSegment(MemorySegment segment) {
		return new Allocation(null, segment);
	}

	/**
	 * Returns an Allocation object that refers to memory at the given {@code address}. No new
	 * memory is allocated. 
	 * @param address the AllocationAddress of the previously-allocated memory
	 * @param size the number of bytes of previously-allocated memory
	 * @param scope the resource scope to be associated with this allocation
	 * @return the allocation
	 */
	public static Allocation ofAddress(AllocationAddress address, long size, ResourceScope scope) {
		return address.heap().createAllocation(address, size, scope);
	}

	Allocation(Heap heap, MemorySegment segment) {
		this.heap = (HighLevelHeap)heap;
		this.segment = segment;
        this.address = heap == null ? null : new AllocationAddress(this);
	}

	MemorySegment segment() {
		return segment;
	}

	HighLevelHeap heap() {
        return heap;
	}

	/**
	 * Returns the AllocationAddress of this Allocation. This stable value can be stored and used later to regain 
	 * access to the memory.
	 * @return the address
	 */
    public AllocationAddress address() {
        return address;
    }

	/**
	 * Returns the resource scope associated with this allocation. 
	 * @return the resource scope
	 */
    public ResourceScope scope() {
        return segment.scope();
    }

	/**
	 * Copies all the bytes from the {@code srcAllocation} to this allocation. 
	 * @param srcAllocation the source allocation
	 */
    public void copyFrom(Allocation srcAllocation) {
        heap.copy(srcAllocation, this);
    }

	/**
	 * Copies all the bytes from the {@code srcSegment} to this allocation. 
	 * @param srcSegment the source segment
	 */
    public void copyFrom(MemorySegment srcSegment) {
        segment.copyFrom(srcSegment);
    }

	/**
	 * Copies {@code length} bytes from the {@code srcAllocation}, starting at {@code srcOffset}, to the destination
	 * allocation starting at {@code dstOffset}.
	 * @param srcAllocation the source allocation
	 * @param srcOffset the starting offset in the source allocation
	 * @param dstAllocation the destination allocation
	 * @param dstOffset the starting offset in the destination allocation
	 * @param length the number of bytes to copy
	 * @throws IllegalStateException if either allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds of either allocation
	 */
    public static void copy(Allocation srcAllocation, long srcOffset, Allocation dstAllocation, long dstOffset, long length) {
        dstAllocation.heap().copyToHeap(srcAllocation, srcOffset, dstAllocation, dstOffset, length);
    }

	/**
	 * Copies {@code length} bytes from the {@code srcAllocation}, starting at {@code srcOffset}, to the destination 
	 * segment starting at {@code dstOffset}.
	 * @param srcAllocation the source allocation
	 * @param srcOffset the starting offset in the source allocation
	 * @param dstSegment the destination segment
	 * @param dstOffset the starting offset in the destination segment
	 * @param length the number of bytes to copy
	 * @throws IllegalStateException if the allocation or segment is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds of the allocation or segment
	 * @throws UnsupportedOperationException if the destination segment is read only
	 */
    public static void copy(Allocation srcAllocation, long srcOffset, MemorySegment dstSegment, long dstOffset, long length) {
        MemorySegment.copy(srcAllocation.segment(), srcOffset, dstSegment, dstOffset, length);
    }

	/**
	 * Copies {@code length} bytes from the {@code srcSegment}, starting at {@code srcOffset}, to the destination
	 * allocation starting at {@code dstOffset}.
	 * @param srcSegment the source segment
	 * @param srcOffset the starting offset in the source segment
	 * @param dstAllocation the destination allocation
	 * @param dstOffset the starting offset in the destination allocation
	 * @param length the number of bytes to copy
	 * @throws IllegalStateException if the allocation or segment is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds of the allocation or segment
	 */
    public static void copy(MemorySegment srcSegment, long srcOffset, Allocation dstAllocation, long dstOffset, long length) {
        dstAllocation.heap().copyToHeap(ofSegment(srcSegment), srcOffset, dstAllocation, dstOffset, length);
    }

	/**
	 * Copies {@code elementCount} elements from the {@code srcAllocation}, starting at {@code srcOffset}, to the destination
	 * allocation starting at {@code dstOffset}.
	 * @param srcAllocation the source allocation
	 * @param srcElementLayout the element layout associated with the source allocation
	 * @param srcOffset the starting offset in the source allocation
	 * @param dstAllocation the destination allocation
	 * @param dstElementLayout the element layout associated with the destination allocation
	 * @param dstOffset the starting offset in the destination allocation
	 * @param elementCount the number of bytes to copy
	 * @throws IllegalStateException if either allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds of either allocation
	 */
    public static void copy(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        dstAllocation.heap().copyToHeap(srcAllocation, srcElementLayout, srcOffset, dstAllocation, dstElementLayout, dstOffset, elementCount);
    }

	/**
	 * Copies {@code elementCount} elements from the {@code srcAllocation}, starting at {@code srcOffset}, to the destination
	 * segment starting at {@code dstOffset}.
	 * @param srcAllocation the source allocation
	 * @param srcElementLayout the element layout associated with the source allocation
	 * @param srcOffset the starting offset in the source allocation
	 * @param dstSegment the destination segment
	 * @param dstElementLayout the element layout associated with the destination segment 
	 * @param dstOffset the starting offset in the destination segment
	 * @param elementCount the number of bytes to copy
	 * @throws IllegalStateException if the allocation or segment is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds the allocation or segment
	 * @throws UnsupportedOperationException if the destination segment is read only
	 */
    public static void copy(Allocation srcAllocation, ValueLayout srcElementLayout, long srcOffset, MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        MemorySegment.copy(srcAllocation.segment(), srcElementLayout, srcOffset, dstSegment, dstElementLayout, dstOffset, elementCount);
    }

	/**
	 * Copies {@code elementCount} elements from the {@code srcSegment}, starting at {@code srcOffset}, to the destination
	 * allocation starting at {@code dstOffset}.
	 * @param srcSegment the source segment 
	 * @param srcElementLayout the element layout associated with the source segment 
	 * @param srcOffset the starting offset in the source segment
	 * @param dstAllocation the destination allocation
	 * @param dstElementLayout the element layout associated with the destination allocation
	 * @param dstOffset the starting offset in the destination allocation
	 * @param elementCount the number of bytes to copy
	 * @throws IllegalStateException if the allocation or segment is not in a valid state for use
	 * @throws IndexOutOfBoundsException if copying would cause access of data outside the bounds of the allocation or segment
	 */
    public static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset, Allocation dstAllocation, ValueLayout dstElementLayout, long dstOffset, long elementCount) {
        dstAllocation.heap().copyToHeap(ofSegment(srcSegment), srcElementLayout, srcOffset, dstAllocation, dstElementLayout, dstOffset, elementCount);
    }

    @Override
	public String toString() {
		return heap.lowLevelHeap.path + " Allocation(" + segment + ")";
	}

	/**
     * Fills an Allocation with the supplied {@code value}
	 * @param value the byte value used to fill the allocation
	 */
    public void fill(byte value) {
        heap.execute(this, () -> { segment.fill(value); });
    }

    /**
     * Deallocates the memory represented by this allocation. The deallocation will be done 
     * transactionally for a {@code Heap} of {@code Kind} TRANSACTIONAL
     * @throws HeapException if the memory could not be deallocated
     */
    public void free() {
        heap().free(address());
    }

	/**
	 * Returns the size of the allocation.
	 * @return size in bytes of the allocation
	 */
	public long byteSize() {
		return segment.byteSize();
	}

	/**
	 * Returns an Allocation object as a view over part of this Allocation.
	 * No new memory is allocated.
	 * @param start the starting offset of the slice
	 * @param size the number of bytes in the slice
	 * @return the Allocation
	 */
	public Allocation asSlice(long start, long size) {
		return heap.createAllocation(segment.asSlice(start, size));
	}
	
    /**
     * Executes the supplied body using the data consistency behavior of the associated heap.
     * This method offers bulk optimization of data consistency operations, e.g., flushes for
     * durability and creation of undo buffers for transactions.  Access operations in the body
     * are bounds-checked to be withing the bounds of the supplied allocation.
     * @param body the code containing access operations
     */
    public void execute(Runnable body) {
        heap().execute(this, body);
    }

    /**
     * Executes the supplied body using the data consistency behavior of the associated heap.
     * This method offers bulk optimization of data consistency operations, e.g., flushes for
     * durability and creation of undo buffers for transactions.  Access operations in the body
     * are bounds-checked to be withing the bounds of the supplied allocation.
     * @param body the code containing access operations
     * @param <T> the return type of the supplier
     * @return the result of the body execution
     */
    public <T> T execute(Supplier<T> body) {
        return heap().execute(this, body);
    }

    /**
     * Retrieves a memory address at {@code offset} within this allocation.  
     * @param layout the value layout of a memory address
     * @param offset the location to be read
     * @return the memory address
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public MemoryAddress get(ValueLayout.OfAddress layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code boolean} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code boolean}
     * @param offset the location to be read
     * @return the boolean value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public boolean get(ValueLayout.OfBoolean layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code byte} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code byte}
     * @param offset the location to be read
     * @return the byte value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public byte get(ValueLayout.OfByte layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code char} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code char}
     * @param offset the location to be read
     * @return the char value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public char get(ValueLayout.OfChar layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code double} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code double}
     * @param offset the location to be read
     * @return the double value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public double get(ValueLayout.OfDouble layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code float} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code float}
     * @param offset the location to be read
     * @return the float value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public float get(ValueLayout.OfFloat layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves an {@code int} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code int}
     * @param offset the location to be read
     * @return the int value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public int get(ValueLayout.OfInt layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code long} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code long}
     * @param offset the location to be read
     * @return the long value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public long get(ValueLayout.OfLong layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a {@code short} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code short}
     * @param offset the location to be read
     * @return the short value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public short get(ValueLayout.OfShort layout, long offset) {
        return segment.get(layout, offset);
    }

    /**
     * Retrieves a memory address at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a memory address
     * @param index the index
     * @return the memory address
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public MemoryAddress getAtIndex(ValueLayout.OfAddress layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves a char at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code char} 
     * @param index the index
     * @return the char value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public char getAtIndex(ValueLayout.OfChar layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves a double at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code double} 
     * @param index the index
     * @return the double value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public double getAtIndex(ValueLayout.OfDouble layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves a float at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code float} 
     * @param index the index
     * @return the float value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public float getAtIndex(ValueLayout.OfFloat layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves an int at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code int} 
     * @param index the index
     * @return the int value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public int getAtIndex(ValueLayout.OfInt layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves a long at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code long} 
     * @param index the index
     * @return the long value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public long getAtIndex(ValueLayout.OfLong layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves a short at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code short} 
     * @param index the index
     * @return the short value
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public short getAtIndex(ValueLayout.OfShort layout, long index) {
        return segment.getAtIndex(layout, index);
    }

    /**
     * Retrieves an allocation address at {@code offset} within this allocation.  
     * @param offset the location to be read 
     * @return the allocation address
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the layout
     */
    public AllocationAddress getReference(long offset) {
        return heap.reformAddress(segment.get(ADDRESS, offset));
    }

    /**
     * Retrieves an allocation address at {@code index}, scaled by the size of {@code ADDRESS}, within this allocation.  
     * @param index the index 
     * @return the allocation address
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the layout
     */
    public AllocationAddress getReferenceAtIndex(long index) {
        return heap.reformAddress(segment.getAtIndex(ADDRESS, index));
    }

    /**
     * Stores the supplied {@code Addressable} value at {@code offset} within this allocation.  
     * @param layout the value layout of a memory address
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfAddress layout, long offset, Addressable value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code boolean} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code boolean} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfBoolean layout, long offset, boolean value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code byte} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code byte} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfByte layout, long offset, byte value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code char} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code char} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfChar layout, long offset, char value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code double} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code double} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfDouble layout, long offset, double value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code float} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code float} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfFloat layout, long offset, float value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code int} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code int} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfInt layout, long offset, int value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code long} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code long} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfLong layout, long offset, long value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied {@code short} value at {@code offset} within this allocation.  
     * @param layout the value layout of a {@code short} 
     * @param offset the location at which to store the value 
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void set(ValueLayout.OfShort layout, long offset, short value) {
		heap.setAtOffset((s) -> segment.set(layout, offset, value), layout.byteSize(), segment, offset);
	}

    /**
     * Stores the supplied memory address at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a memory address
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfAddress layout, long index, Addressable value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code char} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code char} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfChar layout, long index, char value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code double} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code double} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfDouble layout, long index, double value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code float} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code float} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfFloat layout, long index, float value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code int} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code int} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfInt layout, long index, int value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code long} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code long} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfLong layout, long index, long value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied {@code short} value at {@code index}, scaled by the {@code layout} size, within this allocation.  
     * @param layout the value layout of a {@code short} 
     * @param index the index
     * @param value the value to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
	public void setAtIndex(ValueLayout.OfShort layout, long index, short value) {
		heap.setAtOffset((s) -> segment.setAtIndex(layout, index, value), layout.byteSize(), segment, index * layout.byteSize());
	}

    /**
     * Stores the supplied allocation at {@code offset} within this allocation.  
     * @param offset the location at which to store the allocation
     * @param reference the allocation to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public void setReference(long offset, Allocation reference) {
		heap.setAtOffset((s) -> segment.set(ADDRESS, offset, heap.segmentAddress(reference.segment())), ADDRESS.byteSize(), segment, offset);
    }

    /**
     * Stores the supplied allocation address at {@code offset} within this allocation.  
     * @param offset the location at which to store the allocation address
     * @param reference the allocation address to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public void setReference(long offset, AllocationAddress reference) {
		heap.setAtOffset((s) -> segment.set(ADDRESS, offset, heap.transformAddress(reference)), ADDRESS.byteSize(), segment, offset);
    }

    /**
     * Stores the supplied allocation at {@code index}, scaled by the size of {@code ADDRESS}, within this allocation.  
     * @param index the location at which to store the allocation
     * @param reference the allocation to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public void setReferenceAtIndex(long index, Allocation reference) {
		heap.setAtOffset((s) -> segment.setAtIndex(ADDRESS, index, heap.segmentAddress(reference.segment())), ADDRESS.byteSize(), segment, index * ADDRESS.byteSize());
    }

    /**
     * Stores the supplied allocation address at {@code index}, scaled by the size of {@code ADDRESS}, within this allocation.  
     * @param index the location at which to store the allocation address
     * @param reference the allocation address to store
	 * @throws IllegalStateException if the allocation is not in a valid state for use
	 * @throws IndexOutOfBoundsException if this operation would cause access of data outside the bounds of the allocation
	 * @throws IllegalArgumentException if the dereference operation is incompatible with the alignment constraints in the {@code layout}
     */
    public void setReferenceAtIndex(long index, AllocationAddress reference) {
		heap.setAtOffset((s) -> segment.setAtIndex(ADDRESS, index, heap.transformAddress(reference)), ADDRESS.byteSize(), segment, index * ADDRESS.byteSize());
    }

    /**
     * Copies all the bytes in this allocation to a new byte array.
     * @param elementLayout the value layout of a {@code byte}
     * @return the new byte array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public byte[] toArray(ValueLayout.OfByte elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new char array.
     * @param elementLayout the value layout of a {@code char}
     * @return the new char array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public char[] toArray(ValueLayout.OfChar elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new double array.
     * @param elementLayout the value layout of a {@code double}
     * @return the new double array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public double[] toArray(ValueLayout.OfDouble elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new float array.
     * @param elementLayout the value layout of a {@code float}
     * @return the new float array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public float[] toArray(ValueLayout.OfFloat elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new int array.
     * @param elementLayout the value layout of a {@code int}
     * @return the new int array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public int[] toArray(ValueLayout.OfInt elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new long array.
     * @param elementLayout the value layout of a {@code long} 
     * @return the new long array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public long[] toArray(ValueLayout.OfLong elementLayout) {
        return segment.toArray(elementLayout);
    }

    /**
     * Copies all the bytes in this allocation to a new short array.
     * @param elementLayout the value layout of a {@code short} 
     * @return the new short array
	 * @throws IllegalStateException if the allocation is not in a valid state for use
     */
    public short[] toArray(ValueLayout.OfShort elementLayout) {
        return segment.toArray(elementLayout);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Allocation)) return false;
        else return segment.equals(((Allocation)obj).segment);
    }

    @Override
    public int hashCode() {
        return segment.hashCode();
    }
}
