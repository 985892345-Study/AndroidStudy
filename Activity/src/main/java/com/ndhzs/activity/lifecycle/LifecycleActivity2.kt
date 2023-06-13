package com.ndhzs.activity.lifecycle

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ndhzs.activity.R

class LifecycleActivity2 : AbstractLifecycleActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lifecycle2)
  
    findViewById<View>(R.id.activity_btn_lifecycle2).setOnClickListener {
      startActivity(Intent(this, LifecycleActivity1::class.java))
    }
  }
}