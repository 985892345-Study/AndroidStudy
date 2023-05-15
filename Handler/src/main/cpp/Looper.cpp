/// system/core/libutils/Looper.cpp
/// https://cs.android.com/android/platform/superproject/+/master:system/core/libutils/Looper.cpp

//
// Copyright 2010 The Android Open Source Project
//
// A looper implementation based on epoll().
//
#define LOG_TAG "Looper"
// #define LOG_NDEBUG 0
// Debugs poll and wake interactions.
#ifndef DEBUG_POLL_AND_WAKE
#define DEBUG_POLL_AND_WAKE 0
#endif
// Debugs callback registration and invocation.
#ifndef DEBUG_CALLBACKS
#define DEBUG_CALLBACKS 0
#endif
#include <utils/Looper.h>
#include <sys/eventfd.h>
#include <cinttypes>

// ......
/**
 * //#
 * //# 1、native 层的 Looper 跟 java 层的 Handler 类似，有发送、取出、回调消息的作用
 * //# 2、native 层的 Looper 也是一个线程对应一个 Looper，跟 java 层的 Looper 一样
 * //#
 * //#
 *
 */

/// native 层 Looper 的构造函数
Looper::Looper(bool allowNonCallbacks)
    : mAllowNonCallbacks(allowNonCallbacks),
      mSendingMessage(false),
      mPolling(false),
      mEpollRebuildRequired(false),
      mNextRequestSeq(WAKE_EVENT_FD_SEQ + 1),
      mResponseIndex(0),
      mNextMessageUptime(LLONG_MAX) {
    /// 1、构造唤醒事件的 fd（文件描述符）
    mWakeEventFd.reset(eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC));
    LOG_ALWAYS_FATAL_IF(mWakeEventFd.get() < 0, "Could not make wake event fd: %s", strerror(errno));
    AutoMutex _l(mLock);
    /// 2、重建 epoll 事件
    rebuildEpollLocked();
}

/// 被 NativeMessageQueue::NativeMessageQueue 调用
void Looper::setForThread(const sp<Looper>& looper) {
    sp<Looper> old = getForThread(); // also has side-effect of initializing TLS
    if (looper != nullptr) {
        looper->incStrong((void*)threadDestructor);
    }
    pthread_setspecific(gTLSKey, looper.get());
    if (old != nullptr) {
        old->decStrong((void*)threadDestructor);
    }
}

/// NativeMessageQueue::NativeMessageQueue 调用
sp<Looper> Looper::getForThread() {
    int result = pthread_once(& gTLSOnce, initTLSKey);
    LOG_ALWAYS_FATAL_IF(result != 0, "pthread_once failed");
    Looper* looper = (Looper*)pthread_getspecific(gTLSKey);
    return sp<Looper>::fromExisting(looper);
}

/*
 * //# 深入理解 epoll ：
 * //# https://mp.weixin.qq.com/s?__biz=MzUxMjEyNDgyNw==&mid=2247496957&idx=1&sn=3cd57e279181f8ea28066833285fa1c5&
 * //# chksm=f96b8609ce1c0f1f59892e65554626101a90d298df3a845751682116a24fefabb3d2f162497b&
 * //# mpshare=1&scene=23&srcid=0323f9sKTetsHSiHjTkpYc68&sharer_sharetime=1679555631248&sharer_shareid=885dfa789ac8fcaf1dd0fefe3078ae84#rd
 * //#
 * //# 大致描述下如下：
 * //# 把多个文件描述符 fd 放进 epoll 中的监听池(使用红黑树实现，方便快速添加删除)，
 * //# 然后 epoll_ctl 函数会向底层签一个读写回调，在这个文件描述符 fd 可读可写时进行回调，回调后会添加进一个链表中
 * //# 最后 epoll_wait 函数就是挂起函数(挂起时将让出 CPU 调度)，直到回调，回调时将读取链表中的文件描述符 fd
 * //#
 * //# 因为 epoll 能够监听多个 fd，所以叫：IO 多路复用
 * //#
 * //# 比 Linux 中 select 好的原因有两点：
 * //# 1、epoll 通过向底层签发回调来快速判断哪个 fd 可读可写，而 select 是通过遍历来判断的
 * //# 2、select 上限太小
 * //#
 */
/// (重新)创建 epoll
void Looper::rebuildEpollLocked() {
    // Close old epoll instance if we have one.
    /// 关闭旧的管道
    if (mEpollFd >= 0) {
        mEpollFd.reset();
    }

    /// 1、创建一个新的 epoll 文件描述符
    mEpollFd.reset(epoll_create1(EPOLL_CLOEXEC));
    // ...
    /// 2、设置监听事件为写入事件 (EPOLLIN)
    epoll_event wakeEvent = createEpollEvent(EPOLLIN, WAKE_EVENT_FD_SEQ);

    /// 3、将唤醒事件 fd (mWakeEventFd) 添加到 epoll 文件描述符 (mEpollFd) 中
    int result = epoll_ctl(mEpollFd.get(), EPOLL_CTL_ADD, mWakeEventFd.get(), &wakeEvent);
    // ...

    /// 4. 除了监听 mWakeEventFd 外，还监听了其他的 fd，比如键盘输入等事件
}

/// 被 NativeMessageQueue::pollOnce 调用
int Looper::pollOnce(int timeoutMillis, int* outFd, int* outEvents, void** outData) {
    int result = 0;
    for (;;) {
        // ...
        if (result != 0) {
            // ...
            /// 跳出循环
            return result;
        }
        /// 处理内部轮询
        result = pollInner(timeoutMillis);
        /// 这个循环其实跟 java 层 MessageQueue 的死循环是类似的
    }
}

/// 被 Looper::pollOnce 调用
int Looper::pollInner(int timeoutMillis) {
    // ...
    /// epoll 的事件集合
    struct epoll_event eventItems[EPOLL_MAX_EVENTS];
    /// 休眠直到有 fd 可读发生
    int eventCount = epoll_wait(mEpollFd.get(), eventItems, EPOLL_MAX_EVENTS, timeoutMillis);

    // ...

    /// 获取锁
    mLock.lock();

    // ... 这里会检查是否出错，出错就直接 goto Done

    /// 处理可读的 fd
    for (int i = 0; i < eventCount; i++) {
        const SequenceNumber seq = eventItems[i].data.u64;
        uint32_t epollEvents = eventItems[i].events;
        if (seq == WAKE_EVENT_FD_SEQ) {
            /// 如果是唤醒的 fd
            if (epollEvents & EPOLLIN) {
                awoken();
            } else {
                ALOGW("Ignoring unexpected epoll events 0x%x on wake event fd.", epollEvents);
            }
        } else {
            // ... 其他 fd
        }
    }
Done: ;
    mNextMessageUptime = LLONG_MAX;
    /// mMessageEnvelopes 是一个 vector，里面保存着消息队列 (源码在 Looper.h 中，但不是很重要，所以就不展示)
    while (mMessageEnvelopes.size() != 0) {
        nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
        /// MessageEnvelop 有收件人 Handler 和消息内容 Message
        const MessageEnvelope& messageEnvelope = mMessageEnvelopes.itemAt(0);
        if (messageEnvelope.uptime <= now) {
            { // obtain handler
                // MessageHandler 也定义在 Looper.h 中
                sp<MessageHandler> handler = messageEnvelope.handler;
                Message message = messageEnvelope.message;
                mMessageEnvelopes.removeAt(0);
                mSendingMessage = true;
                mLock.unlock();
                /// 回调 native 层的 handler，这里 native 层的 Looper 其实像 java 层的 Handler，可以发送、取出、回调消息
                /// 但对于休眠和唤醒来说，并没有用到这个 native 层的 MessageHandler
                handler->handleMessage(message);
            } // release handler
            mLock.lock();
            mSendingMessage = false;
            result = POLL_CALLBACK;
        } else {
            /// 消息没到执行时间就退出循环
            mNextMessageUptime = messageEnvelope.uptime;
            break;
        }
    }
    mLock.unlock();
    // ...
    return result;
}

/// 被 NativeMessageQueue::wake 调用
void Looper::wake() {
    uint64_t inc = 1;
    /// 使用 write 向 mWakeEventFd 写入数据，就会立马唤醒 Looper::pollInner 中的 epoll_wait()
    ssize_t nWrite = TEMP_FAILURE_RETRY(write(mWakeEventFd.get(), &inc, sizeof(uint64_t)));
    // ...
}