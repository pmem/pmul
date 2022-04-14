/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul.examples.linkedlist;

import com.intel.pmem.pmul.Accessor;
import com.intel.pmem.pmul.Allocation;
import com.intel.pmem.pmul.Heap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ResourceScope;

public class LinkedListExample {	
	public static void main(String[] args) throws Exception {
		int N = 100000;
        String[] kinds = new String[]{"V", "D", "T"};
		long heapSize =(long)( N * 1.5 * 128);
		for (String heapKind : kinds) {
			message("------- kind = %s", heapKind);

			Path path = Path.of("/mnt/mem/"+ heapKind +"_listheap");
			Heap heap = switch(heapKind) {
				case "V" -> Heap.create(Heap.Kind.VOLATILE, path.getParent(), heapSize);
				case "D" -> Files.exists(path) ? Heap.open(path) : Heap.create(Heap.Kind.DURABLE, path, heapSize);
				case "T" -> Files.exists(path) ? Heap.open(path) : Heap.create(Heap.Kind.TRANSACTIONAL, path, heapSize);
				default -> throw new RuntimeException("Unsupported heap kind: " + heapKind);
			};
			boolean firstRun = (heap.getRoot(ResourceScope.globalScope()) == null);
			LinkedList list1 = null;
			long start = System.nanoTime();
			if (firstRun) {
				message("write run");
				list1 = Accessor.execute(heap, () -> {
					LinkedList list = LinkedList.create(heap);
					heap.setRoot(list.getAllocation());
					for (int i = 1; i <= N; i++) {
						list.insert(i);
					}
					return list;
				});
				if (N <= 10) message("wrote list %s", list1);
				long sum = 0;
				for (int value : list1) {
					sum += value;
				}
				message("list sum = %,d", sum);
			}
			else {
				Allocation listAllocation = heap.getRoot(ResourceScope.globalScope());
				LinkedList list = LinkedList.of(heap, listAllocation);
				long sum = 0;
				for (int value : list) {
					sum += value;
				}
				message("list sum = %,d", sum);
				heap.setRoot(null);
			}
			double duration = (double)(System.nanoTime() - start) * 1.e-9d;
			if (N > 10) message("mode = %s, %s %,d element list in %,.2f sec., rate = %,.2f ops/sec.", heapKind, firstRun ? "wrote" : "read", N, duration, N / duration);
		}
	}

    private static void message(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

}
