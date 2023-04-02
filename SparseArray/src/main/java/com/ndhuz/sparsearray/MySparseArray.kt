package com.ndhuz.sparsearray

import android.util.SparseArray
import java.util.*

/**
 * [SparseArray]
 *
 * 原理：
 * https://juejin.cn/post/6844903442901630983
 * https://github.com/zhpanvip/AndroidNote/wiki/SparseArray%E5%AE%9E%E7%8E%B0%E5%8E%9F%E7%90%86
 *
 * @author 985892345
 * 2023/3/24 16:57
 */
class MySparseArray<E> {
  
  // 用来标记是否有元素被移除
  private var mGarbage = false
  
  // key 集合，类型为 int
  private var mKeys: IntArray
  
  // value 集合
  private var mValues: Array<Any?>
  
  private var mSize = 0
  
  // 默认初始化容量为 10 个，跟 ArrayList 默认容量一致
  constructor() : this(10)
  
  constructor(initialCapacity: Int) {
    if (initialCapacity == 0) {
      // 源码中这里肯定是复用的 static 变量，而不是每次都 new 一个新的
      // 这里就暂时这样写
      mKeys = EmptyArrayInt
      mValues = EmptyArrayAny
    } else {
      // 源码中这里调用的底层函数申请 object[]
      if (initialCapacity < 0) throw NegativeArraySizeException(initialCapacity.toString())
      mKeys = IntArray(initialCapacity)
      mValues = arrayOfNulls(initialCapacity)
    }
  }
  
  fun get(key: Int, valueIfKeyNotFound: E? = null): E? {
    val i = ContainerHelpers.binarySearch(mKeys, mSize, key)
    
    if (i < 0 || mValues[i] == DELETED) {
      return valueIfKeyNotFound
    } else {
      @Suppress("UNCHECKED_CAST")
      return mValues[i] as E?
    }
  }
  
  fun delete(key: Int) {
    val i = ContainerHelpers.binarySearch(mKeys, mSize, key)
    
    if (i >= 0) {
      if (mValues[i] != DELETED) {
        /// 并没有真正的移除元素，而是赋值为 DELETED 标记，等待后面 put 时再移除
        mValues[i] = DELETED
        mGarbage = true
      }
    }
    // 注意：这里并没有修改 mSize 大小，mSize 会在调用 size()、keyAt()、valueAt() 等其他需要索引的方法中才会进行 gc，然后修改 mSize
  }
  
  /// 整体逻辑就是把后面 value 不为 DELETED 的往前移动
  /**
   * put() 空间不够时会 gc
   * size() 获取长度时会 gc
   * keyAt()、ValueAt() 跟索引相关的操作会 gc
   */
  private fun gc() {
    val n = mSize
    var o = 0
    val keys = mKeys
    val values = mValues
    
    repeat(n) {
      val v = values[it]
      
      if (v != DELETED) {
        if (it != o) {
          keys[o] = keys[it]
          values[o] = v
          values[it] = null
        }
        
        o++ /// 如果当前 v 是 DELETED 的话就会少加一次
      }
    }
    
    mGarbage = false
    mSize = o
  }
  
  fun put(key: Int, value: E) {
    var i = ContainerHelpers.binarySearch(mKeys, mSize, key)
    
    if (i >= 0) {
      mValues[i] = value
    } else {
      i = i.inv() /// i < 0 时表明没有找到，取反后就是需要插入的位置
      
      if (i < mSize && mValues[i] == DELETED) {
        mKeys[i] = key
        mValues[i] = value
        return
      }
      
      /// 这里有两种情况：1、i 的位置大于或等于 mSize; 2、mValues[i] 当前有数据
      if (mGarbage && mSize >= mKeys.size) {
        /// 如果有删除行为并且数据的长度大于或等于了数组的长度，说明空间不够了，必须要 gc
        gc()
        
        // 因为调用了 gc，所以需要再次查找插入的位置
        i = ContainerHelpers.binarySearch(mKeys, mSize, key).inv()
      }
      
      mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key)
      mValues = GrowingArrayUtils.insert(mValues, mSize, i, value)
      mSize++
    }
  }
  
  fun size(): Int {
    if (mGarbage) {
      gc()
    }
    
    return mSize
  }
  
  fun keyAt(index: Int): Int {
    if (index >= mSize) throw ArrayIndexOutOfBoundsException(index)
    if (mGarbage) {
      gc()
    }
    
    return mKeys[index]
  }
  
  fun valueAt(index: Int): E {
    if (index >= mSize) throw ArrayIndexOutOfBoundsException(index)
    if (mGarbage) {
      gc()
    }
    
    @Suppress("UNCHECKED_CAST")
    return mValues[index] as E
  }
  
  fun indexOfKey(key: Int): Int {
    if (mGarbage) {
      gc()
    }
    /// 如果找不到，则返回负数
    return ContainerHelpers.binarySearch(mKeys, mSize, key)
  }
  
  fun clear() {
    val n = mSize
    val values = mValues
    
    /// 清空只清空了 mValues 数组，并没有清空 mKeys 数组
    repeat(n) {
      values[it] = null
    }
    /*
    * 可能你感觉这里有点奇怪，为什么是直接使用 mSize 来遍历删除，不先调用垃圾回收吗 ?
    *
    * 原因在于如果 mSize 是延后修改的，即 SparseArray 在删除后会把对应的位置暂时清空，
    * 但不会改动 mSize 的大小，所以此时调用 clear() 就会是删之前的总长度，因为期间没有调用 gc() 修改 mSize 大小
    * */
    
    mSize = 0
    mGarbage = false
  }
  
  /// 如果已知插入的元素比队尾大时调用该方法将性能更好
  fun append(key: Int, value: E) {
    /// 当数据需要插入到数组的中间，则调用 put() 来完成
    if (mSize != 0 && key <= mKeys[mSize - 1]) {
      put(key, value)
      return
    }
    
    if (mGarbage && mSize >= mKeys.size) {
      gc()
    }
  
    /// 否则，将数据直接添加到队尾
    mKeys = GrowingArrayUtils.append(mKeys, mSize, key)
    mValues = GrowingArrayUtils.append(mValues, mSize, value)
    mSize++
  }
  
  fun contentHashCode(): Int {
    var hash = 0
    val size = size()
    for (index in 0 until size) {
      val key: Int = keyAt(index)
      val value: E = valueAt(index)
      hash = 31 * hash + Objects.hashCode(key)
      hash = 31 * hash + Objects.hashCode(value)
    }
    return hash
  }
  
  private object ContainerHelpers {
    fun binarySearch(array: IntArray, size: Int, value: Int): Int {
      var l = 0
      var r = size - 1
      while (l <= r) {
        // ushr 算术位移，不改变符号位，也可以防止溢出，但是 a b 不能为负数
        val m = (l + r) ushr 1 // 取正中偏左
        val v = array[m]
        if (v < value) {
          l = m + 1
        } else if (v > value) {
          r = m - 1
        } else {
          return m
        }
      }
      return l.inv() // 取反
      /// 这里取反的原因在于告诉调用者没有找到 key 值，但如果 key 需要插入的话，就是当前位置
    }
  }
  
  private object GrowingArrayUtils {
    fun insert(array: IntArray, currentSize: Int, index: Int, element: Int): IntArray {
      assert(currentSize <= array.size)
      
      if (currentSize + 1 <= array.size) {
        System.arraycopy(array, index, array, index + 1, currentSize - index)
        array[index] = element
        return array
      }
      
      val newArray = IntArray(growSize(currentSize))
      System.arraycopy(array, 0, newArray, 0, index)
      newArray[index] = element
      System.arraycopy(array, index, newArray, index + 1, array.size - index)
      return newArray
    }
    
    fun <T> insert(array: Array<T?>, currentSize: Int, index: Int, element: T): Array<T?> {
      assert(currentSize <= array.size)
      
      if (currentSize + 1 <= array.size) {
        System.arraycopy(array, index, array, index + 1, currentSize - index)
        array[index] = element
        return array
      }
      
      /*
      * 官方底层源码调用的
      * ArrayUtils.newUnpaddedArray((Class<T>)array.getClass().getComponentType(), growSize(currentSize))
      *    ↓
      * (T[])VMRuntime.getRuntime().newUnpaddedArray(clazz, minLen)
      *    ↓
      * native 层方法
      *
      * 应该跟我这种写法相差不大
      * */
      @Suppress("UNCHECKED_CAST")
      val newArray = java.lang.reflect.Array.newInstance(
        array::class.java.componentType!!,
        growSize(currentSize)
      ) as Array<T?>
      System.arraycopy(array, 0, newArray, 0, index)
      newArray[index] = element
      System.arraycopy(array, index, newArray, index + 1, array.size - index)
      return newArray
    }
    
    fun append(array: IntArray, currentSize: Int, element: Int): IntArray {
      assert(currentSize <= array.size)
      
      var newArray = array
      if (currentSize + 1 > array.size) { // 即 currentSize >= array.size
        newArray = IntArray(growSize(currentSize))
        System.arraycopy(array, 0, newArray, 0, currentSize)
      }
      newArray[currentSize] = element
      return array
    }
    
    fun <T> append(array: Array<T?>, currentSize: Int, element: T): Array<T?> {
      assert(currentSize <= array.size)
      
      var newArray = array
      if (currentSize + 1 > array.size) { // 即 currentSize >= array.size
        @Suppress("UNCHECKED_CAST")
        newArray = java.lang.reflect.Array.newInstance(
          array::class.java.componentType!!,
          growSize(currentSize)
        ) as Array<T?>
        System.arraycopy(array, 0, newArray, 0, currentSize)
      }
      newArray[currentSize] = element
      return array
    }
    
    private fun growSize(currentSize: Int): Int {
      /// 二倍扩容，但 ArrayList 是 1.5 倍 (oldCapacity + (oldCapacity >> 1))
      return if (currentSize <= 4) 8 else currentSize * 2
    }
  }
  
  
  companion object {
    // 用来标记此处的值已被删除
    private val DELETED = Any()
    
    private val EmptyArrayInt = intArrayOf()
    private val EmptyArrayAny = emptyArray<Any?>()
  }
}