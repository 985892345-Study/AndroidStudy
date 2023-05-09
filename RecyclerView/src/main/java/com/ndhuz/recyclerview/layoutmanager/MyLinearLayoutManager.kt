package com.ndhuz.recyclerview.layoutmanager

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndhuz.recyclerview.recycler.MyRecycler
import com.ndhuz.recyclerview.utils.MyState

/**
 * https://juejin.cn/post/6844903924256735239
 * https://juejin.cn/post/6844904003394863112
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
  
  override fun onLayoutChildren(recycler: MyRecycler, state: MyState) {
    
    // ...
    mLayoutState.mRecycle = false
    // ...
    /// 脱离屏幕上的所有 view
    detachAndScrapAttachedViews(recycler)
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
    
  }
  
  class MyAnchorInfo {
  
  }
}