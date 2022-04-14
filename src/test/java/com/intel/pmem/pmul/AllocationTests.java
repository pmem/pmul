/*
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 */

package com.intel.pmem.pmul;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.StreamSupport;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.ValueLayout;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static jdk.incubator.foreign.ValueLayout.*;

@Test(singleThreaded = true)
public class AllocationTests {
	Heap heap = null;
    Allocation allocation = null;

	@BeforeMethod
	public void initialize() {
		heap = null;
        allocation = null;
	}

	@SuppressWarnings("deprecation")
	@AfterMethod
	public void testCleanup() {
		if (heap != null) {
            if (allocation != null) allocation.free();
			((HighLevelHeap)heap).close();
        }
		if (TestVars.ISDAX) {
			TestVars.daxCleanUp();
		}
		else TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
	}

    @Test
    public void testReconstructV(){
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation alias = Allocation.ofAddress(allocation.address(), 1024, ResourceScope.newConfinedScope());
        allocation.set(JAVA_LONG, 14, 123456L);
        Assert.assertEquals(alias.byteSize(), 1024); 
        Assert.assertEquals(alias.get(JAVA_LONG, 14), 123456L);
    }

    @Test
    public void testReconstructD(){
        heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation alias = Allocation.ofAddress(allocation.address(), 1024, ResourceScope.newConfinedScope());
        allocation.set(JAVA_LONG, 14, 123456L);
        Assert.assertEquals(alias.byteSize(), 1024); 
        Assert.assertEquals(alias.get(JAVA_LONG, 14), 123456L);
    }

    @Test
    public void testReconstructT(){
        heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation alias = Allocation.ofAddress(allocation.address(), 1024, ResourceScope.newConfinedScope());
        allocation.set(JAVA_LONG, 14, 123456L);
        Assert.assertEquals(alias.byteSize(), 1024); 
        Assert.assertEquals(alias.get(JAVA_LONG, 14), 123456L);
    }

    @Test
    public void testAllocationFree(){
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertTrue(allocation.scope().isAlive());
    }

	@Test
	public void testAllocationWriteByte() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BYTE, 12, (byte)128);
		Assert.assertEquals(allocation.get(JAVA_BYTE, 12), (byte)128);
	}

	@Test
	public void testAllocationWriteByteZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BYTE, 0, (byte)128);
		Assert.assertEquals(allocation.get(JAVA_BYTE, 0), (byte)128);
	}

	@Test
	public void testAllocationWriteByteMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BYTE, 1023, (byte)128);
		Assert.assertEquals(allocation.get(JAVA_BYTE, 1023), (byte)128);
	}

	@Test
	public void testAllocationWriteByteMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_BYTE, 1024, (byte)128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteBoolean() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BOOLEAN, 12, true);
		Assert.assertEquals(allocation.get(JAVA_BOOLEAN, 12), true);
	}

	@Test
	public void testAllocationWriteBooleanZeroOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BOOLEAN, 0, true);
		Assert.assertEquals(allocation.get(JAVA_BOOLEAN, 0), true);
	}

	@Test
	public void testAllocationWriteBooleanMaxOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_BOOLEAN, 1023, true);
		Assert.assertEquals(allocation.get(JAVA_BOOLEAN, 1023), true);
	}

	@Test
	public void testAllocationWriteBooleanMaxOffsetNegative() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_BOOLEAN, 1024, true);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteInt() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_INT, 12, 128);
		Assert.assertEquals(allocation.get(JAVA_INT, 12), 128);
	}

    @Test
	public void testAllocationIndexedWriteInt() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_INT, 3, 128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_INT, 3), 128);
		Assert.assertEquals(allocation.get(JAVA_INT, 12), 128);
	}

	@Test
	public void testAllocationWriteIntZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_INT, 0, 128);
		Assert.assertEquals(allocation.get(JAVA_INT, 0), 128);
	}

    @Test
	public void testAllocationIndexedWriteIntZeroOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_INT, 0, 128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_INT, 0), 128);
		Assert.assertEquals(allocation.get(JAVA_INT, 0), 128);
	}

	@Test
	public void testAllocationWriteIntMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_INT, 1023, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteIntMaxOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_INT, 256, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteIntMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_INT, 1024, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationWriteFloat() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_FLOAT, 12, 12.8F);
		Assert.assertEquals(allocation.get(JAVA_FLOAT, 12), 12.8F);
	}

    @Test
	public void testAllocationIndexedWriteFloat() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_FLOAT, 3, 12.8F);
		Assert.assertEquals(allocation.getAtIndex(JAVA_FLOAT, 3), 12.8F);
		Assert.assertEquals(allocation.get(JAVA_FLOAT, 12), 12.8F);
	}

	@Test
	public void testAllocationWriteFloatZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_FLOAT, 0, 12.8F);
		Assert.assertEquals(allocation.get(JAVA_FLOAT, 0), 12.8F);
	}

    @Test
	public void testAllocationIndexedWriteFloatZeroOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_FLOAT, 0, 12.8F);
		Assert.assertEquals(allocation.getAtIndex(JAVA_FLOAT, 0), 12.8F);
		Assert.assertEquals(allocation.get(JAVA_FLOAT, 0), 12.8F);
	}

    @Test
	public void testAllocationWriteFloatMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_FLOAT, 1023, 12.8F);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteFloatMaxOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_FLOAT, 256, 12.8F);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteFloatMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_FLOAT, 1024, 12.8F);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteShort() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_SHORT, 12, (short)128);
		Assert.assertEquals(allocation.get(JAVA_SHORT, 12), (short)128);
	}

	@Test
	public void testAllocationIndexedWriteShort() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_SHORT, 6, (short)128);
		Assert.assertEquals(allocation.get(JAVA_SHORT, 12), (short)128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_SHORT, 6), (short)128);
	}

	@Test
	public void testAllocationWriteShortZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_SHORT, 0, (short)128);
		Assert.assertEquals(allocation.get(JAVA_SHORT, 0), (short)128);
	}

    @Test
	public void testAllocationIndexedWriteShortZeroOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_SHORT, 0, (short)128);
		Assert.assertEquals(allocation.get(JAVA_SHORT, 0), (short)128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_SHORT, 0), (short)128);
	}

	@Test
	public void testAllocationWriteShortMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_SHORT, 1023, (short)128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteShortMaxOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_SHORT, 512, (short)128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteShortMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_SHORT, 1024, (short)128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationWriteChar() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_CHAR, 12, 'a');
		Assert.assertEquals(allocation.get(JAVA_CHAR, 12), 'a');
	}

	@Test
	public void testAllocationIndexedWriteChar() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_CHAR, 6, 'b');
		Assert.assertEquals(allocation.get(JAVA_CHAR, 12), 'b');
		Assert.assertEquals(allocation.getAtIndex(JAVA_CHAR, 6), 'b');
	}

    @Test
	public void testAllocationWriteCharZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_CHAR, 0, 'c');
		Assert.assertEquals(allocation.get(JAVA_CHAR, 0), 'c');
	}

    @Test
	public void testAllocationIndexedWriteCharZeroOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_CHAR, 0, 'd');
		Assert.assertEquals(allocation.get(JAVA_CHAR, 0), 'd');
		Assert.assertEquals(allocation.getAtIndex(JAVA_CHAR, 0), 'd');
	}

    @Test
	public void testAllocationWriteCharMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_CHAR, 1023, 'e');
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteCharMaxOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_CHAR, 512, 'f');
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteCharMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_CHAR, 1024, 'g');
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteLong() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_LONG, 12, 128);
		Assert.assertEquals(allocation.get(JAVA_LONG, 12), 128);
	}

    @Test
	public void testAllocationIndexedWriteLong() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_LONG, 2, 128);
		Assert.assertEquals(allocation.get(JAVA_LONG, 16), 128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_LONG, 2), 128);
	}

	@Test
	public void testAllocationWriteLongZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_LONG, 0, 128);
		Assert.assertEquals(allocation.get(JAVA_LONG, 0), 128);
	}

    @Test
	public void testAllocationIndexedWriteLongZeroOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_LONG, 0, 128);
		Assert.assertEquals(allocation.get(JAVA_LONG, 0), 128);
		Assert.assertEquals(allocation.getAtIndex(JAVA_LONG, 0), 128);
	}

	@Test
	public void testAllocationWriteLongMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_LONG, 1023, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteLongMaxOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_LONG, 128, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteLongMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_LONG, 1024, 128);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationWriteDouble() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_DOUBLE, 12, 128.01D);
		Assert.assertEquals(allocation.get(JAVA_DOUBLE, 12), 128.01D);
	}

    @Test
	public void testAllocationIndexedWriteDouble() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_DOUBLE, 2, 128.01D);
		Assert.assertEquals(allocation.get(JAVA_DOUBLE, 16), 128.01D);
		Assert.assertEquals(allocation.getAtIndex(JAVA_DOUBLE, 2), 128.01D);
	}

	@Test
	public void testAllocationWriteDoubleZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(JAVA_DOUBLE, 0, 128.01D);
		Assert.assertEquals(allocation.get(JAVA_DOUBLE, 0), 128.01D);
	}

    @Test
	public void testAllocationIndexedWriteDoubleZeroOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(JAVA_DOUBLE, 0, 128.01D);
		Assert.assertEquals(allocation.get(JAVA_DOUBLE, 0), 128.01D);
		Assert.assertEquals(allocation.getAtIndex(JAVA_DOUBLE, 0), 128.01D);
	}

    @Test
	public void testAllocationWriteDoubleMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_DOUBLE, 1023, 128.01D);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteDoubleMaxOffset() {
		heap = TestVars.createDurableHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(JAVA_DOUBLE, 128, 128.01D);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteDoubleMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(JAVA_DOUBLE, 1024, 128.01D);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationWriteAddress() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(ADDRESS, 12, MemoryAddress.NULL);
		Assert.assertEquals(allocation.get(ADDRESS, 12), MemoryAddress.NULL);
	}

    @Test
	public void testAllocationIndexedWriteAddress() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(ADDRESS, 2, MemoryAddress.NULL);
		Assert.assertEquals(allocation.get(ADDRESS, 16), MemoryAddress.NULL);
		Assert.assertEquals(allocation.getAtIndex(ADDRESS, 2), MemoryAddress.NULL);
	}

    @Test
	public void testAllocationWriteAddressZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.set(ADDRESS, 0, MemoryAddress.ofLong(123456789L));
		Assert.assertEquals(allocation.get(ADDRESS, 0), MemoryAddress.ofLong(123456789L));
	}

    @Test
	public void testAllocationIndexedWriteAddressZeroOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setAtIndex(ADDRESS, 0, MemoryAddress.ofLong(123456789L));
		Assert.assertEquals(allocation.get(ADDRESS, 0), MemoryAddress.ofLong(123456789L));
		Assert.assertEquals(allocation.getAtIndex(ADDRESS, 0), MemoryAddress.ofLong(123456789L));
	}

    @Test
	public void testAllocationWriteAddressMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(ADDRESS, 1023, MemoryAddress.NULL);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationIndexedWriteAddressMaxOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setAtIndex(ADDRESS, 128, MemoryAddress.NULL);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	public void testAllocationWriteAddressMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.set(ADDRESS, 1024, MemoryAddress.NULL);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

    @Test
	public void testAllocationWriteReference() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setReference(12, ref.address());
		Assert.assertEquals(allocation.getReference(12).address(), ref.address().address());
        ref.free();
	}

    @Test
	public void testAllocationIndexedWriteReference() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setReferenceAtIndex(2, ref.address());
		Assert.assertEquals(allocation.getReference(16).address(), ref.address().address());
		Assert.assertEquals(allocation.getReferenceAtIndex(2).address(), ref.address().address());
        ref.free();
	}

    @Test
	public void testAllocationWriteReferenceZeroOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setReference(0, ref);
		Assert.assertEquals(allocation.getReference(0).address(), ref.address().address());
        ref.free();
	}

    @Test
	public void testAllocationIndexedWriteReferenceZeroOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		allocation.setReferenceAtIndex(0, ref);
		Assert.assertEquals(allocation.getReference(0).address(), ref.address().address());
		Assert.assertEquals(allocation.getReferenceAtIndex(0).address(), ref.address().address());
        ref.free();
	}

    @Test
	public void testAllocationWriteReferenceMaxOffset() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setReference(1023, ref.address());
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
        ref.free();
	}

    @Test
	public void testAllocationIndexedWriteReferenceMaxOffset() {
		heap = TestVars.createTransactionalHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setReferenceAtIndex(128, ref.address());
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
        ref.free();
	}

	@Test
	public void testAllocationWriteReferenceMaxOffsetNegative() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		Allocation ref = heap.allocate(1024, ResourceScope.newConfinedScope());
        Assert.assertEquals(allocation.byteSize(), 1024);
		try {
			allocation.setReference(1024, ref);
			Assert.fail("IndexOutOfBoundsException not thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
        ref.free();
	}

    @Test
    public void testFill() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
		allocation.fill((byte)44);
		for(int i = 0; i < allocation.byteSize(); i++)		
         	assert(allocation.get(JAVA_BYTE, i) == (byte)44);
    }

	@Test
	public void testCopyFromAllocationAllV() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
        other.fill((byte)-1);
		allocation.copyFrom(other);
		for(int i = 0; i < allocation.byteSize(); i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
		}
        other.free();
    }

    @Test
	public void testCopyFromAllocationAllD() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
        other.fill((byte)-1);
		allocation.copyFrom(other);
		for(int i = 0; i < allocation.byteSize(); i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
		}
        other.free();
    }

    @Test
	public void testCopyFromAllocationAllT() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
        other.fill((byte)-1);
		allocation.copyFrom(other);
		for(int i = 0; i < allocation.byteSize(); i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
		}
        other.free();
    }

    @Test
	public void testCopyFromAllocationPartialV() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
		other.fill((byte)-1);
        Allocation.copy(other, 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
        }
    }

	@Test
	public void testCopyFromAllocationPartialD() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
		other.fill((byte)-1);
        Allocation.copy(other, 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
        }
    }

    @Test
	public void testCopyFromAllocationPartialT() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());           
        Allocation other = heap.allocate(1024, ResourceScope.newConfinedScope());
		other.fill((byte)-1);
        Allocation.copy(other, 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++) {
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
        }
    }

    @Test
    public void testCopyFromAllocationZeroLength() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation allocationNew = heap.allocate(1024, ResourceScope.newConfinedScope());
        allocationNew.fill((byte)-1);
        allocation.copyFrom(allocationNew.asSlice(0, 0));
        Assert.assertNotEquals(allocation.get(JAVA_BYTE, 0), (byte)-1);
        allocationNew.free();
    }

    @Test
    public void testCopyFromLargeLength() {
		heap = TestVars.createVolatileHeap();
		allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation allocationNew = heap.allocate(512, ResourceScope.newConfinedScope());
		try {
           	allocationNew.copyFrom(allocation);
			Assert.fail("IndexOutOfBoundsException wasn't thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
    }

    @Test
    public void testCopyFromArrayPartV() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        Arrays.fill(arr, (byte)-1);
		Allocation.copy(MemorySegment.ofArray(arr), 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++)
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
    }

    @Test
    public void testCopyFromArrayPartD() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        Arrays.fill(arr, (byte)-1);
		Allocation.copy(MemorySegment.ofArray(arr), 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++)
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
    }

    @Test
    public void testCopyFromArrayPartT() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        Arrays.fill(arr, (byte)-1);
		Allocation.copy(MemorySegment.ofArray(arr), 56, allocation, 12, 100);
		for(int i = 12; i < 112; i++)
			Assert.assertEquals(allocation.get(JAVA_BYTE, i), (byte)-1);
    }

    @Test
    public void testCopyFromArrayNegativeSrcOffset() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        try {
            Allocation.copy(MemorySegment.ofArray(arr), -1, allocation, 0, 1024);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyFromArrayNegativeDestOffset() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        try {
            Allocation.copy(MemorySegment.ofArray(arr), 0, allocation, -1, 1024);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyFromArrayNegativeLength() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		byte[] arr = new byte[1024];
        try {
            Allocation.copy(MemorySegment.ofArray(arr), 0, allocation, 0, -1);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyToArrayFull() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        byte[] arr = new byte[1024];
		allocation.fill((byte)-1);
        MemorySegment arraySegment = MemorySegment.ofArray(arr);
        Allocation.copy(allocation, 0, arraySegment, 0, 1024); 
        for(int i = 0; i < 1024; i++)
            Assert.assertEquals(arr[i], (byte)-1);
    }

    @Test
    public void testCopyToArrayPart() {
        heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        byte[] arr = new byte[1024];
		allocation.fill((byte)-1);
        Allocation.copy(allocation, 56, MemorySegment.ofArray(arr), 12, 100);
        for(int i = 12; i < 112; i++)
            Assert.assertEquals(arr[i], (byte)-1);
    }

    @Test
    public void testCopyToArrayNegativeSrcOffset() {
        heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        byte[] arr = new byte[1024];
        try {
            Allocation.copy(allocation, -1, MemorySegment.ofArray(arr), 0, 1024);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyToArrayNegativeDestOffset() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        byte[] arr = new byte[1024];
        try {
            Allocation.copy(allocation, 0, MemorySegment.ofArray(arr), -1, 1024);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyToArrayNegativeLength() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        byte[] arr = new byte[1024];
        try {
            Allocation.copy(allocation, 0, MemorySegment.ofArray(arr), 0, -1);
            Assert.fail("IndexOutOfBoundsException wasn't thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert true;
        }
    }

    @Test
    public void testCopyToByteArray() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		allocation.fill((byte)45);
        byte[] arr = allocation.toArray(JAVA_BYTE);
        for (byte b : arr) Assert.assertEquals(b, (byte)45);
    }

    @Test
    public void testExecuteSliceFullValid() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation allocationInternal = allocation.asSlice(0, 1024);
        allocationInternal.execute(() -> {
            allocationInternal.set(JAVA_BYTE, 0, (byte)1);
            allocationInternal.set(JAVA_INT, 1, 1234);
            allocationInternal.set(JAVA_SHORT, 5, (short)2345);
            allocationInternal.set(JAVA_LONG, 7, 3456);
        });
        assert (allocation.get(JAVA_BYTE, 0) == (byte)1);
        assert (allocation.get(JAVA_INT, 1) == 1234);
        assert (allocation.get(JAVA_SHORT, 5) == (short)2345);
        assert (allocation.get(JAVA_LONG, 7) == 3456);
    }

    @Test
    public void testExecuteSliceFullSupplierValid() {
        heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation allocationInternal = allocation.asSlice(0, 1024);
        Allocation copy = allocationInternal.execute(() -> {
            allocationInternal.set(JAVA_BYTE, 0, (byte)1);
            allocationInternal.set(JAVA_INT, 1, 1234);
            allocationInternal.set(JAVA_SHORT, 5, (short)2345);
            allocationInternal.set(JAVA_LONG, 7, 3456);
            return heap.allocate(1024, ResourceScope.newConfinedScope(), (Allocation a)-> {
                a.copyFrom(allocationInternal);
            });
        });
        assert (allocation.get(JAVA_BYTE, 0) == (byte)1);
        assert (allocation.get(JAVA_INT, 1) == 1234);
        assert (allocation.get(JAVA_SHORT, 5) == (short)2345);
        assert (allocation.get(JAVA_LONG, 7) == 3456);
        assert (copy.get(JAVA_BYTE, 0) == (byte)1);
        assert (copy.get(JAVA_INT, 1) == 1234);
        assert (copy.get(JAVA_SHORT, 5) == (short)2345);
        assert (copy.get(JAVA_LONG, 7) == 3456);
    }

    @Test
    public void testExecuteSliceValid() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation slice = allocation.asSlice(100, 50);
        slice.execute(()-> {
            slice.set(JAVA_BYTE, 1, (byte)1);
            slice.set(JAVA_INT, 2, 1234);
            slice.set(JAVA_SHORT, 6, (short)2345);
            slice.set(JAVA_LONG, 8, 3456);
        });
		Assert.assertEquals(allocation.get(JAVA_BYTE, 101), (byte)1);
		Assert.assertEquals(allocation.get(JAVA_INT, 102), 1234);
        Assert.assertEquals(allocation.get(JAVA_SHORT, 106), (short)2345);
        Assert.assertEquals(allocation.get(JAVA_LONG, 108), 3456);
    }

    @Test
    public void testExecuteSliceSupplierValid() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation slice = allocation.asSlice(100, 50);
        Allocation copy = slice.execute(()-> {
            slice.set(JAVA_BYTE, 1, (byte)1);
            slice.set(JAVA_INT, 2, 1234);
            slice.set(JAVA_SHORT, 6, (short)2345);
            slice.set(JAVA_LONG, 8, 3456);
            return heap.allocate(50, ResourceScope.newConfinedScope(), (Allocation a) -> {
                a.copyFrom(slice);
            });
        });
		Assert.assertEquals(allocation.get(JAVA_BYTE, 101), (byte)1);
		Assert.assertEquals(allocation.get(JAVA_INT, 102), 1234);
        Assert.assertEquals(allocation.get(JAVA_SHORT, 106), (short)2345);
        Assert.assertEquals(allocation.get(JAVA_LONG, 108), 3456);
		Assert.assertEquals(copy.get(JAVA_BYTE, 1), (byte)1);
		Assert.assertEquals(copy.get(JAVA_INT, 2), 1234);
        Assert.assertEquals(copy.get(JAVA_SHORT, 6), (short)2345);
        Assert.assertEquals(copy.get(JAVA_LONG, 8), 3456);
    }

    @Test
    public void testExecuteSliceValidMultiple() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation slice = allocation.asSlice(100, 50);
        slice.execute(()-> {
            slice.set(JAVA_BYTE, 1, (byte)1);
            slice.set(JAVA_INT, 2, 1234);
            slice.set(JAVA_SHORT, 6, (short)2345);
            slice.set(JAVA_LONG, 8, 3456);
            allocation.set(JAVA_LONG, 0, 12345L);
        });
		Assert.assertEquals(allocation.get(JAVA_LONG, 0), 12345L);
		Assert.assertEquals(allocation.get(JAVA_BYTE, 101), (byte)1);
		Assert.assertEquals(allocation.get(JAVA_INT, 102), 1234);
        Assert.assertEquals(allocation.get(JAVA_SHORT, 106), (short)2345);
        Assert.assertEquals(allocation.get(JAVA_LONG, 108), 3456);
    }

    @Test
    public void testExecuteSliceValidSupplierMultiple() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        Allocation slice = allocation.asSlice(100, 50);
        Allocation copy = slice.execute(()-> {
            slice.set(JAVA_BYTE, 1, (byte)1);
            slice.set(JAVA_INT, 2, 1234);
            slice.set(JAVA_SHORT, 6, (short)2345);
            slice.set(JAVA_LONG, 8, 3456);
            allocation.set(JAVA_LONG, 0, 12345L);
            return heap.allocate(50, ResourceScope.newConfinedScope(), (Allocation a) -> {
                a.copyFrom(slice); 
            });
        });
		Assert.assertEquals(allocation.get(JAVA_LONG, 0), 12345L);
		Assert.assertEquals(allocation.get(JAVA_BYTE, 101), (byte)1);
		Assert.assertEquals(allocation.get(JAVA_INT, 102), 1234);
        Assert.assertEquals(allocation.get(JAVA_SHORT, 106), (short)2345);
        Assert.assertEquals(allocation.get(JAVA_LONG, 108), 3456);
		Assert.assertEquals(copy.get(JAVA_BYTE, 1), (byte)1);
		Assert.assertEquals(copy.get(JAVA_INT, 2), 1234);
        Assert.assertEquals(copy.get(JAVA_SHORT, 6), (short)2345);
        Assert.assertEquals(copy.get(JAVA_LONG, 8), 3456);
    }

    @Test
    public void testSliceInvalidRangeLength() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		try {
            Allocation slice = allocation.asSlice(0, 1025);
		    Assert.fail("IndexOutOfBoundsException wasn't thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
    }

    @Test
    public void testSliceNegativeRangeOffset() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		try {
            Allocation slice = allocation.asSlice(-1, 1024);
		    Assert.fail("IndexOutOfBoundsException wasn't thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
    }

    @Test
    public void testSliceNegativeRangeLength() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
		try {
            Allocation slice = allocation.asSlice(0, -1);
		    Assert.fail("IndexOutOfBoundsException wasn't thrown");
		} 
        catch (IndexOutOfBoundsException e) {
			assert true;
		}
    }
    
    @Test
    public void testToByteArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        allocation.fill((byte)45);
        byte[] arr = allocation.toArray(JAVA_BYTE);
        for (byte b : arr) Assert.assertEquals(b, (byte)45);
    }

    @Test
    public void testCopyToCharArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_CHAR.byteSize(); i++) {
            allocation.setAtIndex(JAVA_CHAR, i, 'x');
        }
        char[] arr = allocation.toArray(JAVA_CHAR);
        for (char c : arr) Assert.assertEquals(c, 'x');
    }

    @Test
    public void testCopyToShortArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_SHORT.byteSize(); i++) {
            allocation.setAtIndex(JAVA_SHORT, i, (short)44);
        }
        short[] arr = allocation.toArray(JAVA_SHORT);
        for (short s : arr) Assert.assertEquals(s, (short)44);
    }

    @Test
    public void testCopyToIntArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_INT.byteSize(); i++) {
            allocation.setAtIndex(JAVA_INT, i, 44);
        }
        int[] arr = allocation.toArray(JAVA_INT);
        for (int i : arr) Assert.assertEquals(i, 44);
    }

    @Test
    public void testCopyToIntArrayV2() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_INT.byteSize(); i++) {
            allocation.setAtIndex(JAVA_INT, i, 44);
        }
        int numElements = (int) (allocation.byteSize() / JAVA_INT.byteSize());
        int[] arr = new int[numElements]; 
        Allocation.copy(allocation, JAVA_INT, 0, MemorySegment.ofArray(arr), JAVA_INT, 0, numElements);
        for (int i : arr) Assert.assertEquals(i, 44);
    }

    @Test
    public void testCopyToFloatArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_FLOAT.byteSize(); i++) {
            allocation.setAtIndex(JAVA_FLOAT, i, 4.4F);
        }
        float[] arr = allocation.toArray(JAVA_FLOAT);
        for (float f : arr) Assert.assertEquals(f, 4.4F);
    }

    @Test
    public void testCopyToDoubleArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_DOUBLE.byteSize(); i++) {
            allocation.setAtIndex(JAVA_DOUBLE, i, 44.4D);
        }
        double[] arr = allocation.toArray(JAVA_DOUBLE);
        for (double d : arr) Assert.assertEquals(d, 44.4D);
    }

    @Test
    public void testCopyToLongArray() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_LONG.byteSize(); i++) {
            allocation.setAtIndex(JAVA_LONG, i, 123456789L);
        }
        long[] arr = allocation.toArray(JAVA_LONG);
        for (long l : arr) Assert.assertEquals(l, 123456789L);
    }

    @Test
    public void testCopyFromByteArray() {
		heap = TestVars.createTransactionalHeap();
        int numElements = 1024;
        byte[] arr = new byte[numElements]; 
        Arrays.fill(arr, (byte)123);
        allocation = heap.allocate(numElements * JAVA_BYTE.byteSize(), ResourceScope.newConfinedScope());
        Allocation.copy(MemorySegment.ofArray(arr), JAVA_BYTE, 0, allocation, JAVA_BYTE, 0, numElements);
        for (byte b : arr) Assert.assertEquals(b, (byte)123);
    }

    @Test
    public void testCopyFromShortArray() {
		heap = TestVars.createVolatileHeap();
        int numElements = 512;
        short[] arr = new short[numElements]; 
        Arrays.fill(arr, (short)123);
        allocation = heap.allocate(numElements * JAVA_SHORT.byteSize(), ResourceScope.newConfinedScope());
        Allocation.copy(MemorySegment.ofArray(arr), JAVA_SHORT, 0, allocation, JAVA_SHORT, 0, numElements);
        for (short s : arr) Assert.assertEquals(s, (short)123);
    }

    @Test
    public void testCopyFromIntArray() {
		heap = TestVars.createTransactionalHeap();
        int numElements = 256;
        int[] arr = new int[numElements]; 
        Arrays.fill(arr, 123456);
        allocation = heap.allocate(numElements * JAVA_INT.byteSize(), ResourceScope.newConfinedScope());
        Allocation.copy(MemorySegment.ofArray(arr), JAVA_INT, 0, allocation, JAVA_INT, 0, numElements);
        for (int i : arr) Assert.assertEquals(i, 123456);
    }

    @Test
    public void testCopyFromLongArray() {
		heap = TestVars.createDurableHeap();
        int numElements = 128;
        long[] arr = new long[numElements]; 
        Arrays.fill(arr, 123456789L);
        allocation = heap.allocate(numElements * JAVA_LONG.byteSize(), ResourceScope.newConfinedScope());
        Allocation.copy(MemorySegment.ofArray(arr), JAVA_LONG, 0, allocation, JAVA_LONG, 0, numElements);
        for (long l : arr) Assert.assertEquals(l, 123456789L);
    }
 
    @Test
    public void testCopyToLongArrayV2() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        for (int i = 0; i < 1024 / JAVA_LONG.byteSize(); i++) {
            allocation.setAtIndex(JAVA_LONG, i, 123456789L);
        }
        int numElements = (int) (allocation.byteSize() / JAVA_LONG.byteSize());
        long[] arr = new long[numElements]; 
        Allocation.copy(allocation, JAVA_LONG, 0, MemorySegment.ofArray(arr), JAVA_LONG, 0, numElements);
        for (long l : arr) Assert.assertEquals(l, 123456789L);
    }
    
    @Test
    public void testCopyShortElementsV() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_SHORT.byteSize();
        for(int i = 0; i < numElements / 2; i++) {
            allocation.setAtIndex(JAVA_SHORT, i, (short)i);
        }
        Allocation.copy(allocation, JAVA_SHORT, 0, allocation, JAVA_SHORT, allocation.byteSize() / 2, numElements / 2);
        for(int i = 512; i < numElements; i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_SHORT,i), (short)i);
        }
    }

    @Test
    public void testCopyDoubleElementsD() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_DOUBLE.byteSize();
        for(int i = 0; i < numElements / 2; i++) {
            allocation.setAtIndex(JAVA_DOUBLE, i, i);
        }
        Allocation.copy(allocation, JAVA_DOUBLE, 0, allocation, JAVA_DOUBLE, allocation.byteSize() / 2, numElements / 2);
        for(int i = 512; i < numElements; i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_DOUBLE,i), i);
        }
    }

    @Test
    public void testCopyFloatElementsT() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(1024, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_FLOAT.byteSize();
        for(int i = 0; i < numElements / 2; i++) {
            allocation.setAtIndex(JAVA_FLOAT, i, i);
        }
        Allocation.copy(allocation, JAVA_FLOAT, 0, allocation, JAVA_FLOAT, allocation.byteSize() / 2, numElements / 2);
        for(int i = 512; i < numElements; i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_FLOAT,i), i);
        }
    }

    @Test
    public void testCopyShortElements() {
		heap = TestVars.createVolatileHeap();
        allocation = heap.allocate(512, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_SHORT.byteSize();
        for(int i = 0; i < numElements; i++) {
            allocation.setAtIndex(JAVA_SHORT, i, (short)i);
        }
        Allocation copy = heap.allocate(512, ResourceScope.newConfinedScope());
        Allocation.copy(allocation, JAVA_SHORT, 0, copy, JAVA_SHORT, 0, numElements);
        for(int i = 0; i < numElements; i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_SHORT,i), (short)i);
        }
        copy.free();
    }

    @Test
    public void testCopyIntElements() {
		heap = TestVars.createDurableHeap();
        allocation = heap.allocate(512, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_INT.byteSize();
        for(int i = 0; i < numElements; i++) {
            allocation.setAtIndex(JAVA_INT, i, i);
        }
        Allocation copy = heap.allocate(512, ResourceScope.newConfinedScope());
        Allocation.copy(allocation, JAVA_INT, 0, copy, JAVA_INT, 0, numElements);
        for(int i = 0; i < copy.byteSize() / JAVA_INT.byteSize(); i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_INT,i), i);
        }
        copy.free();
    }

    @Test
    public void testCopyLongElements() {
		heap = TestVars.createTransactionalHeap();
        allocation = heap.allocate(512, ResourceScope.newConfinedScope());
        long numElements = allocation.byteSize() / JAVA_LONG.byteSize();
        for(int i = 0; i < numElements; i++) {
            allocation.setAtIndex(JAVA_LONG, i, i);
        }
        Allocation copy = heap.allocate(512, ResourceScope.newConfinedScope());
        Allocation.copy(allocation, JAVA_LONG, 0, copy, JAVA_LONG, 0, numElements);
        for(int i = 0; i < numElements; i++) {
            Assert.assertEquals(allocation.getAtIndex(JAVA_LONG,i), i);
        }
        copy.free();
    }
}
