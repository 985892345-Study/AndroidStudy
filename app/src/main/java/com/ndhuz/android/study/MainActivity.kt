package com.ndhuz.android.study

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val sparseArray = SparseArray<String>(3)
    sparseArray.append(0, "0")
    sparseArray.append(1, "1")
    sparseArray.append(2, "2")
    try {
      sparseArray.append(3, "3")
    } catch (e: Exception) {
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        e.stackTraceToString())
    }
  }
}