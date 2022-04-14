/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.ValueLayout;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static jdk.incubator.foreign.ValueLayout.*;

@Test(singleThreaded = true)
public class PersistentHeapTests {
	PersistentHeap heap = null;

	@BeforeMethod
	public void initialze() {
		heap = null;
	}

	@SuppressWarnings("deprecation")
	@AfterMethod
	public void testCleanup() {
		if (heap != null)
			heap.close();

		if (TestVars.ISDAX) {
            boolean daxCleanSuccess = TestVars.daxCleanUp();
            if (daxCleanSuccess == false) throw new RuntimeException();
		}
		else TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);

		TestVars.cleanUp(TestVars.INVALID_HEAP_PATH);
		TestVars.cleanUp(TestVars.BLOCK_HANDLE_FILE);
		TestVars.cleanUp(TestVars.POOL_SET_FILE);
		for (int i = 0; i < TestVars.NUM_HEAPS; i++) {
			TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME + i);
		}
	}

    // Create Heap, 5 types
    @Test
    public void testCreatePersistentHeap() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        Path path = Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        long size = TestVars.HEAP_SIZE;
        try {
            heap = PersistentHeap.create(path, size);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertEquals(size, heap.size());
    }

    @Test
    public void testCreatePersistentHeapGrowable() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable case, size must be >0
        String heapPathString = TestVars.HEAP_USER_PATH + "/growable";
        Path heapPath = Path.of(heapPathString);
        try {
            Files.createDirectory(heapPath);
            heap = PersistentHeap.create(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPathString);
    }

    @Test
    public void testCreatePersistentHeapGrowableWithLimit() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable with limit case, path must be a dir
        String heapPathString = TestVars.HEAP_USER_PATH + "/growableLimited";
        Path heapPath = Path.of(heapPathString);
        long size = TestVars.HEAP_SIZE; // 20 * 1024 * 1024;
        try {
            Files.createDirectory(heapPath);
            heap = PersistentHeap.create(heapPath, size);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPathString);
    }

    @Test
    public void testCreatePersistentHeapDAX() {
        if (!TestVars.ISDAX) throw new SkipException("Test is only valid in DAX mode");
        // DEV/DAX case, size must be 0
        Path heapPath = Path.of(TestVars.HEAP_USER_PATH);
        try {
            heap = PersistentHeap.create(heapPath);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertTrue(heap != null);
    }

    @Test
    public void testCreatePersistentHeapFused() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String poolSetFile = TestVars.HEAP_USER_PATH + "/" + TestVars.POOL_SET_FILE;
        Path poolSetPath = Path.of(poolSetFile);
        try (PrintWriter poolsetFile = new PrintWriter(poolSetFile)) {
            poolsetFile.println("PMEMPOOLSET");
            poolsetFile.println("OPTION SINGLEHDR");
            poolsetFile.println(TestVars.HEAP_SIZE + " " + TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        }
        catch (FileNotFoundException e) {
            Assert.assertFalse(true);
            return;
        }
        try {
            heap = PersistentHeap.create(poolSetPath, 0);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertTrue(heap.size() > 0);
        TestVars.cleanUp(TestVars.HEAP_USER_PATH);
    }

    @Test
    public void testNegativeOpenFixedHeapAsGrowable() {
        heap = TestVars.createLLPersistentHeap();
        heap.close();
        heap = null;
        try {
            heap = PersistentHeap.open(Path.of(TestVars.HEAP_USER_PATH));
        } catch(IOException e) {
            Assert.fail();
        } catch(HeapException e) { // catch exception like: The poolset_file specified by the path '/mnt/mem/myobjpool.set' does not exist.
            Assert.assertTrue(heap == null);
        }
    }

	@Test
	public void testMinSize() {
        Assert.assertTrue(PersistentHeap.MINIMUM_HEAP_SIZE >= 0 );
    }

    @Test
    public void testSize() {
        heap = TestVars.createLLPersistentHeap(TestVars.HEAP_SIZE);
        Assert.assertTrue(heap.size() >= PersistentHeap.MINIMUM_HEAP_SIZE);
    }

	@Test
	public void testCreateHeapTooSmallSize() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        try {
            heap = TestVars.createLLPersistentHeap(PersistentHeap.MINIMUM_HEAP_SIZE - 1);
            Assert.fail("Heap Exception was not thrown");
        } catch(HeapException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testFillAllocation(){
        long N = 1000;
        final byte value = 42;
        heap = TestVars.createLLPersistentHeap();
        SequenceLayout bytesLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_BYTE);
        MemorySegment testSegment = heap.allocateSegment(bytesLayout, false, ResourceScope.newConfinedScope());
        VarHandle segHandle = bytesLayout.varHandle(sequenceElement());
        testSegment.fill(value);
        for(long i = 0; i < N; i++) {
            Assert.assertEquals((byte)segHandle.get(testSegment, i), value);
        }
    }

    @Test
    public void testOpenWrongHeapKind() {
        Path path = Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        try {
            Heap durableHeap = TestVars.createDurableHeap();
            ((HighLevelHeap)durableHeap).close();
            PersistentHeap.open(path);
            Assert.fail();
        } catch(HeapException e) {
            Assert.assertTrue(true);
        } catch(IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testBadTransaction(){
        heap = TestVars.createLLPersistentHeap();
		final int N = 1024;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_LONG);
        VarHandle ELEMENT = longsLayout.varHandle(sequenceElement());
        MemorySegment longs = MemorySegment.allocateNative(longsLayout, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(longs, 0, longsLayout.byteSize());
                for (long i = 0; i < N; i++) {
                    ELEMENT.set(longs, i, i + i);
                }
            });
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch(IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testTransactionalSegment(){
        heap = TestVars.createLLPersistentHeap();
		final int N = 1024;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_LONG);
        VarHandle ELEMENT = longsLayout.varHandle(sequenceElement());
        MemorySegment longs = heap.allocateSegment(longsLayout, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(longs, 0, longsLayout.byteSize());
            for (long i = 0; i < N; i++) {
                ELEMENT.set(longs, i, i + i);
            }
        });
        for (int i = 0; i < N; i++) {
            long value = (long)ELEMENT.get(longs, (long)i);
            Assert.assertEquals(value,i + i);
        }
    }

    @Test
    public void rootTest() {
        heap = TestVars.createLLPersistentHeap();
        MemorySegment rootSegment = heap.allocateSegment(1024, false, ResourceScope.newConfinedScope());
        heap.setRoot(rootSegment);
        MemorySegment newSegment = heap.getRoot(ResourceScope.globalScope());
        Assert.assertEquals(newSegment.byteSize(), rootSegment.byteSize());
    }

    @Test
    public void rootTestD() {
        heap = TestVars.createLLPersistentHeap();
        MemorySegment rootSegment = heap.allocateSegment(1024, false, ResourceScope.newConfinedScope());
        heap.setRoot(rootSegment);
        heap.flushRoot();
        MemorySegment newSegment = heap.getRoot(ResourceScope.globalScope());
        Assert.assertEquals(newSegment.byteSize(), rootSegment.byteSize());
    }

    @Test
    public void rootTestT() {
        heap = TestVars.createLLPersistentHeap();
        MemorySegment rootSegment = heap.transaction(() -> {
            MemorySegment segment = heap.allocateSegment(1024, true, ResourceScope.newConfinedScope());
            heap.addRootToTransaction();
            heap.setRoot(segment);
            return segment;
        });
        MemorySegment newSegment = heap.getRoot(ResourceScope.globalScope());
        Assert.assertEquals(newSegment.byteSize(), rootSegment.byteSize());
    }

    @Test
    public void clearRootTest() {
        heap = TestVars.createLLPersistentHeap();
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
        MemorySegment rootSegment = heap.allocateSegment(1024, false, ResourceScope.newConfinedScope());
        heap.setRoot(rootSegment);
        Assert.assertFalse(heap.getRoot(ResourceScope.globalScope()) == null);
        heap.setRoot(null);
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void clearRootTestD() {
        heap = TestVars.createLLPersistentHeap();
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
        MemorySegment rootSegment = heap.allocateSegment(1024, false, ResourceScope.newConfinedScope());
        heap.setRoot(rootSegment);
        Assert.assertFalse(heap.getRoot(ResourceScope.globalScope()) == null);
        heap.setRoot(null);
        heap.flushRoot();
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void clearRootTestT() {
        heap = TestVars.createLLPersistentHeap();
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
        MemorySegment rootSegment = heap.allocateSegment(1024, false, ResourceScope.newConfinedScope());
        heap.setRoot(rootSegment);
        Assert.assertFalse(heap.getRoot(ResourceScope.globalScope()) == null);
        heap.transaction(() -> {
            heap.addRootToTransaction();
            heap.setRoot(null);
        });
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void testTransactionalWholeSegment(){
        heap = TestVars.createLLPersistentHeap();
		final int N = 1024;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_LONG);
        VarHandle ELEMENT = longsLayout.varHandle(sequenceElement());
        MemorySegment longs = heap.allocateSegment(longsLayout, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(longs);
            for (long i = 0; i < N; i++) {
                ELEMENT.set(longs, i, i + i);
            }
        });
        for (int i = 0; i < N; i++) {
            long value = (long)ELEMENT.get(longs, (long)i);
            Assert.assertEquals(value,i + i);
        }
    }

    @Test
    public void referenceVarHandleSet() {
        heap = TestVars.createLLPersistentHeap();

        SequenceLayout layout = MemoryLayout.sequenceLayout(2, ValueLayout.ADDRESS);
        VarHandle referenceVH = heap.varHandle(layout, sequenceElement(0));
        VarHandle regularVH = layout.varHandle(sequenceElement(1));

        MemorySegment hostSegment = heap.allocateSegment(layout, false, ResourceScope.newConfinedScope());
        MemorySegment targetSegment = heap.allocateSegment(layout, false, ResourceScope.newConfinedScope());

        regularVH.set(hostSegment, targetSegment.address());
        referenceVH.set(hostSegment, targetSegment.address());

        Assert.assertEquals((MemoryAddress)referenceVH.get(hostSegment), (MemoryAddress)regularVH.get(hostSegment));
        // maybe iterate through the bytes to compare
        Assert.assertNotEquals(hostSegment.getAtIndex(JAVA_LONG, 0L), hostSegment.getAtIndex(JAVA_LONG, 1L));
    }

    // Fixed-size heap boundary tests
    @Test
    public void testFixedAllocateTooLargeSegment() {
        Path path = Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        long size = TestVars.HEAP_SIZE;
        try {
            heap = PersistentHeap.create(path, size);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(size, heap.size());
        try{
            // allocation fails because PMDK metadata has consumed some of the heap
            MemorySegment segment = heap.allocateSegment(size, false, ResourceScope.newConfinedScope());
        } catch(OutOfMemoryError e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testOpenHeapDAX(){
        if (!TestVars.ISDAX) throw new SkipException("Test is only valid in DAX mode");
        Path heapPath = Path.of(TestVars.HEAP_USER_PATH);
        try {
            heap = PersistentHeap.create(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.close();
        try {
            heap = PersistentHeap.open(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
    }

    @Test
    public void testOpenHeapFixed(){
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        Path heapPath = Path.of(TestVars.HEAP_USER_PATH + "fixed");
        long heapSize = TestVars.HEAP_SIZE;
        try {
            heap = PersistentHeap.create(heapPath, heapSize);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.close();
        try {
            heap = PersistentHeap.open(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPath.toString());
    }

    @Test
    public void testOpenHeapGrowableLimited(){
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String heapPathString = TestVars.HEAP_USER_PATH + "/growableLimited";
        Path heapPath = Path.of(heapPathString);
        long maxGrowableSize = 20L*1024L*1024L; // maximum size of this growable heap
        try {
            Files.createDirectory(heapPath);
            heap = PersistentHeap.create(heapPath, maxGrowableSize);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.close();
        try {
            heap = PersistentHeap.open(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPath.toString());
    }

    @Test
    public void testOpenHeapGrowable(){
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String heapPathString = TestVars.HEAP_USER_PATH + "/growable";
        Path heapPath = Path.of(heapPathString);
        try {
            Files.createDirectory(heapPath);
            heap = PersistentHeap.create(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.close();
        try {
            heap = PersistentHeap.open(heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPathString);
    }

    @Test
    public void testNegativeOpenFixedAsGrowable(){
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String heapPathString = TestVars.HEAP_USER_PATH + "/custom";
        Path heapPath = Path.of(heapPathString);
        try {
            heap = PersistentHeap.create(heapPath, PersistentHeap.MINIMUM_HEAP_SIZE);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.close();
        heap = null;
        try {
           heap = PersistentHeap.open(Path.of(TestVars.HEAP_USER_PATH));
        } catch(IOException e) {
            Assert.fail(e.toString());
        } catch(HeapException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(heap, null);
    }

    @Test
    public void testCreateAdvancedHeap() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String poolSetFile = TestVars.HEAP_USER_PATH + "/" + TestVars.POOL_SET_FILE;
        Path poolSetPath = Path.of(poolSetFile);
        try (PrintWriter poolsetFile = new PrintWriter(poolSetFile)) {
            poolsetFile.println("PMEMPOOLSET");
            poolsetFile.println("OPTION SINGLEHDR");
            poolsetFile.println(TestVars.HEAP_SIZE + " " + TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        }
        catch (FileNotFoundException e) {
            Assert.assertFalse(true);
            return;
        }
        try {
            heap = PersistentHeap.create(poolSetPath, 0);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        long size = heap.size();
        Assert.assertTrue(size > 0);
    }

    @Test
    public void testReopenAdvancedHeap() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String poolSetFile = TestVars.HEAP_USER_PATH + "/" + TestVars.POOL_SET_FILE;
        Path poolSetPath = Path.of(poolSetFile);
        try (PrintWriter poolsetFile = new PrintWriter(poolSetFile)) {
            poolsetFile.println("PMEMPOOLSET");
            poolsetFile.println("OPTION SINGLEHDR");
            poolsetFile.println(TestVars.HEAP_SIZE + " " + TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        }
        catch (FileNotFoundException e) {
            Assert.assertFalse(true);
            return;
        }
        try {
            heap = PersistentHeap.create(poolSetPath, 0);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        long size = heap.size();
        Assert.assertTrue(size > 0);
        heap.close();
        heap = null;
        try {
            heap = PersistentHeap.open(poolSetPath);
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(heap.size(), size);
        TestVars.cleanUp(TestVars.HEAP_USER_PATH);
    }

    // TODO: is this test applicable?
    // test currently fails and dumps the following to STDOUT:
    // Stream 'Open Error message: 'poolset file options (0) do not match incompat feature flags (0x7)''.
    // Maybe just need to fix the poolset file contents below?
    /* 
    @Test
    public void testOpenAdvancedHeapAsFixed() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        String poolSetFile = TestVars.HEAP_USER_PATH + "/" + TestVars.POOL_SET_FILE;
        Path poolSetPath = Path.of(poolSetFile);
        try (PrintWriter poolsetFile = new PrintWriter(poolSetFile)) {
            poolsetFile.println("PMEMPOOLSET");
            poolsetFile.println("OPTION SINGLEHDR");
            poolsetFile.println(TestVars.HEAP_SIZE + " " + TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        }
        catch (FileNotFoundException e) {
            Assert.assertFalse(true);
            return;
        }
        try {
            heap = PersistentHeap.create(poolSetPath, 0);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        long size = heap.size();
        Assert.assertTrue(size > 0);
        heap.close();
        heap = null;
        // attempt to open the advanced heap as a fixed heap
        try {
            // Open with path=dir+filename so should use "fixed heap" code path
            heap = PersistentHeap.open(Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME));
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(heap.size(), size);
        TestVars.cleanUp(TestVars.HEAP_USER_PATH);
    }*/

    @Test
    public void testOpenAdvancedHeapAsGrowable() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Name of pool set file is same as that used in PersistentHeap
        String poolSetFile = TestVars.HEAP_USER_PATH + "/" + "myobjpool.set";
        Path poolSetPath = Path.of(poolSetFile);
        try (PrintWriter poolsetFile = new PrintWriter(poolSetFile)) {
            poolsetFile.println("PMEMPOOLSET");
            poolsetFile.println("OPTION SINGLEHDR");
            poolsetFile.println(TestVars.HEAP_SIZE + " " + TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
        }
        catch (FileNotFoundException e) {
            Assert.assertFalse(true);
            return;
        }
        try {
            heap = PersistentHeap.create(poolSetPath, 0);
        } catch(HeapException e) {
            Assert.fail();
        } catch(IOException e) {
            Assert.fail();
        }
        long size = heap.size();
        Assert.assertTrue(size > 0);
        heap.close();
        heap = null;
        // attempt to open the advanced heap as a fixed heap
        try {
            // Open with directory path only, should look for a pool set file and use that
            heap = PersistentHeap.open(Path.of(TestVars.HEAP_USER_PATH));
        } catch(IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(heap.size(), size);
        TestVars.cleanUp(TestVars.HEAP_USER_PATH);
    }

    @Test
	public void testBadFlush() {
		heap = TestVars.createLLPersistentHeap();
        MemorySegment segment = MemorySegment.allocateNative(1024, ResourceScope.newConfinedScope());
        segment.set(JAVA_LONG, 512, 123456789L);
        try {
            heap.flush(segment);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
    }

    //References
    @Test
	public void testWriteReference() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, false, ResourceScope.newConfinedScope());
		heap.setReference(segment, 0, segment.address());
		Assert.assertEquals(heap.getReference(segment, 0), segment.address());
	}

    @Test
	public void testIndexedWriteReference() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, false, ResourceScope.newConfinedScope());
		heap.setReferenceAtIndex(segment, 2, segment);
		Assert.assertEquals(heap.getReference(segment, 16), segment.address());
		Assert.assertEquals(heap.getReferenceAtIndex(segment, 2), segment.address());
	}
    
    @Test
	public void testWriteReferenceD() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, false, ResourceScope.newConfinedScope());
		heap.setReference(segment, 0, segment);
		Assert.assertEquals(heap.getReference(segment, 0), segment.address());
        heap.flush(segment);
	}

    @Test
	public void testIndexedWriteReferenceD() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, false, ResourceScope.newConfinedScope());
		heap.setReferenceAtIndex(segment, 2, segment.address());
		Assert.assertEquals(heap.getReference(segment, 16), segment.address());
		Assert.assertEquals(heap.getReferenceAtIndex(segment, 2), segment.address());
        heap.flush(segment, 16, 24);
	}

    @Test
	public void testWriteReferenceT() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(segment, 0, 8);
            heap.setReference(segment, 0, segment);
            Assert.assertEquals(heap.getReference(segment, 0), segment.address());
        });
	}

    @Test
	public void testIndexedWriteReferenceT() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(100, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(segment);
            heap.setReferenceAtIndex(segment, 2, segment.address());
            Assert.assertEquals(heap.getReference(segment, 16), segment.address());
            Assert.assertEquals(heap.getReferenceAtIndex(segment, 2), segment.address());
        });
	}

    @Test
	public void testWriteBadReference() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = MemorySegment.allocateNative(100, ResourceScope.newConfinedScope());
        try {
            heap.setReference(segment, 0, segment.address());
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}
}
