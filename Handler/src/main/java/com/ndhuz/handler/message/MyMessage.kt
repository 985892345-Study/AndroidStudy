package com.ndhuz.handler.message

import android.os.SystemClock

/**
 * .
 *
 * @author 985892345
 * 2023/3/20 22:14
 */
class MyMessage {
  
  /**
   * 执行时间，由 [SystemClock.uptimeMillis] 获得
   */
  internal var `when`: Long = 0
  
  /**
   * 关联的下一个 Message
   */
  internal var next: MyMessage? = null
  
  /**
   * 回收 Message
   */
  internal fun recycleUnchecked() {
    `when` = 0
    // todo 待补充
  }
}