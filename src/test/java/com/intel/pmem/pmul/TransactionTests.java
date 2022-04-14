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
public class TransactionTests {
	PersistentHeap heap = null;

	@BeforeMethod
	public void initialize() {
		heap = null;
	}

	@SuppressWarnings("deprecation")
	@AfterMethod
	public void testCleanup() {
		if (heap != null) {
			heap.close();
        }
		if (TestVars.ISDAX) {
			TestVars.daxCleanUp();
		}
		else TestVars.cleanUp(TestVars.HEAP_USER_PATH + TestVars.HEAP_NAME);
	}


	@Test
	public void testStaticRunnable() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(segment);
            segment.set(JAVA_LONG, 4, 1000);
            segment.set(JAVA_INT, 0, 777);
        });
        assert(segment.get(JAVA_LONG, 4) == 1000);
        assert(segment.get(JAVA_INT, 0) == 777);
	}

	@Test
	public void testStaticRunnableAborts() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                segment.set(JAVA_LONG, 4, 1000);
                segment.set(JAVA_INT, 14, 777);
            });
		    Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 4) == 0);
            assert(segment.get(JAVA_SHORT, 14) == 0);
        }
	}

	@Test
	public void testStaticRunnableCommits() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 4, 1000);
                    segment.set(JAVA_INT, 14, 777);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_INT, 0, 777);
                }
            });
            assert(segment.get(JAVA_LONG, 4) == 1000);
            assert(segment.get(JAVA_INT, 0) == 777);
        } 
        catch (Exception e) {
		    Assert.fail("Transaction did not commit");
        }
	}

	@Test
	public void testStaticRunnableTransactional() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(segment);
            segment.set(JAVA_LONG, 4, 1000);
            segment.set(JAVA_INT, 0, 777);
        });
        assert(segment.get(JAVA_LONG, 4) == 1000);
        assert(segment.get(JAVA_INT, 0) == 777);
	}

	@Test
	public void testStaticUncaughtExceptionAbortsTransactional() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                segment.set(JAVA_LONG, 4, 1000);
                segment.set(JAVA_INT, 14, 777);
            });
		    Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 4) == 0);
            assert(segment.get(JAVA_SHORT, 14) == 0);
        }
	}

	@Test
	public void testStaticCaughtExceptionCommitsTransactional() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 4, 1000);
                    segment.set(JAVA_INT, 14, 777);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_INT, 0, 777);
                }
            });
            assert(segment.get(JAVA_LONG, 4) == 1000);
            assert(segment.get(JAVA_INT, 0) == 777);
        } 
        catch (Exception e) {
		    Assert.fail("Transaction did not commit");
        }
	}

	@Test
	public void testStaticSupplier() {
		heap = TestVars.createLLPersistentHeap();
        MemorySegment outerSegment = heap.transaction(() -> {
		    MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
            heap.addToTransaction(segment);
            segment.set(JAVA_LONG, 4, 1000);
            segment.set(JAVA_INT, 0, 777);
            return segment;
        });
        assert(outerSegment.get(JAVA_LONG, 4) == 1000);
        assert(outerSegment.get(JAVA_INT, 0) == 777);
	}

	@Test
	public void testStaticSupplierAborts() {
		heap = TestVars.createLLPersistentHeap();
        MemorySegment outerSegment = null;
        try {
            outerSegment = heap.transaction(() -> {
		        MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
                heap.addToTransaction(segment);
                segment.set(JAVA_LONG, 4, 1000);
                segment.set(JAVA_INT, 14, 777);
                return segment;
            });
		    Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(outerSegment == null);
        }
	}

	@Test
	public void testStaticSupplierCommits() {
		heap = TestVars.createLLPersistentHeap();
        try {
            MemorySegment outerSegment = heap.transaction(() -> {
		        MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 4, 1000);
                    segment.set(JAVA_INT, 14, 777);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_INT, 0, 777);
                }
                return segment;
            });
            assert(outerSegment.get(JAVA_LONG, 4) == 1000);
            assert(outerSegment.get(JAVA_INT, 0) == 777);
        } 
        catch (Exception e) {
		    Assert.fail("Transaction did not commit");
        }
	}

	@Test
	public void testStaticSupplierTransactional() {
		heap = TestVars.createLLPersistentHeap();
        MemorySegment outerSegment = heap.transaction(() -> {
		    MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
            segment.set(JAVA_LONG, 4, 1000);
            segment.set(JAVA_INT, 0, 777);
            return segment;
        });
        assert(outerSegment.get(JAVA_LONG, 4) == 1000);
        assert(outerSegment.get(JAVA_INT, 0) == 777);
	}

	@Test
	public void testStaticSupplierAbortsTransactional() {
		heap = TestVars.createLLPersistentHeap();
        MemorySegment outerSegment = null;
        try {
            outerSegment = heap.transaction(() -> {
		        MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
                segment.set(JAVA_LONG, 4, 1000);
                segment.set(JAVA_INT, 14, 777);
                return segment;
            });
		    Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert (outerSegment == null);
        }
	}

	@Test
	public void testStaticSupplierCommitsTransactional() {
		heap = TestVars.createLLPersistentHeap();
        try {
            MemorySegment outerSegment = heap.transaction(() -> {
		        MemorySegment segment = heap.allocateSegment(16, true, ResourceScope.newConfinedScope());
                try {
                    segment.set(JAVA_LONG, 4, 1000);
                    segment.set(JAVA_INT, 14, 777);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_INT, 0, 777);
                }
                return segment;
            });
            assert(outerSegment.get(JAVA_LONG, 4) == 1000);
            assert(outerSegment.get(JAVA_INT, 0) == 777);
        } 
        catch (Exception e) {
		    Assert.fail("Transaction did not commit");
        }
	}
	@Test
	public void testNestedStatic() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        heap.transaction(() -> {
            heap.addToTransaction(segment);
            segment.set(JAVA_LONG, 0, 12345L);
            heap.transaction(() -> {
                segment.set(JAVA_LONG, 8, 555);
            });
        });
        assert(segment.get(JAVA_LONG, 0) == 12345);
        assert(segment.get(JAVA_LONG, 8) == 555);
	}
    
    @Test
	public void testNestedStaticCommits() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 20, 12345L);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_LONG, 0, 12345L);
                }
                heap.transaction(() -> {
                    try { 
                        segment.set(JAVA_LONG, 28, 555);
                    } 
                    catch (IndexOutOfBoundsException e) {
                        segment.set(JAVA_LONG, 8, 555);
                    }
                });
            });
            assert(segment.get(JAVA_LONG, 0) == 12345);
            assert(segment.get(JAVA_LONG, 8) == 555);
        } 
        catch (Exception e) {
            Assert.fail("Transaction did not commit");
        }
	}
 
    @Test
	public void testNestedStaticCommitAbort() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 20, 12345L);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_LONG, 0, 12345L);
                }
                heap.transaction(() -> {
                    heap.allocateSegment(20 * 1024 * 1024, true, ResourceScope.newConfinedScope());
                });
            });
            Assert.fail("Exception not thrown");
        } 
        catch (Throwable e) {
            assert(segment.get(JAVA_LONG, 0) == 0);
        }
	}
 
    @Test
	public void testNestedStaticAborts() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment, 0, 40);
                segment.set(JAVA_LONG, 0, 12345L);
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 8, 555);
                });
            });
            Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_INT, 20) == 0);
            assert(segment.get(JAVA_LONG, 8) == 0);
        }
	}

	@Test
	public void testNestedStaticSeq1() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 20, 12345L);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_LONG, 0, 12345L);
                }
                heap.transaction(() -> {
                    try {
                        segment.set(JAVA_LONG, 28, 555);
                    } 
                    catch (IndexOutOfBoundsException e) {
                        segment.set(JAVA_LONG, 8, 555);
                    }
                });
                heap.transaction(() -> {
                    try {
                        segment.set(JAVA_LONG, 26, 678910L);
                    } 
                    catch (IndexOutOfBoundsException e) {
                        segment.set(JAVA_LONG, 16, 678910L);
                    }
                });
            });
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 0) == 12345L);
            assert(segment.get(JAVA_LONG, 8) == 555L);
            assert(segment.get(JAVA_LONG, 16) == 678910L);
        }
	}

	@Test
	public void testNestedStaticSeq2() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 20, 12345L);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_LONG, 0, 12345L);
                }
                heap.transaction(() -> {
                    try {
                        segment.set(JAVA_LONG, 28, 555);
                    } 
                    catch (IndexOutOfBoundsException e) {
                        segment.set(JAVA_LONG, 8, 555);
                    }
                });
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 26, 678910L);
                });
            });
            Assert.fail("Exception not thrown");
        }   
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 0) == 0);
            assert(segment.get(JAVA_LONG, 8) == 0);
            assert(segment.get(JAVA_LONG, 16) == 0);
        }
	}

	@Test
	public void testNestedStaticSeq3() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                try {
                    segment.set(JAVA_LONG, 20, 12345L);
                } 
                catch (IndexOutOfBoundsException e) {
                    segment.set(JAVA_LONG, 0, 12345L);
                }
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 28, 555);
                });
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 16, 678910L);
                });
            });
            Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 0) == 0);
            assert(segment.get(JAVA_LONG, 8) == 0);
            assert(segment.get(JAVA_LONG, 16) == 0);
        }
	}

	@Test
	public void testNestedStaticSeq4() {
		heap = TestVars.createLLPersistentHeap();
		MemorySegment segment = heap.allocateSegment(24, true, ResourceScope.newConfinedScope());
        try {
            heap.transaction(() -> {
                heap.addToTransaction(segment);
                segment.set(JAVA_LONG, 20, 12345L);
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 8, 555);
                });
                heap.transaction(() -> {
                    segment.set(JAVA_LONG, 16, 678910L);
                });
            });
            Assert.fail("Exception not thrown");
        } 
        catch (IndexOutOfBoundsException e) {
            assert(segment.get(JAVA_LONG, 0) == 0);
            assert(segment.get(JAVA_LONG, 8) == 0);
            assert(segment.get(JAVA_LONG, 16) == 0);
        }
	}
}
