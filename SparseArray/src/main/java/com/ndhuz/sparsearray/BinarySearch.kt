package com.ndhuz.sparsearray

/**
 * .
 *
 * @author 985892345
 * 2023/4/1 22:36
 */
/*
* 如果 l <= r
* 则最后退出的前一个状态一定是 l = r 时，
* 此时会 m = l = r，最后会再进入判断，进行最后一次移动
* 最后退出的当前状态一定是 l 在 r 右边一个位置
*
* 如果 l < r
* 则最后退出的当前状态一定是 l = r 时
*
*
* l <= r 与 l = m 和 r = m 不能同时出现
* l = m 与 r = m 不能同时出现
*
*
* l = m 则取正中偏右
* r = m 则取正中偏左
*
*
* lr 退出情况表
* v < value，l = m   -> 最后 l 的值一定是最靠右的小于值 (l < r，l 初始值为 -1 或者保证 arr[0] > value)
* v <= value，l = m  -> 最后 l 的值一定是最靠右的小于值或最靠右的等于值 (l < r，l 初始值为 -1 或者保证 arr[0] >= value)
* 同理
* v > value，r = m   -> 最后 r 的值一定是最靠左的大于值 (l < r，r 初始值为 length 或者保证 arr[length - 1] < value)
* v >= value，r = m  -> 最后 r 的值一定是最靠左的大于值或最靠左的等于值 (l < r，r 初始值为 length 或者保证 arr[length - 1] <= value)
*
*
* v < value，l = m + 1   -> 最后 l 的值一定是最靠左的等于值或最靠左的大于值
* v <= value，l = m + 1  -> 最后 l 的值一定是最靠左的大于值
* 同理
* v > value，r = m - 1   -> 最后 r 的值一定是最靠右的等于值或最靠右的小于值
* v >= value，r = m - 1  -> 最后 r 的值一定是最靠右的小于值
*
*
* l < r
* l = m + 1
* r = m - 1
* 这种因为最后退出的当前状态一定是 l = r，导致会出现一个未知的情况
* 按照 lr 退出情况表，l 的值一定是最靠左的等于值或最靠左的大于值，r 的值一定是最靠右的小于值，又因为 l = r，所以会出现冲突
* 因此，当 l <= r 时，只有一种写法
* l <= r
* l = m + 1
* r = m - 1
* 此时退出情况为 l 的值 >=/> value，r 的值 </<= value
* 比如：
* value = 2
* [1, 2]    [1]        [2, 3]
*  r  l      r  l    r  l
*
*
* 结论：
* 常规二分写法可以满足所有要求，记住它就可以了
* 在判断时根据 r 紧挨 l 左边和谁不能取等来快速判断是左侧还是右侧
*
* ```kotlin
* while (l <= r) {
*   val m = (l + r) ushr 1
*   val v = array[m]
*   if (v < value) {
*     l = m + 1
*   } else {
*     r = m - 1
*   }
* }
* ```
*
* 比如：
* v < value  -> [1, 2, 2, 3]  r 不能取等，l 只能取等或大于
*                r  l
*
* v <= value -> [1, 2, 2, 3]  l 不能取等，r 只能取等或小于
*                      r  l
* 不取等的那边最后会取等，
* 比如 if (v < value) l = m + 1;  最后 l 会取 value (如果没有的话就是大于值)
* 所以 if (v <= value) l = m + 1; 最后 r 会取 value (如果没有的话就是小于值)
* */

/**
 * value = 2
 * [1, 2, 2, 3]   [2, 2, 3]   [1, 3]   [1]   [2]
 *  ↑            ↑             ↑        ↑   ↑
 * 要找的是最靠右的小于值索引
 */
fun half11(array: IntArray, value: Int): Int {
  var l = -1
  var r = array.size - 1
  while (l < r) {
    val m = l + (r - l + 1) shr 1
    val v = array[m]
    if (v < value) {
      l = m
    } else {
      r = m - 1
    }
  }
  return l
}

// 常规二分写法
fun half12(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v < value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  return r
}

/**
 * value = 2
 * [1, 2, 2, 3]   [1, 3]   [1, 2]   [3, 4]   [1, 1]
 *     ↑           ↑           ↑   ↑             ↑
 * 要找的是最靠右的小于值索引或最靠左的等于值索引
 */
fun half2(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v < value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  // l 取大于或等于值，r 取小于值
  return if (l < array.size && array[l] == value) l else r
}

/**
 * value = 2
 * [1, 2, 2, 3]   [1, 3]   [1, 2]   [3, 4]   [1, 1]
 *        ↑        ↑           ↑   ↑             ↑
 * 要找的是最靠右的小于值索引或最靠右的等于值索引
 */
fun half31(array: IntArray, value: Int): Int {
  var l = -1
  var r = array.size - 1
  while (l < r) {
    val m = l + (r - l + 1) shr 1
    val v = array[m]
    if (v <= value) {
      l = m
    } else {
      r = m - 1
    }
  }
  return l
}

// 常规二分写法
fun half32(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v <= value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  return r
}

/**
 * value = 2
 * [1, 2, 2, 3]   [1, 3]   [1, 2]   [3, 4]   [1, 1]
 *           ↑        ↑          ↑   ↑             ↑
 * 要找的是最靠左的大于值索引
 */
fun half41(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size
  while (l < r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v <= value) {
      l = m + 1
    } else {
      r = m
    }
  }
  return r
}

// 常规二分写法
fun half42(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v <= value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  return l
}

/**
 * value = 2
 * [1, 2, 2, 3]   [1, 3]   [1, 2]   [3, 4]   [1, 1]
 *        ↑           ↑        ↑     ↑             ↑
 * 要找的是最靠左的大于值索引或者最靠右的等于值索引
 */
fun half5(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v <= value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  // l 取大于值，r 取小于或等于值
  return if (array[r] == value) r else l
}

/**
 * value = 2
 * [1, 2, 2, 3]   [1, 3]   [1, 2]   [3, 4]   [1, 1]
 *     ↑              ↑        ↑     ↑             ↑
 * 要找的是最靠左的大于值索引或最靠左的等于值索引
 */
fun half61(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size
  while (l < r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v < value) {
      l = m + 1
    } else {
      r = m
    }
  }
  return r
}

// 常规二分写法
fun half62(array: IntArray, value: Int): Int {
  var l = 0
  var r = array.size - 1
  while (l <= r) {
    val m = (l + r) ushr 1
    val v = array[m]
    if (v < value) {
      l = m + 1
    } else {
      r = m - 1
    }
  }
  return l
}