/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static jdk.incubator.foreign.ValueLayout.*;

/**
 * Manages a heap of persistent memory suitable for applications that do not need data to persist
 * beyond the lifetime of the current process.
 */
public class VolatileHeap extends LowLevelHeap {
    private final MemorySegment heapSegment;
    private final MemoryAddress heapAddress;
    private final long size;

    static final MethodHandle memkindErrorMessage;
    static final MethodHandle memkindCreatePmem;
    static final MethodHandle memkindMalloc;
    static final MethodHandle memkindFree;
    static final MethodHandle memkindDestroyKind;

    /**
    * The minimum size for a volatile heap, in bytes. Attempting to create a heap with a size smaller that this will throw an 
    * {@code IllegalArgumentException}.
    */
    public static final long MINIMUM_HEAP_SIZE;
    static final long ERROR_MESSAGE_SIZE;

    static {
        System.loadLibrary("pmul");
        System.loadLibrary("memkind");

        MINIMUM_HEAP_SIZE = nativeMinHeapSize0();
        ERROR_MESSAGE_SIZE = nativeErrorMessageSize0();

        CLinker linker = CLinker.systemCLinker();
        memkindErrorMessage = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("memkind_error_message").get(), FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_LONG));
        memkindCreatePmem = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("memkind_create_pmem").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS));
        memkindMalloc = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("memkind_malloc").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        memkindFree = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("memkind_free").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        memkindDestroyKind = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("memkind_destroy_kind").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS));
    }

    /**
     * Creates a new heap, of the given size, using memory available at the given file system directory. 
     * @param directory the path at which memory is available
     * @param size the number of bytes to allocate for the heap
     * @return the heap
     * @throws IllegalArgumentException if {@code path} is {@code null} or if {@code size} 
     * is less than {@code MINIMUM_HEAP_SIZE}
     * @throws HeapException if the heap could not be created
     */
    public static VolatileHeap create(Path directory, long size) throws IOException {
        return new VolatileHeap(directory, LowLevelHeap.Kind.VOLATILE, size);
    }

    static VolatileHeap create(Path path, HeapKind kind, long size) throws IOException {
        return new VolatileHeap(path, kind, size);
    }

    VolatileHeap(Path path, HeapKind kind, long size) throws IOException {
		this.path = path; 
        if (size < MINIMUM_HEAP_SIZE) {
            throw new HeapException("The Heap size must be at least " + MINIMUM_HEAP_SIZE + " bytes.");
        }
        if (!path.toFile().isDirectory()) {
            throw new HeapException("The path \"" + path + "\" does not exist or is not a directory");
        }
        heapAddress = initializeCreate(path.toString(), size);
        if (heapAddress == MemoryAddress.NULL) throw new HeapException("Unable to create heap at " + path);
        heapSegment = MemorySegment.ofAddress(heapAddress, size, ResourceScope.globalScope());
        this.size = size;
        metadata = new Metadata(this, allocateSegment(Metadata.layout, ResourceScope.globalScope()));
        metadata.setKind(kind.value());
	}

    /**
     * Creates a new segment that models a block of persistent memory with the given layout and resource scope. Lifetime
     * of the modeled persistent memory is controled by the close action of the resource scope.
     * 
     * @param  layout        the layout of the persistent memory
     * @param  scope         the segment scope
     * @return               a new persistent memory segment
     */
    public MemorySegment allocateSegment(MemoryLayout layout, ResourceScope scope) {
        return allocateSegment(layout.byteSize(), scope);
    }

    /**
     * Creates a new segment that models a block of persistent memory with the given size and resource scope. Lifetime
     * of the modeled persistent memory is controled by the close action of the resource scope.
     * 
     * @param  byteSize      the size, in bytes, of the persistent memory block backing the segment
     * @param  scope         the segment scope
     * @return               a new persistent memory segment
     */
    public MemorySegment allocateSegment(long byteSize, ResourceScope scope) {
        MemoryAddress address = allocate(byteSize);
        MemorySegment segment = createSegment(address, byteSize, scope);
        return segment; 
    }

    /**
     * Returns the size of this heap, in bytes.
     * @return the size of this heap, in bytes
     */
    public long size() { return size; }

    /**
     * Deallocates a block of memory associated with a memory segment with the given address.
     * @param address the address of the segment
     */
    public void freeSegment(MemoryAddress address) {
        free(address);
    }

    @Override
    MemorySegment allocateSegment(long byteSize, boolean transactional, ResourceScope scope) {
        if (transactional) throw new UnsupportedOperationException("only VOLATILE mode is supported");
        return allocateSegment(byteSize, scope);
    }

    @Override
    void freeSegment(MemoryAddress address, boolean transactional) {
        if (transactional) throw new UnsupportedOperationException("only VOLATILE mode is supported");
        free(address);
    }

    @Override
    long heapAddress() {
        return heapAddress.toRawLongValue();
    }

    @Override
    MemoryAddress transformAddress(MemoryAddress address) { return address; }
	
    @Override
    MemoryAddress reformAddress(MemoryAddress address) { return address; }

    MemorySegment createSegment(MemoryAddress address, long size, ResourceScope scope) {
        MemorySegment segment = MemorySegment.ofAddress(address, size, scope); 
        return segment;
    }

	void free(MemoryAddress address) {
        try {
            memkindFree.invokeExact((Addressable)heapAddress, (Addressable)address);
        } catch(Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
	}

    Path path() {
        return path;
    }
    
    void close() {
        try{
            int ret = (int)memkindDestroyKind.invokeExact((Addressable)heapAddress);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    static String getNativeErrorMessage(int errorNumber) {
        String ret;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment messageSegment = allocator.allocate(ERROR_MESSAGE_SIZE);
            memkindErrorMessage.invokeExact(errorNumber, (Addressable)messageSegment, ERROR_MESSAGE_SIZE);
            ret = messageSegment.getUtf8String(0);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return ret;
    }

    static MemoryAddress initializeCreate(String path, long size) {
        MemoryAddress poolAddress;
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pathSegment = allocator.allocateUtf8String(path);     
            MemorySegment kindSegment = allocator.allocate(ADDRESS, MemoryAddress.NULL);
            int errorNumber= (int)memkindCreatePmem.invokeExact((Addressable)pathSegment, size, (Addressable)kindSegment);
            if (errorNumber != 0) throw new HeapException("memkind_create_pmem " + getNativeErrorMessage(errorNumber));
            poolAddress = kindSegment.get(ADDRESS, 0);
        } catch (HeapException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return poolAddress;
    }

    MemoryAddress allocate(long size) {
        MemoryAddress address;
        try {
            address = (MemoryAddress)memkindMalloc.invokeExact((Addressable)heapAddress, size);
            if (address == MemoryAddress.NULL) throw new OutOfMemoryError("Unable to allocate " + size + " bytes in heap " + path);
        } catch (HeapException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return address;
    }

    static native long nativeMinHeapSize0();
    static native long nativeErrorMessageSize0();
}
