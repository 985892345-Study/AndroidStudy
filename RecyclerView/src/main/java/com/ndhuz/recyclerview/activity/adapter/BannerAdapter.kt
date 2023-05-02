package com.ndhuz.recyclerview.activity.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * 使用 ConcatAdapter 来注入 Banner 是最好的一种方式
 *
 * rv.adapter = ConcatAdapter(BannerAdapter(), ListItemAdapter())
 *
 * @author 985892345
 * 2023/5/2 13:41
 */
class BannerAdapter : Adapter<BannerAdapter.BannerVH>() {
  class BannerVH(itemView: View) : ViewHolder(itemView)
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
    return BannerVH(
      View(parent.context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          400,
        ).apply {
          topMargin = 10
          bottomMargin = 10
        }
        setBackgroundColor(Color.YELLOW)
      }
    )
  }
  
  override fun getItemCount(): Int {
    return 1
  }
  
  override fun onBindViewHolder(holder: BannerVH, position: Int) {
  
  }
}