package com.ndhuz.android.study

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.ArrayMap

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val map1 = ArrayMap<Int, String>(4)
    map1.put(0, "0")
    map1.put(1, "1")
    map1.put(2, "2")
    map1.put(3, "3")
    map1.put(4, "4")
    
    val map2 = ArrayMap<Int, String>(4)
    map2.put(0, "123")
    map2.put(1, "456")
  }
}