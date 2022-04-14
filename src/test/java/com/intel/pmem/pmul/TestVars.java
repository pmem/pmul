/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import com.intel.pmem.pmul.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.testng.Assert;
import static java.nio.file.StandardCopyOption.*;

public class TestVars {
    public static String HEAP_USER_PATH;
	public static long TOTAL_SIZE;
    public static long MULTIPLE_HEAP_SIZE;
    public static long HEAP_SIZE_3G;
    public static long MEMORY_BLOCK_SIZE_2G;
    public static long INVALID_LARGE_MEM_BLOCK_SIZE;
	public static boolean ISDAX;

	public static final int NUM_HEAPS = 10;
	public static final int NUM_MIXED_HEAPS = 3;
	public static String HEAP_NAME = "custom";
	public static final long HEAP_SIZE = 20 * 1024 * 1024;
	public static final String INVALID_HEAP_PATH = "INVALID";
	public static final String POOL_SET_FILE = "poolset.txt";

	public static final long SMALL_HEAP_SIZE = 5 * 1024 * 1024;
	public static final long NEGATIVE_HEAP_SIZE = 10L * 1024 * 1024 * -1;
	public static final long MEMORY_BLOCK_SIZE = 1024;
	public static final long MEMORY_BLOCK_SIZE_100MB = 100L * 1024 * 1024;
	public static final long NEGATIVE_MEMORY_BLOCK_SIZE = 1024 * (-1);
	public static final boolean NON_TRANSACTIONAL = false;
	public static final boolean TRANSACTIONAL = true;
	public static final int MEM_BLOCK_OFFSET = 12;
	public static final int MEM_BLOCK_WRITE_DATA = 12345;
	public static final short SHORT_DATA = 2;
	public static final int INT_DATA = 4;
	public static final long LONG_DATA = 6L;
	public static final byte BYTE_DATA = -1;
	public static final String BLOCK_HANDLE_FILE = "block_handle.txt";
	public static final int HEAP_ROOT_DATA = 100;

    // volatile heaps
	public static VolatileHeap createLLVolatileHeap() {
		return createLLVolatileHeap(HEAP_SIZE); 
	}

	public static VolatileHeap createLLVolatileHeap(long size) {
        VolatileHeap heap = null;
        try {
            Path path = Path.of(HEAP_USER_PATH);
            heap = VolatileHeap.create(path, size); 
        } catch (IOException e) {
            Assert.fail();
        }
        return heap;
	}

    // persistent heaps
	public static PersistentHeap createLLPersistentHeap() {
		return createLLPersistentHeap(HEAP_SIZE);
	}

	public static PersistentHeap createLLPersistentHeap(long size) {
        PersistentHeap heap = null;
        try {
            if (ISDAX) return PersistentHeap.create(Path.of(HEAP_USER_PATH));
            Path path = Path.of(HEAP_USER_PATH + HEAP_NAME);
            heap = PersistentHeap.create(path, size);
        } catch (IOException e) {
            Assert.fail();
        }
        return heap;
	}

    // transactional heaps
	public static Heap createTransactionalHeap() {
		return createTransactionalHeap(HEAP_SIZE);
	}

	public static Heap createTransactionalHeap(long size) {
        Heap heap = null;
        try {
            if (ISDAX) return Heap.create(Heap.Kind.TRANSACTIONAL, Path.of(HEAP_USER_PATH));
            Path path = Path.of(HEAP_USER_PATH + HEAP_NAME);
            heap =  Heap.create(Heap.Kind.TRANSACTIONAL, path, size);
        } catch (IOException e) {
            Assert.fail();
        }
        return heap;
	}

    // durable heaps
    public static Heap createDurableHeap() {
        return createDurableHeap(HEAP_SIZE);
    }

	public static Heap createDurableHeap(long size) {
        Heap heap = null;
        try {
            if (ISDAX) return Heap.create(Heap.Kind.DURABLE, Path.of(HEAP_USER_PATH));
            Path path = Path.of(HEAP_USER_PATH + HEAP_NAME);
            heap = Heap.create(Heap.Kind.DURABLE, path, size);
        } catch (IOException e) {
            Assert.fail();
        }
        return heap;
	}

    // HL volatile heap
	public static Heap createVolatileHeap() {
		//if (ISDAX) return TransactionalHeap.createHeap(HEAP_USER_PATH);		
		Heap heap = null;
		try {
            Path path = Path.of(HEAP_USER_PATH);
            heap = Heap.create(Heap.Kind.VOLATILE, path, HEAP_SIZE);
        } catch (IOException e) {
            Assert.fail();
        }
        return heap;
	}

	public static Heap createVolatileHeap(long size) throws IOException {
		//if (ISDAX) return TransactionalHeap.createHeap(HEAP_USER_PATH);		
        Path path = Path.of(HEAP_USER_PATH);
		return Heap.create(Heap.Kind.VOLATILE, path, size);
	}

	public static boolean createFolder(String path) {
		boolean ret = false;
		File file = new File(path);
		if (file.exists()) {
			return ret;
		}
		ret = file.mkdir();
		if (!ret)
			System.out.println("could not create directory");
		return ret;
	}

    public static boolean copyFile(String srcPath, String destPath){
        boolean ret = false;
        Path newPath = null;
        try {
            newPath = Files.copy(Paths.get(srcPath), Paths.get(destPath), REPLACE_EXISTING);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        ret = Files.exists(newPath);
        if (!ret)
            System.out.println("could not copy file");
        return ret;
    }

	public static boolean createFile(String path) {
		boolean ret = false;
		File file = new File(path);
		if (file.exists()) {
			return ret;
		}
		try {
			ret = file.createNewFile();
		} 
        catch (IOException e) {
			e.printStackTrace();
		}
		if (!ret)
			System.out.println("could not create file");
		return ret;
	}

	public static boolean daxCleanUp() {
		return (PersistentHeap.removePool(HEAP_USER_PATH) == 0) ? true : false;
	}

	public static boolean cleanUp(String path) {
		boolean ret = false;
		Path pathToBeDeleted = new File(path).toPath();
		if (Files.exists(pathToBeDeleted)) {
			try {
				Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			} 
            catch (IOException e) {
			    throw new RuntimeException("An Error occured during files cleanup");	
			}
		}
		if (!Files.exists(pathToBeDeleted))
			ret = true;
		return ret;
	}
}
