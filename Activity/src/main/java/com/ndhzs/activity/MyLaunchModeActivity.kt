package com.ndhzs.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MyLaunchModeActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch_mode)
  }
  
  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
  }
}