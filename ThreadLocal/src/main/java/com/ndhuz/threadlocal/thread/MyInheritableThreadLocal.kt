package com.ndhuz.threadlocal.thread

/**
 * 可用于父子线程间共享的 ThreadLocal
 *
 * /// InheritableThreadLocal 实现机制
 * 1、在 Thread 构造时复制父线程的 inheritableThreadLocals 变量
 * 2、但这种简单复制，会在线程池中因为复用机制而不能实现更新的效果。
 *   比如：线程池先执行一个任务，这个时候会初始化，复制父线程的 inheritableThreadLocals 值，
 *       在任务运行完后更新父线程的值，再让线程池执行新的任务，可能会因为复用线程而导致子线程值不更新
 *       虽然感觉好像没什么大问题，但如果在大量任务时将出现值不一致的问题
 *
 *
 * @author 985892345
 * 2023/3/24 15:27
 */
class MyInheritableThreadLocal<T> : MyThreadLocal<T>() {
  
  override fun getMap(thread: MyThread): MyThreadLocalMap? {
    return thread.inheritableThreadLocals
  }
  
  override fun createMap(thread: MyThread, value: T?) {
    thread.inheritableThreadLocals = MyThreadLocalMap(this, value)
  }
}