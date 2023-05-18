package com.ndhuz.recyclerview.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.ndhuz.recyclerview.R
import com.ndhuz.recyclerview.activity.adapter.BannerAdapter
import com.ndhuz.recyclerview.activity.adapter.ListItemAdapter
import com.ndhuz.recyclerview.activity.adapter.PageAdapter

class RecyclerViewActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_recyclerview)
    
    val rv: RecyclerView = findViewById(R.id.rv)
    rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
//    rv.adapter = ConcatAdapter(BannerAdapter(), ListItemAdapter())
    
    val adapter = PageAdapter(rv)
    rv.adapter = adapter
    rv.setRecycledViewPool(PageRecycledViewPool())

    val et: EditText = findViewById(R.id.et)
    val btn: Button = findViewById(R.id.btn)
    btn.setOnClickListener {
      adapter.notifyItemChanged(et.text.toString().toInt())
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "mCacheViews = ${adapter.mCacheViews}   mViewCacheMax = ${adapter.mViewCacheMax}")
    }
  }
  
  class PageRecycledViewPool : RecycledViewPool() {
    override fun putRecycledView(scrap: RecyclerView.ViewHolder) {
      super.putRecycledView(scrap)
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "putRecycledView: holder = ${(scrap as PageAdapter.PageVH).code}")
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "getRecycledViewCount = ${getRecycledViewCount(0)}")
    }
  }
}