package com.ndhuz.recyclerview.activity.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * .
 *
 * @author 985892345
 * 2023/5/2 13:42
 */
class ListItemAdapter : Adapter<ListItemAdapter.ListItemVH>() {
  class ListItemVH(itemView: View) : ViewHolder(itemView)
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemVH {
    return ListItemVH(
      View(parent.context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          400,
        ).apply {
          topMargin = 10
          bottomMargin = 10
        }
        setBackgroundColor(Color.BLUE)
      }
    )
  }
  
  override fun getItemCount(): Int {
    return 3
  }
  
  override fun onBindViewHolder(holder: ListItemVH, position: Int) {
  
  }
}