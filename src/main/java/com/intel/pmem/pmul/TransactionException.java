/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */
package com.intel.pmem.pmul;

/**
 * Thrown to indicate that a transaction failed to execute successfully.  
 */
public class TransactionException extends RuntimeException {
    public TransactionException(String message) {
        super(message);
    }

	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}
}
