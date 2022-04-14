/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.ValueLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static jdk.incubator.foreign.ValueLayout.*;

/**
 * A low-level heap in which the data consistency of writes is manually controlled by the 
 * developer.  Basic writes are suitable for data that need not be recovered after a 
 * data consistency uses.  Writes within the context of a transaction and preceded by an 
 * addToTransaction call are suitable for fail-safe data consistency uses. 

 * Manages a heap of persistent memory suitable for applications that need to recover memory contents
 * after an application or machine restart. This low-level heap supports flexible, manual control
 * of data consistency of written data.  
 * See {@link com.intel.pmem.pmul.Heap} for a higher-level Heap 
 * 
 */
public class PersistentHeap extends LowLevelHeap {
	private final long poolAddress;
	private long currentSize;
	private boolean valid;
    static MemoryLayout pmemOid = MemoryLayout.structLayout(
		JAVA_LONG.withName("pool_uuid_lo"),
		JAVA_LONG.withName("offset")
	);
 
    private static final long MAX_HEAP_SIZE = 12 * 1024 * 1024 * 1024L * 1024L;
    private static final String POOL_SET_FILE = "myobjpool.set";
    static final long TYPE_NUM = 1017;

    static final MethodHandle pmemPersist;
    static final MethodHandle pmemobjCreate;
    static final MethodHandle pmemobjErrormsg;
    static final MethodHandle pmemobjOpen;
    static final MethodHandle pmemobjZalloc;
    static final MethodHandle pmemobjTxZalloc;
    static final MethodHandle pmemobjOid;
    static final MethodHandle pmemobjFree;
    static final MethodHandle pmemobjTxFree;
    static final MethodHandle pmemobjAllocUsableSize;
    static final MethodHandle pmemobjClose;
    static final MethodHandle pmemobjRoot;
    static final MethodHandle pmemobjPoolByOid;
    static final MethodHandle pmempoolRm;

    /**
    * The minimum size for a Persistent heap, in bytes. Attempting to create a heap with a size smaller that this will throw an 
    * {@code IllegalArgumentException}.
    */
    public static final long MINIMUM_HEAP_SIZE;

    private static final int RM_POOL_FLAG;
    static final long MODE_FLAG; 

    static {
        System.loadLibrary("pmul");
        System.loadLibrary("pmemobj");
        System.loadLibrary("pmem");
        System.loadLibrary("pmempool");

        MINIMUM_HEAP_SIZE = nativeMinHeapSize0();
        RM_POOL_FLAG = nativeRemovePoolFlag();
        MODE_FLAG = nativeModeFlag();

        CLinker linker = CLinker.systemCLinker();
        pmemobjErrormsg = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_errormsg").get(), FunctionDescriptor.of(ADDRESS));
        pmemPersist = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmem_persist").get(), FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG));
        pmemobjCreate = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_create").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG));
        pmemobjOpen = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_open").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        pmemobjZalloc = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_zalloc").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG));
        pmemobjTxZalloc = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_zalloc").get(), FunctionDescriptor.of(pmemOid, JAVA_LONG, JAVA_LONG));
        pmemobjOid = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_oid").get(), FunctionDescriptor.of(pmemOid, ADDRESS));
        pmemobjFree = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_free").get(), FunctionDescriptor.ofVoid(ADDRESS));
        pmemobjTxFree = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_tx_free").get(), FunctionDescriptor.of(JAVA_INT, pmemOid));
        pmemobjAllocUsableSize = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_alloc_usable_size").get(), FunctionDescriptor.of(JAVA_LONG, pmemOid));
        pmemobjClose = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_close").get(), FunctionDescriptor.ofVoid(ADDRESS));
        pmemobjRoot = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_root").get(), FunctionDescriptor.of(pmemOid, ADDRESS, JAVA_LONG));
        pmemobjPoolByOid = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmemobj_pool_by_oid").get(), FunctionDescriptor.of(ADDRESS, pmemOid));
        pmempoolRm = linker.downcallHandle(SymbolLookup.loaderLookup().lookup("pmempool_rm").get(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    }

    static void createPoolSetFile(File file, long size) throws IOException {
        File poolFile = new File(file, POOL_SET_FILE);
        long capacity = (size == 0L) ? file.getTotalSpace() : size;
        if (capacity < MINIMUM_HEAP_SIZE)
            throw new HeapException("The partition \"" + file.getAbsolutePath() + "\" must have at least " + MINIMUM_HEAP_SIZE + " bytes");

        Charset charset = Charset.forName("US-ASCII");
        StringBuffer sb = new StringBuffer();
        sb.append( "PMEMPOOLSET\n"
                  + "OPTION SINGLEHDR\n"
                  + capacity + " " + file.getAbsolutePath() + "\n");
        if (poolFile.createNewFile()) {
            BufferedWriter writer = Files.newBufferedWriter(poolFile.toPath(), charset);
            writer.write(sb.toString(), 0, sb.toString().length());
            writer.close();
        }
        else throw new HeapException("Heap \"" + file.getAbsolutePath() + "\" already exists");
    }

    /**
     * Opens an existing heap. Provides access to the heap associated with the specified {@code path}.
     * @param path the path to the heap
     * @return the heap at the specified path
     * @throws IllegalArgumentException if {@code path} is {@code null}
     * @throws HeapException if the heap could not be opened
     */
    public static PersistentHeap open(Path path) throws IOException {
        return open(path, Kind.PERSISTENT);
    }

    static PersistentHeap open(Path path, HeapKind kind) throws IOException {
        boolean isDEVDAX = path.toString().startsWith("/dev/dax");
        if(path.toFile().isDirectory() && !isDEVDAX) {
            path = Path.of(path.toString() + "/" + POOL_SET_FILE);
            if (!Files.exists(path)) throw new HeapException("The poolset_file specified by the path '" + path.toString() + "' does not exist.");
            return new PersistentHeap(path, 0, kind, false); 
        } else {
            long size = 0;
            if (!isDEVDAX) size = Files.size(path); 
            return new PersistentHeap(path, size, kind, false);
        }
    }

    /**
     * Creates a new heap. If {@code path} refers to a directory, a 
     * growable heap will be created.  If {@code path} refers to a DAX device, a heap over that 
     * entire device will be created.  
     * @param path a path to the new heap
     * @return the heap at the specified path
     * @throws IllegalArgumentException if {@code path} is {@code null}
     * @throws HeapException if the heap could not be created
     */
    public static PersistentHeap create(Path path) throws IOException {
        return create(path, Kind.PERSISTENT);
    }

    static PersistentHeap create(Path path, HeapKind kind) throws IOException {
        Path heapPath;
        long heapSize;
        if(path.toString().startsWith("/dev/dax")){
            heapPath = path;
            heapSize = 0;
        } else { // growable heap with no limit
            File file = path.toFile(); 
            if (!file.isDirectory()) {
                throw new HeapException("The path \"" + path + "\" does not exist or is not a directory");
            }
            heapSize = 0;
            heapPath = Path.of((new File(file, POOL_SET_FILE)).getAbsolutePath());
            try {
                createPoolSetFile(file, 0);
            }
            catch (IOException e) {
                throw new HeapException(e.getMessage());
            }
        }
        return new PersistentHeap(heapPath, heapSize, kind, true);
    }

    /**
     * Creates a new heap. If {@code path} refers to a file, a fixed-size heap of {@code size} bytes will be created.
     * If {@code path} refers to a directory, a growable heap, limited to {@code size} bytes, will be created.
     * If {@code size} is {@code 0}, the path will be interpreted as an advanced "fused pool" descriptor file.
     * @param path the path to the heap
     * @param size the number of bytes to allocate for the heap
     * @return the heap at the specified path
     * @throws IllegalArgumentException if {@code path} is {@code null} or if {@code size} 
     * is less than {@code MINIMUM_HEAP_SIZE}
     * @throws HeapException if the heap could not be created
     */
    public static PersistentHeap create(Path path, long size) throws IOException {
        return create(path, size, Kind.PERSISTENT);
    }

    static PersistentHeap create(Path path, long size, HeapKind kind) throws IOException {
        if (path == null) throw new IllegalArgumentException("The provided path must not be null");
        if (path.startsWith("/dev/dax")) throw new IllegalArgumentException("The path is invalid for this method");
        if (size != 0L && size  < MINIMUM_HEAP_SIZE)
            throw new HeapException("The Heap size must be at least " + MINIMUM_HEAP_SIZE + " bytes");
        File file = new File(path.toString());
        Path heapPath;
        long heapSize;

        if (size == 0) { // advanced fused case
            // size must be 0 and path is an existing poolSetFile
            if (file.isFile()) {
                heapPath = path;
                heapSize = size;
            }
            else throw new HeapException("The path \"" + path + "\" does not exist or is not a file");
        }
        else if (file.isDirectory()) { // growable with limit
            // size must be > 0 and path is an existing directory
            heapPath = Path.of((new File(file, POOL_SET_FILE)).getAbsolutePath());
            try {
                createPoolSetFile(file, size);
            }
            catch (IOException e) {
                throw new HeapException(e.getMessage());
            }
            heapSize = 0L;
        }
        else { // fixed-size heap
            // size must be > 0 and path is a path to a non-existent file
            if (file.exists()) {
                throw new HeapException("Heap \"" + path.toString() + "\" already exists");
            }
            heapPath = path;
            heapSize = size;
        }
        PersistentHeap heap = new PersistentHeap(heapPath, heapSize, kind, true);
        return heap;
    } 

    PersistentHeap(Path path, long size, HeapKind kind, boolean create) throws IOException {
		this.path = path;
        if (size != 0 && size < MINIMUM_HEAP_SIZE) {
            if (!create) size = 0;
            else throw new HeapException("The Heap size must be at least " + MINIMUM_HEAP_SIZE + " bytes.");
        }
        poolAddress = create ? initializeCreate(path.toString(), size) : initializeOpen(path.toString());
		this.valid = true;
        if (size == 0) {
            currentSize = (long)probeHeapSize();
        }
        else {
            this.currentSize = size;
        }
        MemorySegment metadataSegment = getRootInternal(Metadata.layout);
        if (create) {
            Transaction.run(this, () -> {
                addToTransaction(metadataSegment);
                metadata = new Metadata(this, metadataSegment);
                metadata.setKind(kind.value());
                metadata.setVersion(HEAP_VERSION);
            });
        }
        else {
            if (metadataSegment == null) throw new HeapException("Failed to open Heap. Heap corrupted");
            metadata = new Metadata(this, metadataSegment);
            long currentVersion = metadata.getVersion();
            if(kind.value() != Kind.NOKIND.value() && kind.value() != metadata.getKind()) {
                this.close();
                throw new HeapException("Wrong heap type specified");
            }
            if (currentVersion < MIN_HEAP_VERSION || currentVersion > HEAP_VERSION) throw new HeapException("Failed to open heap. Incompatible heap version.");
        }
	}

    /**
     * Returns the size of this heap, in bytes.
     * @return the size of this heap, in bytes
     */
    public long size() { 
        return currentSize;
    }

    @Override
    long poolAddress() {
        return poolAddress;
    }

    /**
     * Creates a new segment that models a block of persistent memory with the given layout and resource scope. Lifetime
     * of the modeled persistent memory is controled by the close action of the resource scope.
     * 
     * @param  layout        the layout of the persistent memory
     * @param  transactional if true, the allocation will be done in a fail-safe manner
     * @param  scope         the segment scope
     * @return               a new persistent memory segment
     */
    public MemorySegment allocateSegment(MemoryLayout layout, boolean transactional, ResourceScope scope) {
        return allocateSegment(layout.byteSize(), transactional, scope);
    }

    /**
     * Stores a reference to a memory segment at the given offset within the supplied segment.  A translation
     * from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param segment   the segment within which to store the reference
     * @param offset    the offset within the supplied segment at which to store the reference
     * @param reference the source of the memory reference
     */
    public void setReference(MemorySegment segment, long offset, Addressable reference) {
        //bounds check reference?
        segment.set(ADDRESS, offset, transformAddress(reference.address()));
    }

    /**
     * Stores a reference to a memory segment at the given index within the supplied segment.  A translation
     * from absolute address to relocatable address will be done in support of reaccessing the reference
     * after a process or machine restart.
     * @param segment   the segment within which to store the reference
     * @param index    the index within the supplied segment at which to store the reference
     * @param reference the source of the memory reference
     */
    public void setReferenceAtIndex(MemorySegment segment, long index, Addressable reference) {
        segment.setAtIndex(ADDRESS, index, transformAddress(reference.address()));
    }

    /**
     * Retrieves a reference to a memory segment stored previously with {@code setReference}. A
     * translation from relocatable address to absolute address will be done to enable use of the
     * previously store reference, in this process.
     * @param  segment the segement from which to retrieve the reference
     * @param  offset  the offset within the supplied segment from which to retrieve the reference
     * @return         the memory address of the previously store reference
     */
    public MemoryAddress getReference(MemorySegment segment, long offset) {
        return reformAddress(segment.get(ADDRESS, offset));
    }

    /**
     * Retrieves a reference to a memory segment stored previously with {@code setReference}. A
     * translation from relocatable address to absolute address will be done to enable use of the
     * previously stored reference, in this process.
     * @param  segment the segement from which to retrieve the reference
     * @param  index  the index within the supplied segment from which to retrieve the reference
     * @return         the memory address of the previously store reference
     */
    public MemoryAddress getReferenceAtIndex(MemorySegment segment, long index) {
        return reformAddress(segment.getAtIndex(ADDRESS, index));
    }

    // LowLevelHeap methods
    /**
     * Creates a new segment that models a block of persistent memory with the given size and resource scope. Lifetime
     * of the modeled persistent memory is controled by the close action of the resource scope.
     * 
     * @param  byteSize      the size, in bytes, of the persistent memory block backing the segment
     * @param  transactional if true, the allocation will be done in a fail-safe manner
     * @param  scope         the segment scope
     * @return               a new persistent memory segment
     */
    @Override
    public MemorySegment allocateSegment(long byteSize, boolean transactional, ResourceScope scope) {
        long offset = allocate(byteSize, transactional);
        MemoryAddress address = MemoryAddress.ofLong(poolAddress + offset);
        MemorySegment segment = createSegment(address, byteSize, scope);//, accessModes);
        return segment; 
    }

    /**
     * Deallocates a block of memory associated with a memory segment with the given address.
     * @param address the address of the segment
     * @param transactional if true, the deallocation will be done in a fail-safe manner
     */
    @Override
    public void freeSegment(MemoryAddress address, boolean transactional) {
        //long offset = transformAddress(address).toRawLongValue();
        //free0(poolAddress(), transactional, offset);
        free(address, transactional);
    }

    /**
     * Creates a memory access var handle which can be used to dereference memory at the given path
     * within the given layout
     * @param  layout   the layout 
     * @param  elements the path within the given layout
     * @return          a var handle which can be used to dereference memory at the given path
     */
    public VarHandle varHandle(MemoryLayout layout, PathElement... elements) {
        try {
            VarHandle target = layout.varHandle(elements);
            if (!target.varType().equals(MemoryAddress.class)) return target;
            MethodHandle filterTo = MethodHandles.lookup().findStatic(PersistentHeap.class, "filterTo", MethodType.methodType(MemoryAddress.class, long.class, Addressable.class));
            MethodHandle filterFrom = MethodHandles.lookup().findStatic(PersistentHeap.class, "filterFrom", MethodType.methodType(Addressable.class, long.class, MemoryAddress.class));
            VarHandle tmp = MemoryHandles.filterValue(target, filterTo, filterFrom);
            return MemoryHandles.insertCoordinates(tmp, target.coordinateTypes().size(), heapAddress());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    static MemoryAddress filterTo(long poolAddress, Addressable address) {
        return address.address().addOffset(0 - poolAddress);
    }

    static Addressable filterFrom(long poolAddress, MemoryAddress address) {
        return address.address().addOffset(poolAddress);
    }

    @Override
    long heapAddress() {
        return poolAddress;
    }

    @Override
    MemoryAddress transformAddress(MemoryAddress address) {
        MemoryAddress ret;
        ret = address.addOffset(0 - poolAddress);
        checkHandle(ret.toRawLongValue());
        return ret;
    }
	
    @Override
    MemoryAddress reformAddress(MemoryAddress address) {
        checkHandle(address.toRawLongValue());
        return address.addOffset(poolAddress);
    }

    /**
    * Ensures that the given range of bytes within the given segment's memory are written to persistent memory media.
    * @param segment the segment containing bytes to be flushed
    * @param offset the starting location with the semgent memory from which to flush bytes
    * @param byteCount the number of bytes to flush
    * @throws IndexOutOfBoundsException if the operation would cause access of data outside of segment bounds 
    */

	@Override
	public void flush(MemorySegment segment, long offset, long byteCount) {
        checkRange(segment, offset, byteCount);
		flush0(segment, offset, byteCount);
	}

    /**
     * Ensures that the contents of the given segment are written to persistent memory media.
     * @param segment the segment whose memory is to be flushed
     */
	public void flush(MemorySegment segment) {
		flush(segment, 0, segment.byteSize());
	}


    /**
     * Adds the bytes of the given segment's memory to the current transaction.
     * Any modifications to the memory segement will be committed on successful completion of the current
     * transaction or rolled-back on abort of the current transaction.
     * @param segment the segment 
     * @throws IndexOutOfBoundsException if the operation would cause access of data outside of accessor bounds 
     */
	public void addToTransaction(MemorySegment segment) {
		Transaction.addToTransaction(segment, 0, segment.byteSize());
	}

    /**
     * Adds the specified range of bytes, within the given segment's memory, to the current transaction.
     * Any modifications to the range of bytes will be committed on successful completion of the current
     * transaction or rolled-back on abort of the current transaction.
     * @param segment the segment 
     * @param offset the start of the range of bytes to add
     * @param byteCount the number of bytes to add
     * @throws IndexOutOfBoundsException if the operation would cause access of data outside of accessor bounds 
     */
	@Override
	public void addToTransaction(MemorySegment segment, long offset, long byteCount) {
        checkRange(segment, offset, byteCount);
		Transaction.addToTransaction(segment, offset, byteCount);
	}

    /**
     * Stores a reference to the given memory segment at this heap's root location. The root location can be used for bootstrapping 
     * a persistent heap. Setting a {@code null} value will clear the root location of this heap.
     * @param segment the segment to be stored
     */
	@Override
    public synchronized void setRoot(MemorySegment segment) {
        if (segment == null) metadata.setRoot(MemoryAddress.NULL, 0L);
        else metadata.setRoot(transformAddress(segment.address()), segment.byteSize());  
    }

    /**
     * Ensures that the reference stored at the root location is written to persistent memory media.
     */
    public void flushRoot() {
        flush(metadata.rootAsSlice());
    }

    /**
     * Adds the reference stored at the root to the current transaction.
     */
    public void addRootToTransaction() {
        addToTransaction(metadata.rootAsSlice());
    }

    /**
     * Returns a memory segment stored at this heap's root location. The root location can be used for bootstrapping 
     * a persistent heap
     * @param scope a {@code ResourceScope} to which the returned segment will be associated with 
     * @return a memory segment stored at the root location, or {@code null} if cleared
     */
    @Override
    public synchronized MemorySegment getRoot(ResourceScope scope) {
        long rootSize = metadata.getUserRootSize();
        MemoryAddress address = metadata.getUserRoot();
        if (address == MemoryAddress.NULL) return null;
        return createSegment(reformAddress(address), rootSize, scope);
    }

    private MemorySegment getRootInternal(MemoryLayout layout) {
        long offset = getRoot0(layout.byteSize());
        return createSegment(MemoryAddress.ofLong(poolAddress + offset), layout.byteSize(), ResourceScope.globalScope());
    }

    /**
    * Transactionally executes the supplied body function within either an existing transaction, or within a new transaction, 
    * if no transaction is active. Modifictions to memory within the transaction body are limited to memory in this heap.
    * @param body a function is the transaction body
    * @throws TransactionException if a tranaction could not be created
    */
	@Override
	public void transaction(Runnable body) {
		Transaction.run(this, body);
	}
	
    /**
    * Transactionally executes the supplied body function within either an existing transaction, or within a new transaction, 
    * if no transaction is active. Modifictions to memory within the transaction body are limited to memory in this heap.
    * @param body a function is the transaction body
    * @throws TransactionException if a tranaction could not be created
    */
	@Override
	public <T> T transaction(Supplier<T> body) {
		return Transaction.run(this, body);
	}


    long allocate(long size, boolean transactional) {
        long offset = allocate0(poolAddress(), transactional, size);
        return offset;
    }

    MemorySegment createSegment(MemoryAddress address, long size, ResourceScope scope) {
        MemorySegment segment = MemorySegment.ofAddress(address, size, scope); 
        return segment;
    }

    void checkRange(MemorySegment segment, long offset, long byteCount) {
   		if (segment == null) throw new IllegalStateException("Segment is null: " + segment);
        long heapOffset = segment.address().toRawLongValue() - poolAddress + offset;
        if (outOfBounds(heapOffset, byteCount)) {
            String message = String.format("offset range (%d, %d) is outside of heap offset range (%d, %d)", heapOffset, heapOffset + byteCount, 0, currentSize);
   			throw new IndexOutOfBoundsException(message);
   		}
	}	

	void checkHandle(long handle) {
        if (outOfBounds(handle, 0)) {
   			throw new IndexOutOfBoundsException("handle is not within heap bounds");
   		}
   	}

    @Override
    synchronized void close() {
        try {
            pmemobjClose.invokeExact((Addressable)MemoryAddress.ofLong(poolAddress));
        } catch(Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    boolean outOfBounds(long offset, long count) {
        if (offset < 0) return true;
        if ((offset + count) >= currentSize) {
            currentSize = (long)probeHeapSize();
            if ((offset + count) >= currentSize) return true;
        }
        return false;
    }

    static int removePool(String path) {
	   int ret;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pathSegment = allocator.allocateUtf8String(path);
            ret = (int)pmempoolRm.invokeExact((Addressable)pathSegment, RM_POOL_FLAG);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return ret;
    }

    static void flush0(MemorySegment segment, long offset, long byteCount) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            scope.keepAlive(segment.scope());
            if (offset == 0) {
                pmemPersist.invokeExact((Addressable)segment, byteCount);
            } else {
                pmemPersist.invokeExact((Addressable)segment.asSlice(offset), byteCount);
            }
        } catch(Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    static long initializeCreate(String path, long size) {
        MemoryAddress poolAddress;
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pathSegment = allocator.allocateUtf8String(path);     
            MemorySegment layoutSegment = allocator.allocateUtf8String("bridge_persistent_heap");
            poolAddress = (MemoryAddress)pmemobjCreate.invokeExact((Addressable)pathSegment, (Addressable)layoutSegment, size, MODE_FLAG);
            if (poolAddress == MemoryAddress.NULL) {
                MemoryAddress messageAddress = (MemoryAddress)pmemobjErrormsg.invokeExact();
                String message = messageAddress.getUtf8String(0);
                throw new HeapException("Unable to create heap at " + path+". "+message);
            }
        } catch (HeapException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return poolAddress.toRawLongValue();
    }

    static long initializeOpen(String path) {
        MemoryAddress poolAddress;
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pathSegment = allocator.allocateUtf8String(path);     
            MemorySegment layoutSegment = allocator.allocateUtf8String("bridge_persistent_heap");
            poolAddress = (MemoryAddress)pmemobjOpen.invokeExact((Addressable)pathSegment, (Addressable)layoutSegment);
            if (poolAddress == MemoryAddress.NULL) {
                MemoryAddress messageAddress = (MemoryAddress)pmemobjErrormsg.invokeExact();
                String message = messageAddress.getUtf8String(0);
                throw new HeapException("Unable to open heap at " + path+". "+message);
            }
        } catch (HeapException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return poolAddress.toRawLongValue();
    }

    long allocate0(long poolAddress, boolean transactional, long size) {
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pmemoid;     
            long ret = 0;
            if (!transactional) {
                pmemoid = allocator.allocate(pmemOid);     
                pmemoid.fill((byte)0); //pmemoid is null
                int result = (int)pmemobjZalloc.invokeExact((Addressable)MemoryAddress.ofLong(poolAddress), (Addressable)pmemoid, size, TYPE_NUM);
                if (result == 0) ret = pmemoid.getAtIndex(JAVA_LONG, 1);
                else throw new OutOfMemoryError("Unable to allocate " + size + " bytes in heap " + path);
            }
            else {
                pmemoid = Transaction.run(this, () -> {
                    try {
                        MemorySegment segment = (MemorySegment)pmemobjTxZalloc.invokeExact(allocator, size, TYPE_NUM);  
                        if (oidIsNull(segment)) throw new InternalTransactionException("Transaction aborted.", new OutOfMemoryError("Unable to allocate " + size + " bytes in heap " + path));
                        return segment;  
                    } catch (TransactionException e) {
                        throw e;
                    } catch(Throwable t) {
                        throw new RuntimeException(t.getMessage());
                    }
                });
                ret = pmemoid.getAtIndex(JAVA_LONG, 1);
            }
            return ret;
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (RuntimeException f) {
            throw f;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    boolean oidIsNull(MemorySegment oid) {
        return (oid.getAtIndex(JAVA_LONG, 0) == 0 && oid.getAtIndex(JAVA_LONG, 1) == 0);
    }

    void free(MemoryAddress address, boolean transactional) {
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment pmemoid = (MemorySegment)pmemobjOid.invokeExact(allocator, (Addressable)address);    
            if (!transactional) {
                pmemobjFree.invokeExact((Addressable)pmemoid);
            }
            else {
                Transaction.run(this, () -> {
                    try {
                        int ret = (int)pmemobjTxFree.invokeExact(pmemoid);  
                        if (ret != 0) throw new InternalTransactionException("Transaction aborted.", new HeapException("Failed to free memory"));
                    } catch(TransactionException e) {
                        throw e;
                    } catch(Throwable t) {
                        throw new RuntimeException(t.getMessage());
                    }
                });
            }
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    private long getRoot0(long layoutSize) {
        long offset;
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment oid = (MemorySegment)pmemobjRoot.invokeExact(allocator, (Addressable)MemoryAddress.ofLong(poolAddress), layoutSize);    
            offset = oid.getAtIndex(JAVA_LONG, 1);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
        return offset;
    }

    long probeHeapSize() {
        // find maxHeap value
        long maxHeap = MAX_HEAP_SIZE;
        if (poolAddress + maxHeap < 0) {
            do {
                maxHeap = maxHeap / 2;
            } while (poolAddress + maxHeap < 0);
            long delta = maxHeap;
            do {
                delta = delta / 2;
                maxHeap = maxHeap + delta;
            } while (poolAddress + maxHeap > 0);
            maxHeap = maxHeap - delta;
        }
        long min = poolAddress + currentSize;
        long max = poolAddress + maxHeap;
        try (var scope = ResourceScope.newConfinedScope()) {
            var allocator = SegmentAllocator.nativeAllocator(scope);
            MemorySegment oid;
            for (long i = max; min < i; i = (max + min) / 2) {
                oid = (MemorySegment)pmemobjOid.invokeExact(allocator, (Addressable)MemoryAddress.ofLong(i));
                MemoryAddress poolAddr = (MemoryAddress)pmemobjPoolByOid.invokeExact(oid);
                if (!oidIsNull(oid) && poolAddr.toRawLongValue() == poolAddress) {
                    min = i;
                }
                else {
                    max = i;
                }
            }
            return min - poolAddress;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    static native long nativeMinHeapSize0();
    static native int nativeRemovePoolFlag();
    static native long nativeModeFlag();
}
