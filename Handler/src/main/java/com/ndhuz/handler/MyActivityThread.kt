package com.ndhuz.handler

import com.ndhuz.handler.looper.MyLooper

/**
 * .
 *
 * @author 985892345
 * 2023/3/20 22:34
 */
class MyActivityThread {
  companion object {
    fun main() {
      
      // ...
      
      // 设置当前应用进程的主 Looper，该方法只允许系统调用
      MyLooper.prepareMainLooper()
      
      // ...
      
      MyLooper.loop()
      
      // 这里永远不会执行
      throw RuntimeException("Main thread loop unexpectedly exited")
    }
  }
}