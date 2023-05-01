package com.ndhuz.recyclerview.utils

import androidx.collection.LongSparseArray
import androidx.collection.SimpleArrayMap
import androidx.core.util.Pools
import com.ndhuz.recyclerview.animation.MyItemAnimator
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * [androidx.recyclerview.widget.ViewInfoStore]
 *
 * @author 985892345
 * 2023/5/1 11:28
 */
internal class MyViewInfoStore {
  
  private val mLayoutHolderMap = SimpleArrayMap<MyViewHolder, MyInfoRecord>()
  
  private val mOldChangedHolders = LongSparseArray<MyViewHolder>()
  
  fun clear() {
    mLayoutHolderMap.clear()
    mOldChangedHolders.clear()
  }
  
  /**
   * 将 item 信息添加到 pre layout
   */
  fun addToPreLayout(holder: MyViewHolder, info: MyItemAnimator.MyItemHolderInfo) {
    var record = mLayoutHolderMap[holder]
    if (record == null) {
      record = MyInfoRecord.obtain()
      mLayoutHolderMap.put(holder, record)
    }
    record.preInfo = info
    record.flags = record.flags or MyInfoRecord.FLAG_PRE
  }
  
  fun isDisappearing(holder: MyViewHolder): Boolean {
    val record = mLayoutHolderMap[holder]
    // flags 包含 FLAG_DISAPPEARED 标志
    return record != null && ((record.flags and MyInfoRecord.FLAG_DISAPPEARED) != 0)
  }
  
  /**
   * 在 preLayout 列表中查找给定 ViewHolder 的 ItemHolderInfo 并将其删除。
   */
  fun popFromPreLayout(holder: MyViewHolder): MyItemAnimator.MyItemHolderInfo? {
    return popFromLayoutStep(holder, MyInfoRecord.FLAG_PRE)
  }
  
  /**
   * 在 postLayout 列表中查找给定 ViewHolder 的 ItemHolderInfo 并将其删除。
   */
  fun popFromPostLayout(holder: MyViewHolder): MyItemAnimator.MyItemHolderInfo? {
    return popFromLayoutStep(holder, MyInfoRecord.FLAG_POST)
  }
  
  private fun popFromLayoutStep(
    holder: MyViewHolder,
    flags: Int
  ): MyItemAnimator.MyItemHolderInfo? {
    val index = mLayoutHolderMap.indexOfKey(holder)
    if (index < 0) return null
    val record = mLayoutHolderMap.valueAt(index)
    if (record != null && (record.flags and flags) != 0) {
      record.flags = record.flags and flags.inv()
      val info = when (flags) {
        MyInfoRecord.FLAG_PRE -> record.preInfo
        MyInfoRecord.FLAG_POST -> record.postInfo
        else -> throw IllegalArgumentException("Must provide flag PRE or POST")
      }
      // 如果没有留下 pre-post 标志，则清除
      if ((record.flags and (MyInfoRecord.FLAG_PRE or MyInfoRecord.FLAG_POST)) == 0) {
        mLayoutHolderMap.removeAt(index)
        MyInfoRecord.recycle(record)
      }
      return info
    }
    return null
  }
  
  /**
   * 将给定的 ViewHolder 添加到 oldChangeHolders 列表
   */
  fun addToOldChangeHolders(key: Long, holder: MyViewHolder) {
    mOldChangedHolders.put(key, holder)
  }
  
  /**
   * 将给定的 ViewHolder 添加到出现在预布局列表中。这些是 LayoutManager 在预布局过程中添加的视图。
   * 我们将它们与已经在预布局中的其他视图区分开来，以便 ItemAnimator 可以选择为它们运行不同的动画
   */
  fun addToAppearedInPreLayoutHolders(holder: MyViewHolder, info: MyItemAnimator.MyItemHolderInfo) {
    var record = mLayoutHolderMap[holder]
    if (record == null) {
      record = MyInfoRecord.obtain()
      mLayoutHolderMap.put(holder, record)
    }
    record.flags = record.flags or MyInfoRecord.FLAG_APPEAR
    record.preInfo = info
  }
  
  /**
   * 检查给定的 ViewHolder 是否在 preLayout 列表中
   */
  fun isInPreLayout(holder: MyViewHolder): Boolean {
    val record = mLayoutHolderMap[holder]
    // flags 包含 FLAG_DISAPPEARED 标志
    return record != null && ((record.flags and MyInfoRecord.FLAG_PRE) != 0)
  }
  
  /**
   * 查询给定键的 oldChangeHolder 列表。如果他们没有被跟踪，则简单地返回 null。
   */
  fun getFromOldChangeHolders(key: Long): MyViewHolder? {
    return mOldChangedHolders[key]
  }
  
  /**
   * 将 item 信息添加到 post layout
   */
  fun addToPostLayout(holder: MyViewHolder, info: MyItemAnimator.MyItemHolderInfo) {
    var record = mLayoutHolderMap.get(holder)
    if (record == null) {
      record = MyInfoRecord.obtain()
      mLayoutHolderMap.put(holder, record)
    }
    record.postInfo = info
    record.flags = record.flags or MyInfoRecord.FLAG_POST
  }
  
  /**
   * 添加 item Disappeared 标志
   */
  fun addToDisappearedInLayout(holder: MyViewHolder) {
    var record = mLayoutHolderMap.get(holder)
    if (record == null) {
      record = MyInfoRecord.obtain()
      mLayoutHolderMap.put(holder, record)
    }
    record.flags = record.flags or MyInfoRecord.FLAG_DISAPPEARED
  }
  
  /**
   * 去掉 item Disappeared 标志
   */
  fun removeFromDisappearedInLayout(holder: MyViewHolder) {
    val record = mLayoutHolderMap[holder] ?: return
    // 去掉 FLAG_DISAPPEARED 标志
    record.flags = record.flags and MyInfoRecord.FLAG_DISAPPEARED.inv()
  }
  
  fun process(callback: MyProcessCallback) {
    for (index in mLayoutHolderMap.size() - 1 downTo 0) {
      val holder = mLayoutHolderMap.keyAt(index)
      val record = mLayoutHolderMap.removeAt(index)
      if ((record.flags and MyInfoRecord.FLAG_APPEAR_AND_DISAPPEAR) == MyInfoRecord.FLAG_APPEAR_AND_DISAPPEAR) {
        // 先 APPEAR 再 DISAPPEAR，不执行动画
        callback.unused(holder)
      } else if ((record.flags and MyInfoRecord.FLAG_DISAPPEARED) != 0) {
        // DISAPPEAR 动画
        if (record.preInfo == null) {
          // 类似于出现消失但发生在不同的布局阶段。当布局管理器使用自动测量时可能会发生这种情况
          callback.unused(holder)
        } else {
          callback.processDisappeared(holder, record.preInfo!!, record.postInfo)
        }
      } else if ((record.flags and MyInfoRecord.FLAG_APPEAR_PRE_AND_POST) == MyInfoRecord.FLAG_APPEAR_PRE_AND_POST) {
        // 预布局过程中添加的视图
        callback.processAppeared(holder, record.preInfo, record.postInfo!!)
      } else if ((record.flags and MyInfoRecord.FLAG_PRE_AND_POST) == MyInfoRecord.FLAG_PRE_AND_POST) {
        // item 未消失，仍然存在时，但可能发生了位置的改变
        callback.processPersistent(holder, record.preInfo!!, record.postInfo!!)
      } else if ((record.flags and MyInfoRecord.FLAG_PRE) != 0) {
        // 只存在预布局中，但未添加到后期布局
        callback.processDisappeared(holder, record.preInfo!!, null)
      } else if ((record.flags and MyInfoRecord.FLAG_POST) != 0) {
        // 不存在预布局中，但添加到了后期布局
        callback.processAppeared(holder, record.preInfo, record.postInfo!!)
      } else if ((record.flags and MyInfoRecord.FLAG_APPEAR) != 0) {
        // 临时的 item，rv 会自动回收它们
      }
      MyInfoRecord.recycle(record)
    }
  }
  
  fun removeViewHolder(holder: MyViewHolder) {
    for (i in mOldChangedHolders.size() - 1 downTo 0) {
      if (holder === mOldChangedHolders.valueAt(i)) {
        mOldChangedHolders.removeAt(i)
        break
      }
    }
    val info = mLayoutHolderMap.remove(holder)
    if (info != null) {
      MyInfoRecord.recycle(info)
    }
  }
  
  
  interface MyProcessCallback {
    fun processDisappeared(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo,
      postInfo: MyItemAnimator.MyItemHolderInfo?
    )
    
    fun processAppeared(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo?,
      postInfo: MyItemAnimator.MyItemHolderInfo
    )
  
    /**
     * item 没有被移除，仍然存在时的回调，但可能发生了位置的改变
     */
    fun processPersistent(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo,
      postInfo: MyItemAnimator.MyItemHolderInfo
    )
  
    /**
     * 因为一些原因导致此 holder 只是临时出现时的回调
     */
    fun unused(holder: MyViewHolder)
  }
  
  
  class MyInfoRecord {
    companion object {
      val FLAG_DISAPPEARED = 1 // 0001
      val FLAG_APPEAR = 1 shl 1 // 0010
      val FLAG_PRE = 1 shl 2 // 0100
      val FLAG_POST = 1 shl 3 // 1000
      val FLAG_APPEAR_AND_DISAPPEAR = FLAG_APPEAR or FLAG_DISAPPEARED // 0011
      val FLAG_PRE_AND_POST = FLAG_PRE or FLAG_POST // 1100
      val FLAG_APPEAR_PRE_AND_POST = FLAG_APPEAR or FLAG_PRE or FLAG_POST // 1110
      
      private val sPool = Pools.SimplePool<MyInfoRecord>(20)
      
      fun obtain(): MyInfoRecord {
        return sPool.acquire() ?: MyInfoRecord()
      }
      
      fun recycle(record: MyInfoRecord) {
        record.flags = 0
        record.preInfo = null
        record.postInfo = null
        sPool.release(record)
      }
    }
    
    var flags = 0
    
    var preInfo: MyItemAnimator.MyItemHolderInfo? = null
    var postInfo: MyItemAnimator.MyItemHolderInfo? = null
  }
}