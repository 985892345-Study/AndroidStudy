package com.ndhuz.recyclerview.utils

import android.view.View

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 15:03
 */
class MyChildHelper(callback: Callback) {
  
  private val mCallBack: Callback = callback
  
  private val mBucket: MyBucket = MyBucket()
  
  private val mHiddenViews = mutableListOf<View>()
  
  internal fun getChildCount(): Int {
    return mCallBack.getChildCount() - mHiddenViews.size
  }
  
  internal fun getChildAt(index: Int): View? {
    val offset = getOffset(index)
    return mCallBack.getChildAt(offset)
  }
  
  private fun getOffset(index: Int): Int {
    if (index < 0) return -1
    val limit = mCallBack.getChildCount()
    var offset = index
    while (offset < limit) {
      val removeBefore = mBucket.countOnesBefore(offset)
      val diff = index - (offset - removeBefore)
      if (diff == 0) {
        while (mBucket.get(offset)) {
          // 确保偏移量没有被隐藏
          offset++
        }
        return offset
      } else {
        offset += diff
      }
    }
    return -1
  }
  
  interface Callback {
    
    // 最后调用的 RecyclerView#getChildCount
    fun getChildCount(): Int
    
    // 最后调用的 RecyclerView#getChildAt
    fun getChildAt(offset: Int): View?
  }
}