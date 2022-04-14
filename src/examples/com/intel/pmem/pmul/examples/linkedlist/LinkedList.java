/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul.examples.linkedlist;

import com.intel.pmem.pmul.Accessor;
import com.intel.pmem.pmul.Allocation;
import com.intel.pmem.pmul.AllocationAddress;
import com.intel.pmem.pmul.Heap;
import java.util.Iterator;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;

public class LinkedList implements Iterable<Integer> {	
	// 	     root             head             node0                     
	// ---|0, headAddr|---|v0, nextAddr|---|v1, nextAddr|--- ... --- EMPTY

	private Heap heap;
	private Node root;
	private Node head;
    private ResourceScope scope;

	// create new
	public static LinkedList create(Heap heap) {
		Node head = null;
		LinkedList ans = new LinkedList(heap, head);
		return ans;
	}

	public void delete() {
		Accessor.execute(heap, () -> {
			while (head != null) {
				deleteHead();
			}
		});
	}

	// recreate from address
	public static LinkedList of(Heap heap, Allocation allocation) {
		return new LinkedList(heap, allocation);
	}

	// constructor
	private LinkedList(Heap heap, Node head) {
		this.heap = heap;
        scope = ResourceScope.newSharedScope();
		this.root = new Node(heap, 0, head, scope);
		this.head = head;
	}

	// reconstructor
	private LinkedList(Heap heap, Allocation rootAllocation) {
		this.heap = heap;
		root = new Node(heap, rootAllocation);
        scope = rootAllocation.scope();
		this.head = root.getNext();
	}

	public Iterator<Integer> iterator() {
		return new Iter(this);
	}

	private static class Iter implements Iterator<Integer> {
		private Node nextNode;

		Iter(LinkedList list) {
			this.nextNode = list.head;
		}

		public boolean hasNext() {
			return nextNode != null;
		}

		public Integer next() {
			int ans = nextNode.getPayload();
			nextNode = nextNode.getNext();
			return ans;
		}
	}

	public int head() {
		return head.getPayload();
	}

	public boolean hasNext() {
		return head != null;
	}

	public LinkedList tail() {
		if (head == null) throw new IllegalStateException("tail of empty list");
		return new LinkedList(heap, head.getNext());
	}

	public Allocation getAllocation() {
		return root.getAllocation();
	}

	public void insert(int value) {
		Accessor.execute(heap, () -> {
			root.setNext(Node.of(heap, value, head, scope));
		});
        head = root.getNext();
	}

	public int deleteHead() {
		int value = Accessor.execute(heap, () -> {
			Node oldHead = head;
			root.setNext(oldHead.getNext());
			int ans = oldHead.getPayload();
			oldHead.free();
			return ans;
		});
        head = root.getNext();
        return value;
	}

	public String toString() {
		StringBuilder buff = new StringBuilder();
		Node node = head;
		while (node != null) {
			buff.append(node + " --> ");
			node = node.getNext();
		}
		buff.append("END");
		return buff.toString();
	}

	private static class Node {
		static MemoryLayout layout = MemoryLayout.structLayout(
			ValueLayout.ADDRESS.withName("next"),
			ValueLayout.JAVA_INT.withName("payload")
		);

		private static Accessor PAYLOAD = Accessor.of(layout, PathElement.groupElement("payload"));
		private static Accessor NEXT = Accessor.of(layout, PathElement.groupElement("next"));
		private Heap heap;
		private Allocation allocation;

		public static Node of(Heap heap, int payload, Node next, ResourceScope scope) {
			return new Node(heap, payload, next, scope);
		}

		private Node(Heap heap, int payload, Node next, ResourceScope scope) {
			this.heap = heap;
			this.allocation = heap.allocate(layout, scope, (allocation) -> {
				this.allocation = allocation;  // initialization segment needed by setters
				setPayload(payload);
				setNext(next);				
			});
		}

		private Node(Heap heap, Allocation allocation) {
			this.heap = heap;
			this.allocation = allocation;
		}

		public void free() {
			allocation.free();
		}

		public void setPayload(int payload) {
			PAYLOAD.set(getAllocation(), payload);
		}

		public int getPayload() {
			return (int)PAYLOAD.get(getAllocation());
		}

		public void setNext(Node next) {
			NEXT.setReference(getAllocation(), next == null ? null : next.getAllocation().address());
		}

		public Node getNext() {
			AllocationAddress nextAllocationAddress = NEXT.getReference(getAllocation());
            if (nextAllocationAddress == null) return null;
			Allocation nextAllocation = Allocation.ofAddress(nextAllocationAddress, layout.byteSize(), allocation.scope());
			return new Node(heap, nextAllocation);
		}

		private Allocation getAllocation() {
			return allocation;
		}

		public String toString() {
			return String.format("Node(%d)", getPayload());
		} 
	}
}
