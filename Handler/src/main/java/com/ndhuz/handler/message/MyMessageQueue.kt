package com.ndhuz.handler.message

import android.os.SystemClock
import com.ndhuz.handler.handler.MyHandler
import com.ndhuz.handler.looper.MyLooper
import kotlin.math.max
import kotlin.math.min

/**
 * 文章推荐：
 * - https://www.jianshu.com/p/57a426b8f145
 *
 * 思考一下：
 * 1、如果同步屏障后面又是同步屏障会发生什么？
 * 同步屏障因为在 [MyMessage.isAsynchronous] 返回 false，说明它是一种特殊的同步消息，
 * 所以在遇到第一个同步屏障往后遍历寻找异步消息时会忽略掉第二个同步屏障
 *
 * 2、上面这种情况如果在两个同步屏障后不存在异步消息又会发生什么？
 * 同步屏障属于一种特殊的同步消息，其实就跟只有一个同步屏障是几乎一样的效果。
 * 在添加异步消息时会唤醒队列
 * 但在 remove 同步屏障时会有所不同，如果 remove 第一个同步屏障，则会因为 next 也是同步屏障而不唤醒队列
 *
 * @author 985892345
 * 2023/3/20 22:14
 */
class MyMessageQueue(quitAllowed: Boolean) {
  
  private var mQuitAllowed: Boolean = quitAllowed // 是否可以终止 Message
  
  private var mPtr: Long = nativeInit() // 这个是 native 层 MessageQueue 的引用地址
  
  private var mMessages: MyMessage? = null
  
  private val mIdleHandlers = ArrayList<MyIdleHandler>()
  
  private var mPendingIdleHandlers: Array<MyIdleHandler?>? = null
  
  private var mQuitting: Boolean = false // 是否已经停止
  
  private var mBlocked: Boolean = false // 是否处于死循环中，此时处于休眠状态，可能是休眠一会，也可能是无限休眠
  
  private var mNextBarrierToken: Int = 0 // 同步屏障的 token，屏障消息的 arg1 参数为该值
  
  
  // 处理底层消息队列。只能在循环线程或终结器上调用。
  private fun dispose() {
    if (mPtr != 0L) {
      nativeDestroy(mPtr)
      mPtr = 0L
    }
  }
  
  fun addIdleHandler(handler: MyIdleHandler) {
    synchronized(this) {
      mIdleHandlers.add(handler)
    }
  }
  
  fun removeIdleHandler(handler: MyIdleHandler) {
    synchronized(this) {
      mIdleHandlers.remove(handler)
    }
  }
  
  
  
  /**
   * 返回下一次执行的 Message
   *
   * 由 [MyLooper.loopOnce] 调用
   *
   * # 如果当前是屏障消息，则会跳过后面的同步消息，只处理其后的异步消息
   * # 同步屏障不会自动移除，需要手动移除
   * # IdleHandler 只会在没有 Message 或者 Message 时间未到时才会执行，且只执行一次(在一次 next 中)。并且是 MessageQueue 执行，而不是返回给 Looper 执行
   * # 只有 mPtr = 0 和 mQuitting = true 才会返回 null，返回 null 后也会同步的终止 Looper 的死循环
   */
  internal fun next(): MyMessage? {
    val ptr = mPtr
    if (ptr == 0L) {
      return null
    }
    
    var pendingIdleHandlerCount = -1 // 循环期间只会执行一次
    var nextPollTimeoutMillis = 0
    
    // MessageQueue 这里也是一个死循环
    while (true) {
      /// 0: 不休眠立即返回   > 0: 休眠   < 0: 无限休眠
      nativePollOnce(ptr, nextPollTimeoutMillis)
      /// 加锁确保 MessageQueue 队列安全，但这里加锁只是暂时的，
      /// 在未找到 Message 或 Message 未到时间时将触发上一句的 nativePollOnce() 进行休眠
      synchronized(this) {
        val now = SystemClock.uptimeMillis()
        var prevMeg: MyMessage? = null
        var msg = mMessages
        
        /// 如果当前 msg 为同步屏障
        if (msg != null && msg.target == null) {
          // 当前 msg 是同步屏障的 Message，则在队列中查找下一条异步 Message
          // 注意：同步屏障的 Message.target 为 null，同步消息和异步消息的 target 都不允许为 null
          do {
            prevMeg = msg
            msg = msg!!.next
          } while (msg != null && !msg.isAsynchronous()) // 如果 msg 是同步消息就继续循环
          /*
          * 如果后面出现了第二个同步屏障，其实是会被跳过的
          * 因为同步屏障在 isAsynchronous() 返回 false，表明它是一种特殊的同步消息
          * */
          /// 这里退出循环是 msg 为第一个异步消息或 null
          // # 同步屏障就是为了确保异步消息的优先级，设置了屏障后，只能处理其后的异步消息，同步消息会被挡住
        }
        
        /// msg 为下一条即将执行的消息，可能是同步消息，也可能是异步消息
        
        if (msg != null) {
          if (now < msg.`when`) {
            /// 没有到达执行 msg 的时间，设置休眠时间
            nextPollTimeoutMillis = min(msg.`when` - now, Int.MAX_VALUE.toLong()).toInt()
          } else {
            /// 已经到达执行 msg 的时间
            mBlocked = false
            if (prevMeg != null) {
              // 这里 prevMeg 不为 null，这说明之前一定触发了屏障消息
              // 所以就不能对 mMessages 进行修改
              prevMeg.next = msg.next
            } else {
              mMessages = msg.next
            }
            msg.next = null
            msg.markInUse()
            return msg /// 退出函数并返回 msg
          }
        } else {
          /// 如果 msg 为 null，这里将设置为无限休眠
          nextPollTimeoutMillis = -1
        }
        
        // 终止操作
        if (mQuitting) {
          dispose()
          return null /// 终止函数并返回 null
        }
        
        /// 只有没有到达 msg 执行的时间或者 msg 为 null 时才能到这一步
        
        // 如果是第一次执行 IdleHandler ，则保存 IdlerHandler 数量
        // # IdleHandler 仅在队列为空或队列中的第一条消息（可能是屏障）将在未来处理时运行
        if (pendingIdleHandlerCount < 0 && (mMessages == null || now < mMessages!!.`when`)) {
          pendingIdleHandlerCount = mIdleHandlers.size
        }
        if (pendingIdleHandlerCount <= 0) {
          /// 如果 pendingIdleHandlerCount = 0 说明已经执行过 IdleHandler 了
          // # 之后分为两种情况，如果 nextPollTimeoutMillis > 0: 休眠一会; < 0: 无限休眠; 不存在等于 0 的情况
          // 没有要处理的 IdleHandler，就跳到下一次循环
          mBlocked = true
          return@synchronized /// 源码中这里是直接 continue 到下一次循环
        }
        
        /// 到这里说明有 IdlerHandler 需要处理
        
        if (mPendingIdleHandlers == null) {
          mPendingIdleHandlers = Array(max(pendingIdleHandlerCount, 4)) { null }
        }
        mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers!!)
        /// synchronized 块退出
      }
      
      if (pendingIdleHandlerCount <= 0) {
        // 本来源码中没有这个判断，但因为 kt 无法实现在 synchronized 中使用 continue，所以放在这里
        continue /// 跳到下一次循环
      }
      
      /// 开始运行 IdleHandler
      
      // 只会在第一次迭代期间到达此代码块
      for (i in 0 until pendingIdleHandlerCount) {
        val idler = mPendingIdleHandlers!![i]
        mPendingIdleHandlers!![i] = null
        
        var keep = false
        try {
          // 执行 IdleHandler，返回 true 则保留到下一次继续执行；false 则移除
          keep = idler!!.queueIdle()
        } catch (t: Throwable) {
          // 打印信息
        }
        
        if (!keep) {
          synchronized(this) {
            mIdleHandlers.remove(idler)
          }
        }
      }
      
      // 将 IdleHandler 计数重置为 0，确保 IdlerHandler 只执行一次
      pendingIdleHandlerCount = 0
      
      // 在调用 IdleHandler 后可能有新的 Message 需要处理，所以这里清空休眠时间
      nextPollTimeoutMillis = 0
      
      /// while 退出
    }
  }
  
  
  
  /**
   * 终止 Message
   * @param safe 是否安全终止。true -> 将 Message.when > now 的移掉; false -> 全部移掉
   */
  internal fun quit(safe: Boolean) {
    if (!mQuitAllowed) {
      throw IllegalStateException("Main thread not allowed to quit.")
    }
    
    if (mQuitting) {
      return
    }
    mQuitting = true
    
    // 处理 Message
    if (safe) {
      // 如果是安全终止
      removeAllFutureMessagesLocked()
    } else {
      // 不是安全终止
      removeAllMessagesLocked()
    }
    
    // 唤醒 native 层的 MessageQueue
    nativeWake(mPtr)
  }
  
  /**
   * 移除执行时间 when > now 的 Message
   */
  private fun removeAllFutureMessagesLocked() {
    val now = SystemClock.uptimeMillis()
    var p = mMessages
    if (p != null) {
      if (p.`when` > now) {
        removeAllMessagesLocked()
      } else {
        var n: MyMessage?
        while (true) {
          n = p!!.next
          if (n == null) {
            return
          }
          if (n.`when` > now) {
            // 找到第一个 when > nod 的 Message
            break;
          }
          p = n
        }
        // p 后面所有的 Message 的 when > now，这里直接断链
        p!!.next = null
        do {
          p = n
          n = p!!.next
          p.recycleUnchecked() // 通知后面的 Message 进行回收
        } while (n != null)
      }
    }
  }
  
  /**
   * 移除掉所有 Message
   */
  private fun removeAllMessagesLocked() {
    var p = mMessages
    while (p != null) {
      val n = p.next
      p.recycleUnchecked()
      p = n
    }
    mMessages = null
  }
  
  
  /**
   * 添加同步屏障
   *
   * # 同步屏障是强制性插入的，不经过 Handler
   * # 添加同步屏障的位置在 ViewRootImpl.scheduleTraversals() todo 之后写到 ViewRootImpl 时补充
   * # 添加同步屏障会返回对应的 token，用于后期进行遍历比对删除
   * # 添加屏障不会唤醒队列
   */
  internal fun postSyncBarrier(): Int {
    return postSyncBarrier(SystemClock.uptimeMillis())
  }
  
  private fun postSyncBarrier(`when`: Long): Int {
    synchronized(this) {
      val token = mNextBarrierToken++ // 这个不会减少(没什么用的小知识)
      val msg = MyMessage.obtain()
      msg.markInUse()
      msg.`when` = `when`
      msg.arg1 = token
      
      var prev: MyMessage? = null
      var p: MyMessage? = mMessages
      if (`when` != 0L) {
        while (p != null && p.`when` <= `when`) {
          prev = p
          p = p.next
        }
      }
      
      if (prev != null) {
        /// 插入同步屏障，左.when <= 同步屏障.when < 右.when
        msg.next = p
        prev.next = msg
      } else {
        /// 这里只有 when = 0 时才会触发，此时 会把同步屏障直接添加到队列头
        msg.next = p
        mMessages = msg
      }
      return token
    }
  }
  
  /**
   * 移除同步屏障
   *
   * # 根据之前 [postSyncBarrier] 返回的 token 来从头遍历移除同步屏障
   * # 如果移除的同步屏障就是队列头时就唤醒消息队列
   */
  private fun removeSyncBarrier(token: Int) {
    synchronized(this) {
      var prev: MyMessage? = null
      var p = mMessages
      while (p != null && (p.target != null || p.arg1 != token)) {
        /// 找到 arg1 = token 的 Message
        prev = p
        p = p.next
      }
      if (p == null) {
        throw IllegalStateException("不存在 token = $token 的同步屏障")
      }
      val needWake: Boolean
      if (prev != null) {
        /// 这里说明同步屏障在后面还没有执行
        prev.next = p.next
        needWake = false
      } else {
        /// 这里说明队列头就是同步屏障
        mMessages = p.next
        /// 如果队列头的同步屏障后面的 next 为 null 或者不是同步屏障就唤醒消息队列
        // 注意： 这里 remove 同步屏障，在同步屏障是队列头并且队列头的 next 也是同步屏障时不唤醒队列
        needWake = mMessages == null || mMessages!!.target != null
      }
      p.recycleUnchecked()
      
      if (needWake && !mQuitting) {
        nativeWake(mPtr) /// 唤醒消息队列
      }
    }
  }
  
  /**
   * 添加 Message
   *
   * # 相同时间执行的消息会添加在最前面
   * # 如果新添加的 Message 会被放在队列头时，是否唤醒取决于当前是否休眠
   * # 如果队列头的 Message 是同步屏障，则根据后面是否存在异步消息决定是否唤醒，存在时不唤醒
   */
  internal fun enqueueMessage(msg: MyMessage, `when`: Long): Boolean {
    if (msg.target == null) {
      throw IllegalArgumentException("Message 必须有 target")
    }
    
    synchronized(this) {
      if (msg.isInUse()) {
        throw IllegalStateException("Message 已经被使用")
      }
      
      if (mQuitting) {
        msg.recycle()
        return false
      }
      
      msg.markInUse()
      msg.`when` = `when`
      var p = mMessages
      var needWake: Boolean
      if (p == null || `when` == 0L || `when` < p.`when`) {
        msg.next = p
        mMessages = msg
        needWake = mBlocked // 是否唤醒值取决于当前是否处于死循环
      } else {
        /// 如果队列头部是同步屏障并且即将添加的 msg 是异步消息时可能需要唤醒，具体还需要看后面是否已经存在异步消息
        needWake = mBlocked && p.target == null && msg.isAsynchronous()
        var prev: MyMessage?
        while (true) {
          prev = p
          p = p!!.next
          if (p == null || `when` < p.`when`) {
            break
          }
          if (needWake && p.isAsynchronous()) {
            /// 如果队列中前面已经存在异步消息，则取消唤醒
            needWake = false
          }
        }
        /// 插入 Message，左.when < msg.when <= 右.when
        msg.next = p
        prev!!.next = msg
      }
      
      if (needWake) {
        nativeWake(mPtr)
      }
    }
    return true
  }
  
  /**
   * 移除 Message
   *
   * 会遍历完整个队列，并移除掉所有符合要求的 Message
   */
  internal fun removeMessage(h: MyHandler, what: Int, any: Any?) {
    synchronized(this) {
      var p = mMessages
      
      // 如果队列头就是需要 remove 的 Message
      while (p != null && p.target == h && p.what == what && (any == null || p.any == any)) {
        val n = p.next
        mMessages = n
        p.recycleUnchecked()
        p = n
      }
      
      // 移除队列中间的 Message
      while (p != null) {
        val n = p.next
        if (n != null) {
          if (n.target == h && n.what == what && (any == null || p.any == any)) {
            val nn = n.next
            n.recycleUnchecked()
            p.next = nn
            continue
          }
        }
        p = n
      }
    }
  }
  
  /**
   * 移除 post 发送的 Runnable
   *
   * 通过 [MyMessage.callback] 来判断
   */
  internal fun removeMessage(h: MyHandler, r: Runnable, any: Any?) {
    synchronized(this) {
      var p = mMessages
  
      // 如果队列头就是需要 remove 的 Message
      while (p != null && p.target == h && p.callback == r && (any == null || p.any == any)) {
        val n = p.next
        mMessages = n
        p.recycleUnchecked()
        p = n
      }
  
      // 移除队列中间的 Message
      while (p != null) {
        val n = p.next
        if (n != null) {
          if (n.target == h && n.callback == r && (any == null || p.any == any)) {
            val nn = n.next
            n.recycleUnchecked()
            p.next = nn
            continue
          }
        }
        p = n
      }
    }
  }
  
  
  
  // 返回 native 层 MessageQueue 的引用地址 (
  private fun nativeInit(): Long {
    /// 请移步 android_os_MessageQueue.cpp
    /**
     * // frameworks/base/core/jni/android_os_MessageQueue.cpp
     * static jlong android_os_MessageQueue_nativeInit(JNIEnv* env, jclass clazz) {
     *     // 创建 native 消息队列 NativeMessageQueue
     *     NativeMessageQueue* nativeMessageQueue = new NativeMessageQueue();
     *     //...
     *     // 增加引用计数
     *     nativeMessageQueue->incStrong(env);
     *     // 使用 C++ 强制类型转换符 reinterpret_cast 把 NativeMessageQueue 指针强转成 long 类型并返回到 java 层
     *     return reinterpret_cast<jlong>(nativeMessageQueue);
     * }
     *
     * // 创建 NativeMessageQueue 后会创建 native 层的 Looper
     * // frameworks/base/core/jni/android_os_MessageQueue.cpp
     * NativeMessageQueue::NativeMessageQueue() : : mPollEnv(NULL), mPollObj(NULL), mExceptionObj(NULL) {
     *     // 获取 TLS 中的 Looper (Looper::getForThread 相当于 java层 的 Looper.mLooper 中的 ThreadLocal.get 方法)
     *     // TLS: C++ 中的线程局部存储，相当于  java 的 ThreadLocal
     *     mLooper = Looper::getForThread();
     *     if (mLooper == NULL) {
     *         // 创建 native 层的 Looper
     *         mLooper = new Looper(false);
     *         // 保存 Looper 到 TLS 中 (Looper::setForThread 相当于 java 层的 ThreadLocal.set 方法)
     *         Looper::setForThread(mLooper);
     *     }
     * }
     */
    return 1
  }
  
  // 唤醒 native 层的 MessageQueue
  private fun nativeWake(ptr: Long) {
    // # Handler 的阻塞唤醒机制基于 Linux 的 epoll 机制实现。
    // https://www.jianshu.com/p/57a426b8f145
    // # 创建 java 层的 MessageQueue 时就会创建对应 native 层的 NativeMessageQueue，并返回自身地址(mPtr)用于后面使用
    // # 创建 NativeMessageQueue 时也会创建 native 层的 Looper
    // # 创建 Looper 时会通过管道与 epoll 机制建立一套 native 层的消息机制
    
  }
  
  // 用于等待执行下一条消息
  // https://www.jianshu.com/p/57a426b8f145
  private fun nativePollOnce(ptr: Long, timeoutMillis: Int) {
  
  }
  
  // 摧毁 native 层的  MessageQueue
  private fun nativeDestroy(ptr: Long) {
  
  }
  
  /**
   * 用于发现线程何时将阻塞等待更多消息的回调接口。
   *
   * 推荐文章：
   * - https://github.com/zhpanvip/AndroidNote/wiki/IdleHandler
   *
   * # IdleHandler 是 Handler 提供的一种在消息队列空闲时，执行任务的机制
   * # 当 MessageQueue 当前没有立即需要处理的消息时（消息队列为空，或者消息未到执行时间），会执行 IdleHandler
   * # 在 Activity 的 onStop 和 onDestroy 的回调由 IdleHandler 调用 todo 待补充
   *
   */
  interface MyIdleHandler {
    /**
     * 当消息队列用完消息时调用，现在将等待更多消息。返回 true 以保持您的空闲处理程序处于活动状态，返回 false 以将其删除。
     * 如果队列中仍有待处理的消息，但它们都计划在当前时间之后调度，则可能会调用此方法。
     */
    fun queueIdle(): Boolean
  }
}