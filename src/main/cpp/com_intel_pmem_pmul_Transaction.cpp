/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include "com_intel_pmem_pmul_Transaction.h"
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <libpmem.h>
#include <libpmemobj.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

JNIEXPORT jint JNICALL Java_com_intel_pmem_pmul_Transaction_nativeTxFlag(JNIEnv *env, jobject obj) {
    return (jint)TX_PARAM_NONE;
}
