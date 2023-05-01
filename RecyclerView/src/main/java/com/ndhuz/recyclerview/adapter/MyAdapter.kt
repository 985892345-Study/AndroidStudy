package com.ndhuz.recyclerview.adapter

import androidx.recyclerview.widget.RecyclerView.Adapter
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * [Adapter]
 *
 * @author 985892345
 * 2023/4/4 10:24
 */
abstract class MyAdapter<VH : MyViewHolder> {
  
  private val mObservable = MyAdapterDataObservable()
  private var mHasStableIds = false
  
  abstract fun getItemCount(): Int
  
  /**
   * 如果此适配器发布一个唯一的 long 值，该值可以充当数据集中给定位置的项的键，则返回 true。
   * 如果该项目在数据集中重新定位，则为该项目返回的 ID 应该相同。
   */
  fun hasStableIds(): Boolean {
    return mHasStableIds
  }
  
  /**
   * 指示数据集中的每个项目是否可以用 Long 类型的唯一标识符表示。
   */
  open fun setHasStableIds(hasStableIds: Boolean) {
    check(!hasObservers()) {
      "Cannot change whether this adapter has stable IDs while the adapter has registered observers."
    }
    mHasStableIds = hasStableIds
  }
  
  fun hasObservers(): Boolean {
    return mObservable.hasObservers()
  }
}