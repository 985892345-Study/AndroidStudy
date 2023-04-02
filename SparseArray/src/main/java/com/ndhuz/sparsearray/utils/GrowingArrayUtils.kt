package com.ndhuz.sparsearray.utils

/**
 * .
 *
 * @author 985892345
 * 2023/4/2 22:24
 */
object GrowingArrayUtils {
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