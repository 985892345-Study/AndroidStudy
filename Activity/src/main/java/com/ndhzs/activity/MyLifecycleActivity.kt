package com.ndhzs.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// https://developer.android.google.cn/guide/components/activities/activity-lifecycle?hl=zh-cn

/**
 *
 */
class MyLifecycleActivity : AppCompatActivity() {
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lifecycle)
  }
  
  override fun onRestart() {
    super.onRestart()
  }
  
  override fun onStart() {
    super.onStart()
  }
  
  override fun onResume() {
    super.onResume()
  }
  
  override fun onPause() {
    super.onPause()
  }
  
  override fun onStop() {
    super.onStop()
  }
  
  override fun onDestroy() {
    super.onDestroy()
  }
}