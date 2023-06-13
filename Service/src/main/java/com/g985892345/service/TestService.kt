package com.g985892345.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

// https://blog.csdn.net/qq_40959750/article/details/119140133
// https://blog.csdn.net/weixin_43615488/article/details/104687977

/**
 * .
 *
 * @author 985892345
 * 2023/6/13 15:22
 */
class TestService : Service() {
  
  override fun onCreate() {
    super.onCreate()
  }
  
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return super.onStartCommand(intent, flags, startId)
  }
  
  override fun onBind(intent: Intent?): IBinder? {
    // 使用 aidl 创建
    return null
  }
  
  override fun onDestroy() {
    super.onDestroy()
  }
}