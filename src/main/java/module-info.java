/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

/**
 * Defines the experimental persistent memory API.
 */

module com.intel.pmem.pmul {
	requires jdk.incubator.foreign;
	requires jdk.unsupported;
    exports com.intel.pmem.pmul;
}
