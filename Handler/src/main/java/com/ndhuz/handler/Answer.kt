package com.ndhuz.handler

/**
 * .
 *
 * @author 985892345
 * 2023/3/23 17:44
 */

/**
 * Handler 面试题总结:
 * https://juejin.cn/post/7076314847637405710
 * https://github.com/zhpanvip/AndroidNote#android%E6%B6%88%E6%81%AF%E6%9C%BA%E5%88%B6
 *
 * /// 描述整个消息机制运行：
 * 1、首页系统调用 ActivityThread 中 main 函数初始化主线程的 Looper，并把 Looper 保存在 ThreadLocal (保证一个线程只会有一个 Looper)
 * 2、然后调用 Looper.loop() 进入一个死循环，一直循环调用 MessageQueue.next() 获取下一个 Message，
 * 3、MessageQueue.next() 中也有一个死循环，但是在获取不到消息或者消息执行时间未到时会调用 native 层 NativeMessageQueue 的休眠方法，
 * 4、之后会调用到 native 层的 Looper，native 层的 Looper 使用 Linux 的 epoll 机制进行休眠 (epoll 机制会监听一个休眠的文件描述符)
 *
 * 5、如果有消息添加进来了，就会唤醒 native 层的 Looper，随之也唤醒了 java 层的 MessageQueue
 * 6、最后 java 层的 MessageQueue 的 next() 就会读取到新加入的 Message 并返回给 Looper
 *
 * 7、Looper 拿到 Message 后会根据 Message 中保存的 target 变量(Handler 类型)回调 Handler 的 dispatchMessage() 方法
 *
 *
 * ///
 */