/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include "com_intel_pmem_pmul_PersistentHeap.h"
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <libpmem.h>
#include <libpmemobj.h>
#include <libpmempool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeMinHeapSize0(JNIEnv *env, jobject obj)
{
    return PMEMOBJ_MIN_POOL;
}

JNIEXPORT jint JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeRemovePoolFlag(JNIEnv *env, jobject obj)
{
    return  PMEMPOOL_RM_FORCE;
}

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeModeFlag(JNIEnv *env, jobject obj)
{
    return (jlong)(S_IRUSR | S_IWUSR);
}
