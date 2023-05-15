/// frameworks/base/core/jni/android_os_MessageQueue.cpp
/// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/jni/android_os_MessageQueue.cpp

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "MessageQueue-JNI"

#include <nativehelper/JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>

#include <utils/Looper.h>
#include <utils/Log.h>
#include "android_os_MessageQueue.h"

#include "core_jni_helpers.h"

// ......

/// NativeMessageQueue 构造方法，被 android_os_MessageQueue_nativeInit() 调用
NativeMessageQueue::NativeMessageQueue() :
        mPollEnv(NULL), mPollObj(NULL), mExceptionObj(NULL) {
    /// 获取 TLS 中的 Looper，TLS 跟 java 的 ThreadLocal 都是线程局部存储
    /// 意思就是这里 native 的 Looper 也是跟线程一对一的关系
    mLooper = Looper::getForThread();
    if (mLooper == NULL) {
        /// 创建 native 层的 Looper
        mLooper = new Looper(false);
        /// 保存 Looper 到 TLS 中
        Looper::setForThread(mLooper);
    }
}

/// 被 android_os_MessageQueue_nativePollOnce() 调用
void NativeMessageQueue::pollOnce(JNIEnv* env, jobject pollObj, int timeoutMillis) {
    // ...
    /// 调用 Looper 的 pollOnce 方法，里面会休眠
    // 这里调用的是 Looper.h 中的 inline int pollOnce(int) ，最后会调用 pollOnce(timeoutMillis, NULL, NULL, NULL)
    mLooper->pollOnce(timeoutMillis);
    // ...
}

/// 被 android_os_MessageQueue_nativeWake() 调用
void NativeMessageQueue::wake() {
    /// 调用 Looper::wake
    mLooper->wake();
}

/// 被 java 层 MessageQueue 的 nativeInit() 方法调用
static jlong android_os_MessageQueue_nativeInit(JNIEnv* env, jclass clazz) {
    /// 创建 native 消息队列 NativeMessageQueue
    NativeMessageQueue* nativeMessageQueue = new NativeMessageQueue();
    if (!nativeMessageQueue) {
        jniThrowRuntimeException(env, "Unable to allocate native queue");
        return 0;
    }
    /// 增加引用计数
    nativeMessageQueue->incStrong(env);
    /// 返回 NativeMessageQueue 地址到 java 层的 nativeInit() 方法
    return reinterpret_cast<jlong>(nativeMessageQueue); // //使用 C++ 强制类型转换符 reinterpret_cast
}

/// 被 java 层 MessageQueue 的 nativeDestroy() 方法调用
static void android_os_MessageQueue_nativeDestroy(JNIEnv* env, jclass clazz, jlong ptr) {
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    nativeMessageQueue->decStrong(env);
}

/// 被 java 层 MessageQueue 的 nativePollOnce(long) 方法调用
static void android_os_MessageQueue_nativePollOnce(JNIEnv* env, jobject obj,
        jlong ptr, jint timeoutMillis) {
    /// ptr 是 java 层 MessageQueue传过来的 NativeMessageQueue 的地址
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    /// 调用 NativeMessageQueue::pollOnce
    nativeMessageQueue->pollOnce(env, obj, timeoutMillis);
}

/// 被 java 层 MessageQueue 的 nativeWake(long) 方法调用
static void android_os_MessageQueue_nativeWake(JNIEnv* env, jclass clazz, jlong ptr) {
    /// ptr 是 java 层 MessageQueue传过来的 NativeMessageQueue 的地址
    NativeMessageQueue* nativeMessageQueue = reinterpret_cast<NativeMessageQueue*>(ptr);
    /// 调用 NativeMessageQueue::wake
    nativeMessageQueue->wake();
}