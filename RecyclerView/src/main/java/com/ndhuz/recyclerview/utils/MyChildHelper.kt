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
  
  // 这个跟 mHiddenViews 相关，主要是记录隐藏 item 的位置
  private val mBucket: MyBucket = MyBucket()
  
  private val mHiddenViews = mutableListOf<View>()
  
  // 返回未隐藏的 item 总数
  internal fun getChildCount(): Int {
    return mCallBack.getChildCount() - mHiddenViews.size
  }
  
  // 返回孩子的总数，包括隐藏的 item
  internal fun getUnfilteredChildCount(): Int {
    return mCallBack.getChildCount()
  }
  
  internal fun getChildAt(index: Int): View {
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
  
  internal fun isHidden(view: View): Boolean {
    return mHiddenViews.contains(view)
  }
  
  private fun unhideViewInternal(child: View) {
    if (mHiddenViews.remove(child)) {
      mCallBack
    }
  }
  
  internal fun removeViewAt(index: Int) {
    val offset = getOffset(index)
    if (mBucket.remove(offset)) {
      // ...
    }
    mCallBack.removeViewAt(index)
  }
  
  internal fun detachViewFromParent(index: Int) {
    val offset = getOffset(index)
    mBucket.remove(offset)
    mCallBack.detachViewFromParent(index)
  }
  
  interface Callback {
    
    // 最后调用的 RecyclerView#getChildCount
    fun getChildCount(): Int
    
    // 移除 view
    fun removeViewAt(index: Int)
    
    // 最后调用的 RecyclerView#getChildAt
    fun getChildAt(offset: Int): View
    
    // 暂时分离子 View
    fun detachViewFromParent(offset: Int)
  }
}