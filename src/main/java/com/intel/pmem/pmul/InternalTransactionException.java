/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */
package com.intel.pmem.pmul;

/**
 * Thrown to indicate that a transaction was aborted implicitly.  
 */
class InternalTransactionException extends TransactionException {
    public InternalTransactionException(String message) {
        super(message);
    }

	public InternalTransactionException(String message, Throwable cause) {
		super(message, cause);
	}
}
