/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul.examples.intarray;

import com.intel.pmem.pmul.Accessor;
import com.intel.pmem.pmul.Allocation;
import com.intel.pmem.pmul.Heap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.incubator.foreign.ResourceScope;

public class IntArrayExample{
    public static void main(String[] args) throws IOException, InterruptedException {
        String heapName = "/mnt/mem/int_array_example";
        Path path = Path.of(heapName);
        Heap heap = Files.exists(path)
                               ? Heap.open(path)
                               : Heap.create(Heap.Kind.TRANSACTIONAL, path, 500_000_000L);

        if (heap.getRoot(ResourceScope.globalScope()) == null) {
            long size = 10;
            System.out.println("Creating New Array of size " + size);
            Accessor.execute(heap, ()-> {
                IntArray ia = new IntArray(heap, size);
                ia.set(5, 10);
                ia.set(7, 20);
                heap.setRoot(ia.getAllocation());
            });
        }
        else {
            Allocation rootAllocation = heap.getRoot(ResourceScope.globalScope());
            IntArray ia = new IntArray(heap, rootAllocation);
            System.out.println("Retrieved IntArray of size " + ia.size());
            for (int i = 0; i < ia.size(); i++) {
                int val = ia.get(i);
                System.out.println("IntArray[" + i + "] = " + val);
            }
        }
    }
}
