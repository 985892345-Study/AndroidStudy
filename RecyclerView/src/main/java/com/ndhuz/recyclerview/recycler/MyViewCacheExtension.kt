package com.ndhuz.recyclerview.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

/**
 * 三级缓存
 *
 * [RecyclerView.ViewCacheExtension]
 *
 * @author 985892345
 * 2023/5/1 17:31
 */
abstract class MyViewCacheExtension {
  
  /**
   * 返回一个可以绑定到给定适配器位置的视图。
   *
   * 此方法不应创建新视图。
   * 相反，它应该返回一个已经创建的视图，可以为给定的类型和位置重新使用。
   * 如果 View 被标记为忽略，则应在返回 View 之前先调用 RecyclerView.LayoutManager.stopIgnoringView(View) 。
   *
   * 如果需要，RecyclerView 会重新绑定返回的 View 到该位置。
   */
  abstract fun getViewForPositionAndType(
    recycler: Recycler,
    position: Int,
    type: Int
  ): View?
}