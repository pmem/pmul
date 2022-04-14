/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

/**
 * A high-level heap for which the data consistency of writes are specialized 
 * by a Heap.Kind supplied at heap creation. Supported data consistency choices
 * include Volatile, Durable, or Transactional. <br>
 *
 * Heap create factory methods accept a path argument that specifies
 * the identitiy of the heap and an optional size argument. There are 4 ways to 
 * configure the size of a heap:<br><br>
 * 
 * 1. fixed size -- the path argument is a file path and a supplied size arugument sets both the minimum and 
 * maximum size of the heap.<br>
 * 2. growable -- the path argument is a file path and the heap size starts with size returned by
 * {@code getMinimumHeapSize()}, growing in size as needed up to the available memory.<br>
 * 3. growable with limit -- the path argument is a file path and the heap size starts with the minimum heap size,
 * growing up to the supplied size argument.<br>
 * 4. DAX device -- the path argument is a DAX device path and the size of the heap will match the size of the 
 * dax device.<br><br>
 * 
 * A previously created heap can be re-opened after a restart using the {@code open()} method which accepts 
 * the path argument that was used when the heap was created.<br><br>
 *
 */        
public interface Heap {
    /**
     * Heap.Kind values describe heap data consistency policies. <br>  
     * <b>Volatile</b>: a heap for which no access is required after a process exits. <br>
     * <b>Durable</b>: A heap which provides durable data as long as write operations
     * are not interrupted.  Under these conditions, the heap can be 
     * reopened after a process exit.<br>
     * <b>Transactional</b>: A heap which provides durable writes even if write operations
     * are interrupted, e.g., by a power failure. Such fail-safe writes
     * apply at a single set operation, bulk writes in a copy operation, 
     * or user supplied bodies in an Accessor::execute operation. The 
     * heap can be reopened after a controlled or uncontrolled process exit.
     * */    
    public static final class Kind implements HeapKind {
        /**
         * Describes a heap for which no access is required after a process exits.
         */
        public static final Kind VOLATILE = new Kind(1);
        /**
         * Describes a heap which provides durable data as long as write operations
         * are not interrupted.  Under these conditions, the heap can be 
         * reopened after a process exit.
         */
        public static final Kind DURABLE = new Kind(2);
        /**
         * Describes a heap which provides durable writes even if write operations
         * are interrupted, e.g., by a power failure. Such fail-safe writes
         * apply at a single set operation, bulk writes in a copy operation, 
         * or user supplied bodies in an Accessor::execute operation. The 
         * heap can be reopened after a controlled or uncontrolled process exit.
         */
        public static final Kind TRANSACTIONAL = new Kind(3);
        int val;
        private Kind(int value) {
            this.val = value;
        }
        public int value() {return val;}
        /**
         * Returns an array of available Kinds
         * @return an array of available Kinds 
         */
        public static Kind[] values() { return new Kind[]{VOLATILE, DURABLE, TRANSACTIONAL}; }

    }

    /**
     * Creates a new heap of given {@code Kind}. If {@code path} refers to a file, a fixed-size
     * heap of {@code size} bytes will be created. If {@code path} refers to an existing directory, 
     * a growable heap, limited to {@code size} bytes, will be created.
     * @param kind the desired heap kind
     * @param path the path to the heap
     * @param size the number of bytes to allocate for the heap
     * @return the heap at the specified pah
     * @throws IllegalArgumentException if an invalid {@code Kind} is specified 
     * @throws HeapException if the heap could not be created
     */
    public static Heap create(Kind kind, Path path, long size) throws IOException {
        if (kind.value() == Kind.VOLATILE.value())
            return new VolatileHeapImpl(path, size);
        if (kind.value() == Kind.DURABLE.value())
            return new DurableHeapImpl(path, size);
        if (kind.value() == Kind.TRANSACTIONAL.value())
            return new TransactionalHeapImpl(path, size);
        else
            throw new IllegalArgumentException("Invalid heap kind specified");

    }

    /**
     * Creates a new heap of given {@code Kind}. If {@code path} refers to an existing directory, a
     * growable heap will be created. If {@code path} refers to a DAX device, a heap that spans the 
     * entire device will be created.
     * @param kind the desired heap kind
     * @param path the path to the heap
     * @return the heap at the specified path
     * @throws IllegalArgumentException if an invalid {@code Kind} is specified 
     * @throws HeapException if the heap could not be created
     */
    public static Heap create(Kind kind, Path path) throws IOException {
        if (kind.value() == Kind.VOLATILE.value())
            throw new HeapException("Volatile heap construction is not supported with no size");
        if (kind.value() == Kind.DURABLE.value())
            return new DurableHeapImpl(path);
        if (kind.value() == Kind.TRANSACTIONAL.value())
            return new TransactionalHeapImpl(path);
        else
            throw new IllegalArgumentException("Invalid heap kind specified");
    }

    /**
     * Opens an existing heap. Provides access to the heap associated with the specified {@code path}.
     * @param path the path to the heap
     * @return the heap at the specified path
     * @throws IllegalArgumentException if an invalid {@code Kind} is specified 
     * @throws HeapException if the heap could not be opened
     */
    public static Heap open(Path path) throws IOException {
        LowLevelHeap lowLevelHeap = PersistentHeap.open(path, LowLevelHeap.Kind.NOKIND);
        int kind = lowLevelHeap.metadata.getKind();
        if (kind == Kind.DURABLE.value()) {
            return new DurableHeapImpl(lowLevelHeap); }
        if (kind == Kind.TRANSACTIONAL.value())
            return new TransactionalHeapImpl(lowLevelHeap);
        else
            throw new IllegalArgumentException("Invalid heap kind specified");
    }

    /**
     * Creates an allocation that represents a block of persistent memory of {@code byteSize} bytes. 
     * For a transactional heap, allocation will be done transactionally
     * @param byteSize the number of bytes to allocate
     * @param scope a {@code ResourceScope} to which this allocation will be associated with
     * @return an {@code Allocation} that represents the allocated memory
     * @throws OutOfMemoryError if the memory could not be allocated
     */
    public Allocation allocate(long byteSize, ResourceScope scope);

    /**
     * Creates an allocation that represents a block of persistent memory of {@code byteSize} bytes. 
     * For a transactional heap, allocation will be done transactionally
     * The supplied {@code initializer} function is executed on  
     * the new {@code Allocation}. Allocating with an initializer can be more efficient than separate
     * allocation and initialization.
     * @param byteSize the number of bytes to allocate
     * @param scope a {@code ResourceScope} to which this allocation will be associated with
     * @param initializer a function to be executed on the new allocation
     * @return an {@code Allocation} that represents the allocated memory
     * @throws OutOfMemoryError if the memory could not be allocated
     */
    public Allocation allocate(long byteSize, ResourceScope scope, Consumer<Allocation> initializer);         

    /**
     * Deallocates the memory referenced by the given {@code address}. The deallocation will be done 
     * transactionally for a transactional heap
     * @param address the memory location of the bytes to be deallocated
     * @throws HeapException if the memory could not be deallocated
     */
	public void free(AllocationAddress address);

    /**
     * Stores an {@code allocation} in this heap's root location. The root location can be used for bootstrapping 
     * a persistent heap. Setting a {@code null} value will clear the root location
     * @param allocation the {@code Allocation} to be stored
     */
    public void setRoot(Allocation allocation);

    /**
     * Returns an {@code allocation} stored at this heap's root location. The root location can be used for bootstrapping 
     * a persistent heap
     * @param scope a {@code ResourceScope} to which the returned allocation will be associated with 
     * @return an {@code Allocation} stored at the root location, or {@code null} if cleared
     */
	public Allocation getRoot(ResourceScope scope);

    /**
     * Returns the size of this heap, in bytes.
     * @return the size of this heap, in bytes
     */
    public long size();
    
    /**
     * Returns the {@code Kind} of this heap.
     * @return the {@code Kind} of this heap
     */
    public Kind getKind();

    /**
     * Returns the minimum size for a the specified {@code kind} of heap, in bytes. Attempting to create a heap with a size smaller that this will throw an 
     * {@code IllegalArgumentException}.
     * @param kind the {@code Kind} of this heap
     * @return the minimum heap size
     */
    public static long getMinimumHeapSize(Kind kind) {
        if (kind.value() == Kind.VOLATILE.value())
            return VolatileHeap.MINIMUM_HEAP_SIZE;
        if (kind.value() == Kind.DURABLE.value())
            return PersistentHeap.MINIMUM_HEAP_SIZE;
        if (kind.value() == Kind.TRANSACTIONAL.value())
            return PersistentHeap.MINIMUM_HEAP_SIZE;
        else
            throw new IllegalArgumentException("Invalid heap kind specified");
    }

    /**
     * Creates an {@code Allocation} that represents a block of persistent memory with the given {@code layout}. 
     * For a {@code Heap} of {@code Kind} TRANSACTIONAL, allocation will be done transactionally.  
     * @param layout the layout of the persistent memory to be allocated
     * @param scope a {@code ResourceScope} to which this allocation will be associated with
     * @return an {@code Allocation} that represents the allocated memory
     * @throws OutOfMemoryError if the memory could not be allocated
     */
    default Allocation allocate(MemoryLayout layout, ResourceScope scope) {
        return allocate(layout.byteSize(), scope);
    }

    /**
     * Creates an {@code Allocation} that represents a block of persistent memory with the given {@code layout}. 
     * For a {@code Heap} of {@code Kind} TRANSACTIONAL, allocation will be done transactionally. The supplied 
     * {@code initializer} function is exececuted on the new {@code Allocation}. Allocating with an initializer 
     * can be more efficient than separate  allocation and initialization.
     * @param layout the layout of the persistent memory to be allocated
     * @param scope a {@code ResourceScope} to which this allocation will be associated with
     * @param initializer a function to be executed on the new allocation
     * @return an {@code Allocation} that represents the allocated memory
     * @throws OutOfMemoryError if the memory could not be allocated
     */
    default Allocation allocate(MemoryLayout layout, ResourceScope scope, Consumer<Allocation> initializer) {
        return allocate(layout.byteSize(), scope, initializer);
    }        
}
