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
  
  // 是否处于预布局
  internal var mInPreLayout = false
  
  enum class LayoutStep {
    STEP_START,
    STEP_LAYOUT,
    STEP_ANIMATIONS
  }
}