/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.util.concurrent.ConcurrentHashMap;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;

interface AccessorImpl extends Accessor {
    static final ConcurrentHashMap<String, Info> infos = new ConcurrentHashMap<>();

   	public static class Info {
   		final MemoryLayout layout;
   		final PathElement[] pathElements;

   		public Info(MemoryLayout layout, PathElement[] elements) {
   			this.layout = layout;
   			this.pathElements = elements;
   		}
   	}

    public static Info getInfo(String name) {
    	return infos.get(name);
    }

    public static void addInfo(String name, Info info) {
    	infos.put(name, info);
    }
}
