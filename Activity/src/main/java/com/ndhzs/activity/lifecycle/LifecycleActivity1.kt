package com.ndhzs.activity.lifecycle

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ndhzs.activity.R

class LifecycleActivity1 : AbstractLifecycleActivity() {
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lifecycle1)
    
    findViewById<View>(R.id.activity_btn_lifecycle1).setOnClickListener {
      startActivity(Intent(this, LifecycleActivity2::class.java))
    }
  }
}
/*
* 1. 点击跳转然后返回
* A.onCreate
* A.onStart
* A.onResume
* 点击跳转至 B
* A.onPause
* B.onCreate
* B.onStart
* B.onResume
* A.onStop
* (如果 A 点击按钮后调用了 finish，则触发 A.onDestroy，则将不会有后面的返回)
* 然后 B 返回至 A
* B.onPause
* A.onRestart
* A.onStart
* A.onResume
* B.onStop
* B.onDestroy
*
* 2. onCreate 中跳转
* A.onCreate
* A.onStart
* A.onResume
* A.onPause
* B.onCreate
* B.onStart
* B.onResume
* A.onStop
*
* 3. onCreate 中先跳转再 finish
* A.onCreate
* B.onCreate
* B.onStart
* B.onResume
* A.onDestroy
*
* 4. 直接在 onCreate 中 finish
* A.onCreate
* A.onDestroy
*
* */