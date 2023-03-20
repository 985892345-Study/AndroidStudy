package com.ndhuz.handler.message

import android.os.SystemClock
import kotlin.random.Random

/**
 * .
 *
 * @author 985892345
 * 2023/3/20 22:14
 */
class MyMessageQueue(quitAllowed: Boolean) {
  
  private var mQuitAllowed: Boolean = quitAllowed // 是否可以终止 Message
  
  private var mQuitting: Boolean = false // 是否已经停止
  
  private val mPtr: Long = nativeInit() // 这个是 native 层 MessageQueue 的引用地址
  
  
  private var mMessages: MyMessage? = null
  
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
  
  
  // 返回 native 层 MessageQueue 的引用地址(https://juejin.cn/post/6844903653589909517)
  private fun nativeInit(): Long {
    return Random.nextLong()
  }
  
  // 唤醒 native 层的 MessageQueue
  private fun nativeWake(ptr: Long) {
  
  }
}