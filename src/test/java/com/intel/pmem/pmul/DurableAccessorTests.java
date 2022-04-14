/*
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 */

package com.intel.pmem.pmul;

import java.io.IOException;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;
import java.util.stream.StreamSupport;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.ValueLayout;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static jdk.incubator.foreign.ValueLayout.*;

@Test(singleThreaded = true)
public class DurableAccessorTests {
	Heap heap = null;
    Allocation allocation = null;
    Allocation l0Slice = null;
    Allocation l1Slice = null;
    Allocation l2Slice = null;

	static final MemoryLayout typesLayout = MemoryLayout.structLayout(
			ValueLayout.JAVA_BOOLEAN.withName("booleans"),
			ValueLayout.JAVA_BYTE.withName("bytes"),
			ValueLayout.JAVA_SHORT.withName("shorts"),
			ValueLayout.JAVA_CHAR.withName("chars"),
			ValueLayout.JAVA_INT.withName("ints"),
			ValueLayout.JAVA_FLOAT.withName("floats"),
			ValueLayout.JAVA_DOUBLE.withName("doubles"),
			ValueLayout.JAVA_LONG.withName("longs"),
			ValueLayout.ADDRESS.withName("addresses"),
			ValueLayout.ADDRESS.withName("references")
	);
    static final MemoryLayout level1 = MemoryLayout.sequenceLayout(4, typesLayout);
    static final MemoryLayout level2 = MemoryLayout.sequenceLayout(4, level1);
    static final MemoryLayout level3 = MemoryLayout.sequenceLayout(4, level2);

    final Accessor bytesL0 = Accessor.of(typesLayout, groupElement("bytes"));
    final Accessor bytesL1 = Accessor.of(level1, sequenceElement(), groupElement("bytes"));
    final Accessor bytesL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("bytes"));
    final Accessor bytesL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("bytes"));

    final Accessor boolL0 = Accessor.of(typesLayout, groupElement("booleans"));
    final Accessor boolL1 = Accessor.of(level1, sequenceElement(), groupElement("booleans"));
    final Accessor boolL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("booleans"));
    final Accessor boolL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("booleans"));

    final Accessor shortL0 = Accessor.of(typesLayout, groupElement("shorts"));
    final Accessor shortL1 = Accessor.of(level1, sequenceElement(), groupElement("shorts"));
    final Accessor shortL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("shorts"));
    final Accessor shortL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("shorts"));

    final Accessor charL0 = Accessor.of(typesLayout, groupElement("chars"));
    final Accessor charL1 = Accessor.of(level1, sequenceElement(), groupElement("chars"));
    final Accessor charL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("chars"));
    final Accessor charL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("chars"));

    final Accessor intL0 = Accessor.of(typesLayout, groupElement("ints"));
    final Accessor intL1 = Accessor.of(level1, sequenceElement(), groupElement("ints"));
    final Accessor intL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("ints"));
    final Accessor intL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("ints"));

    final Accessor floatL0 = Accessor.of(typesLayout, groupElement("floats"));
    final Accessor floatL1 = Accessor.of(level1, sequenceElement(), groupElement("floats"));
    final Accessor floatL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("floats"));
    final Accessor floatL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("floats"));

    final Accessor doubleL0 = Accessor.of(typesLayout, groupElement("doubles"));
    final Accessor doubleL1 = Accessor.of(level1, sequenceElement(), groupElement("doubles"));
    final Accessor doubleL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("doubles"));
    final Accessor doubleL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("doubles"));

    final Accessor longL0 = Accessor.of(typesLayout, groupElement("longs"));
    final Accessor longL1 = Accessor.of(level1, sequenceElement(), groupElement("longs"));
    final Accessor longL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("longs"));
    final Accessor longL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("longs"));

    final Accessor addressL0 = Accessor.of(typesLayout, groupElement("addresses"));
    final Accessor addressL1 = Accessor.of(level1, sequenceElement(), groupElement("addresses"));
    final Accessor addressL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("addresses"));
    final Accessor addressL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("addresses"));

    final Accessor referenceL0 = Accessor.of(typesLayout, groupElement("references"));
    final Accessor referenceL1 = Accessor.of(level1, sequenceElement(), groupElement("references"));
    final Accessor referenceL2 = Accessor.of(level2, sequenceElement(), sequenceElement(), groupElement("references"));
    final Accessor referenceL3 = Accessor.of(level3, sequenceElement(), sequenceElement(), sequenceElement(), groupElement("references"));

    @BeforeClass
    public void initializeHeapAndAllocation () {
        heap = TestVars.createDurableHeap();
        allocation = heap.allocate(level3, ResourceScope.globalScope());
        l2Slice = allocation.asSlice(0, level2.byteSize());
        l1Slice = allocation.asSlice(0, level1.byteSize());
        l0Slice = allocation.asSlice(0, typesLayout.byteSize());
    }

	@BeforeMethod
	public void initialize() {
        allocation.fill((byte)0);
	}

	@SuppressWarnings("deprecation")
	@AfterClass
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

// Byte
	@Test
	public void testByteL0() {
		bytesL0.set(l0Slice, (byte)128);
		Assert.assertEquals(bytesL0.get(l0Slice), (byte)128);
	}

	@Test
	public void testByteL0WrongType() {
        try {
		    bytesL0.set(l0Slice, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL0TooManyCoordinates() {
        try {
		    bytesL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL1() {
		bytesL1.set(l1Slice, 0, (byte)128);
		Assert.assertEquals(bytesL1.get(l1Slice, 0), (byte)128);
	}

	@Test
	public void testByteL1WrongType() {
        try {
		    bytesL1.set(l1Slice, 1, (short)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL1BadCoordinates() {
        try {
		    bytesL1.set(l1Slice, 12, (byte)128);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL1TooManyCoordinates() {
        try {
		    bytesL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL1TooFewCoordinates() {
        try {
		    bytesL1.set(l1Slice, (byte)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }
	
    @Test
	public void testByteL2() {
		bytesL2.set(l2Slice, 0, 1, (byte)128);
		Assert.assertEquals(bytesL2.get(l2Slice, 0, 1), (byte)128);
	}

	@Test
	public void testByteL2WrongType() {
        try {
		    bytesL2.set(l2Slice, 1, 2, (int)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL2BadCoordinates() {
        try {
		    bytesL2.set(l2Slice, -1, 2, (byte)128);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL2TooManyCoordinates() {
        try {
		    bytesL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL2TooFewCoordinates() {
        try {
		    bytesL2.set(l2Slice, 2, (byte)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testByteL3() {
		bytesL3.set(allocation, 0, 1, 3, (byte)128);
		Assert.assertEquals(bytesL3.get(allocation, 0, 1, 3), (byte)128);
	}

	@Test
	public void testByteL3WrongType() {
        try {
		    bytesL3.set(allocation, 1, 2, 0, (long)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL3BadCoordinates() {
        try {
		    bytesL3.set(allocation, 33, 2, 1, (byte)128);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL3TooManyCoordinates() {
        try {
		    bytesL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testByteL3TooFewCoordinates() {
        try {
		    bytesL3.set(allocation, (byte)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Boolean
	@Test
	public void testBooleanL0() {
		boolL0.set(l0Slice, true);
		Assert.assertEquals(boolL0.get(l0Slice), true);
	}

	@Test
	public void testBooleanL0WrongType() {
        try {
		    boolL0.set(l0Slice, (byte)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL0TooManyCoordinates() {
        try {
		    boolL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL1() {
		boolL1.set(l1Slice, 0, true);
		Assert.assertEquals(boolL1.get(l1Slice, 0), true);
	}

	@Test
	public void testBooleanL1WrongType() {
        try {
		    boolL1.set(l1Slice, 1, (short)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL1BadCoordinates() {
        try {
		    boolL1.set(l1Slice, 12, true);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL1TooManyCoordinates() {
        try {
		    boolL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL1TooFewCoordinates() {
        try {
		    boolL1.set(l1Slice, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }
	
    @Test
	public void testBooleanL2() {
		boolL2.set(l2Slice, 0, 1, true);
		Assert.assertEquals(boolL2.get(l2Slice, 0, 1), true);
	}

	@Test
	public void testBooleanL2WrongType() {
        try {
		    boolL2.set(l2Slice, 1, 2, (int)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL2BadCoordinates() {
        try {
		    boolL2.set(l2Slice, -1, 2, true);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL2TooManyCoordinates() {
        try {
		    boolL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL2TooFewCoordinates() {
        try {
		    boolL2.set(l2Slice, 2, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testBooleanL3() {
		boolL3.set(allocation, 0, 1, 3, true);
		Assert.assertEquals(boolL3.get(allocation, 0, 1, 3), true);
	}

	@Test
	public void testBooleanL3WrongType() {
        try {
		    boolL3.set(allocation, 1, 2, 0, (long)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL3BadCoordinates() {
        try {
		    boolL3.set(allocation, 33, 2, 1, true);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL3TooManyCoordinates() {
        try {
		    boolL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testBooleanL3TooFewCoordinates() {
        try {
		    boolL3.set(allocation, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Short
	@Test
	public void testShortL0() {
		shortL0.set(l0Slice, (short)200);
		Assert.assertEquals(shortL0.get(l0Slice), (short)200);
	}

	@Test
	public void testShortL0WrongType() {
        try {
		    shortL0.set(l0Slice, 'r');
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL0TooManyCoordinates() {
        try {
		    shortL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL1() {
		shortL1.set(l1Slice, 0, (short)200);
		Assert.assertEquals(shortL1.get(l1Slice, 0), (short)200);
	}

	@Test
	public void testShortL1WrongType() {
        try {
		    shortL1.set(l1Slice, 1, 128F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL1BadCoordinates() {
        try {
		    shortL1.set(l1Slice, 12, (short)200);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL1TooManyCoordinates() {
        try {
		    shortL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL1TooFewCoordinates() {
        try {
		    shortL1.set(l1Slice, (short)200);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testShortL2() {
		shortL2.set(l2Slice, 0, 1, (short)200);
		Assert.assertEquals(shortL2.get(l2Slice, 0, 1), (short)200);
	}

	@Test
	public void testShortL2WrongType() {
        try {
		    shortL2.set(l2Slice, 1, 2, (int)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL2BadCoordinates() {
        try {
		    shortL2.set(l2Slice, -1, 2, (short)200);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL2TooManyCoordinates() {
        try {
		    shortL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL2TooFewCoordinates() {
        try {
		    shortL2.set(l2Slice, 2, (short)200);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testShortL3() {
		shortL3.set(allocation, 0, 1, 3, (short)200);
		Assert.assertEquals(shortL3.get(allocation, 0, 1, 3), (short)200);
	}

	@Test
	public void testShortL3WrongType() {
        try {
		    shortL3.set(allocation, 1, 2, 0, (long)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL3BadCoordinates() {
        try {
		    shortL3.set(allocation, 33, 2, 1, (short)200);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL3TooManyCoordinates() {
        try {
		    shortL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testShortL3TooFewCoordinates() {
        try {
		    shortL3.set(allocation, (short)200);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Char 
	@Test
	public void testCharL0() {
		charL0.set(l0Slice, 'x');
		Assert.assertEquals(charL0.get(l0Slice), 'x');
	}

	@Test
	public void testCharL0WrongType() {
        try {
		    charL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL0TooManyCoordinates() {
        try {
		    charL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL1() {
		charL1.set(l1Slice, 0, 'x');
		Assert.assertEquals(charL1.get(l1Slice, 0), 'x');
	}

	@Test
	public void testCharL1WrongType() {
        try {
		    charL1.set(l1Slice, 1, 128F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL1BadCoordinates() {
        try {
		    charL1.set(l1Slice, 12, 'x');
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL1TooManyCoordinates() {
        try {
		    charL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL1TooFewCoordinates() {
        try {
		    charL1.set(l1Slice, 'x');
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testCharL2() {
		charL2.set(l2Slice, 0, 1, 'x');
		Assert.assertEquals(charL2.get(l2Slice, 0, 1), 'x');
	}

	@Test
	public void testCharL2WrongType() {
        try {
		    charL2.set(l2Slice, 1, 2, (int)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL2BadCoordinates() {
        try {
		    charL2.set(l2Slice, -1, 2, 'x');
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL2TooManyCoordinates() {
        try {
		    charL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL2TooFewCoordinates() {
        try {
		    charL2.set(l2Slice, 2, 'x');
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testCharL3() {
		charL3.set(allocation, 0, 1, 3, 'x');
		Assert.assertEquals(charL3.get(allocation, 0, 1, 3), 'x');
	}

	@Test
	public void testCharL3WrongType() {
        try {
		    charL3.set(allocation, 1, 2, 0, (long)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL3BadCoordinates() {
        try {
		    charL3.set(allocation, 33, 2, 1, 'x');
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL3TooManyCoordinates() {
        try {
		    charL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testCharL3TooFewCoordinates() {
        try {
		    charL3.set(allocation, 'x');
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Int
	@Test
	public void testIntL0() {
		intL0.set(l0Slice, 12345);
		Assert.assertEquals(intL0.get(l0Slice), 12345);
	}

	@Test
	public void testIntL0WrongType() {
        try {
		    intL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL0TooManyCoordinates() {
        try {
		    intL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL1() {
		intL1.set(l1Slice, 0, 12345);
		Assert.assertEquals(intL1.get(l1Slice, 0), 12345);
	}

	@Test
	public void testIntL1WrongType() {
        try {
		    intL1.set(l1Slice, 1, 128F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL1BadCoordinates() {
        try {
		    intL1.set(l1Slice, 12, 12345);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL1TooManyCoordinates() {
        try {
		    intL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL1TooFewCoordinates() {
        try {
		    intL1.set(l1Slice, 12345);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testIntL2() {
		intL2.set(l2Slice, 0, 1, 12345);
		Assert.assertEquals(intL2.get(l2Slice, 0, 1), 12345);
	}

	@Test
	public void testIntL2WrongType() {
        try {
		    intL2.set(l2Slice, 1, 2, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL2BadCoordinates() {
        try {
		    intL2.set(l2Slice, -1, 2, 12345);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL2TooManyCoordinates() {
        try {
		    intL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL2TooFewCoordinates() {
        try {
		    intL2.set(l2Slice, 2, 12345);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testIntL3() {
		intL3.set(allocation, 0, 1, 3, 12345);
		Assert.assertEquals(intL3.get(allocation, 0, 1, 3), 12345);
	}

	@Test
	public void testIntL3WrongType() {
        try {
		    intL3.set(allocation, 1, 2, 0, (long)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL3BadCoordinates() {
        try {
		    intL3.set(allocation, 33, 2, 1, 12345);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL3TooManyCoordinates() {
        try {
		    intL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testIntL3TooFewCoordinates() {
        try {
		    intL3.set(allocation, 12345);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Float
	@Test
	public void testFloatL0() {
		floatL0.set(l0Slice, 6789F);
		Assert.assertEquals(floatL0.get(l0Slice), 6789F);
	}

	@Test
	public void testFloatL0WrongType() {
        try {
		    floatL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL0TooManyCoordinates() {
        try {
		    floatL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL1() {
		floatL1.set(l1Slice, 0, 6789F);
		Assert.assertEquals(floatL1.get(l1Slice, 0), 6789F);
	}

	@Test
	public void testFloatL1WrongType() {
        try {
		    floatL1.set(l1Slice, 1, 128D);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL1BadCoordinates() {
        try {
		    floatL1.set(l1Slice, 12, 6789F);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL1TooManyCoordinates() {
        try {
		    floatL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL1TooFewCoordinates() {
        try {
		    floatL1.set(l1Slice, 6789F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testFloatL2() {
		floatL2.set(l2Slice, 0, 1, 6789F);
		Assert.assertEquals(floatL2.get(l2Slice, 0, 1), 6789F);
	}

	@Test
	public void testFloatL2WrongType() {
        try {
		    floatL2.set(l2Slice, 1, 2, MemoryAddress.NULL);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL2BadCoordinates() {
        try {
		    floatL2.set(l2Slice, -1, 2, 6789F);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL2TooManyCoordinates() {
        try {
		    floatL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL2TooFewCoordinates() {
        try {
		    floatL2.set(l2Slice, 2, 6789F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testFloatL3() {
		floatL3.set(allocation, 0, 1, 3, 6789F);
		Assert.assertEquals(floatL3.get(allocation, 0, 1, 3), 6789F);
	}

	@Test
	public void testFloatL3WrongType() {
        try {
		    floatL3.set(allocation, 1, 2, 0, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL3BadCoordinates() {
        try {
		    floatL3.set(allocation, 33, 2, 1, 6789F);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL3TooManyCoordinates() {
        try {
		    floatL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testFloatL3TooFewCoordinates() {
        try {
		    floatL3.set(allocation, 6789F);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Double 
	@Test
	public void testDoubleL0() {
		doubleL0.set(l0Slice, 12345.6789D);
		Assert.assertEquals(doubleL0.get(l0Slice), 12345.6789D);
	}

	@Test
	public void testDoubleL0WrongType() {
        try {
		    doubleL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL0TooManyCoordinates() {
        try {
		    doubleL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL1() {
		doubleL1.set(l1Slice, 0, 12345.6789D);
		Assert.assertEquals(doubleL1.get(l1Slice, 0), 12345.6789D);
	}

	@Test
	public void testDoubleL1WrongType() {
        try {
		    doubleL1.set(l1Slice, 1, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL1BadCoordinates() {
        try {
		    doubleL1.set(l1Slice, 12, 12345.6789D);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL1TooManyCoordinates() {
        try {
		    doubleL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL1TooFewCoordinates() {
        try {
		    doubleL1.set(l1Slice, 12345.6789D);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testDoubleL2() {
		doubleL2.set(l2Slice, 0, 1, 12345.6789D);
		Assert.assertEquals(doubleL2.get(l2Slice, 0, 1), 12345.6789D);
	}

	@Test
	public void testDoubleL2WrongType() {
        try {
		    doubleL2.set(l2Slice, 1, 2, MemoryAddress.NULL);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL2BadCoordinates() {
        try {
		    doubleL2.set(l2Slice, -1, 2, 12345.6789D);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL2TooManyCoordinates() {
        try {
		    doubleL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL2TooFewCoordinates() {
        try {
		    doubleL2.set(l2Slice, 2, 12345.6789D);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testDoubleL3() {
		doubleL3.set(allocation, 0, 1, 3, 12345.6789D);
		Assert.assertEquals(doubleL3.get(allocation, 0, 1, 3), 12345.6789D);
	}

	@Test
	public void testDoubleL3WrongType() {
        try {
		    doubleL3.set(allocation, 1, 2, 0, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL3BadCoordinates() {
        try {
		    doubleL3.set(allocation, 33, 2, 1, 12345.6789D);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL3TooManyCoordinates() {
        try {
		    doubleL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testDoubleL3TooFewCoordinates() {
        try {
		    doubleL3.set(allocation, 12345.6789D);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Long
	@Test
	public void testLongL0() {
		longL0.set(l0Slice, 987654321L);
		Assert.assertEquals(longL0.get(l0Slice), 987654321L);
	}

	@Test
	public void testLongL0WrongType() {
        try {
		    longL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL0TooManyCoordinates() {
        try {
		    longL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL1() {
		longL1.set(l1Slice, 0, 987654321L);
		Assert.assertEquals(longL1.get(l1Slice, 0), 987654321L);
	}

	@Test
	public void testLongL1WrongType() {
        try {
		    longL1.set(l1Slice, 1, true);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL1BadCoordinates() {
        try {
		    longL1.set(l1Slice, 12, 987654321L);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL1TooManyCoordinates() {
        try {
		    longL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL1TooFewCoordinates() {
        try {
		    longL1.set(l1Slice, 987654321L);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testLongL2() {
		longL2.set(l2Slice, 0, 1, 987654321L);
		Assert.assertEquals(longL2.get(l2Slice, 0, 1), 987654321L);
	}

	@Test
	public void testLongL2WrongType() {
        try {
		    longL2.set(l2Slice, 1, 2, MemoryAddress.NULL);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL2BadCoordinates() {
        try {
		    longL2.set(l2Slice, -1, 2, 987654321L);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL2TooManyCoordinates() {
        try {
		    longL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL2TooFewCoordinates() {
        try {
		    longL2.set(l2Slice, 2, 987654321L);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testLongL3() {
		longL3.set(allocation, 0, 1, 3, 987654321L);
		Assert.assertEquals(longL3.get(allocation, 0, 1, 3), 987654321L);
	}

	@Test
	public void testLongL3WrongType() {
        try {
		    longL3.set(allocation, 1, 2, 0, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL3BadCoordinates() {
        try {
		    longL3.set(allocation, 33, 2, 1, 987654321L);
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL3TooManyCoordinates() {
        try {
		    longL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testLongL3TooFewCoordinates() {
        try {
		    longL3.set(allocation, 987654321L);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Address
	@Test
	public void testAddressL0() {
		addressL0.set(l0Slice, MemoryAddress.ofLong(987654321));
		Assert.assertEquals(addressL0.get(l0Slice), MemoryAddress.ofLong(987654321));
	}

	@Test
	public void testAddressL0WrongType() {
        try {
		    addressL0.set(l0Slice, false);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL0TooManyCoordinates() {
        try {
		    addressL0.get(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL1() {
		addressL1.set(l1Slice, 0, MemoryAddress.ofLong(987654321));
		Assert.assertEquals(addressL1.get(l1Slice, 0), MemoryAddress.ofLong(987654321));
	}

	@Test
	public void testAddressL1WrongType() {
        try {
		    addressL1.set(l1Slice, 1, (byte)128);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL1BadCoordinates() {
        try {
		    addressL1.set(l1Slice, 12, MemoryAddress.ofLong(987654321));
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL1TooManyCoordinates() {
        try {
		    addressL1.get(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL1TooFewCoordinates() {
        try {
		    addressL1.set(l1Slice, MemoryAddress.ofLong(987654321));
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testAddressL2() {
		addressL2.set(l2Slice, 0, 1, MemoryAddress.ofLong(987654321));
		Assert.assertEquals(addressL2.get(l2Slice, 0, 1), MemoryAddress.ofLong(987654321));
	}

	@Test
	public void testAddressL2WrongType() {
        try {
		    addressL2.set(l2Slice, 1, 2, 1234);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL2BadCoordinates() {
        try {
		    addressL2.set(l2Slice, -1, 2, MemoryAddress.ofLong(987654321));
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL2TooManyCoordinates() {
        try {
		    addressL2.get(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL2TooFewCoordinates() {
        try {
		    addressL2.set(l2Slice, 2, MemoryAddress.ofLong(987654321));
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testAddressL3() {
		addressL3.set(allocation, 0, 1, 3, MemoryAddress.ofLong(987654321));
		Assert.assertEquals(addressL3.get(allocation, 0, 1, 3), MemoryAddress.ofLong(987654321));
	}

	@Test
	public void testAddressL3WrongType() {
        try {
		    addressL3.set(allocation, 1, 2, 0, allocation.address());
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL3BadCoordinates() {
        try {
		    addressL3.set(allocation, 33, 2, 1, MemoryAddress.ofLong(987654321));
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL3TooManyCoordinates() {
        try {
		    addressL3.get(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testAddressL3TooFewCoordinates() {
        try {
		    addressL3.set(allocation, MemoryAddress.ofLong(987654321));
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Reference
	@Test
	public void testReferenceL0() {
		referenceL0.setReference(l0Slice, allocation.address());
		Assert.assertEquals(referenceL0.getReference(l0Slice), allocation.address());
	}

	@Test
	public void testReferenceL0WrongType() {
        try {
		    referenceL0.setReference(l0Slice, false);
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

	@Test
	public void testReferenceL0WrongCall() {
        try {
		    referenceL0.set(l0Slice, allocation.address());
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}


    @Test
	public void testReferenceL0TooManyCoordinates() {
        try {
		    referenceL0.getReference(l0Slice, 12);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL1() {
		referenceL1.setReference(l1Slice, 0, allocation.address());
		Assert.assertEquals(referenceL1.getReference(l1Slice, 0), allocation.address());
	}

	@Test
	public void testReferenceL1WrongType() {
        try {
		    referenceL1.setReference(l1Slice, 1, (byte)128);
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    /*@Test
	public void testReferenceL1WrongCall() {
        // try {
		    referenceL1.get(l1Slice, 3);
        //     Assert.fail("WrongMethodTypeException was not thrown");
        // } catch (WrongMethodTypeException e) {
        //     Assert.assertTrue(true);
        //}
	}*/

    @Test
	public void testReferenceL1BadCoordinates() {
        try {
		    referenceL1.setReference(l1Slice, 12, allocation.address());
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL1TooManyCoordinates() {
        try {
		    referenceL1.getReference(l1Slice, 1, 0);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL1TooFewCoordinates() {
        try {
		    referenceL1.setReference(l1Slice, allocation.address());
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testReferenceL2() {
		referenceL2.setReference(l2Slice, 0, 1, allocation.address());
		Assert.assertEquals(referenceL2.getReference(l2Slice, 0, 1), allocation.address());
	}

	@Test
	public void testReferenceL2WrongType() {
        try {
		    referenceL2.setReference(l2Slice, 1, 2, 1234);
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    //TODO cannot distinguish wrong getReference from get
    /*@Test
	public void testReferenceL2WrongCall() {
        //try {
		    referenceL2.get(l2Slice, 0, 3);
         //   Assert.fail("WrongMethodTypeException was not thrown");
        //} catch (WrongMethodTypeException e) {
        //    Assert.assertTrue(true);
        //}
	}*/

    @Test
	public void testReferenceL2BadCoordinates() {
        try {
		    referenceL2.setReference(l2Slice, -1, 2, allocation.address());
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL2TooManyCoordinates() {
        try {
		    referenceL2.getReference(l2Slice, 1, 0, 3);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL2TooFewCoordinates() {
        try {
		    referenceL2.setReference(l2Slice, 2, allocation.address());
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
	public void testReferenceL3() {
		referenceL3.setReference(allocation, 0, 1, 3, allocation.address());
		Assert.assertEquals(referenceL3.getReference(allocation, 0, 1, 3), allocation.address());
	}

	@Test
	public void testReferenceL3WrongType() {
        try {
		    referenceL3.setReference(allocation, 1, 2, 0, 12345L);
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL3WrongCall() {
        try {
		    referenceL3.set(allocation, 1, 2, 3, allocation.address());
            Assert.fail("ClassCastException was not thrown");
        } catch (ClassCastException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL3BadCoordinates() {
        try {
		    referenceL3.setReference(allocation, 33, 2, 1, allocation.address());
            Assert.fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL3TooManyCoordinates() {
        try {
		    referenceL3.getReference(allocation, 1, 0, 3, 1);
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
	}

    @Test
	public void testReferenceL3TooFewCoordinates() {
        try {
		    referenceL3.setReference(allocation, allocation.address());
            Assert.fail("WrongMethodTypeException was not thrown");
        } catch (WrongMethodTypeException e) {
            Assert.assertTrue(true);
        }
    }

// Execute
    @Test
    public void testExecuteValid() {
        Accessor.execute(heap, () -> {
            bytesL0.set(l0Slice, (byte)1);
            intL1.set(l1Slice, 1, 1234);
            shortL2.set(l2Slice, 2, 2, (short)2345);
            longL3.set(allocation, 0, 0, 0, 3456L);
        });
        Assert.assertEquals(bytesL0.get(l0Slice), (byte)1);
        Assert.assertEquals(intL1.get(l1Slice, 1), 1234);
        Assert.assertEquals(shortL2.get(l2Slice, 2, 2), (short)2345);
        Assert.assertEquals(longL3.get(allocation, 0,0,0), 3456L);
    }

    @Test
    public void testExecuteValid2() {
        Accessor.execute(heap, () -> {
            boolL3.set(allocation, 2, 3, 1, true);
            floatL2.set(l2Slice, 1, 3, 1234F);
            charL1.set(l1Slice, 2, 'z');
            doubleL0.set(l0Slice, 3456D);
        });
        Assert.assertEquals(boolL3.get(allocation, 2, 3, 1), true);
        Assert.assertEquals(floatL2.get(l2Slice, 1, 3), 1234F);
        Assert.assertEquals(charL1.get(l1Slice, 2), 'z');
        Assert.assertEquals(doubleL0.get(l0Slice), 3456D);
    }

    @Test
    public void testExecuteValid3() {
        Accessor.execute(heap, () -> {
            addressL1.set(l1Slice, 3, MemoryAddress.ofLong(123456789L));
            addressL3.set(allocation, 0, 1, 3, MemoryAddress.ofLong(987654321L));
            referenceL0.setReference(l0Slice, allocation.address());
            referenceL3.setReference(allocation, 0, 1, 3, allocation.address());
        });
        Assert.assertEquals(addressL1.get(l1Slice, 3), MemoryAddress.ofLong(123456789L));
        Assert.assertEquals(addressL3.get(allocation, 0, 1, 3), MemoryAddress.ofLong(987654321L));
        Assert.assertEquals(referenceL0.getReference(l0Slice), allocation.address());
        Assert.assertEquals(referenceL3.getReference(allocation, 0, 1, 3), allocation.address());
    }

    @Test
    public void testExecuteSupplierValid() {
        Allocation copy = Accessor.execute(heap, () -> {
            bytesL0.set(l0Slice, (byte)1);
            intL0.set(l0Slice, 1234);
            shortL0.set(l0Slice,(short)2345);
            longL0.set(l0Slice, 3456L);
            return heap.allocate(l0Slice.byteSize(), ResourceScope.newConfinedScope(), (Allocation a)-> {
                a.copyFrom(l0Slice);
            });
        });
        Assert.assertEquals(bytesL0.get(copy), (byte)1);
        Assert.assertEquals(intL0.get(copy), 1234);
        Assert.assertEquals(shortL0.get(copy), (short)2345);
        Assert.assertEquals(longL0.get(copy), 3456L);
        copy.free();
    }

    @Test
    public void testExecuteSupplierValid2() {
        Allocation copy = Accessor.execute(heap, () -> {
            addressL3.set(allocation, 0, 0, 0, MemoryAddress.ofLong(123456789L));
            addressL3.set(allocation, 1, 1, 1, MemoryAddress.ofLong(987654321L));
            referenceL3.setReference(allocation, 2, 2, 2, allocation.address());
            referenceL3.setReference(allocation, 3, 3, 3, allocation.address());
            return heap.allocate(allocation.byteSize(), ResourceScope.newConfinedScope(), (Allocation a)-> {
                a.copyFrom(allocation);
            });
        });
        Assert.assertEquals(addressL3.get(copy, 0, 0, 0), MemoryAddress.ofLong(123456789L));
        Assert.assertEquals(addressL3.get(copy, 1, 1, 1), MemoryAddress.ofLong(987654321L));
        Assert.assertEquals(referenceL3.getReference(copy, 2, 2, 2), allocation.address());
        Assert.assertEquals(referenceL3.getReference(copy, 3, 3, 3), allocation.address());
        copy.free();
    }
}
