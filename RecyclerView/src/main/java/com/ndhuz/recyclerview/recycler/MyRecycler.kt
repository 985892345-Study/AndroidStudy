package com.ndhuz.recyclerview.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewCacheExtension
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 11:16
 */
class MyRecycler {
  
  // 一级缓存
  private val mAttachedScrap = mutableListOf<MyViewHolder>()
  internal val mChangedScrap = mutableListOf<MyViewHolder>()
  
  // 二级缓存
  private val mCachedView = mutableListOf<MyViewHolder>()
  
  // 三级缓存
  private var mViewCacheExtension: ViewCacheExtension? = null
  
  // 四级缓存
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