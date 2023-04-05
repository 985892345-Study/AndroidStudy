package com.ndhuz.recyclerview.utils

/**
 * 提供偏移索引方法的位集实现
 *
 * 一个工具类，类似于 Map<Int, Boolean> 和 List<Boolean> 的结合
 *
 * https://blog.csdn.net/u012227177/article/details/73381598
 *
 * @author 985892345
 * 2023/4/4 15:15
 */
class MyBucket {
  
  companion object {
    val BITS_PER_WORD = Long.SIZE_BITS // Long 的二进制位数
    val LAST_BIT = 1L shl (Long.SIZE_BITS - 1) // Long 二进制位数的最后一位
  }
  
  // 当前的标记位
  private var mData = 0L
  
  // 下一个 Bucket
  lateinit var mNext: MyBucket
  
  /**
   * 设置 [index] 位置标记位为 1
   *
   * 即
   * ```
   * map[index] = true
   * ```
   */
  fun set(index: Int) {
    if (index >= BITS_PER_WORD) {
      ensureNext()
      mNext.set(index - BITS_PER_WORD)
    } else {
      mData = mData or (1L shl index)
    }
  }
  
  /**
   * 得到 [index] 的 Boolean 状态
   *
   * ```
   * map[index] ?: false
   * ```
   */
  fun get(index: Int): Boolean {
    if (index >= BITS_PER_WORD) {
      ensureNext()
      return mNext.get(index - BITS_PER_WORD)
    } else {
      return mData and (1L shl index) != 0L
    }
  }
  
  /**
   * 设置 [index] 位置标记位为 0
   *
   * ```
   * map[index] = false
   * ```
   */
  fun clear(index: Int) {
    if (index >= BITS_PER_WORD) {
      if (this::mNext.isInitialized) {
        mNext.clear(index - BITS_PER_WORD)
      }
    } else {
      mData = mData and (1L shl index).inv()
    }
  }
  
  /**
   * 标志位全部清空为 0
   *
   * ```
   * map.clear()
   * ```
   */
  fun reset() {
    mData = 0
    if (this::mNext.isInitialized) {
      mNext.reset()
    }
  }
  
  /**
   * 插入新的标记到 [index] 位置
   *
   * ```
   * list.add(index, value) // index 后的位置都会往后移动
   *
   * bucket.set(2)            // 0x00100
   * bucket.set(3)            // 0x01100
   * bucket.insert(3, false)  // 0x11100
   * ```
   */
  fun insert(index: Int, value: Boolean) {
    if (index >= BITS_PER_WORD) {
      ensureNext()
      mNext.insert(index - BITS_PER_WORD, value)
    } else {
      val lastBit = (mData and LAST_BIT) != 0L
      val mask = (1L shl index) - 1
      val before = mData and mask
      val after = (mData and mask.inc()) shl 1
      mData = before or after
      if (value) {
        set(index)
      } else {
        clear(index)
      }
      if (lastBit || this::mNext.isInitialized) {
        ensureNext()
        // 将当前 bucket 最高位的 bit 插入到 next 的 index 为 0 的位置上（实现左移操作）
        mNext.insert(0, lastBit)
      }
    }
  }
  
  /**
   * 移除 [index] 位置的标记
   *
   * ```
   * list.remove(index) // index 后的位置都会往前移动
   * ```
   */
  fun remove(index: Int): Boolean {
    if (index >= BITS_PER_WORD) {
      ensureNext()
      return mNext.remove(index - BITS_PER_WORD)
    } else {
      var mask = (1L shl index)
      val value = (mData and mask) != 0L
      mData = mData and mask.inc()
      mask -= 1
      val before = mData and mask
      val after = java.lang.Long.rotateRight(mData and mask.inv(), 1)
      mData = before or after
      if (this::mNext.isInitialized) {
        if (mNext.get(0)) {
          set(BITS_PER_WORD - 1)
        }
        mNext.remove(0)
      }
      return value
    }
  }
  
  /**
   * 计算比 [index] 小的所有位数上 bit 为 1 的总个数
   *
   * ```
   * 位数：  543210
   * 例如：0x011010
   * countOnesBefore(5) = 3
   * countOnesBefore(4) = 2
   * ```
   */
  fun countOnesBefore(index: Int): Int {
    if (!this::mNext.isInitialized) {
      if (index >= BITS_PER_WORD) {
        return java.lang.Long.bitCount(mData)
      }
      return java.lang.Long.bitCount(mData and (1L shl index) - 1)
    }
    if (index < BITS_PER_WORD) {
      return java.lang.Long.bitCount(mData and (1L shl index) - 1)
    } else {
      return mNext.countOnesBefore(index - BITS_PER_WORD) + java.lang.Long.bitCount(mData)
    }
  }
  
  private fun ensureNext() {
    if (!this::mNext.isInitialized) {
      mNext = MyBucket()
    }
  }
}