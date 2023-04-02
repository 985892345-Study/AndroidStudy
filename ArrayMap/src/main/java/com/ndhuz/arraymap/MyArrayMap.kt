package com.ndhuz.arraymap

import android.util.ArrayMap

/**
 * [ArrayMap]
 *
 * 原理：
 * http://gityuan.com/2019/01/13/arraymap/
 *
 * @author 985892345
 * 2023/4/2 15:23
 */
class MyArrayMap<K, V> : Map<K, V> {
  override val entries: Set<Map.Entry<K, V>>
    get() = TODO("Not yet implemented")
  override val keys: Set<K>
    get() = TODO("Not yet implemented")
  override val size: Int
    get() = TODO("Not yet implemented")
  override val values: Collection<V>
    get() = TODO("Not yet implemented")
  
  
  
  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }
  
  override fun get(key: K): V? {
    TODO("Not yet implemented")
  }
  
  override fun containsValue(value: V): Boolean {
    TODO("Not yet implemented")
  }
  
  override fun containsKey(key: K): Boolean {
    TODO("Not yet implemented")
  }
  
  companion object {
    private var mBaseCache = emptyArray<Any>()
    private var mBaseCacheSize = 0
  
    private var mTwiceBaseCache = emptyArray<Any>()
    private var mTwiceBaseCacheSize = 0
    
    private val sBaseCacheLock = Any()
    private val sTwiceBaseCacheLock = Any()
  }
}