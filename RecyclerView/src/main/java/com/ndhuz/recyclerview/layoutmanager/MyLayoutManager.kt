package com.ndhuz.recyclerview.layoutmanager

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.MeasureSpec
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.ndhuz.recyclerview.recycler.MyRecycler
import com.ndhuz.recyclerview.utils.MyChildHelper
import com.ndhuz.recyclerview.utils.MyState
import com.ndhuz.recyclerview.view.MyRecyclerView
import kotlin.math.max
import kotlin.math.min

/**
 * [LayoutManager]
 *
 * @author 985892345
 * 2023/4/4 10:25
 */
abstract class MyLayoutManager {
  
  private var _recyclerView: MyRecyclerView? = null
  private val mRecyclerView: MyRecyclerView get() = _recyclerView!!
  
  private var mChildHelper: MyChildHelper? = null
  
  private var mWidth = 0
  private var mHeight = 0
  private var mWidthMode = 0
  private var mHeightMode = 0
  
  internal fun setRecyclerView(recyclerView: MyRecyclerView?) {
    if (recyclerView == null) {
      _recyclerView = null
      mChildHelper = null
      mWidth = 0
      mHeight = 0
    } else {
      _recyclerView = recyclerView
      mChildHelper = recyclerView.mChildHelper
      mWidth = recyclerView.width
      mHeight = recyclerView.height
    }
    mWidthMode = MeasureSpec.EXACTLY
    mHeightMode = MeasureSpec.EXACTLY
  }
  
  /**
   * 给 [mWidth]、[mWidthMode]、[mHeight]、[mHeightMode] 赋值
   */
  @SuppressLint("ObsoleteSdkInt")
  internal fun setMeasureSpecs(wSpec: Int, hSpec: Int) {
    mWidth = MeasureSpec.getSize(wSpec)
    mWidthMode = MeasureSpec.getMode(wSpec)
    if (mWidthMode == MeasureSpec.UNSPECIFIED && Build.VERSION.SDK_INT < 23) {
      // 如果 sdk 小于 23 的话，在 UNSPECIFIED 模式下宽度将设置为 0
      // 这个跟 ViewGroup#getChildMeasureSpec 行为一致
      mWidth = 0
    }
    
    mHeight = MeasureSpec.getSize(hSpec)
    mHeightMode = MeasureSpec.getMode(hSpec)
    if (mWidthMode == MeasureSpec.UNSPECIFIED && Build.VERSION.SDK_INT < 23) {
      mHeight = 0
    }
  }
  
  /**
   * 测量过程是否交给 RecyclerView 的 AutoMeasure 机制
   *
   * [LayoutManager.isAutoMeasureEnabled]
   *
   * @return true -> rv 自己测量; false -> 将交给 LayoutManager 的 [onMeasure] 方法
   */
  open fun isAutoMeasureEnabled(): Boolean {
    return false
  }
  
  /**
   *
   */
  open fun onMeasure(
    recycler: MyRecycler,
    state: MyState,
    widthSpec: Int,
    heightSpec: Int
  ) {
    mRecyclerView.defaultOnMeasure(widthSpec, heightSpec)
  }
  
  open fun onLayoutChildren(recycler: MyRecycler, state: MyState) {
  
  }
  
  // 遍历所有子 View 计算边界
  internal fun setMeasuredDimensionFromChildren(widthSpec: Int, heightSpec: Int) {
    val count = getChildCount()
    if (count == 0) {
      mRecyclerView.defaultOnMeasure(widthSpec, heightSpec)
      return
    }
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE
    repeat(count) {
      val child = getChildAt(it)!!
      val bounds = mRecyclerView.mTempRect
      MyRecyclerView.getDecoratedBoundsWithMarginsInt(child, bounds)
      if (bounds.left < minX) minX = bounds.left
      if (bounds.right > maxX) maxX = bounds.right
      if (bounds.top < minY) minY = bounds.top
      if (bounds.bottom > maxY) maxY = bounds.bottom
    }
    mRecyclerView.mTempRect.set(minX, minY, maxX, maxY)
  }
  
  // 设置自身的宽高
  open fun setMeasuredDimension(childrenBounds: Rect, wSpec: Int, hSpec: Int) {
    val usedWidth: Int = childrenBounds.width() + mRecyclerView.paddingLeft + mRecyclerView.paddingRight
    val usedHeight: Int = childrenBounds.height() + mRecyclerView.paddingTop + mRecyclerView.paddingBottom
    val width = MyLayoutManager.chooseSize(wSpec, usedWidth, mRecyclerView.minimumWidth)
    val height = MyLayoutManager.chooseSize(hSpec, usedHeight, mRecyclerView.minimumHeight)
    mRecyclerView.setMeasuredDimensionInternal(width, height)
  }
  
  // 返回附加到父 RecyclerView 的当前子视图数。这不包括临时分离和/或废弃的子视图。
  fun getChildCount(): Int {
    return mChildHelper?.getChildCount() ?: 0
  }
  
  fun getChildAt(index: Int): View? {
    return mChildHelper?.getChildAt(index)
  }
  
  // 是否支持 item 预测动画 (当项目在布局中添加、删除或移动时，会显示项目的来源和去向)
  open fun supportsPredictiveItemAnimations(): Boolean {
    // LinearLayoutManager、GridLayoutManager、StaggeredGridLayoutManager 都重写以支持项目预测动画
    return false
  }
  
  // 是否二次测量，这是一个内部的 api
  // 用于宽和高都不为精确值，并且至少有一个孩子的宽高也不为精确值时
  internal open fun shouldMeasureTwice(): Boolean {
    return false
  }
  
  fun getWidth(): Int = mWidth
  fun getHeight(): Int = mHeight
  
  companion object {
    fun chooseSize(spec: Int, desired: Int, min: Int): Int {
      val mode = View.MeasureSpec.getMode(spec)
      val size = View.MeasureSpec.getSize(spec)
      return when (mode) {
        View.MeasureSpec.EXACTLY -> size
        View.MeasureSpec.AT_MOST -> min(size, max(desired, min))
        View.MeasureSpec.UNSPECIFIED -> max(desired, min) /// 如果是 UNSPECIFIED，则跟父布局提供的宽高无关联
        else -> max(desired, min)
      }
    }
  }
}