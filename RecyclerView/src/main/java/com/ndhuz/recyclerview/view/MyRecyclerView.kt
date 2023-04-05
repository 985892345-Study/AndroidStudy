package com.ndhuz.recyclerview.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ndhuz.recyclerview.adapter.MyAdapter
import com.ndhuz.recyclerview.adapter.MyAdapterHelper
import com.ndhuz.recyclerview.animation.MyDefaultItemAnimator
import com.ndhuz.recyclerview.animation.MyItemAnimator
import com.ndhuz.recyclerview.layoutmanager.MyLayoutManager
import com.ndhuz.recyclerview.recycler.MyRecycler
import com.ndhuz.recyclerview.utils.MyChildHelper
import com.ndhuz.recyclerview.utils.MyLayoutParams
import com.ndhuz.recyclerview.utils.MyState

/**
 * [RecyclerView]
 *
 * 布局：
 * https://juejin.cn/post/6931894526160142350
 * https://juejin.cn/post/6910128327681474567
 *
 * @author 985892345
 * 2023/4/4 10:21
 */
class MyRecyclerView(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
  
  private lateinit var mAdapter: MyAdapter<*>
  
  private lateinit var mLayout: MyLayoutManager
  
  private val mRecycler = MyRecycler()
  
  private val mState = MyState()
  
  internal val mTempRect = Rect()
  
  // 如果 RV 的大小不受 Adapter 影响则可以设置为 true 进行优化
  private var mHasFixedSize = false
  
  // 是否在测量期间发生了 Adapter 更新
  private var mAdapterUpdateDuringMeasure = false
  
  // item 动画
  private var mItemAnimator: MyItemAnimator? = MyDefaultItemAnimator()
  
  internal val mChildHelper = MyChildHelper(
    object : MyChildHelper.Callback {
      override fun getChildCount(): Int {
        return this@MyRecyclerView.childCount
      }
  
      override fun getChildAt(offset: Int): View? {
        return this@MyRecyclerView.getChildAt(offset)
      }
    }
  )
  
  private val mAdapterHelper = MyAdapterHelper(
    object : MyAdapterHelper.Callback {
    
    }
  )
  
  override fun onMeasure(widthSpec: Int, heightSpec: Int) {
    if (!this::mLayout.isInitialized) {
      /// 在未设置 mLayout 调用默认的测量
      defaultOnMeasure(widthSpec, heightSpec)
      return
    }
    if (mLayout.isAutoMeasureEnabled()) {
      /// 如果交给 RV 的自动测量机制
      // 常用的 LinearLayoutManager 就是自动测量的
      val widthMode = MeasureSpec.getMode(widthSpec)
      val heightMode = MeasureSpec.getMode(heightSpec)
      
      // 这里为了兼容旧代码才调用的 mLayout.onMeasure()
      // 官方源码中表示在 mLayout.isAutoMeasureEnabled() 返回 true 时不建议重写 LayoutManager#onMeasure 方法
      // 正确逻辑来说，这里应该是直接调用 defaultOnMeasure(int, int) 的
      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
      
      val measureSpecModeIsExactly =
        widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY
      if (measureSpecModeIsExactly || !this::mAdapter.isInitialized) {
        /// 如果宽和高都是精确值，则测量已经完成，后面将走 onLayout() 流程
        return
      }
      
      /// 走到这里说明宽和高有一边不为精确值
      
      if (mState.mLayoutStep == MyState.LayoutStep.STEP_START) {
        dispatchLayoutStep1()
      }
      // 在第二步中设置尺寸。为了保持一致性，应该使用旧尺寸进行预布局
      mLayout.setMeasureSpecs(widthSpec, heightSpec)
      mState.mIsMeasuring = true
      dispatchLayoutStep2()
      
      
      // 现在可以从子 View 中获取宽和高，从而获得自身边界值
      mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec)
      
      /// 如果 RecyclerView 的宽度和高度不准确，并且至少有一个孩子的宽度和高度也不准确，会重新测量
      if (mLayout.shouldMeasureTwice()) {
        mLayout.setMeasureSpecs(
          MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
        mState.mIsMeasuring = true
        dispatchLayoutStep2()
        mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec)
      }
    } else {
      /// 交给 LayoutManager 的自定义 onMeasure() 方法进行测量
      if (mHasFixedSize) {
        /// 如果 RV 宽高不受 Adapter 影响，则直接进行测量
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
        return
      }
      if (mAdapterUpdateDuringMeasure) {
        /// 如果 Adapter 发生了更新操作
        
        /// 预加载动画
        processAdapterUpdatesAndSetAnimationFlags()
        
        if (mState.mRunPredictiveAnimations) {
          /// 如果需要预测动画，则会开启预布局
          mState.mInPreLayout = true
        } else {
          mAdapterHelper.consumeUpdatesInOnePass()
          mState.mInPreLayout = false
        }
      } else if (mState.mRunPredictiveAnimations) {
        // 如果 mAdapterUpdateDuringMeasure 为假且 mRunPredictiveAnimations 为真：
        // 这意味着已经执行了一个 onMeasure() 调用来处理挂起的适配器更改，
        // 如果 RV 是 layout_width = MATCH_PARENT 的 LinearLayout 的子布局，
        // 则可能会发生两次 onMeasure() 调用。
        // RV 不能第二次调用 LM.onMeasure()，因为 LM 使用 child 测量时 getViewForPosition() 会崩溃。
        setMeasuredDimension(measuredWidth, measuredHeight)
        return
      }
      
      // ...
      
      /// 前面处理了动画，最后调用 LayoutManager.onMeasure() 进行自定义布局
      /// 但值得注意的是这里仅仅是在 mLayout.isAutoMeasureEnabled() = false 时才会调用
      /// LinearLayoutManager 等官方 LM 都是使用了自动测量机制
      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
      mState.mInPreLayout = false
    }
  }
  
  
  
  /**
   * 使用适配器更新并计算我们要运行的动画类型。在 onMeasure 和 dispatchLayout 中调用。
   * 此方法可能仅处理更新的预布局状态或所有更新。
   */
  private fun processAdapterUpdatesAndSetAnimationFlags() {
    // ...
    if (predictiveItemAnimationsEnabled()) {
      // 如果支持 item 移动动画
      mAdapterHelper.preProcess()
    } else {
      // 不支持 item 移动动画
      mAdapterHelper.consumeUpdatesInOnePass()
    }
    // 源码中这里有一堆判断，如果是添加、删除或修改就会执行动画
    mState.mRunSimpleAnimations = mItemAnimator != null
    // 源码中这里有一堆判断
    mState.mRunPredictiveAnimations = mState.mRunSimpleAnimations
      && predictiveItemAnimationsEnabled()
  }
  
  private fun predictiveItemAnimationsEnabled(): Boolean {
    return mItemAnimator != null && mLayout.supportsPredictiveItemAnimations()
  }
  
  internal fun defaultOnMeasure(widthSpec: Int, heightSpec: Int) {
    val width = MyLayoutManager.chooseSize(
      widthSpec,
      paddingLeft + paddingRight,
      ViewCompat.getMinimumWidth(this)
    )
    val height = MyLayoutManager.chooseSize(
      heightSpec,
      paddingTop + paddingBottom,
      ViewCompat.getMinimumHeight(this)
    )
    setMeasuredDimension(width, height)
  }
  
  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    dispatchLayout()
  }
  
  private fun dispatchLayout() {
    if (this::mAdapter.isInitialized) return
    if (this::mLayout.isInitialized) return
    mState.mIsMeasuring = false
    if (mState.mLayoutStep == MyState.LayoutStep.STEP_START) {
      /// 在 mLayout.isAutoMeasureEnabled() = false，即不采用自动测量机制时会到这里面
      /// 一般发生在自定义 LayoutManager 中
      /// 或者在宽高都为精确值时也会走到这里，因为 onMeasure() 中没有调用 dispatchLayoutStep1()
      dispatchLayoutStep1()
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
      dispatchLayoutStep2()
    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != width || mLayout.getHeight() != height) {
      /// 这里说明 onMeasure() 中已经调用了 dispatchLayoutStep1() 和 dispatchLayoutStep2()
      /// 但因为有了新的更新或宽高发生改变，需要再次调用 dispatchLayoutStep2() 进行布局
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
      dispatchLayoutStep2()
    } else {
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
    }
    dispatchLayoutStep3()
  }
  
  /**
   * //# 1、判断是否需要开启动画
   * //# 2、如果开启动画的话就保存旧的 ViewHolder 信息
   * //# 2、如果是 item 移动动画就调用 LayoutManager.onLayoutChildren() 进行预布局并记录新增的 ViewHolder
   */
  private fun dispatchLayoutStep1() {
    // ...
    mState.mIsMeasuring = false
    // ...
    /// 判断是否需要开启动画
    processAdapterUpdatesAndSetAnimationFlags()
    mState.mInPreLayout = mState.mRunPredictiveAnimations
    // ...
    if (mState.mRunSimpleAnimations) {
      repeat(mChildHelper.getChildCount()) {
        /// 保存旧的 item 信息
      }
    }
    if (mState.mRunPredictiveAnimations) {
      // ...
      /// 调用 onLayoutChildren() 预布局
      mLayout.onLayoutChildren(mRecycler, mState)
      // ...
      repeat(mChildHelper.getChildCount()) {
        /// 跟之前的旧信息进行比对，记录新增的 item 信息
      }
    }
    // ...
    mState.mLayoutStep = MyState.LayoutStep.STEP_LAYOUT
  }
  
  /**
   * //# 真正用于布局的方法，会回调 LayoutManager.onLayoutChildren()
   */
  private fun dispatchLayoutStep2() {
    // ...
    /// 正式布局
    mLayout.onLayoutChildren(mRecycler, mState)
    // ...
  }
  
  /**
   * //# 根据 dispatchLayoutStep1() 保存的信息执行动画
   * //# 该方法不参与实际的布局
   */
  private fun dispatchLayoutStep3() {
    /// 只执行动画
  }
  
  // 供 MyLayoutManager 调用，因为 setMeasuredDimension() 是 protected
  internal fun setMeasuredDimensionInternal(measuredWidth: Int, measuredHeight: Int) {
    setMeasuredDimension(measuredWidth, measuredHeight)
  }
  
  companion object {
    
    /**
     * 返回视图的边界，包括它的装饰和边距。
     */
    fun getDecoratedBoundsWithMarginsInt(view: View, outBounds: Rect) {
      val lp = view.layoutParams as MyLayoutParams
      val insets = lp.mDecorInsets
      outBounds.set(
        view.left - insets.left - lp.leftMargin,
        view.top - insets.top - lp.topMargin,
        view.right + insets.right + lp.rightMargin,
        view.bottom + insets.bottom + lp.bottomMargin
      )
    }
  }
}