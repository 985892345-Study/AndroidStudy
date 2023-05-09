package com.ndhuz.recyclerview.utils

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 11:18
 */
class MyState {
  
  // 当前测量和布局的步骤
  internal var mLayoutStep = LayoutStep.STEP_START
  
  // 是否处于 onMeasure() 中
  internal var mIsMeasuring = false
  
  // 是否运行 item 动画
  internal var mRunSimpleAnimations = false
  
  // 是否是 item 预测动画
  internal var mRunPredictiveAnimations = false
  
  // 是否调用了 notifyDataSetChanged
  internal var mStructureChanged = false
  
  // 是否处于预布局
  internal var mInPreLayout = false
  
  // 需要运行 item 动画 (mRunSimpleAnimations = true) 并且 item 发生了更新操作
  internal var mTrackOldChangeHolders = false
  
  // 当前 item 数量
  internal var mItemCount = 0
  
  // 上一次布局时的 item 数量
  internal var mPreviousLayoutItemCount = 0
  
  // 未布局，但在上一次布局后删除的 item 数量
  internal var mDeletedInvisibleItemCountSincePreviousLayout = 0
  
  // 在处于预布局时会得到
  fun getItemCount(): Int {
    return if (mInPreLayout) {
      mPreviousLayoutItemCount - mDeletedInvisibleItemCountSincePreviousLayout
    } else mItemCount
  }
  
  enum class LayoutStep {
    STEP_START, // 未布局或布局已完全结束
    STEP_LAYOUT, // dispatchLayoutStep1 已经执行完
    STEP_ANIMATIONS // dispatchLayoutStep2 已经执行完
  }
}