/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include <jni.h>

#ifndef _Included_com_intel_pmem_pmul_VolatileHeap
#define _Included_com_intel_pmem_pmul_VolatileHeap
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_VolatileHeap_nativeMinHeapSize0(JNIEnv *env, jobject obj); 
JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_VolatileHeap_nativeErrorMessageSize0(JNIEnv *env, jobject obj); 

#ifdef __cplusplus
}
#endif
#endif

