package com.ndhzs.activity.lifecycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// https://developer.android.google.cn/guide/components/activities/activity-lifecycle?hl=zh-cn
// https://blog.csdn.net/yu749942362/article/details/107978083

/**
 * .
 *
 * @author 985892345
 * 2023/5/21 16:28
 */
abstract class AbstractLifecycleActivity : AppCompatActivity() {
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log("onCreate")
  }
  
  override fun onRestart() {
    super.onRestart()
    log("onRestart")
  }
  
  override fun onStart() {
    super.onStart()
    log("onStart")
  }
  
  override fun onResume() {
    super.onResume()
    log("onResume")
  }
  
  override fun onPause() {
    super.onPause()
    log("onPause")
  }
  
  override fun onStop() {
    super.onStop()
    log("onStop")
  }
  
  override fun onDestroy() {
    super.onDestroy()
    log("onDestroy")
  }
  
  protected fun log(msg: String) {
    android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "${this::class.simpleName}: $msg")
  }
}