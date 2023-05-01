package com.ndhuz.recyclerview.adapter

/**
 * https://blog.csdn.net/wei7017406/article/details/106150851
 *
 * @author 985892345
 * 2023/4/4 17:36
 */
class MyAdapterHelper(callback: Callback) {
  
  private val mCallback = callback
  
  private val mPendingUpdates = ArrayList<UpdateOp>()
  
  private val mPostponedList = ArrayList<UpdateOp>()
  
  // 动画预处理
  internal fun preProcess() {
  
  }
  
  // 一次性应用所有更新
  internal fun consumeUpdatesInOnePass() {
  
  }
  
  // 根据当前位置由待定的更新计算出更新后的位置
  fun applyPendingUpdatesToPosition(position: Int): Int {
    // 这里面实际的操作为遍历 mPendingUpdates 根据 增、删、移 动画来给 position 加一或减一
    return position
  }
  
  // 是否有更新
  internal fun hasUpdates(): Boolean {
    return mPostponedList.isNotEmpty() && mPendingUpdates.isNotEmpty()
  }
  
  class UpdateOp {
    companion object {
      val ADD = 1
      val REMOVE = 1 shl 1
      val UPDATE = 1 shl 2
      val MOVE = 1 shl 3
    }
  }
  
  interface Callback {
  }
}