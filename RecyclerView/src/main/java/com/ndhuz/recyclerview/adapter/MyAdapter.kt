package com.ndhuz.recyclerview.adapter

import androidx.recyclerview.widget.RecyclerView.Adapter
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * [Adapter]
 *
 * @author 985892345
 * 2023/4/4 10:24
 */
abstract class MyAdapter<VH : MyViewHolder> {
  
  abstract fun getItemCount(): Int
}