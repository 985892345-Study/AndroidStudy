package com.ndhuz.recyclerview.utils

import android.graphics.Rect
import android.view.ViewGroup.MarginLayoutParams
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 16:23
 */
class MyLayoutParams(width: Int, height: Int) : MarginLayoutParams(width, height) {
  internal val mDecorInsets: Rect = Rect()
  
  internal var mViewHolder: MyViewHolder? = null
}