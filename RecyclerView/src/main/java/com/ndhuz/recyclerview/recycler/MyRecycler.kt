package com.ndhuz.recyclerview.recycler

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.ViewCacheExtension
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ndhuz.recyclerview.activity.extension.BannerViewCacheExtension
import com.ndhuz.recyclerview.adapter.MyAdapter
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 11:16
 */
class MyRecycler {
  
  // 一级缓存
  /**
   * 一级缓存只在布局期间使用
   * 并且只处理除了 notifyDataSetChanged (hasStableIds = false 时)外的其他 notify* 方法
   * - [Adapter.notifyItemChanged]
   * - [Adapter.notifyItemRemoved]
   * - [Adapter.notifyItemInserted]
   * - [Adapter.notifyItemMoved]
   *
   *
   * notifyDataSetChanged 会把所有 holder 标记上 UPDATE 和 INVALID，
   * 在没有设置 [Adapter.hasStableIds] 时会直接回收二级缓存全部进四级缓存中，
   * 并不会重新调用 onCreateViewHolder，只是从四级缓存中出来的 holder 需要重新 onBindViewHolder
   *
   * notifyItemInserted 会先修改添加位置后面 holder 的 mPreLayoutPosition，
   * 然后等待重新布局，并不会调用 onBindViewHolder
   *
   * notifyItemMoved 与 notifyItemInserted 处理基本一致
   *
   *
   * /// 一级缓存触发的地方主要在 [LayoutManager.detachAndScrapAttachedViews]，当然也包含其他地方，只是没这里重要
   */
  
  /**
   * mAttachedScrap 只保存删除或无效的 holder
   *
   * 满足其中一个条件的 holder
   * - [MyViewHolder.isRemoved] = true，即 notifyItemRemoved()
   * - [MyViewHolder.isInvalid] = true，即 holder 非法时，但只能是 [MyAdapter.hasStableIds] = true 时的 notifyDataSetChanged()
   * - [MyViewHolder.isUpdate] = false，受 notifyItemMoved()、notifyItemInserted() 影响的 holder
   * - [MyViewHolder.mPayloads] 不为空时 (原因在 [DefaultItemAnimator.canReuseUpdatedViewHolder])
   *
   * 虽然允许保存无效的 holder，但是在 [LayoutManager.detachAndScrapAttachedViews] 方法中会将
   * [MyViewHolder.isInvalid] = true 和 [MyViewHolder.isRemoved] = false 的 holder 优先保存进
   * 二级缓存中 (但除了 [MyAdapter.hasStableIds] = true 以外，有了稳定 id 会优先一级缓存)
   */
  private val mAttachedScrap = mutableListOf<MyViewHolder>()
  
  /**
   * 保存 mAttachedScrap 以外的 holder，一般只有需要重新绑定时的 holder
   *
   * 通常包含：
   * - 调用没有 payload 的 notifyItemChanged(int)，即 [MyViewHolder.isUpdate] = true
   *
   * //# 在调用 notifyItemChanged(int) 时，int 对应的 holder 会被添加进该 mChangedScrap 中
   * //# 但是对于 notifyItemChanged(int, Object) 的调用，因为有 payload，只会被添加进 mAttachedScrap 中
   *
   * /// 比较重要的几点在于：
   * /// 1. 这个 mChangedScrap 只有在预布局时才使用，正常布局是不会使用的，
   * /// 所以在 notifyItemChanged(int) 后会与第四级缓存中的 holder 互换
   *
   * /// 2. 因为接收的都是发生了改变需要重新绑定的 holder，所以有些文章讲这个会重新调用 onBindViewHolder，
   * /// 确实是会重新调用，但根本原因是因为 mChangedScrap 会被全部回收，然后从四级缓存中获取同类型的 holder，
   * /// 而四级缓存中的 holder 本来就需要重新绑定
   *
   * /// 3. mChangedScrap 因为在预布局后就不再使用直到被 clear()，但是并不是完全就丢弃了，
   * /// 对于 [MyViewHolder.isUpdate] = true 的 holder 会调用一个更新的动画，
   * /// 然后在动画结束的回调里回收进二级缓存，步骤如下：
   * /// [RecyclerView.animateChange] → [RecyclerView.ItemAnimatorRestoreListener.onAnimationFinished]
   */
  internal val mChangedScrap = mutableListOf<MyViewHolder>()
  
  // 二级缓存
  /**
   * 二级缓存只保存离开屏幕且不带有任何 移除、更新、非法标志的 holder，默认容量为 2 个
   *
   * 如果超出容量会移除最先进来的那个 holder 到第四级缓存 [RecycledViewPool] 中去 (移除第一个)
   *
   *
   * /// 二级缓存触发的地方在 [Recycler.recycleViewHolderInternal]
   * /// 但二级缓存主要靠 LayoutManager 管理，而非 rv 管理，比如 [LinearLayoutManager.recycleChildren]
   */
  private val mCachedView = mutableListOf<MyViewHolder>()
  
  // 三级缓存
  /**
   * 提供给使用者自定义的缓存，有一些缺点，具体可以看 [BannerViewCacheExtension]
   *
   *
   * /// 三级缓存触发的地方在 [Recycler.tryGetViewHolderForPositionByDeadline]
   */
  private var mViewCacheExtension: ViewCacheExtension? = null
  
  // 四级缓存
  /**
   * 由 [RecycledViewPool] 实现
   *
   * 四级缓存根据 [ViewHolder.getItemViewType] 来保存进 SparseArray 中，
   * 每个 ViewType 对应的容量默认为 5 个
   *
   *
   * /// 四级缓存触发的地方在 [Recycler.addViewHolderToRecycledViewPool]，
   * /// 但一般是在二级缓存装满时把二级缓存中第一个移到四级缓存中，具体逻辑可看 [Recycler.recycleViewHolderInternal]
   * /// 或者不满足二级缓存条件，会直接移到四级缓存
   */
  private val mRecyclerViewPool: MyRecyclerViewPool = MyRecyclerViewPool()
  
  
  // 清理 holder 旧的位置信息，由 dispatchLayoutStep1 中调用
  internal fun clearOldPosition() {
    mCachedView.forEach { it.clearOldPosition() }
    mAttachedScrap.forEach { it.clearOldPosition() }
    mChangedScrap.forEach { it.clearPayload() }
  }
  
  // 从符合条件的 scrap 池中移除先前的 holder。
  // 在重新回收到一级缓存或明确删除和回收之前，此视图将不再符合重用条件。
  internal fun unscrapView(holder: MyViewHolder) {
    if (holder.mInChangeScrap) {
      mChangedScrap.remove(holder)
    } else {
      mAttachedScrap.remove(holder)
    }
    holder.mScrapContainer = null
    holder.mInChangeScrap = false
    holder.clearReturnedFromScrapFlag()
  }
  
  internal fun getScrapCount(): Int {
    return mAttachedScrap.size
  }
  
  internal fun getScrapViewAt(index: Int): View {
    return mAttachedScrap[index].itemView
  }
  
  internal fun clearScrap() {
    mAttachedScrap.clear()
    mChangedScrap.clear()
  }
}