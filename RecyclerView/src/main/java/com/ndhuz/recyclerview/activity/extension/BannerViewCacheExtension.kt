package com.ndhuz.recyclerview.activity.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewCacheExtension
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.lang.reflect.Field

/**
 * 之前想到了一种给 rv 注入 Banner 的想法，使用 ViewCacheExtension 来拦截
 * 但是经过实验后发现了 ViewCacheExtension 的一些缺点，这里记录一下:
 *
 * 1. [ViewCacheExtension] 返回的是一个 View 对象，虽然可以拦截 [Adapter.onCreateViewHolder]，
 * 但是 [Adapter.onBindViewHolder] 仍然会调用
 *
 * 2. [ViewCacheExtension] 返回 View 时立马就会获取对应的 ViewHolder，如果没得就会抛错，
 * 而这个 [ViewHolder] 默认通过 View.layoutParams 获取，
 * 并且官方没有提供直接设置 [RecyclerView.LayoutParams.mViewHolder] 的方法，只能靠反射写入 holder
 *
 * 3. 虽然 [ViewCacheExtension] 可以拦截某个位置的 View 的获取，
 * 但是在回收时 rv 没有通知 [ViewCacheExtension]，是从二级缓存直接回到四级缓存 [RecycledViewPool] 中，
 * [RecycledViewPool] 根据 [ViewHolder.getItemViewType] 来分别保存，
 * 所以使用 [ViewCacheExtension] 需要设置额外不重复的 ViewType (这个也没提供直接设置的方法，要么用反射，要么改动 Adapter)，
 * 还需要设置 [RecycledViewPool.setMaxRecycledViews] 对应的容量为 0
 *
 *
 * 综上，因为 [ViewCacheExtension] 限制很多，并且需要联合 Adapter 才能正常使用
 * 所以使用场景很少见，唯一想到的是配合 setIsRecyclable 一起使用，因为设置不可回收后，在离开屏幕后的 holder 会被直接丢弃
 * 如果想设置 Banner 的话，还不如 ConcatAdapter
 *
 * @author 985892345
 * 2023/5/2 13:44
 */
class BannerViewCacheExtension(
  private val rv: RecyclerView
) : ViewCacheExtension() {
  override fun getViewForPositionAndType(
    recycler: RecyclerView.Recycler,
    position: Int,
    type: Int
  ): View? {
    if (position != 0) return null
    return Banner(rv.context)
  }
  
  class Banner(context: Context) : View(context) {
    init {
      layoutParams = BannerLayoutParams(this)
    }
  }
  
  class BannerLayoutParams(
    val banner: Banner
  ) : RecyclerView.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT,
  ) {
    init {
      setBannerViewHolder(this, banner)
    }
    
    companion object {
      private var ViewHolderField: Field? = null
      private fun setBannerViewHolder(lp: BannerLayoutParams, banner: Banner) {
        var field = ViewHolderField
        if (field == null) {
          val clazz = RecyclerView.LayoutParams::class.java
          /*
           * 这样写了后仍然不得行，因为会回调 onBindViewHolder，导致 ViewHolder 强转报错
           * 所以 ViewCacheExtension 仍然需要与 Adapter 配合，不然绕不过 ViewHolder 问题
           * */
          field = clazz.getDeclaredField("mViewHolder")
          field!!.isAccessible = true
          ViewHolderField = field
        }
        field.set(lp, BannerVH(banner))
      }
    }
  }
  
  class BannerVH(itemView: Banner) : ViewHolder(itemView) {
  }
}