/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include "com_intel_pmem_pmul_VolatileHeap.h"
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <memkind.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_VolatileHeap_nativeMinHeapSize0(JNIEnv *env, jobject obj) {
    return MEMKIND_PMEM_MIN_SIZE;
}

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_VolatileHeap_nativeErrorMessageSize0(JNIEnv *env, jobject obj) {
    return MEMKIND_ERROR_MESSAGE_SIZE;
}

