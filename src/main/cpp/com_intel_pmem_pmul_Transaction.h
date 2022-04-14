/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

#include <jni.h>

#ifndef _Included_com_intel_pmem_pmul_Transaction
#define _Included_com_intel_pmem_pmul_Transaction
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_intel_pmem_pmul_Transaction_nativeTxFlag(JNIEnv *env, jobject obj); 

#ifdef __cplusplus
}
#endif
#endif
