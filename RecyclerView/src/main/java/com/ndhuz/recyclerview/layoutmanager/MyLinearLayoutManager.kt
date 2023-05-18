package com.ndhuz.recyclerview.layoutmanager

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndhuz.recyclerview.recycler.MyRecycler
import com.ndhuz.recyclerview.utils.MyState
import com.ndhuz.recyclerview.viewholder.MyViewHolder

// https://juejin.cn/post/6844903924256735239
// https://juejin.cn/post/6844904003394863112

/**
 *
 * [LinearLayoutManager]
 *
 * @author 985892345
 * 2023/4/4 23:51
 */
class MyLinearLayoutManager : MyLayoutManager() {
  
  // 当 LayoutManager 需要滚动到某个位置时，它会设置此变量并请求一个布局，该布局将检查此变量并相应地重新布局。
  private var mPendingScrollPosition = RecyclerView.NO_POSITION
  
  private var mPendingSavedState: MySavedState? = null
  
  private val mLayoutState = MyLayoutState()
  
  // 锚点信息
  private val mAnchorInfo = MyAnchorInfo()
  
  // 隐藏以避免分配，目前仅在#fill() 中使用
  private val mLayoutChunkResult = MyLayoutChunkResult()
  
  // 这保留了 LayoutManager 应如何开始布局视图的最终值。
  private var mShouldReverseLayout = false
  
  override fun onLayoutChildren(recycler: MyRecycler, state: MyState) {
    
    // ...
    mLayoutState.mRecycle = false
    
    /// 计算锚点信息，保存进 mAnchorInfo ...
  
    var startOffset = 0
    var endOffset = 0
    
    /// 脱离屏幕上的所有 view 进一级缓存或者四级缓存
    detachAndScrapAttachedViews(recycler)
    // ...
    mLayoutState.mIsPreLayout = state.mInPreLayout
    
    if (mAnchorInfo.mLayoutFromEnd) {
      // 先填充上面再填充下面
    } else {
      // ...
      /// 从锚点位置向后填充
      fill(recycler, mLayoutState, state, false)
      // ...
      /// 从锚点位置向前填充
      fill(recycler, mLayoutState, state, false)
      // ...
      /// 如果还有额外空间，就向后填充更多的子 View
      if (mLayoutState.mAvailable > 0) {
        // ...
        fill(recycler, mLayoutState, state, false)
        // ...
      }
    }
    // ...
    /// 为预测动画 (即 item 移动动画) 布局新 item
    layoutForPredictiveAnimations(recycler, state, startOffset, endOffset)
    // ...
  }
  
  private fun fill(
    recycler: MyRecycler,
    layoutState: MyLayoutState,
    state: MyState,
    stopOnFocusable: Boolean
  ): Int {
    val start = layoutState.mAvailable
    if (layoutState.mScrollingOffset != MyLayoutState.SCROLLING_OFFSET_NaN) {
      // ...
      /// 滑动发生时回收 ViewHolder
      recycleByLayoutState(recycler, layoutState)
    }
    var remainingSpace = layoutState.mAvailable + layoutState.mExtraFillSpace
    val layoutChunkResult = mLayoutChunkResult
    while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
      // ...
      
      /// 布局当个 item
      layoutChunk(recycler, state, layoutState, layoutChunkResult)
      
      // ...
    }
    // 返回消费空间，这个返回值只在 scroll 中用到
    return start - layoutState.mAvailable
  }
  
  // 回收滑出屏幕的子 View
  private fun recycleByLayoutState(recycler: MyRecycler, layoutState: MyLayoutState) {
    // 最后调用到 removeAndRecycleViewAt() 方法
    val index = 0
    removeAndRecycleViewAt(index, recycler)
  }
  
  private fun layoutChunk(
    recycler: MyRecycler,
    state: MyState,
    layoutState: MyLayoutState,
    result: MyLayoutChunkResult
  ) {
    /// 从 Recycler 中获取一个 ViewHolder
    val view = layoutState.next(recycler)
    // ...
    if (layoutState.mScrapList == null) {
      if (mShouldReverseLayout == (layoutState.mLayoutDirection == MyLayoutState.LAYOUT_START)) {
        addView(view)
      } else {
        addView(view, 0)
      }
    } else {
      if (mShouldReverseLayout == (layoutState.mLayoutDirection == MyLayoutState.LAYOUT_START)) {
        addDisappearingView(view)
      } else {
        addDisappearingView(view, 0)
      }
    }
    /// 测量子 view
    measureChildWithMargins(view, 0, 0)
    // ...
    var left = 0
    var top = 0
    var right = 0
    var bottom = 0
    // ...
    /// 布局子 View
    layoutDecoratedWithMargins(view, left, top, right, bottom)
    // ... 设置LayoutChunkResult参数
  }
  
  // 为预测动画 (即 item 移动动画) 布局新 item
  private fun layoutForPredictiveAnimations(
    recycler: MyRecycler,
    state: MyState,
    startOffset: Int,
    endOffset: Int
  ) {
    if (state.mInPreLayout) return
    // ...
  }
  
  // 用于在 RV 被摧毁时保存的数据
  class MySavedState {
    var mAnchorPosition = 0
    var mAnchorOffset = 0
    var mAnchorLayoutFromEnd = false
  }
  
  class MyLayoutState {
    // 在某些情况下我们可能不想回收孩子（例如布局）
    var mRecycle = false
    var mOffset = 0
    var mAvailable = 0
    var mIsPreLayout = false
    
    /**
     * 在滚动状态下构造 LayoutState 时使用。
     * 它应该设置我们在不创建新视图的情况下可以进行的滚动量。
     * 设置这是有效视图回收所必需的。
     */
    var mScrollingOffset = 0
    
    /**
     * 如果您想预布局尚不可见的项目，请使用。
     * 与 mAvailable 区别在于，在回收时，不考虑为 mExtraFillSpace 布局的距离，以避免回收可见的孩子。
     */
    var mExtraFillSpace = 0
    
    /**
     * 当可以布局的视图数量没有限制时使用。
     */
    var mInfinite = false
    
    /**
     * 适配器上的当前位置以获取下一个项目
     */
    var mCurrentPosition = 0
  
    /**
     * 定义遍历数据适配器的方向。应该是 ITEM_DIRECTION_HEAD 或 ITEM_DIRECTION_TAIL
     */
    var mItemDirection = 0
  
    /**
     * 当 LLM 需要布局特定视图时，它会设置此列表，在这种情况下，LayoutState 将仅返回此列表中的视图，如果找不到项目则返回 null。
     */
    var mScrapList: List<MyViewHolder>? = null
  
    /**
     * 定义布局的填充方向。应为 LAYOUT_START 或 LAYOUT_END
     */
    var mLayoutDirection = 0
    
    fun hasMore(state: MyState): Boolean {
      return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount()
    }
    
    fun next(recycler: MyRecycler): View {
      if (mScrapList != null) {
        return nextViewFromScrapList()
      }
      val view = recycler.getViewForPosition(mCurrentPosition)
      mCurrentPosition += mItemDirection
      return view
    }
    
    // 从 mScrapList 中返回下一项
    private fun nextViewFromScrapList(): View = TODO()
    
    companion object {
      const val SCROLLING_OFFSET_NaN = Int.MIN_VALUE
      const val LAYOUT_START = -1
      const val LAYOUT_END = 1
    }
  }
  
  class MyAnchorInfo {
    // anchor 所对应的 item 位置
    var mPosition = 0
    // 是否从底部往上布局
    var mLayoutFromEnd = false
    
  }
  
  class MyLayoutChunkResult
}