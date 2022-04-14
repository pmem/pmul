/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.lang.invoke.WrongMethodTypeException;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

/**
 * An accessor for a Heap-resident element of a {@code java.foreign.MemoryLayout}.
 * Accessor methods (currently set, get) behave like memory access var handle plain read and write methods.
 * Accessor methods offer context-sensitive execution, where the write semantics are customized according to the specified heap.
 */

public interface Accessor {
    /**
     * Creates an accessor for dereferencing memory at a layout selected by the provided layout path, 
     * where the path is rooted in the given layout.
     * @param layout the root layout
     * @param elements the layout path elements
     * @return the accessor
     */
    public static Accessor of(MemoryLayout layout, PathElement... elements) {
        return Generator.accessorOf(layout, elements);
    }

    /**
     * Executes the supplied body using the data consistency behavior of the supplied heap.
     * @param heap the heap in which the access will take place
     * @param body the code containing access operations
     */
    public static void execute(Heap heap, Runnable body) {
        ((HighLevelHeap)heap).execute(body);
    }        

    /**
     * Executes the supplied body using the data consistency behavior of the supplied heap.
     * @param heap the heap in which the access will take place
     * @param body the code containing access operations
     * @param <T> the return type of the supplier
     * @return the result of the body execution
     */
    public static <T> T execute(Heap heap, Supplier<T> body) {
        return ((HighLevelHeap)heap).execute(body);
    }        

    /**
     * Stores the supplied byte value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, byte value);

    /**
     * Stores the supplied byte value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, byte value);

    /**
     * Stores the supplied byte value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, byte value);

    /**
     * Stores the supplied boolean value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, boolean value);

    /**
     * Stores the supplied boolean value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, boolean value);

    /**
     * Stores the supplied boolean value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, boolean value);

    /**
     * Stores the supplied short value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, short value);

    /**
     * Stores the supplied short value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, short value);

    /**
     * Stores the supplied short value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, short value);

    /**
     * Stores the supplied int value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, int value);

    /**
     * Stores the supplied int value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, int value);

    /**
     * Stores the supplied int value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, int value);

    /**
     * Stores the supplied long value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long value);

    /**
     * Stores the supplied long value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, long value);

    /**
     * Stores the supplied long value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, long value);

    /**
     * Stores the supplied float value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, float value);

    /**
     * Stores the supplied float value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, float value);

    /**
     * Stores the supplied float value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, float value);

    /**
     * Stores the supplied double value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, double value);

    /**
     * Stores the supplied double value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, double value);

    /**
     * Stores the supplied double value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, double value);

    /**
     * Stores the supplied char value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, char value);

    /**
     * Stores the supplied char value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, char value);

    /**
     * Stores the supplied char value at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, char value);

    /**
     * Stores the supplied address at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, MemoryAddress value);

    /**
     * Stores the supplied address at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index, MemoryAddress value);

    /**
     * Stores the supplied address at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void set(Allocation allocation, long index1, long index2, MemoryAddress value);

    /**
     * Stores the supplied allocation address at the target layout within the supplied allocation.  
     * A transalation from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param allocation the allocation whose memory is to be dereferenced
     * @param value the allocation address to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void setReference(Allocation allocation, AllocationAddress value);

    /**
     * Stores the supplied allocation address at the target layout within the supplied allocation.  
     * A transalation from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @param value the allocation address to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void setReference(Allocation allocation, long index, AllocationAddress value);

    /**
     * Stores the supplied allocation address at the target layout within the supplied allocation.  
     * A transalation from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @param value the allocation address to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public void setReference(Allocation allocation, long index1, long index2, AllocationAddress value);

    /**
     * Stores the supplied value at the target layout within the supplied allocation.  
     * @param args a list of access coordinates followed by the value to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
	 * @throws ClassCastException if this operation would cause access of data outside the bounds of the allocation
     */
    public void set(Object... args);

    /**
     * Stores the supplied allocation address at the target layout within the supplied {@code allocation}.  
     * A transalation from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param args a list of access coordinates followed by the allocation address to store
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
	 * @throws ClassCastException if this operation would cause access of data outside the bounds of the allocation
     */
    public void setReference(Object... args);

    /**
     * Retrieves the value stored at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @return the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public Object get(Allocation allocation);

    /**
     * Retrieves the value stored at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @return the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public Object get(Allocation allocation, long index);

    /**
     * Retrieves the value stored at the target layout within the supplied allocation.  
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @return the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public Object get(Allocation allocation, long index1, long index2);

    /**
     * Retrieves the value stored at the target layout within the supplied allocation.  
     * @param args a list of access coordinates used to navigate to the target layout
     * @return the value
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
	 */
    public Object get(Object... args);

    /**
     * Retrieves the address stored at the target layout within the supplied allocation.  
     * A translation from relocatable address to absolute address will be done to enable use of the previously 
     * stored reference, in this process.
     * @param allocation the allocation whose memory is to be dereferenced
     * @return the address
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public AllocationAddress getReference(Allocation allocation);

    /**
     * Retrieves the address stored at the target layout within the supplied allocation.  
     * A translation from relocatable address to absolute address will be done to enable use of the previously 
     * stored reference, in this process.
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index an access coordinate used to navigate to the target layout
     * @return the address
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public AllocationAddress getReference(Allocation allocation, long index);

    /**
     * Retrieves the adress stored at the target layout within the supplied allocation.  
     * A translation from relocatable address to absolute address will be done to enable use of the previously 
     * stored reference, in this process.
     * @param allocation the allocation whose memory is to be dereferenced
     * @param index1 an access coordinate used to navigate to the target layout
     * @param index2 an access coordinate used to navigate to the target layout
     * @return the address
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public AllocationAddress getReference(Allocation allocation, long index1, long index2);

    /**
     * Retrieves the value stored at the target layout within the supplied allocation.  
     * A translation from relocatable address to absolute address will be done to enable use of the previously 
     * stored reference, in this process.
     * @param args a list of access coordinates used to navigate to the target layout
     * @return the address
	 * @throws WrongMethodTypeException if the allocation is not in a valid state for use
     */
    public AllocationAddress getReference(Object... args);
}
