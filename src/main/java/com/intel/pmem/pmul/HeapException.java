/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

/**
 * Thrown to indicate that a heap operation failed to execute successfully.  
 */
public class HeapException extends RuntimeException {
	public HeapException(String message) {
		super(message);
	}

	public HeapException(String message, Throwable cause) {
		super(message, cause);
	}
}
