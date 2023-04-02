package com.ndhuz.arraymap

import android.util.ArrayMap
import com.ndhuz.arraymap.utils.ContainerHelpers

/**
 * [ArrayMap]
 *
 * 原理：
 * http://gityuan.com/2019/01/13/arraymap/
 *
 * @author 985892345
 * 2023/4/2 15:23
 */
class MyArrayMap<K, V> : MutableMap<K, V> {
  
  /// 只保存 key hash 值的数组
  private lateinit var mHashes: IntArray
  
  /// 保存 key 和 value 的数组，是 mHashes 数组的两倍长度
  private lateinit var mArray: Array<Any?>
  
  private var mSize = 0
  
  constructor() : this(0)
  
  constructor(capacity: Int) {
    if (capacity < 0) {
      // 如果 capacity < 0 将表示该 ArrayMap 永远为空，且不允许添加元素
      mHashes = EMPTY_IMMUTABLE_INTS
      mArray = EmptyArrayAny
    } else if (capacity == 0) {
      mHashes = EmptyArrayInt
      mArray = EmptyArrayAny
    } else {
      allocArrays(capacity)
    }
  }
  
  override fun isEmpty(): Boolean = mSize <= 0
  
  override fun remove(key: K): V? {
    val index = indexOf(key, key.hashCode())
    if (index >= 0) return removeAt(index)
    return null
  }
  
  @Suppress("UNCHECKED_CAST")
  fun removeAt(index: Int): V? {
    if (index >= mSize) throw ArrayIndexOutOfBoundsException(index)
    
    val old = mArray[(index shl 1) + 1]
    val osize = mSize
    val nsize: Int
    if (osize <= 1) {
      val ohashes = mHashes
      val oarray = mArray
      mHashes = EmptyArrayInt
      mArray = EmptyArrayAny
      freeArrays(ohashes, oarray, osize)
      nsize = 0
    } else {
      nsize = osize - 1
      if (mHashes.size > (BASE_SIZE * 2) && mSize < mHashes.size / 3) {
        //# 如果 mHashes 实际长度大于 2 倍 BASE_SIZE，mSize 小于 mHashes 实际长度的 3 分之 1，则会收紧容量
        val n = if ((osize > (BASE_SIZE * 2))) osize + (osize shr 1) else BASE_SIZE * 2
        
        val ohashes = mHashes
        val oarray = mArray
        allocArrays(n)
        
        /// 对数组进行移动, O(n) 的时间复杂度
        if (index > 0) {
          System.arraycopy(ohashes, 0, mHashes, 0, index)
          System.arraycopy(oarray, 0, mArray, 0, index shl 1)
        }
        if (index < nsize) {
          System.arraycopy(ohashes, index + 1, mHashes, index, nsize - index)
          System.arraycopy(oarray, (index + 1) shl 1, mArray, index shl 1, (nsize - index) shl 1)
        }
      } else {
        if (index < nsize) {
          System.arraycopy(mHashes, index + 1, mHashes, index, nsize - index)
          System.arraycopy(mArray, index + 1 shl 1, mArray, index shl 1, nsize - index shl 1)
        }
        mArray[nsize shl 1] = null
        mArray[(nsize shl 1) + 1] = null
      }
    }
    mSize = nsize
    return old as V?
  }
  
  override fun putAll(from: Map<out K, V>) {
    TODO("Not yet implemented")
  }
  
  /**
   * //# put 新元素时 O(n) 的时间复杂度 (因为 indexOf() 方法采用线性探测法解决 hash 冲突，找不到原本位置的 hash 时将遍历整个数组)
   * //# put 用来修改旧元素时 O(logn) 的时间复杂度
   *
   * //# 扩容时如果 mSize < 4 将扩容为 4   (原因在于直接使用缓存池)
   * //# 4 <= mSize <= 8 时将扩容至 8    (原因在于直接使用缓存池)
   * //# mSize > 8 时将扩容 1.5 倍 (size + (size >> 1))
   */
  @Suppress("UNCHECKED_CAST")
  override fun put(key: K, value: V): V? {
    val osize = mSize
    val hash: Int
    var index: Int
    if (key == null) {
      hash = 0
      index = indexOf(null, 0)
    } else {
      hash = key.hashCode()
      index = indexOf(key, hash)
    }
    if (index > 0) {
      /// 找到对应 key 的情况
      index = index shl 1 + 1 /// 因为 mArray 数组中两个为一组保存 key value
      val old = mArray[index] as V?
      mArray[index] = value
      return old
    }
    
    /// 未找到对应 key，即 put 新元素
    index = index.inv() /// 取反获取插入的位置
    if (osize >= mHashes.size) {
      /// 空间不够需要扩容
      val n =
        if (osize >= (BASE_SIZE * 2)) (osize + (osize shr 1)) /// 如果大于 2 倍 BASE_SIZE，则扩容 1.5 倍
        else if (osize >= BASE_SIZE) BASE_SIZE * 2 /// 如果大于 BASE_SIZE 但小于 2 倍 BASE_SIZE，则扩容至 2 倍 BASE_SIZE，将使用缓存池
        else BASE_SIZE /// 如果小于 BASE_SIZE 则扩容至 BASE_SIZE，也将使用缓存池
      //# 所以这里在长度小于 2 倍 BASE_SIZE 时将扩容至 1 倍或 2 倍 BASE_SIZE，以便直接使用缓存池
      
      val ohashes = mHashes
      val oarray = mArray
      allocArrays(n) /// 重新修改数组长度
      
      if (mHashes.isNotEmpty()) {
        System.arraycopy(ohashes, 0, mHashes, 0, ohashes.size)
        System.arraycopy(oarray, 0, mArray, 0, oarray.size)
      }
      
      freeArrays(ohashes, oarray, osize) /// 回收旧数组
    }
    
    if (index < osize) {
      /// 如果 index 在长度以内，就移动后面的元素，O(n) 的时间复杂度
      System.arraycopy(mHashes, index, mHashes, index + 1, osize - index)
      System.arraycopy(mArray, index shl 1, mArray, (index + 1) shl 1, (mSize - index) shl 1)
    }
    
    mHashes[index] = hash
    mArray[index shl 1] = key
    mArray[(index shl 1) + 1] = value
    mSize++
    return null
  }
  
  @Suppress("UNCHECKED_CAST")
  override fun get(key: K): V? {
    val index = indexOf(key, key.hashCode())
    return if (index >= 0) mArray[(index shl 1) + 1] as V? else null
  }
  
  override fun containsValue(value: V): Boolean {
    val N = mSize * 2
    val array = mArray
    for (i in 1 until N step 2) { // 2 步 2 步的寻找
      if (array[i] == value) return true
    }
    return false
  }
  
  override fun containsKey(key: K): Boolean {
    return indexOf(key, key.hashCode()) >= 0
  }
  
  override fun clear() {
    if (mSize > 0) {
      val ohashes = mHashes
      val oarray = mArray
      val osize = mSize
      mHashes = EmptyArrayInt
      mArray = EmptyArrayAny
      mSize = 0
      freeArrays(ohashes, oarray, osize)
    }
  }
  
  private fun allocArrays(size: Int) {
    if (mHashes === EMPTY_IMMUTABLE_INTS) {
      throw UnsupportedOperationException("ArrayMap is immutable")
    }
    if (size == (BASE_SIZE * 2)) {
      synchronized(sTwiceBaseCacheLock) {
        val array = mTwiceBaseCache
        if (array != null) {
          mArray = array
          try {
            @Suppress("UNCHECKED_CAST")
            mTwiceBaseCache = array[0] as Array<Any?>
            val hashes = array[1] as IntArray?
            if (hashes != null) {
              mHashes = hashes
              array[0] = null
              array[1] = null
              mTwiceBaseCacheSize--
              return
            }
          } catch (_: ClassCastException) {
            // 如果触发了 ClassCastException，则说明缓存池被破坏
            // 大概率是多线程并发出现的问题，具体可以看头注释给出的文章
          }
          // 因为缓存池被破坏，所以这里丢弃缓存池
          mTwiceBaseCache = null
          mTwiceBaseCacheSize = 0
        }
      }
    } else if (size == BASE_SIZE) {
      synchronized(sBaseCacheLock) {
        val array = mBaseCache
        if (array != null) {
          mArray = array
          try {
            @Suppress("UNCHECKED_CAST")
            mBaseCache = array[0] as Array<Any?>
            val hashes = array[1] as IntArray?
            if (hashes != null) {
              mHashes = hashes
              array[0] = null
              array[1] = null
              mBaseCacheSize--
              return
            }
          } catch (_: ClassCastException) {
            // 缓存池被破坏
          }
          mBaseCache = null
          mBaseCacheSize = 0
        }
      }
    }
    
    mHashes = IntArray(size)
    mArray = arrayOfNulls(size shl 1)
  }
  
  private fun freeArrays(hashes: IntArray, array: Array<Any?>, size: Int) {
    if (hashes.size == (BASE_SIZE * 2)) {
      synchronized(sTwiceBaseCacheLock) {
        if (mTwiceBaseCacheSize < CACHE_SIZE) {
          array[0] = mTwiceBaseCache /// 0 位置放之前的缓存
          array[1] = hashes /// 1 位置放 hashes 数组
          for (i in (size shl 1) - 1 downTo 2) {
            array[i] = null /// 清空所有 key 和 value 值
          }
          // 注意：这里并没有清理 hashes 数组
          mTwiceBaseCache = array
          mTwiceBaseCacheSize++
        }
      }
    } else if (hashes.size == BASE_SIZE) {
      synchronized(sBaseCacheLock) {
        if (mBaseCacheSize < CACHE_SIZE) {
          array[0] = mBaseCache /// 0 位置放之前的缓存
          array[1] = hashes /// 1 位置放 hashes 数组
          for (i in (size shl 1) - 1 downTo 2) {
            array[i] = null
          }
          mBaseCache = array
          mBaseCacheSize++
        }
      }
    }
  }
  
  // 官方分为 indexOf(Object, int) 和 indexOfNull() 方法，单独对 null 的情况重写了，因为 key.equals() 会报错
  // 但 kt 使用 == 表示 equals()，遇到 null 时会特殊处理而不报错，所以我只写了这一个方法
  private fun indexOf(key: Any?, hash: Int): Int {
    val N = mSize
    
    if (N == 0) return 0.inv() // 0 取反为 -1
    
    val index = binarySearchHashes(mHashes, N, hash)
    
    if (index < 0) return index // 未找到 hash 值时将返回负数，取反后为插入位置值
    
    if (key == mArray[index shl 1]) {
      return index
    }
    
    /// 由于 mHashes 被回收时并不会被清理，所以存在 mHashes 有值，但 mArray 却没值
    
    var end = index + 1
    while (end < N && mHashes[end] == hash) {
      /// 线性探测法解决 hash 冲突：一直加一寻找空位置
      if (key == mArray[end shl 1]) return end
      
      end++
    }
    
    var i = index - 1
    while (i >= 0 && mHashes[i] == hash) {
      // 前面寻找完了 index 后面的位置都没找到，但仍可能存在 index 前面
      if (key == mArray[i shl 1]) return i
      
      i--
    }
    
    //# 在 put 新元素时这里将遍历整个数组，O(n) 的时间复杂度
    // 彻底没有找到时只能返回 end 的取反值，表示没有找到，取反后还可以表示新元素应该插入的位置
    return end.inv()
  }
  
  companion object {
    private var mBaseCache: Array<Any?>? = null
    private var mBaseCacheSize = 0
    
    private var mTwiceBaseCache: Array<Any?>? = null
    private var mTwiceBaseCacheSize = 0
    
    private val sBaseCacheLock = Any()
    private val sTwiceBaseCacheLock = Any()
    
    // 用来表示不可变的 ArrayMap
    // 在构造 ArrayMap 时传入负的初始长度将表示该 ArrayMap 永远为空，且不允许添加元素
    private val EMPTY_IMMUTABLE_INTS = IntArray(0)
    
    private val EmptyArrayInt = intArrayOf()
    private val EmptyArrayAny = emptyArray<Any?>()
    
    // 会放进缓存池的数组长度
    private val BASE_SIZE = 4
    
    // 缓存池最大容量
    private val CACHE_SIZE = 10
    
    fun binarySearchHashes(hashes: IntArray, N: Int, hash: Int): Int {
      try {
        return ContainerHelpers.binarySearch(hashes, N, hash)
      } catch (e: ArrayIndexOutOfBoundsException) {
        throw ConcurrentModificationException() // 并发修改异常
      }
    }
  }
  
  
  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    get() = TODO("Not yet implemented")
  override val keys: MutableSet<K>
    get() = TODO("Not yet implemented")
  override val size: Int
    get() = mSize
  override val values: MutableCollection<V>
    get() = TODO("Not yet implemented")
}