package com.ndhuz.handler.looper

import android.os.Looper
import com.ndhuz.handler.message.MyMessageQueue
import com.ndhuz.threadlocal.thread.MyThreadLocal

/**
 * [Looper]
 *
 * @author 985892345
 * 2023/3/20 22:14
 */
@Suppress("FoldInitializerAndIfToElvis")
class MyLooper(quitAllowed: Boolean) {
  
  companion object {
  
    /**
     * 可看 [MyThreadLocal] 源码仿写
     */
    private val sThreadLocal = ThreadLocal<MyLooper>()
    private var sMainLooper: MyLooper? = null // 主线程的 Looper
    
    /**
     * 将当前线程初始化为循环器，将其标记为应用程序的主循环器
     *
     * 该方法只允许系统内部调用
     */
    fun prepareMainLooper() {
      prepare(false)
      synchronized(MyLooper::class.java) {
        if (sMainLooper != null) {
          // 一个应用进程只允许一个 Looper
          throw IllegalStateException("The main Looper has already been prepared.")
        }
        sMainLooper = myLooper()
      }
    }
  
    // 调用 Looper.loop() 前的准备工作(供其他线程调用)
    fun prepare() {
      prepare(true)
    }
    
    /**
     * 调用 Looper.loop() 前的准备工作
     *
     * 主要是使用 [ThreadLocal] 设置当前线程内的 Looper 对象 ([ThreadLocal] 也是一个很重要的面试点)
     * @param quitAllowed 是否允许终止
     */
    private fun prepare(quitAllowed: Boolean) {
      if (sThreadLocal.get() != null) {
        throw RuntimeException("Only one Looper may be created per thread")
      }
      sThreadLocal.set(MyLooper(quitAllowed))
    }
  
    /**
     * 在此线程中运行消息队列。一定要调用 [quit] 来结束循环。
     */
    fun loop() {
      val me = myLooper()
        ?: throw RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.")
      // 必须要执行 prepare 后才能执行 loop()
      
      me.mInLoop = true
      
      // ...
      
      // 开始进入死循环
      while (true) {
        if (!loopOnce(me)) {
          // 每执行一个 Message 就判断是否退出循环
          return
        }
      }
    }
  
    /**
     * 轮询并传递单个消息
     */
    private fun loopOnce(me: MyLooper): Boolean {
      val msg = me.mQueue.next()
      if (msg == null) {
        // # 只有 mPtr = 0 和 mQuitting = true 才会返回 null
        // # 这里返回 false 后将退出死循环
        return false
      }
      
      // ... 有很多用于回调以及打印 log 的代码
      
      // 除了消息屏障外 target 都不会为 null
      msg.target!!.dispatchMessage(msg) // 这里可能会抛出异常
      
      // ...
      
      msg.recycleUnchecked()
      
      return true
    }
  
    
    // 返回主线程的 Looper
    fun getMainLooper(): MyLooper {
      synchronized(MyLooper::class.java) {
        return sMainLooper!!
      }
    }
    
    // 返回与当前线程关联的 Looper 对象
    fun myLooper(): MyLooper? {
      return sThreadLocal.get()
    }
    
    // 返回与当前线程关联的 MessageQueue 对象
    fun myQueue(): MyMessageQueue {
      return myLooper()!!.mQueue
    }
  }
  
  internal val mQueue = MyMessageQueue(quitAllowed)
  internal val mThread = Thread.currentThread()
  
  private var mInLoop: Boolean = false
  
  /**
   * 退出 [loop] 的循环
   *
   * 使用此方法可能不安全，因为某些消息可能无法在循环程序终止之前传递
   * 考虑改用 [quitSafely] 以确保所有待处理的工作有序完成
   */
  fun quit() {
    mQueue.quit(false)
  }
  
  /**
   * 安全地退出 [loop] 循环
   *
   * - 调用后将不再接受新的 Message，
   * -
   */
  fun quitSafely() {
    mQueue.quit(true)
  }
}