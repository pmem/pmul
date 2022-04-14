/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.ValueLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;

abstract class LowLevelHeap {
    abstract MemorySegment allocateSegment(long byteSize, boolean transactional, ResourceScope scope);         	
    abstract void freeSegment(MemoryAddress address, boolean transactional);
    abstract long heapAddress();
    abstract MemoryAddress transformAddress(MemoryAddress address);
    abstract MemoryAddress reformAddress(MemoryAddress adddress);
    abstract long size();
    abstract void close();
    static final int HEAP_VERSION = 900;
    static final int MIN_HEAP_VERSION = 900;
    Metadata metadata;
    Path path;

    static final class Kind implements HeapKind {
        public static final Kind NOKIND = new Kind(0); 
        public static final Kind VOLATILE = new Kind(1001);
        public static final Kind PERSISTENT = new Kind(1002);
        int val;
        private Kind(int value) {
            this.val = value;
        }
        public int value() {return val;}
    }

    // default impls, not called for VolatileHeap, must be overridden by PersistentHeap
    long poolAddress() {
        throw new UnsupportedOperationException();
    }

    void setRoot(MemorySegment segment) {
        throw new UnsupportedOperationException();
    }

    MemorySegment getRoot(ResourceScope scope) {
        throw new UnsupportedOperationException();
    }

    void flush(MemorySegment segment, long offset, long byteCount) {
        throw new UnsupportedOperationException();
    }

    void flush(MemorySegment segment) {
        flush(segment, 0, segment.byteSize());
    }

    void transaction(Runnable body) {
        throw new UnsupportedOperationException();
    }

    <T> T transaction(Supplier<T> body) {
        throw new UnsupportedOperationException();
    }

    <T> T transaction(MemorySegment segment, Supplier<T> body) {
        throw new UnsupportedOperationException();
    }

    void addToTransaction(MemorySegment segment, long offset, long byteCount) {
        throw new UnsupportedOperationException();
    }  

    static class Metadata {
        static final MemoryLayout layout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("kind"),
            ValueLayout.JAVA_INT.withName("version"),
            ValueLayout.ADDRESS.withName("user_root"),
            ValueLayout.JAVA_LONG.withName("user_root_size")
        );
        static final VarHandle KIND = layout.varHandle(PathElement.groupElement("kind"));
        static final VarHandle VERSION = layout.varHandle(PathElement.groupElement("version"));
        static final VarHandle USER_ROOT = layout.varHandle(PathElement.groupElement("user_root"));
        static final VarHandle USER_ROOT_SIZE = layout.varHandle(PathElement.groupElement("user_root_size"));
        private static final long ROOT_LAYOUT_OFFSET = layout.byteOffset(groupElement("user_root"));
        private static final long ROOT_LAYOUT_SIZE = layout.select(groupElement("user_root")).byteSize() + layout.select(groupElement("user_root_size")).byteSize();
        private final MemorySegment metadata;

        Metadata(LowLevelHeap heap, MemorySegment segment) {
            metadata = segment;
        }

        public MemorySegment getSegment() {return metadata;} 

        public void setKind(int kind) {KIND.set(metadata, kind);}
        public int getKind() {return (int)KIND.get(metadata);}

        public int getVersion() {return (int)VERSION.get(metadata);}
        public void setVersion(int version) {VERSION.set(metadata, version);} 

        public MemoryAddress getUserRoot() {return (MemoryAddress)USER_ROOT.get(metadata);}
        public long getUserRootSize() {return (long)USER_ROOT_SIZE.get(metadata);}
        public void setRoot(MemoryAddress address, long size) {
            USER_ROOT.set(metadata, address);
            USER_ROOT_SIZE.set(metadata, size);
        }
        public MemorySegment rootAsSlice() { return metadata.asSlice(ROOT_LAYOUT_OFFSET, ROOT_LAYOUT_SIZE); }
    }
 }
