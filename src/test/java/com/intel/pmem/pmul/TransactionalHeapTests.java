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
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.incubator.foreign.MemoryLayout;
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
public class TransactionalHeapTests {
	Heap heap = null;

	@BeforeMethod
	public void initialze() {
		heap = null;
	}

	@SuppressWarnings("deprecation")
	@AfterMethod
	public void testCleanup() {
		if (heap != null)
			((HighLevelHeap)heap).close();

		if (TestVars.ISDAX) {
            boolean daxCleanSuccess = TestVars.daxCleanUp();
            if (daxCleanSuccess == false) throw new RuntimeException();
		}
		else TestVars.cleanUp(TestVars.HEAP_USER_PATH);

		TestVars.cleanUp(TestVars.INVALID_HEAP_PATH);
		TestVars.cleanUp(TestVars.BLOCK_HANDLE_FILE);
		TestVars.cleanUp(TestVars.POOL_SET_FILE);
		for (int i = 0; i < TestVars.NUM_HEAPS; i++) {
			TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME + i);
		}
	}

	@Test
	public void testMinSizeTransactional() {
        Assert.assertEquals(Heap.getMinimumHeapSize(Heap.Kind.TRANSACTIONAL), PersistentHeap.MINIMUM_HEAP_SIZE);
    }

    @Test
	public void testKind() {
        for (Heap.Kind k : Heap.Kind.values()) {
            if (k.equals(Heap.Kind.TRANSACTIONAL)) {
                Assert.assertTrue(true);
                return;
            }
        }
        Assert.fail("Heap.Kind.TRANSACTIONAL not found");
    }

    @Test
    public void testSize() {
        heap = TestVars.createTransactionalHeap();
        // Assert.assertEquals(heap.size(), TestVars.HEAP_SIZE);
        Assert.assertTrue(heap.size() >= PersistentHeap.MINIMUM_HEAP_SIZE);
    }

	@Test
	public void testCreateHeapTooSmallSize() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        try {
            heap = TestVars.createTransactionalHeap(PersistentHeap.MINIMUM_HEAP_SIZE - 1);
            Assert.fail("Heap Exception was not thrown");
        } catch(HeapException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testFillAllocation(){
        final long N = 1000;
        final byte value = 42;
        heap = TestVars.createTransactionalHeap();
        SequenceLayout bytesLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_BYTE);
        Allocation testAlloc = heap.allocate(bytesLayout, ResourceScope.globalScope());
        Accessor ELEMENT = Accessor.of(bytesLayout, sequenceElement());
        testAlloc.fill(value);
        for(long i = 0; i < N; i++) {
            Assert.assertEquals((byte)ELEMENT.get(testAlloc, i), value);
        }
    }

    @Test
    public void testCreateTransactionalHeapGrowable() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable case, size must be >0
        String heapPathString = TestVars.HEAP_USER_PATH + "/growable";
        Path heapPath = Path.of(heapPathString);
        try {
            Files.createDirectory(heapPath);
            heap = Heap.create(Heap.Kind.TRANSACTIONAL, heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        TestVars.cleanUp(heapPathString);
    }

    @Test
    public void testOpenTransactionalHeapGrowable() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable case, size must be >0
        String heapPathString = TestVars.HEAP_USER_PATH + "/growable";
        Path heapPath = Path.of(heapPathString);
        try {
            Files.createDirectory(heapPath);
            heap = Heap.create(Heap.Kind.TRANSACTIONAL, heapPath);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        ((HighLevelHeap)heap).close();
        try {
            heap = Heap.open(heapPath);
            Assert.assertTrue(true);
        } catch(HeapException e) {
            heap = null;
            Assert.fail(e.toString());
        } catch(IOException e) {
            heap = null;
            Assert.fail(e.toString());
        }
        TestVars.cleanUp(heapPathString);
    }


    @Test
    public void testCreatePersistentHeapGrowableWithLimit() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable with limit case, path must be a dir
        String heapPathString = TestVars.HEAP_USER_PATH + "/growableLimited";
        Path heapPath = Path.of(heapPathString);
        long size = 100_000_000L; // 20 * 1024 * 1024;
        try {
            Files.createDirectory(heapPath);
            heap = Heap.create(Heap.Kind.TRANSACTIONAL, heapPath, size);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        Assert.assertTrue(heap.size() > 0);
        TestVars.cleanUp(heapPathString);
    }
    
    /*@Test
    public void testReopenTransactionalHeapGrowableWithLimit() {
        if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        // Growable with limit case, path must be a dir
        String heapPathString = TestVars.HEAP_USER_PATH + "/growableLimited";
        Path heapPath = Path.of(heapPathString);
        long size = 100_000_000; // 20 * 1024 * 1024;
        try {
            Files.createDirectory(heapPath);
            heap = Heap.create(Heap.Kind.TRANSACTIONAL, heapPath, size);
        } catch(HeapException e) {
            Assert.fail(e.toString());
        } catch(IOException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        heap.allocate(10*1024*1024, ResourceScope.globalScope());
        ((HighLevelHeap)heap).close();
        try {
            heap = Heap.open(heapPath);
        } catch(HeapException e) {
            heap = null;
            Assert.fail(e.toString());
        } catch(IOException e) {
            heap = null;
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap.size() >= 0);
        TestVars.cleanUp(heapPathString);
    }*/

    @Test
    public void testOpenHeap(){
        // if (TestVars.ISDAX) throw new SkipException("Test not valid in DAX mode");
        try {
            heap = TestVars.createTransactionalHeap();
        } catch(HeapException e) {
            Assert.fail(e.toString());
        }
        Assert.assertTrue(heap != null);
        ((HighLevelHeap)heap).close();
        try {
            heap = Heap.open(Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME));
        } catch(HeapException e) {
            heap = null;
            Assert.fail(e.toString());
        } catch(IOException e) {
            heap = null;
            Assert.fail(e.toString());
        }
    }

    @Test
    public void rootTest() {
        heap = TestVars.createTransactionalHeap();
        Allocation rootAllocation = heap.allocate(1024, ResourceScope.globalScope());
        heap.setRoot(rootAllocation);
        Allocation newAllocation = heap.getRoot(ResourceScope.globalScope());
        Assert.assertEquals(newAllocation.byteSize(), rootAllocation.byteSize());
    }

    @Test
    public void clearRootTest() {
        heap = TestVars.createTransactionalHeap();
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
        Allocation rootAllocation = heap.allocate(1024, ResourceScope.globalScope());
        heap.setRoot(rootAllocation);
        Assert.assertFalse(heap.getRoot(ResourceScope.globalScope()) == null);
        heap.setRoot(null);
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void testCreateTransactionalHeap() {
        try {
            heap = Heap.create(Heap.Kind.TRANSACTIONAL, Path.of(TestVars.HEAP_USER_PATH));
            Assert.assertTrue(heap != null);
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testAllocateLayout() {
        heap = TestVars.createTransactionalHeap();
        final long N = 1024;
        final long byteSize = N * Long.BYTES;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_LONG);
        Accessor ELEMENT = Accessor.of(longsLayout, sequenceElement());
        Allocation longs = heap.allocate(longsLayout, ResourceScope.globalScope(), (Allocation A) -> {
            ELEMENT.set(A, 0, 42L);
        });
        Assert.assertEquals(ELEMENT.get(longs, 0), 42L);
        Assert.assertEquals(longs.byteSize(), byteSize);
    }

    @Test
    public void testAllocateByteSize() {
        heap = TestVars.createTransactionalHeap();
        final long N = 1024;
        final long byteSize = N * Long.BYTES;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_LONG);
        Accessor ELEMENT = Accessor.of(longsLayout, sequenceElement());
        Allocation longs = heap.allocate(byteSize, ResourceScope.globalScope(), (Allocation A) -> {
            ELEMENT.set(A, 0, 42L);
        });
        Assert.assertEquals(ELEMENT.get(longs, 0), 42L);
        Assert.assertEquals(longs.byteSize(), byteSize);
    }

    @Test
    public void testGetKind(){
        heap = TestVars.createTransactionalHeap();
        Assert.assertEquals(Heap.Kind.TRANSACTIONAL, heap.getKind());
    }
}
