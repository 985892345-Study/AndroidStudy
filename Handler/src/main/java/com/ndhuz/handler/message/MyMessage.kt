package com.ndhuz.handler.message

import android.annotation.SuppressLint
import android.os.Build
import android.os.Message
import android.os.SystemClock
import com.ndhuz.handler.handler.MyHandler

/**
 * [Message]
 *
 * - Message 提供了一个回收池，容量 50，可以通过 [MyMessage.obtain] 获得，虽然提供了 [MyMessage.recycle] 方法，
 *  但这个方法限制了只能 [MyMessage.isInUse] = false 时才使用，通常情况下回收发生在 [MyHandler.removeMessage] 中调用 [MyMessage.recycleUnchecked] 时
 *
 * @author 985892345
 * 2023/3/20 22:14
 */
class MyMessage {
  
  // 用户定义的编号
  var what: Int = 0
  
  // 一个低成本保存 int 的变量
  // 如果是屏障消息则为屏障消息对应的 token(id)
  var arg1: Int = 0
  
  // 要发送给收件人的任意对象
  var any: Any? = null
  
  /**
   * 标志位
   */
  internal var flags = 0
  
  /**
   * 执行时间，由 [SystemClock.uptimeMillis] 获得
   */
  internal var `when`: Long = 0
  
  /**
   * 关联的 Handler
   */
  internal var target: MyHandler? = null
  
  // 内部使用的回调
  internal var callback: Runnable? = null
  
  /**
   * 关联的下一个 Message
   */
  internal var next: MyMessage? = null
  
  /**
   * 回收 Message，供外部调用的方法
   *
   * 如果当前 Message 正处于使用中，在 SDK 21 以上时会抛异常，
   * 如果你手动调用，只有没有在使用的 Message 才能被回收
   *
   * 通常 Message 回收发生在 [MyHandler.removeMessage] 中调用 [MyMessage.recycleUnchecked] 时
   */
  fun recycle() {
    if (isInUse()) {
      @SuppressLint("ObsoleteSdkInt")
      if (Build.VERSION.SDK_INT > 21) {
        throw IllegalStateException("Message 仍然在使用中，无法进行回收")
      }
      return
    }
    recycleUnchecked()
  }
  
  /**
   * 回收 Message
   */
  internal fun recycleUnchecked() {
    flags = FLAG_IN_USE // 回收进池子时仍然设置为在使用的状态
    what = 0
    arg1 = 0
    any = null
    `when` = 0
    target = null
    callback = null
    // todo 待补充
    
    // 用于回收 Message 并添加进回收链表中
    synchronized(sPoolSync) {
      if (sPoolSize < MAX_POOL_SIZE) {
        next = sPool
        sPool = this
        sPoolSize++
      }
    }
  }
  
  /**
   * 消息是否是异步的
   *
   * 如果是异步的，则不受 Looper 同步障碍的约束
   *
   * # Message可以分为三类：同步消息、异步消息、屏障消息(同步屏障)，
   * # 前两者可以通过该方法进行判断
   * # 屏障消息则需要单独判断 Message.target == null (但根据该方法返回值可以得知同步屏障属于同步消息)
   */
  fun isAsynchronous(): Boolean {
    return flags and FLAG_ASYNCHRONOUS != 0
  }
  
  fun setAsynchronous(async: Boolean) {
    flags = if (async) {
      flags or FLAG_ASYNCHRONOUS
    } else {
      flags and FLAG_ASYNCHRONOUS.inv()
    }
  }
  
  // 是否正在使用
  internal fun isInUse(): Boolean {
    return flags and FLAG_IN_USE == FLAG_IN_USE
  }
  
  // 标记开始使用
  internal fun markInUse() {
    flags = flags or FLAG_IN_USE
  }
  
  companion object {
    /**
     * 异步消息标志位
     */
    private val FLAG_ASYNCHRONOUS = 1 shl 1
  
    /**
     * 是否正在使用
     *
     * 该标志仅在创建或获取新消息时清除，即使被回收也是处于使用状态
     */
    private val FLAG_IN_USE = 1 shl 0
  
    /**
     * 用于进行 synchronized 的对象
     *
     * # 不用 MyMessage::class 的原因在于：
     * # 当一个线程使用 object 作为 synchronized 时，它就获得了这个 object 的对象锁
     * # 其它线程对该 object 对象所有同步代码部分的访问都会被暂时阻塞
     */
    private val sPoolSync = Any()
  
    // Message 的回收队列
    private var sPool: MyMessage? = null
  
    // 回收队列的长度
    private var sPoolSize = 0
    
    // 最大的回收队列长度
    private var MAX_POOL_SIZE = 50
    
    // 从回收池中返回一个空闲的 Message
    fun obtain(): MyMessage {
      synchronized(sPoolSync) {
        val m = sPool
        if (m != null) {
          sPool = m.next
          m.next = null
          m.flags = 0 // 这里使用时才清除使用标志
          sPoolSize--
          return m
        }
      }
      return MyMessage()
    }
  }
}