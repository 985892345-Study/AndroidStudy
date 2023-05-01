package com.ndhuz.recyclerview.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ndhuz.recyclerview.view.MyRecyclerView

/**
 * [ViewHolder]
 *
 * @author 985892345
 * 2023/4/4 10:26
 */
abstract class MyViewHolder(
  val itemView: View
) {

  internal var mFlags = 0
  
  internal var mPosition = NO_POSITION
  internal var mOldPosition = NO_POSITION
  internal var mPreLayoutPosition = NO_POSITION
  internal var mItemId = NO_ID
  
  // payload 参数
  private val mPayloads = mutableListOf<Any>()
  
  // 如果非空，视图当前被认为是已被回收并且可以被重用。
  internal var mScrapContainer: Recycler? = null
  
  // 保持此 ViewHolder 是否存在于 ChangeScrap 或 AttachedScrap 中
  internal var mInChangeScrap = false
  
  private var mOwnerRecyclerView: MyRecyclerView? = null
  
  // 是否已经绑定
  internal fun isBound(): Boolean = (mFlags and FLAG_BOUND) != 0
  
  // holder 数据是否无效
  internal fun isInvalid(): Boolean = (mFlags and FLAG_INVALID) != 0
  
  // 是否忽略该 holder
  internal fun shouldIgnore(): Boolean = (mFlags and FLAG_IGNORE) != 0
  
  // 是否需要更新
  internal fun isUpdate(): Boolean = (mFlags and FLAG_UPDATE) != 0
  
  // 是否已经被移除
  internal fun isRemoved(): Boolean = (mFlags and FLAG_REMOVED) != 0
  
  
  /**
   * 保存 mPosition 至 mOldPosition
   * [MyRecyclerView.dispatchLayoutStep1] 中需要进行 item 预测动画动画时调用
   */
  internal fun saveOldPosition() {
    if (mOldPosition == NO_POSITION) {
      mOldPosition = mPosition
    }
  }
  
  /**
   * 清理 mOldPosition 和 mPreLayoutPosition
   * [MyRecyclerView.dispatchLayoutStep1] 中需要进行 item 预测动画动画时调用
   */
  internal fun clearOldPosition() {
    mOldPosition = NO_POSITION
    mPreLayoutPosition = NO_POSITION
  }
  
  // 得到当前位置 (上一次布局后的位置，如果此时处于即将布局的状态，使用 getAdapterPosition() 才能得到下一次布局后的实际位置)
  fun getLayoutPosition(): Int {
    return if ((mPreLayoutPosition == NO_POSITION)) mPosition else mPreLayoutPosition
  }
  
  // 获取下一次布局后的位置，如果此时处于即将布局的状态，则使用该方法可以根据 adapter 提交的增删移提前计算出布局后的位置
  fun getAdapterPosition(): Int {
    return mOwnerRecyclerView?.getAdapterPositionFoe(this) ?: NO_POSITION
  }
  
  // mPosition 的旧值，但只在 dispatchLayoutStep1() 中执行预测动画时才有效
  fun getOldPosition(): Int {
    return mOldPosition
  }
  
  // 返回此 ViewHolder 表示的 itemId，只有在 Adapter.hasStableIds() = true 时才有用
  fun getItemId(): Long {
    return mItemId
  }
  
  
  internal fun setFlags(flags: Int, mask: Int) {
    // mask 中为 1 的位表示对应标志位需要被更新，为 0 的位表示对应标志位保持不变
    // mFlags = 0xABCD   mask = 0x00FF   flags = 0x0034
    // mFlags and mask.inv()  = 0xAB00
    // flags and mask         = 0x0034
    // mFlags                 = 0xAB34
    mFlags = mFlags and mask.inv() or (flags and mask)
  }
  
  // 添加 payload
  internal fun addChangePayload(payload: Any?) {
    if (payload == null) {
      // 因为一旦出现 payload = null 就打上了全量更新的标志，
      // 所以网上有说法：
      // notifyItemChanged(pos, "1")
      // notifyItemChanged(pos)       // 这里调用了这个后就会导致 payload 失效
      // notifyItemChanged(pos, "2")  // 添加 payload 失败
      mFlags = mFlags or FLAG_ADAPTER_FULLUPDATE
    } else if ((mFlags and FLAG_ADAPTER_FULLUPDATE) == 0) {
      mPayloads.add(payload)
    }
  }
  
  internal fun clearPayload() {
    mPayloads.clear()
    mFlags = mFlags and FLAG_ADAPTER_FULLUPDATE.inv()
  }
  
  internal fun getUnmodifiedPayloads(): List<Any> {
    if ((mFlags and FLAG_ADAPTER_FULLUPDATE) == 0) {
      // 不是全量更新时
      if (mPayloads.size == 0) return emptyList()
      return mPayloads
    } else {
      // 全量更新时只放回空 list
      return emptyList()
    }
  }
  
  // 从回收池中释放
  internal fun clearReturnedFromScrapFlag() {
    mFlags = mFlags and FLAG_RETURNED_FROM_SCRAP.inv()
  }

  companion object {
  
    /**
     * 这个 ViewHolder 已经绑定到一个位置； mPosition、mItemId 和 mItemViewType 均有效。
     */
    val FLAG_BOUND = 1
  
    /**
     * 这个 ViewHolder 的视图反映的数据是陈旧的，需要由适配器重新启动。
     * mPosition 和 mItemId 是一致的。
     */
    val FLAG_UPDATE = 1 shl 1
  
    /**
     * 此 ViewHolder 的数据无效。
     * mPosition 和 mItemId 隐含的标识不可信，可能不再匹配项目视图类型。
     * 这个 ViewHolder 必须完全重新绑定到不同的数据。
     */
    val FLAG_INVALID = 1 shl 2
  
    /**
     * 此 ViewHolder 指向代表先前从数据集中删除的项目的数据。
     * 它的视图可能仍用于诸如传出动画之类的事情。
     */
    val FLAG_REMOVED = 1 shl 3
  
    /**
     * 这个 ViewHolder 是从 scrap 返回的，这意味着我们期待这个 itemView 的 addView 调用。
     * 当从 scrap 返回时，ViewHolder 会停留在 scrap 列表中直到布局过程结束，
     * 然后如果它没有被添加回 RecyclerView 则由 RecyclerView 回收。
     */
    val FLAG_RETURNED_FROM_SCRAP = 1 shl 5
    
    /**
     * 这个 ViewHolder 完全由 LayoutManager 管理。
     * 除非更换 LayoutManager，否则我们不会报废、回收或移除它。
     * 它仍然对 LayoutManager 完全可见。
     */
    val FLAG_IGNORE = 1 shl 7
  
    /**
     * 全量更新标志，在调用 [addChangePayload] 传入 null 时设置
     */
    val FLAG_ADAPTER_FULLUPDATE = 1 shl 10
  
    /**
     * 当 ViewHolder 的位置改变时由 ItemAnimator 使用
     */
    val FLAG_MOVED = 1 shl 11
  
    /**
     * 当 ViewHolder 出现在预布局中时由 ItemAnimator 使用
     */
    val FLAG_APPEARED_IN_PRE_LAYOUT = 1 shl 12
  
    /**
     * 当 ViewHolder 作为隐藏的 ViewHolder 启动布局传递但从隐藏列表中重新使用（就好像它是废料）而不在其间被回收时使用。
     * [ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST]
     */
    val FLAG_BOUNCED_FROM_HIDDEN_LIST = 1 shl 13
    
  }
}