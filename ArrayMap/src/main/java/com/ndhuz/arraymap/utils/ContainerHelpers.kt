package com.ndhuz.arraymap.utils

/**
 * .
 *
 * @author 985892345
 * 2023/4/2 22:24
 */
object ContainerHelpers {
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