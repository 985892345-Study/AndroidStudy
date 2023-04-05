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
  
  // 跳过预处理并一次性应用所有更新动画
  internal fun consumeUpdatesInOnePass() {
  
  }
  
  // 是否有更新
  internal fun hasUpdates(): Boolean {
    return mPostponedList.isNotEmpty() && mPendingUpdates.isNotEmpty()
  }
  
  class UpdateOp {
  
  }
  
  interface Callback {
  
  }
}