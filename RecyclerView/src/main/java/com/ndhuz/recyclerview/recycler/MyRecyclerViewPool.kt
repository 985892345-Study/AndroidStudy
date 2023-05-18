package com.ndhuz.recyclerview.recycler

import android.util.SparseArray
import com.ndhuz.recyclerview.adapter.MyAdapter
import com.ndhuz.recyclerview.viewholder.MyViewHolder
import java.util.Collections
import java.util.IdentityHashMap

/**
 * .
 *
 * @author 985892345
 * 2023/5/1 17:30
 */
open class MyRecyclerViewPool {
  
  class MyScrapData {
    val mScrapHeap = mutableListOf<MyViewHolder>()
    var mMaxScrap = 5 // 最大容量为 5
  }
  
  internal val mScrap = SparseArray<MyScrapData>()
  
  private var mAttachCountForClearing = 0
  
  // 使用 IdentityHashMap，使用地址相等而非 equals
  private val mAttachedAdaptersForPoolingContainer =
    Collections.newSetFromMap<MyAdapter<*>>(IdentityHashMap())
  
  internal fun attachForPoolingContainer(adapter: MyAdapter<*>) {
    mAttachedAdaptersForPoolingContainer.add(adapter)
  }
  
  // 回收 holder
  fun putRecycledView(holder: MyViewHolder) {
    val viewType = holder.getItemViewType()
    val scrapHeap = getScrapDataForType(viewType).mScrapHeap
    if (mScrap.get(viewType).mMaxScrap <= scrapHeap.size) {
      return
    }
    holder.resetInternal()
    scrapHeap.add(holder)
  }
  
  private fun getScrapDataForType(viewType: Int): MyScrapData {
    var scrapData = mScrap.get(viewType)
    if (scrapData == null) {
      scrapData = MyScrapData()
      mScrap.put(viewType, scrapData)
    }
    return scrapData
  }
  
  fun getRecycledView(viewType: Int): MyViewHolder? {
    val scrapData = mScrap.get(viewType)
    if (scrapData != null && scrapData.mScrapHeap.isNotEmpty()) {
      val scrapHeap = scrapData.mScrapHeap
      for (i in scrapHeap.size - 1 downTo 0) {
        if (!scrapHeap[i].isAttachedToTransitionOverlay()) {
          return scrapHeap.removeAt(i)
        }
      }
    }
    return null
  }
}