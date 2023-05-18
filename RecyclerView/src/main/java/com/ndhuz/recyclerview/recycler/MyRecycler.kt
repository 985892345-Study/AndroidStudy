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
import com.ndhuz.recyclerview.utils.MyLayoutParams
import com.ndhuz.recyclerview.view.MyRecyclerView
import com.ndhuz.recyclerview.viewholder.MyViewHolder
import java.lang.IllegalArgumentException

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 11:16
 */
class MyRecycler(
  val rv: MyRecyclerView // 源码中 Recycler 是作为 RV 的内部类，这里进行了分离
) {
  
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
   * 一级缓存中的 view 会被暂时分离 RV，调用的 ViewGroup.detachViewFromParent()，此时 view.parent = null
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
   * 简单来说：
   * //# holder 放进 mAttachedScrap 需要满足以下三种情况：
   * //# 1. holder 被移除
   * //# 2. holder 是带有 payload 的更新
   * //# 3. holder 失效但 adapter 有 StableId
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
   * //# holder 不带有 payload 的更新时放进 mChangedScrap
   * 即在调用 notifyItemChanged(int) 时，int 对应的 holder 会被添加进该 mChangedScrap 中
   * 但是对于 notifyItemChanged(int, Object) 的调用，因为有 payload，只会被添加进 mAttachedScrap 中
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
   * 二级缓存只保存最近离开屏幕且不带有任何 移除、更新、非法标志的 holder，默认容量为 2 个
   *
   * 移到二级缓存前要求 View 已经被 RV 彻底移除，即 view.parent = null
   *
   * 如果超出容量会移除最先进来的那个 holder (移除第一个) 到第四级缓存 [RecycledViewPool] 中去
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
  private var mViewCacheExtension: MyViewCacheExtension? = null
  
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
  private lateinit var mRecyclerViewPool: MyRecyclerViewPool
  
  
  // 清理 holder 旧的位置信息，由 dispatchLayoutStep1 中调用
  internal fun clearOldPosition() {
    mCachedView.forEach { it.clearOldPosition() }
    mAttachedScrap.forEach { it.clearOldPosition() }
    mChangedScrap.forEach { it.clearPayload() }
  }
  
  // 一级缓存核心方法
  internal fun scrapView(view: View) {
    val holder = MyRecyclerView.getChildViewHolderInt(view)
    // 这个 canReuseUpdatedViewHolder 源码中包含下面这几个判断
    val canReuseUpdatedViewHolder = rv.mItemAnimator == null || holder.getUnmodifiedPayloads().isNotEmpty() || holder.isInvalid()
    if (holder.isRemoved() || holder.isInvalid() || !holder.isUpdate() || canReuseUpdatedViewHolder) {
      if (holder.isInvalid() && !holder.isRemoved() && !rv.mAdapter.hasStableIds()) {
        // 如果 holder 非法，但没有被移除，并且没有 stableIds 时将直接回收，应该回收进四级缓存，而不是一级缓存
        throw IllegalArgumentException()
      }
      //# holder 放进 mAttachedScrap 需要满足以下三种情况：
      //# 1. holder 被移除
      //# 2. holder 是带有 payload 的更新
      //# 3. holder 失效但 adapter 有 StableId
      holder.setScrapContainer(this, false)
      mAttachedScrap.add(holder)
    } else {
      //# holder 不带有 payload 的更新时放进 mChangedScrap
      holder.setScrapContainer(this, true)
      mChangedScrap.add(holder)
    }
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
  
  // 二级缓存核心方法
  internal fun recycleViewHolderInternal(holder: MyViewHolder) {
    check(holder.itemView.parent == null) // 回收时不允许 view 没有被移除
    check(holder.isTmpDetached())
    check(holder.shouldIgnore())
    var cached = false
    var recycled = false
    if (holder.isRecyclable()) {
      // 还包括其他判断，这里只给出重要判断
      if (!(holder.isInvalid() || holder.isRemoved() || holder.isUpdate())) {
        var cachedViewSize = mCachedView.size
        if (cachedViewSize >= 2) {
          // mCachedView 默认大小为 2，大于 2 的会回收进四级缓存
          recycleCachedViewAt(0) // 先回收最先进来的
          cachedViewSize--
        }
        // ...
        mCachedView.add(cachedViewSize, holder)
        cached = true
      }
      if (!cached) {
        addViewHolderToRecycledViewPool(holder, true)
        recycled = true
      }
    } else {
      // 如果有移动动画，则可能会进入这个分支，但移动动画最后会在动画结束时自动回收
      // 如果是自己调用了 setIsRecyclable 导致的不可回收也会进入该分支，但需要自己进行管理，否则将直接丢弃
    }
    rv.mViewInfoStore.removeViewHolder(holder)
    if (!cached && !recycled) {
      holder.mBindingAdapter = null
      holder.mOwnerRecyclerView = null
    }
  }
  
  internal fun recycleCachedViewAt(cachedViewIndex: Int) {
    val holder = mCachedView[cachedViewIndex]
    addViewHolderToRecycledViewPool(holder, true)
    mCachedView.removeAt(cachedViewIndex)
  }
  
  internal fun addViewHolderToRecycledViewPool(holder: MyViewHolder, dispatchRecycled: Boolean) {
    getRecycledViewPool().putRecycledView(holder)
  }
  
  fun getRecycledViewPool(): MyRecyclerViewPool {
    if (!this::mRecyclerViewPool.isInitialized) {
      mRecyclerViewPool = MyRecyclerViewPool()
      // ...
      mRecyclerViewPool.attachForPoolingContainer(rv.mAdapter)
    }
    return mRecyclerViewPool
  }
  
  fun getViewForPosition(position: Int): View {
    return tryGetViewHolderForPositionByDeadline(position, false, Long.MAX_VALUE).itemView
  }
  
  /// 获取缓存 VH 或者调用 onCreate 加载 VH 的核心方法
  private fun tryGetViewHolderForPositionByDeadline(position: Int, dryRun: Boolean, deadlineNs: Long): MyViewHolder {
    var holder: MyViewHolder? = null
    
    /// 1. 如果是预布局，先从 ChangedScrap 中寻找 holder
    if (rv.mState.mInPreLayout) {
      holder = getChangedScrapViewForPosition(position)
    }
    
    /// 2. 从 mAttachedScrap 和 mCachedViews 中寻找相同位置的 holder
    if (holder == null) {
      holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun)
      // ... 如果得到的 holder 有问题会回收然后重置 holder = null
    }
    
    if (holder == null) {
      val type = rv.mAdapter.getItemViewType(position)
      if (rv.mAdapter.hasStableIds()) {
        val id = rv.mAdapter.getItemId(position)
        /// 3. 如果 Adapter 设置了 StableIds，则使用 item 的 id 再次在 mAttachedScrap 和 mCachedViews 中寻找
        holder = getScrapOrCachedViewForId(id, type, dryRun)
      }
      
      if (holder == null && mViewCacheExtension != null) {
        /// 4. 如果设置了 mViewCacheExtension，则从第三级缓存中寻找
        val view = mViewCacheExtension!!.getViewForPositionAndType(this, position, type)
        if (view != null) {
          holder = (view.layoutParams as? MyLayoutParams)?.mViewHolder
          if (holder == null) {
            error("如果第三级缓存的 view 中拿不到 holder，就会抛异常")
          }
        }
      }
      
      if (holder == null) {
        /// 5. 从第四级缓存 RecyclerViewPool 中寻找
        holder = getRecycledViewPool().getRecycledView(type)
      }
      
      if (holder == null) {
        /// 6. 最后调用 createViewHolder 创建新的 ViewHolder
        holder = rv.mAdapter.createViewHolder(rv, type)
      }
    }
    
    if (rv.mState.mInPreLayout && holder.isBound()) {
      holder.mPreLayoutPosition = position
    } else {
      if (!holder.isBound() || holder.isUpdate() || holder.isInvalid()) {
        // 只有 holder 未绑定或需要更新或失效时才会重新调用 onBind
        // tryBindViewHolderByDeadline() -> mAdapter.bindViewHolder(holder, offsetPosition);
      }
    }
    return holder
  }
  
  private fun getChangedScrapViewForPosition(position: Int): MyViewHolder? = TODO()
  private fun getScrapOrHiddenOrCachedHolderForPosition(position: Int, dryRun: Boolean): MyViewHolder? = TODO()
  private fun getScrapOrCachedViewForId(id: Long, type: Int, dryRun: Boolean): MyViewHolder? = TODO()
}