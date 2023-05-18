package com.ndhuz.recyclerview.activity.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * .
 *
 * @author 985892345
 * 2023/5/2 18:31
 */
class PageAdapter(
  val rv: RecyclerView
) : Adapter<PageAdapter.PageVH>() {
  class PageVH(itemView: View) : ViewHolder(itemView) {
    
    val code = list.removeFirst()
    
    override fun toString(): String {
      return code
    }
    
    companion object {
      val list = mutableListOf(
        "A", "B", "C", "D", "E"
      )
    }
  }
  
  private var count = 0
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
    val holder = PageVH(
      FrameLayout(parent.context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        ).apply {
          topMargin = 20
          bottomMargin = 20
          leftMargin = 20
          rightMargin = 20
        }
        setBackgroundColor(Color.GRAY)
        addView(
          TextView(parent.context).apply {
            textSize = 30F
            layoutParams = FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            gravity = Gravity.CENTER
          }
        )
      }
    )
    count++
    android.util.Log.d(
      "ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "onCreate: mCacheViews = $mCacheViews   holder = ${holder.code}\n${Exception().stackTraceToString()}"
    )
    return holder
  }
  
  override fun getItemCount(): Int {
    return 10
  }
  
  val mCacheViews = rv.let {
    val field = RecyclerView::class.java.getDeclaredField("mRecycler")
    field.isAccessible = true
    val recycler = field.get(it)
    val field2 = Recycler::class.java.getDeclaredField("mCachedViews")
    field2.isAccessible = true
    field2.get(recycler)!!
  }
  
  val mViewCacheMax
    get() = rv.let {
      val field = RecyclerView::class.java.getDeclaredField("mRecycler")
      field.isAccessible = true
      val recycler = field.get(it)
      val field2 = Recycler::class.java.getDeclaredField("mViewCacheMax")
      field2.isAccessible = true
      field2.get(recycler)!!
    }
  
  @SuppressLint("SetTextI18n")
  override fun onBindViewHolder(holder: PageVH, position: Int) {
    val view = holder.itemView as ViewGroup
    (view.getChildAt(0) as TextView).apply {
      text = holder.code + "\n第 ${position + 1} 页"
    }
    android.util.Log.d(
      "ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "onBind: mCacheViews = $mCacheViews   position = $position   holder = ${holder.code}\nonBind: ${Exception().stackTraceToString()}"
    )
  }
}