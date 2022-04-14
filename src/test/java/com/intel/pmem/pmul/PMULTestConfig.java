/*
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 */

package com.intel.pmem.pmul;

import java.io.File;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class PMULTestConfig {

	@BeforeSuite
	public void testBeforeSuite() {
		File file = null;
        try {
		    TestVars.HEAP_USER_PATH = System.getProperty("test.heap.path");
			if(TestVars.HEAP_USER_PATH == null) {
                throw new SkipException("ERROR: Set property test.heap.path in order to run tests. Skipping tests...");
			}
            try {
                System.loadLibrary("pmul");
                System.loadLibrary("pmemobj");
                System.loadLibrary("pmem");
                System.loadLibrary("pmempool");
                System.loadLibrary("memkind");
            } catch (UnsatisfiedLinkError err) {
                throw new SkipException(err.getMessage()+"\nEnsure that the environment variable, LD_LIBRARY_PATH, contains the path to the missing native library.");
            }
        	if (TestVars.HEAP_USER_PATH.startsWith("/dev/dax")) {
		    	TestVars.ISDAX = true;
				System.out.println("ISDAX = " + TestVars.ISDAX);
				TestVars.HEAP_NAME = "";
				System.out.println("Path: " + TestVars.HEAP_USER_PATH);
			}
			else {
				if(!TestVars.HEAP_USER_PATH.endsWith(File.separator))
					TestVars.HEAP_USER_PATH += File.separator;
				file = new File(TestVars.HEAP_USER_PATH);
				if(!file.exists()) {
					throw new SkipException(TestVars.HEAP_USER_PATH + " doesn't exist. Skipping tests...");
				}
				long totalSize = file.getTotalSpace();
				if(totalSize < (1L * 1024 * 1024 * 1024)) {
					throw new SkipException("Available space is : " + totalSize + " It is not sufficient to run tests. Minimum 1GB is required. Skipping tests...");
				}

				System.out.println("Path: " + TestVars.HEAP_USER_PATH);
				System.out.println("Size: " + totalSize + " bytes");

				TestVars.TOTAL_SIZE = totalSize;
				TestVars.MULTIPLE_HEAP_SIZE = TestVars.TOTAL_SIZE / (2 * 3);
				TestVars.HEAP_SIZE_3G = TestVars.TOTAL_SIZE / 4;
				TestVars.MEMORY_BLOCK_SIZE_2G = TestVars.TOTAL_SIZE / 5;
				TestVars.INVALID_LARGE_MEM_BLOCK_SIZE = TestVars.TOTAL_SIZE + 3L * 1024 * 1024 * 1024;
			}
        } 
        catch(Exception e) {
            System.out.println(e.getMessage());
            throw new SkipException(e.getMessage());
        }
	}
}
