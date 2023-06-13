package com.ndhuz.android.study

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ndhzs.activity.lifecycle.LifecycleActivity1

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    startActivity(Intent(this, LifecycleActivity1::class.java))
    finish()
  }
}