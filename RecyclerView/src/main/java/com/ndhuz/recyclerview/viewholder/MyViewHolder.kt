package com.ndhuz.recyclerview.viewholder

import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ndhuz.recyclerview.adapter.MyAdapter
import com.ndhuz.recyclerview.recycler.MyRecycler
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
  private var mIsRecyclableCount = 0
  private var mItemViewType = 0
  
  // payload 参数
  private val mPayloads = mutableListOf<Any>()
  
  // 如果非空，视图当前被认为是已被回收并且可以被重用。
  internal var mScrapContainer: MyRecycler? = null
  
  // 此 ViewHolder 是否存在于 ChangeScrap 或 AttachedScrap 中
  internal var mInChangeScrap = false
  
  internal var mOwnerRecyclerView: MyRecyclerView? = null
  
  // 1.2.0 新增
  // 绑定的 Adapter
  internal var mBindingAdapter: MyAdapter<*>? = null
  
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
  
  // 是否被临时 detach，出现在放进一级缓存前
  internal fun isTmpDetached(): Boolean = (mFlags and FLAG_TMP_DETACHED) != 0
  
  // 设置回收，里面对回收次数进行判断
  fun setIsRecyclable(recyclable: Boolean) {
    mIsRecyclableCount = if (recyclable) mIsRecyclableCount - 1 else mIsRecyclableCount + 1
    if (mIsRecyclableCount < 0) {
      mIsRecyclableCount = 0
    } else if (!recyclable && mIsRecyclableCount == 1) {
      mFlags = mFlags or FLAG_NOT_RECYCLABLE
    } else if (recyclable && mIsRecyclableCount == 0) {
      mFlags = mFlags and FLAG_NOT_RECYCLABLE.inv()
    }
  }
  
  // 是否可回收
  fun isRecyclable(): Boolean {
    return (mFlags and FLAG_NOT_RECYCLABLE) != 0 && !ViewCompat.hasTransientState(itemView)
  }
  
  // 是否正在动画的 Overlay 层中
  fun isAttachedToTransitionOverlay(): Boolean {
    return itemView.parent != null && itemView.parent != mOwnerRecyclerView
  }
  
  
  // 获取类型
  fun getItemViewType(): Int = mItemViewType
  
  
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
  
  @Deprecated("1.2.0 版本废弃，因为新增了 ConcatAdapter", ReplaceWith("getBindingAdapterPosition()"))
  fun getAdapterPosition(): Int {
//    return mOwnerRecyclerView?.getAdapterPositionInRecyclerView(this) ?: NO_POSITION // 旧版本写法
    return getBindingAdapterPosition() // 新版本写法
  }
  
  // 1.2.0 新增
  // 获取下一次布局后的位置，如果此时处于即将布局的状态，则使用该方法可以根据 adapter 提交的增删移提前计算出布局后的位置
  fun getBindingAdapterPosition(): Int {
    if (mBindingAdapter == null) return NO_POSITION
    if (mOwnerRecyclerView == null) return NO_POSITION
    val rvAdapter = mOwnerRecyclerView!!.getAdapter() ?: return NO_POSITION
    val globalPosition = mOwnerRecyclerView!!.getAdapterPositionInRecyclerView(this)
    if (globalPosition == NO_POSITION) return NO_POSITION
    return rvAdapter.findRelativeAdapterPositionIn(mBindingAdapter!!, this, globalPosition)
  }
  
  // 1.2.0 新增
  // 获取下一次布局后的位置，与 getBindingAdapterPosition() 类似，但这个是是获得绝对的位置，用于 ConcatAdapter 中
  fun getAbsoluteAdapterPosition(): Int {
    return mOwnerRecyclerView?.getAdapterPositionInRecyclerView(this) ?: NO_POSITION
  }
  
  // mPosition 的旧值，但只在 dispatchLayoutStep1() 中执行预测动画时才有效，其他地方获取都为 NO_POSITION
  fun getOldPosition(): Int {
    return mOldPosition
  }
  
  // 返回此 ViewHolder 表示的 itemId，只有在 Adapter.hasStableIds() = true 时才有用
  fun getItemId(): Long {
    return mItemId
  }
  
  fun hasAnyOfTheFlags(flags: Int): Boolean {
    return (mFlags and flags) != 0
  }
  
  internal fun setFlags(flags: Int, mask: Int) {
    // mask 中为 1 的位表示对应标志位需要被更新，为 0 的位表示对应标志位保持不变
    // mFlags = 1010   flags = 0001   mask = 0011
    // mFlags and mask.inv()  = 0010
    // flags and mask         = 0001
    // mFlags                 = 0011
    mFlags = mFlags and mask.inv() or (flags and mask)
    // setFlags(0, mask) 相当于去掉 mask 标志
    // setFlags(flags, mask) = setFlags(0, mask) or (flags and mask)
    // 意思就是先去掉 mask 标志，再添加 flags 标志 (一般情况下 flags and mask = flags)
    // 你可以这样用: setFlags(A, A or B or C) 去掉 A、B、C 标志，再添加 A 标志
  }
  
  internal fun addFlags(flags: Int) {
    mFlags = mFlags or flags
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
  
  internal fun resetInternal() {
    mFlags = 0
    mPosition = NO_POSITION
    mOldPosition = NO_POSITION
    mItemId = NO_ID
    mPreLayoutPosition = NO_POSITION
    mIsRecyclableCount = 0
    clearPayload()
  }
  
  // 放进一级缓存时调用
  internal fun setScrapContainer(recycler: MyRecycler, isChangeScrap: Boolean) {
    mScrapContainer = recycler
    mInChangeScrap = isChangeScrap
  }

  companion object {
  
    /**
     * 这个 ViewHolder 已经绑定到一个位置； mPosition、mItemId 和 mItemViewType 均有效。
     *
     * ViewHolder 已经绑定数据
     *
     * - 调用了 onBindViewHolder()
     */
    val FLAG_BOUND = 1
  
    /**
     * 这个 ViewHolder 的视图反映的数据是陈旧的，需要由适配器重新绑定。
     * mPosition 和 mItemId 是一致的。
     *
     * ViewHolder 需要重新绑定数据
     *
     * - 调用了 notifyItemChanged()
     * - 调用了 notifyDataSetChanged()
     */
    val FLAG_UPDATE = 1 shl 1
  
    /**
     * 此 ViewHolder 的数据无效。
     * mPosition 和 mItemId 隐含的标识不可信，可能不再匹配项目视图类型。
     * 这个 ViewHolder 必须完全重新绑定到不同的数据。
     *
     * ViewHolder 已经无效
     *
     * - 调用了 notifyDataSetChanged()
     */
    val FLAG_INVALID = 1 shl 2
  
    /**
     * 此 ViewHolder 指向代表先前从数据集中删除的项目的数据。
     * 它的视图可能仍用于诸如传出动画之类的事情。
     *
     * - 调用了 notifyItemRemoved()
     */
    val FLAG_REMOVED = 1 shl 3
  
    /**
     * 是否可回收
     */
    val FLAG_NOT_RECYCLABLE = 1 shl 4
  
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
     * 当视图与父视图分离时，我们设置此标志，以便在需要删除它或将其添加回来时可以采取正确的操作。
     *
     * 出现在将 holder 放进一级缓存前设置，可以查看 [LayoutManager.scrapOrRecycleView]
     */
    val FLAG_TMP_DETACHED = 1 shl 8
    
  
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