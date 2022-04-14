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

@Test(singleThreaded = true)
public class HLVolatileHeapTests {
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
	public void testMinSizeVolatile() {
        Assert.assertEquals(Heap.getMinimumHeapSize(Heap.Kind.VOLATILE), VolatileHeap.MINIMUM_HEAP_SIZE);
    }

    @Test
	public void testKind() {
        for (Heap.Kind k : Heap.Kind.values()) {
            if (k.equals(Heap.Kind.VOLATILE)) {
                Assert.assertTrue(true);
                return;
            }
        }
        Assert.fail("Heap.Kind.VOLATILE not found");
    }

    @Test
    public void testSize() {
        heap = TestVars.createVolatileHeap();
        Assert.assertEquals(heap.size(), TestVars.HEAP_SIZE);
    }

	@Test
	public void testCreateHeapTooSmallSize() {
        try {
            heap = TestVars.createVolatileHeap(PersistentHeap.MINIMUM_HEAP_SIZE - 1);
            Assert.fail("Heap Exception was not thrown");
        } catch(HeapException e) {
            Assert.assertTrue(true);
        } catch(IOException f) {
            Assert.fail();
        }
    }

    @Test
    public void testFillAllocation(){
        final long N = 1000;
        final byte value = 42;
        heap = TestVars.createVolatileHeap();
        SequenceLayout bytesLayout = MemoryLayout.sequenceLayout(N, ValueLayout.JAVA_BYTE);
        Allocation testAlloc = heap.allocate(bytesLayout, ResourceScope.globalScope());
        Accessor ELEMENT = Accessor.of(bytesLayout, sequenceElement());
        testAlloc.fill(value);
        for(long i = 0; i < N; i++) {
            Assert.assertEquals((byte)ELEMENT.get(testAlloc, i), value);
        }
    }

    @Test
    public void testCreateVolatileHeapNoSize() {
        try {
            heap = Heap.create(Heap.Kind.VOLATILE, Path.of(TestVars.HEAP_USER_PATH));
        } catch (IOException e) {
            Assert.fail();
        } catch (HeapException e) {
            Assert.assertTrue(heap == null);
        }
        // TODO: fix the cleanup routine, it's not deleting pool set files
        TestVars.cleanUp(TestVars.HEAP_USER_PATH);
    }

    public void testAllocateWithInitializer() {
        heap = TestVars.createVolatileHeap();
        final long N = 1024;
        final long byteSize = N * Long.BYTES;
        SequenceLayout longsLayout = MemoryLayout.sequenceLayout(byteSize, ValueLayout.JAVA_LONG);
        Accessor ELEMENT = Accessor.of(longsLayout, sequenceElement());
        Allocation longs = heap.allocate(byteSize, ResourceScope.globalScope(), (Allocation A) -> {
            ELEMENT.set(A, 0, 42L);
        });
        Assert.assertEquals(42L, ELEMENT.get(longs, 0));
    }

    @Test
    public void testSetRoot() {
        heap = TestVars.createVolatileHeap();
        Allocation refsAlloc = heap.allocate(1024, ResourceScope.newConfinedScope());
        heap.setRoot(refsAlloc);
        Assert.assertFalse(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void testGetRoot() {
        heap = TestVars.createVolatileHeap();
        Allocation refsAlloc = heap.allocate(1024, ResourceScope.newConfinedScope());
        heap.setRoot(refsAlloc);
        Allocation rootAlloc = heap.getRoot(ResourceScope.globalScope());
        Assert.assertTrue(rootAlloc != null);
    }

    @Test
    public void testClearRoot() {
        heap = TestVars.createVolatileHeap();
        Allocation refsAlloc = heap.allocate(1024, ResourceScope.newConfinedScope());
        heap.setRoot(refsAlloc);
        heap.setRoot(null);
        Assert.assertTrue(heap.getRoot(ResourceScope.globalScope()) == null);
    }

    @Test
    public void testGetKind(){
        heap = TestVars.createVolatileHeap();
        Assert.assertEquals(heap.getKind(), Heap.Kind.VOLATILE);
    }

    @Test
    public void testNegativeReopen(){
        heap = TestVars.createVolatileHeap();
        ((HighLevelHeap)heap).close();
        try {
            Heap.open(Path.of(TestVars.HEAP_USER_PATH));
            System.out.println("HLVolatileHeapTests.testOpenVolatileHeapAsHeap: HLVolatileHeap successfully opened as Heap with no Kind");
            Assert.fail();
        } catch(IOException e) {
            Assert.fail("HeapException wasn't thrown"); // should get 'The poolset_file specified by the path '/mnt/mem/myobjpool.set' does not exist.'
        } catch(HeapException f) {
            Assert.assertTrue(true); // should get 'The poolset_file specified by the path '/mnt/mem/myobjpool.set' does not exist.'
        } finally {
            heap = null; // set heap to null since it never re-opened properly so shouldn't be closed again in the testCleanup
        }
    }
}
