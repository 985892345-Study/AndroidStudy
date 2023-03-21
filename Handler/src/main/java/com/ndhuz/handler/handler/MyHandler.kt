package com.ndhuz.handler.handler

import android.os.SystemClock
import com.ndhuz.handler.looper.MyLooper
import com.ndhuz.handler.message.MyMessage
import com.ndhuz.handler.message.MyMessageQueue

/**
 * .
 *
 * @author 985892345
 * 2023/3/20 22:13
 */
open class MyHandler {
  
  @Deprecated("")
  constructor() : this(null, false)
  constructor(callback: Callback?, async: Boolean) {
    mLooper = MyLooper.myLooper() ?: throw RuntimeException("当前线程下不存在 Looper")
    mQueue = mLooper.mQueue
    mCallback = callback
    mAsynchronous = async
  }
  constructor(looper: MyLooper, callback: Callback? = null, async: Boolean = false) {
    mLooper = looper
    mQueue = mLooper.mQueue
    mCallback = callback
    mAsynchronous = async
  }
  
  private val mLooper: MyLooper
  private val mQueue: MyMessageQueue
  private val mCallback: Callback?
  private val mAsynchronous: Boolean // 是否发送异步消息
  
  /**
   * 子类实现它用于接收回调
   */
  open fun handleMessage(msg: MyMessage) {
  
  }
  
  fun post(r: Runnable): Boolean {
    /// post 只需要 Runnable 不需要 Message 的原因在于它从 Message 回收池里拿了一个，并设置了 callback
    val m = MyMessage.obtain()
    m.callback = r
    return sendMessageDelayed(m, 0)
  }
  
  fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
    val m = MyMessage.obtain()
    m.callback = r
    return sendMessageDelayed(m, delayMillis)
  }
  
  fun sendMessageDelayed(msg: MyMessage, delayMillis: Long): Boolean {
    var delay = delayMillis
    if (delayMillis < 0) {
      delay = 0
    }
    return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delay)
  }
  
  fun sendMessageAtTime(msg: MyMessage, uptimeMillis: Long): Boolean {
    return enqueueMessage(mQueue, msg, uptimeMillis)
  }
  
  private fun enqueueMessage(queue: MyMessageQueue, msg: MyMessage, uptimeMillis: Long): Boolean {
    msg.target = this /// 设置 Message 的 target 为当前 Handler
    if (mAsynchronous) {
      msg.setAsynchronous(true)
    }
    return queue.enqueueMessage(msg, uptimeMillis)
  }
  
  fun removeMessage(what: Int) {
    mQueue.removeMessage(this, what, null)
  }
  
  fun removeCallbacks(r: Runnable) {
    mQueue.removeMessage(this, r, null)
  }
  
  /**
   * Looper 分发消息
   */
  fun dispatchMessage(msg: MyMessage) {
    if (msg.callback != null) {
      /// 如果 callback 不为 null，这说明是通过 post 添加进来的
      msg.callback?.run()
    } else {
      if (mCallback != null) {
        /// mCallback 不为 null 则说明是直接用的 Handler 而不是 Handler 的子类
        if (mCallback.handleMessage(msg)) {
          return
        }
      }
      handleMessage(msg)
    }
  }
  
  /**
   * 在不继承 Handler 的情况下可以使用该 Callback 接口用于接收回调
   */
  interface Callback {
  
    /**
     * @return 如果不需要进一步处理则为真
     */
    fun handleMessage(msg: MyMessage): Boolean
  }
}