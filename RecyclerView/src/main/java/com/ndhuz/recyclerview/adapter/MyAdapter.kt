package com.ndhuz.recyclerview.adapter

import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
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
  
  open fun getItemId(position: Int): Long {
    return RecyclerView.NO_ID
  }
  
  fun hasObservers(): Boolean {
    return mObservable.hasObservers()
  }
  
  // 1.2.0 新增
  // 用于寻找 position 的方法，ConcatAdapter 重写
  open fun findRelativeAdapterPositionIn(
    adapter: MyAdapter<*>,
    holder: MyViewHolder,
    localPosition: Int
  ): Int {
    if (adapter === this) return localPosition
    return RecyclerView.NO_POSITION
  }
  
  open fun getItemViewType(position: Int): Int {
    return 0
  }
  
  fun createViewHolder(parent: ViewParent, viewType: Int): VH {
    // 调用 onCreateViewHolder
    TODO()
  }
  
  fun bindViewHolder(holder: VH, position: Int) {
    // 调用 onBindViewHolder
  }
}