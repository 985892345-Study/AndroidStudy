package com.ndhuz.java.thread

/**
 * [Thread]
 *
 * @author 985892345
 * 2023/3/23 18:22
 */
class MyThread : Runnable {
  
  /// 线程局部变量
  internal var threadLocals: MyThreadLocal.MyThreadLocalMap? = null
  
  /// 父子线程局部变量 (但在线程池中会因为线程被复用导致不会更新子线程的值)
  internal var inheritableThreadLocals: MyThreadLocal.MyThreadLocalMap? = null
  
  private var target: Runnable? = null
  
  override fun run() {
    target?.run()
  }
  
  companion object {
    
    private val mThread = ThreadLocal<MyThread>()
    
    fun currentThread(): MyThread {
      var thread = mThread.get()
      if (thread == null) {
        thread = MyThread()
        mThread.set(thread)
      }
      return thread
    }
  }
}