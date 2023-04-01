package com.ndhuz.android.study

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val sparseArray = SparseArray<String>()
    sparseArray.put(0, "0")
    sparseArray.put(1, "1")
    sparseArray.put(2, "2")
    
    sparseArray.removeAt(1)
    
    sparseArray.clear()
    sparseArray.put(0, "0")
  }
}