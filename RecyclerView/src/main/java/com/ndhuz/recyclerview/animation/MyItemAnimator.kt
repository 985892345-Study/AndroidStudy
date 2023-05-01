package com.ndhuz.recyclerview.animation

import androidx.recyclerview.widget.RecyclerView
import com.ndhuz.recyclerview.utils.MyState
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * [RecyclerView.ItemAnimator]
 *
 * @author 985892345
 * 2023/5/1 11:53
 */
abstract class MyItemAnimator {
  
  /**
   * 用于 ItemAnimator 在 View 被重新绑定、移动或删除之前记录有关 View 的必要信息。
   *
   * 在布局开始之前由 rv 调用。
   *
   * 此方法返回的数据将传递给相关的 animate** 方法。
   *
   * 请注意，如果 LayoutManager 在预布局阶段向布局添加新视图，则可能会在预布局阶段后调用此方法。
   */
  fun recordPreLayoutInformation(
    state: MyState,
    holder: MyViewHolder,
    changeFlags: Int,
    payloads: List<Any>
  ): MyItemHolderInfo {
    return MyItemHolderInfo().setFrom(holder)
  }
  
  /**
   * 用于 ItemAnimator 在 View 最终状态时记录有关 View 的必要信息。
   *
   * 布局完成后由 RecyclerView 调用。
   *
   * 此方法返回的数据将传递给相关的animate**方法。
   */
  fun recordPostLayoutInformation(state: MyState, holder: MyViewHolder): MyItemHolderInfo {
    return MyItemHolderInfo().setFrom(holder)
  }
  
  companion object {
    val FLAG_CHANGED = MyViewHolder.FLAG_UPDATE
    val FLAG_REMOVED = MyViewHolder.FLAG_REMOVED
    val FLAG_INVALIDATED = MyViewHolder.FLAG_INVALID
    val FLAG_MOVED = MyViewHolder.FLAG_MOVED
    val FLAG_APPEARED_IN_PRE_LAYOUT = MyViewHolder.FLAG_APPEARED_IN_PRE_LAYOUT
    
    // 根据 holder.mFlags 得到一个新的标志位 (用于 ItemAnimator)
    fun buildAdapterChangeFlagsForAnimations(holder: MyViewHolder): Int {
      // 第一步只保留 FLAG_INVALIDATED、FLAG_REMOVED、FLAG_CHANGED 标志位，其他位都置 0
      var flags = holder.mFlags and (FLAG_INVALIDATED or FLAG_REMOVED or FLAG_CHANGED)
      if (holder.isInvalid()) return FLAG_INVALIDATED
      if ((flags and FLAG_INVALIDATED) == 0) {
        val oldPos = holder.getOldPosition()
        val pos = holder.getAdapterPosition()
        if (oldPos != RecyclerView.NO_POSITION && pos != RecyclerView.NO_POSITION && oldPos != pos) {
          flags = flags or FLAG_MOVED
        }
      }
      return flags
    }
  }
  
  class MyItemHolderInfo {
    var left = 0
    var top = 0
    var right = 0
    var bottom = 0
    
    var changeFlags = 0
    
    fun setFrom(
      holder: MyViewHolder,
      flags: Int = 0
    ): MyItemHolderInfo {
      val view = holder.itemView
      left = view.left
      top = view.top
      right = view.right
      bottom = view.bottom
      changeFlags = flags
      return this
    }
  }
}