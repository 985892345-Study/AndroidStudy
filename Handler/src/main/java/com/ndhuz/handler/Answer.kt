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
 * https://www.zhihu.com/question/34652589/answer/90344494
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
 * /// Handler 为什么会发生内存泄漏 ?
 * 1、在使用非静态内部类时就容易导致内存泄漏，原因在于非静态内部类默认持有外部类的引用
 * 2、泄漏的时机在于发送了一条延时的 Message，但宿主，比如 Activity，已经被回调了 onDestroy()，此时却因为 Message 还没有执行发送内存泄漏
 * 3、发送泄漏的引用链为：Looper -> MessageQueue -> Message -> Handler -> Activity
 *
 *
 * /// 该怎么解决内存泄漏 ?
 * 1、可以使用静态内部类 + 弱引用 Activity 解决。在 Message 被回调时判断当前 Activity 的弱引用是否为 null，不为 null 时才执行
 * 2、在 Activity 被摧毁时更建议调用 Handler.removeCallbacksAndMessages() 把相关联的 Message 清空
 *
 *
 * /// MessageQueue 怎么保证线程安全 ?
 * MessageQueue 在读取 Message 队列时会使用 synchronized 加锁保证线程安全
 *
 *
 * /// MessageQueue 为什么不会造成死锁 ?
 * MessageQueue 在读取下一条 Message 时，如果没有 Message 可以执行，就会退出 synchronized，然后调用 native 层的方法进行休眠，等待 Message 入队唤醒
 *
 *
 * /// MessageQueue 在没有消息时会休眠，那是什么促使 Message 的产生 ?
 * 虽然主线程会进行休眠，但是可以通过其他线程对主线程的 Looper 发送 Message，
 * 比如：AMS 通过 Binder 线程向主线程发送作用于 Activity 启动的 Message，然后触发一系列的 Message
 *
 *
 * /// Looper 死循环为什么不会导致应用卡死 ?
 * 1、在 Android 中一切皆是消息，包括触摸事件，视图的绘制、显示和刷新等等都是消息，
 *    正是因为 Looper 死循环一直读取消息才使应用一直在运行
 * 2、而应用卡死一般是因为单个 Message 执行时间过长，导致后面 Message 一直不被执行而产生的卡顿
 *
 *
 * /// Looper 死循环会特别消耗 CPU 资源吗 ?
 * 1、并不会消耗太多的资源。在 MessageQueue 当前没有消息要执行时会进行休眠，调用了 native 层 NativeMessageQueue，
 *   NativeMessageQueue 调用了 native 层 Looper，该 Looper 会使用 Linux 的 epoll 机制进行休眠，休眠时会让出 CPU 调度
 * 2、epoll 会监听了一个专门用于休眠操作的文件描述符，并向底层签写了一个当前文件描述符可读的回调，
 *   在 java 层 MessageQueue 入队时会对休眠的文件描述符进行写入，然后唤醒之前的休眠操作
 *
 *
 * /// 同步屏障是什么 ? 它的原理是怎么实现的 ?
 * 1、同步屏障是一种特殊的消息，可以使 Handler 优先执行异步消息。
 *   在 ViewRootImpl.scheduleTraversals() 方法中发送了一个同步屏障，并紧接着发送了一个用于测量布局绘制的异步消息
 * 2、在 MessageQueue.next() 读取下一条消息时，会先判断队列头是否是同步屏障，如果是的话，就会跳过同步消息，只寻找异步消息，
 *   最后返回给 Looper
 * 3、通过同步屏障和异步消息来保证了 View 的绘制会优先执行，避免了消息过多的情况下出现掉帧的情况
 */