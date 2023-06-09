package com.ndhuz.threadlocal.thread

import java.lang.ref.WeakReference
import kotlin.random.Random

/**
 * [ThreadLocal] 线程局部变量，主要用于线程间数据隔离
 *
 * 讲解 + 面试题：
 * https://juejin.cn/post/7097754858593189901
 *
 *
 *
 * @author 985892345
 * 2023/3/23 18:23
 */
open class MyThreadLocal<T> {
  
  /**
   * ThreadLocal 的自定义 hashcode 值，只用于 ThreadLocalMap 中
   *
   * 为什么用自定义的 hashcode，我也不知道，官方只是这样提道：
   * 它消除了相同线程使用连续构造的 ThreadLocal 的常见情况下的冲突，同时在不太常见的情况下保持良好的行为。
   *
   * 源码中是使用了一个 static 的 AtomicInteger 变量加上一个 int 值，每创建一个 ThreadLocal 就多加一次
   */
  private val threadLocalHashCode: Int = Random.nextInt()
  
  protected open fun initialValue(): T? {
    return null
  }
  
  fun get(): T? {
    val t = MyThread.currentThread()
    val map = getMap(t)
    if (map != null) {
      val e = map.getEntry(this)
      if (e != null) {
        @Suppress("UNCHECKED_CAST")
        return e.value as T?
      }
    }
    return setInitialValue()
  }
  
  private fun setInitialValue(): T? {
    val value = initialValue()
    val thread = MyThread.currentThread()
    val map = getMap(thread)
    if (map != null) {
      map.set(this, value)
    } else {
      createMap(thread, value)
    }
    return value
  }
  
  fun set(value: T?) {
    val thread = MyThread.currentThread()
    val map  = getMap(thread)
    if (map != null) {
      map.set(this, value)
    } else {
      createMap(thread, value)
    }
  }
  
  fun remove() {
    getMap(MyThread.currentThread())?.remove(this)
  }
  
  protected open fun getMap(thread: MyThread): MyThreadLocalMap? {
    return thread.threadLocals
  }
  
  protected open fun createMap(thread: MyThread, value: T?) {
    thread.threadLocals = MyThreadLocalMap(this, value)
  }
  
  class MyThreadLocalMap {
    
    constructor(firstKey: MyThreadLocal<*>, firstValue: Any?) {
      table = arrayOfNulls(INITIAL_CAPACITY) // 初始长度为 16，跟 HashMap 一样
      val i = firstKey.threadLocalHashCode and (INITIAL_CAPACITY - 1)
      table[i] = Entry(firstKey, firstValue)
      threshold = INITIAL_CAPACITY * 2 / 3
    }
    
    constructor(parentMap: MyThreadLocalMap) {
      val parentTable = parentMap.table
      val len = parentTable.size
      table = arrayOfNulls(len)
      threshold = len * 2 / 3
      
      for (j in 0 until len) {
        val e = parentTable[j]
        if (e != null) {
          val key = e.get()
          if (key != null) {
            val value = e.value
            val c = Entry(key, value)
            var h = key.threadLocalHashCode and (len - 1)
            while (table[h] != null) {
              /// 如果 hash 冲突就一直取下一个位置
              h = nextIndex(h, len)
            }
            table[h] = c
            size++
          }
        }
      }
    }
    
    // Entry 数组
    private val table: Array<Entry?>
    
    // 需要扩容 Entry 数组时的长度
    private val threshold: Int
    
    // Entry 数组长度值
    private var size = 0
    
    
    internal fun getEntry(key: MyThreadLocal<*>): Entry? {
      val i = key.threadLocalHashCode and (table.size - 1) // 这个相当于取模运算
      val e = table[i]
      if (e != null && e.get() == key) {
        return e
      } else {
        return getEntryAfterMiss(key, i, e)
      }
    }
    
    // 如果 hash 没有命中就遍历寻找后面的位置
    private fun getEntryAfterMiss(key: MyThreadLocal<*>, i: Int, e: Entry?): Entry? {
      val tab = table
      val len = tab.size
      var index = i
      var entry = e
      
      while (entry != null) {
        if (entry.get() === key) {
          return entry
        }
        if (entry.get() == null) {
          /// 已被回收，清理脏数据
          // 清理过程是遍历 index 后的所有已回收的数据，并同时将之前添加时 hash 冲突的数据往前移动，
          // 将数据尽量靠拢左侧，减少以后的遍历长度
          expungeStaleEntry(index)
        } else {
          index = nextIndex(index, len)
        }
        entry = tab[index]
      }
      return null
    }
    
    // 本来是 private 方法，但 kt 不允许外部类调用内部类的 private 方法
    internal fun set(key: MyThreadLocal<*>, value: Any?) {
      val tab = table
      val len = tab.size
      var i = key.threadLocalHashCode and (len - 1)
      
      var e = tab[i]
      while (e != null) {
        if (e.get() === key) {
          e.value = value
          return
        }
        
        if (e.get() == null) {
          /// 已被回收，替换 value 并清理脏数据
          replaceStaleEntry(key, value, i)
          return
        }
        
        i = nextIndex(i, len)
        e = tab[i]
      }
      
      tab[i] = Entry(key, value)
      size++
      // ...
      /// 如果当前长度大于 threshold - threshold / 4，则会进行扩容，每次扩容倍数为 2 倍，与 HashMap 一致
    }
    
    internal fun remove(key: MyThreadLocal<*>) {
      val tab = table
      val len = tab.size
      var i = key.threadLocalHashCode and (len - 1)
      var e = tab[i]
      while (e != null) {
        if (e.get() === key) {
          e.clear()
          expungeStaleEntry(i)
          return
        }
        
        i = nextIndex(i, len)
        e = tab[i]
      }
    }
    
    private fun replaceStaleEntry(key: MyThreadLocal<*>, value: Any?, staleSlot: Int) {
      // ... 替换 value 并清理脏数据
    }
    
    //
    private fun expungeStaleEntry(staleSlot: Int) {
      // ... 清理脏数据
    }
    
    class Entry(
      key: MyThreadLocal<*>,
      var value: Any?
    ) : WeakReference<MyThreadLocal<*>>(key) // 注意: 这里是继承的 WeakReference
    
    companion object {
      // 初始化时 Entry 数组的长度，必须是 2 的幂
      private const val INITIAL_CAPACITY = 16
      
      // 取 Entry 数组中的下一个位置
      private fun nextIndex(i: Int, len: Int): Int {
        return if (i + 1 < len) i + 1 else 0
      }
    }
  }
}