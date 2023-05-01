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
import com.ndhuz.recyclerview.view.MyRecyclerView.Companion.getChildViewHolderInt
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
  
  /**
   * 执行布局的核心方法
   *
   * 可能会回调多次
   *
   * 支持预测动画意味着该方法会被调用两次；
   * 一次作为“预”布局步骤确定项目在实际布局之前的位置，然后再次进行“实际”布局。
   *
   * 在预布局阶段，rv 会记住它们在布局前的位置，以允许它们被适当地布局。
   * 此外，被移除的 item 将从 scrap 中返回，以帮助确定其他项目的正确放置。
   * 这些移除的项目不应该添加到子列表中，但应该用于帮助计算其他视图的正确位置，
   * 包括以前不在屏幕上的视图（称为 APPEARING 视图），但可以确定其预布局屏幕外位置给出有关预布局删除视图的额外信息。
   *
   * 第二个布局阶段是真正的布局，其中只使用未移除的视图。
   * 在此过程中唯一的附加要求是，如果 [supportsPredictiveItemAnimations] 返回 true，
   * 请注意哪些视图在布局之前存在于子列表中，哪些在布局之后不存在（称为消失视图），
   * 并在不考虑 rv 实际边界的情况下定位或布局这些视图。
   *
   * 这允许动画系统知道这些消失的 item 需要移动到什么位置。
   */
  open fun onLayoutChildren(recycler: MyRecycler, state: MyState) {
  
  }
  
  /**
   * 布局完成后的回调
   *
   * 一次重新布局中 [onLayoutChildren] 可能会回调多次，但 [onLayoutCompleted] 只会回调一次
   */
  open fun onLayoutCompleted(state: MyState) {
  
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
  
  // 回收一级缓存
  internal fun removeAndRecycleScrapInt(recycler: MyRecycler) {
    val scrapCount = recycler.getScrapCount()
    for (i in scrapCount - 1 downTo 0) {
      val scrap = recycler.getScrapViewAt(i)
      val vh = getChildViewHolderInt(scrap)
      if (vh.shouldIgnore()) continue
      // 回收 vh
    }
    recycler.clearScrap()
  }
  
  
  companion object {
    fun chooseSize(spec: Int, desired: Int, min: Int): Int {
      val mode = MeasureSpec.getMode(spec)
      val size = MeasureSpec.getSize(spec)
      return when (mode) {
        MeasureSpec.EXACTLY -> size
        MeasureSpec.AT_MOST -> min(size, max(desired, min))
        MeasureSpec.UNSPECIFIED -> max(desired, min) /// 如果是 UNSPECIFIED，则跟父布局提供的宽高无关联
        else -> max(desired, min)
      }
    }
  }
}