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
import java.nio.file.Path;
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

@Test(singleThreaded = true)
public class LLVolatileHeapTests {
	VolatileHeap heap = null;

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
			//TestVars.daxCleanUp();
		}
		else TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);

		TestVars.cleanUp(TestVars.INVALID_HEAP_PATH);
		TestVars.cleanUp(TestVars.BLOCK_HANDLE_FILE);
		TestVars.cleanUp(TestVars.POOL_SET_FILE);
		for (int i = 0; i < TestVars.NUM_HEAPS; i++) {
			TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME + i);
		}
	}

	@Test
	public void testMinSize() {
        Assert.assertTrue(VolatileHeap.MINIMUM_HEAP_SIZE >= 0 );
    }

    @Test
    public void testSize() {
        heap = TestVars.createLLVolatileHeap();
        Assert.assertEquals(heap.size(), TestVars.HEAP_SIZE);
    }

	@Test
	public void testCreateHeapTooSmallSize() {
        try {
            heap = TestVars.createLLVolatileHeap(PersistentHeap.MINIMUM_HEAP_SIZE - 1);
            Assert.fail("Heap Exception was not thrown");
        } catch(HeapException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCreateBadPath(){
        try {
            heap = VolatileHeap.create(Path.of(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME), TestVars.HEAP_SIZE);
            Assert.fail("Heap Exception was not thrown");
        } catch(IOException e) {
            Assert.fail("Heap Exception was not thrown");
        } catch(HeapException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testAllocateSegmentBytes(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment segment = heap.allocateSegment(1024, ResourceScope.globalScope());
        segment.set(ValueLayout.JAVA_BYTE, 0, (byte)42);
        Assert.assertEquals(segment.byteSize(), 1024);
        Assert.assertEquals(segment.get(ValueLayout.JAVA_BYTE, 0), (byte)42);
    }

    @Test
    public void testAllocateSegmentLayout(){
        heap = TestVars.createLLVolatileHeap();
        MemoryLayout longs = MemoryLayout.sequenceLayout(128, ValueLayout.JAVA_LONG);
        MemorySegment segment = heap.allocateSegment(longs, ResourceScope.globalScope());
        segment.set(ValueLayout.JAVA_BYTE, 0, (byte)42);
        Assert.assertEquals(segment.byteSize(), 1024);
        Assert.assertEquals(segment.get(ValueLayout.JAVA_BYTE, 0), (byte)42);
    }

    @Test
    public void testFreeSegment(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment segment = heap.allocateSegment(1024, false, ResourceScope.globalScope());
        heap.freeSegment(segment.address());
        Assert.assertTrue(true);
    }

    @Test
    public void testPath(){
        heap = TestVars.createLLVolatileHeap();
        Path path = heap.path();
        Assert.assertTrue(path != null);
    }

    // Negative tests
    @Test
    public void testNegativeAllocateSegmentTransactional(){
        heap = TestVars.createLLVolatileHeap();
        try {
            MemorySegment segment = heap.allocateSegment(1024, true, ResourceScope.globalScope());
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeFreeSegmentTransactional(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment segment = heap.allocateSegment(1024, ResourceScope.newConfinedScope());
        try {
            heap.freeSegment(segment.address(), true);
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativePoolAddress(){
        heap = TestVars.createLLVolatileHeap();
        try {
            long address = heap.poolAddress();
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeSetRoot(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment refsSegment = heap.allocateSegment(10 * Long.BYTES, false, ResourceScope.newConfinedScope());
        try {
            heap.setRoot(refsSegment);
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeGetRoot(){
        heap = TestVars.createLLVolatileHeap();
        try {
            heap.getRoot(ResourceScope.globalScope());
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeClearRoot(){
        heap = TestVars.createLLVolatileHeap();
        try {
            heap.setRoot(null);
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeFlushWithLength(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment bytes = heap.allocateSegment(64, false, ResourceScope.globalScope());
        try {
            heap.flush(bytes, 0, 64);
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNegativeFlush(){
        heap = TestVars.createLLVolatileHeap();
        MemorySegment bytes = heap.allocateSegment(64, false, ResourceScope.globalScope());
        try {
            heap.flush(bytes);
            Assert.fail();
        } catch(UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }
}
