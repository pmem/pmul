/*
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 */

package com.intel.pmem.pmul;

sealed interface HeapKind
permits Heap.Kind, LowLevelHeap.Kind {
    public abstract int value();
}

