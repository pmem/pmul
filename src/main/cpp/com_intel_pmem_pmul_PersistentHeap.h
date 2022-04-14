/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include <jni.h>

#ifndef _Included_com_intel_pmem_pmul_PersistentHeap
#define _Included_com_intel_pmem_pmul_PersistentHeap
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeMinHeapSize0(JNIEnv *env, jobject obj);
JNIEXPORT jint JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeRemovePoolFlag(JNIEnv *env, jobject obj);
JNIEXPORT jlong JNICALL Java_com_intel_pmem_pmul_PersistentHeap_nativeModeFlag(JNIEnv *env, jobject obj);

#ifdef __cplusplus
}
#endif
#endif

