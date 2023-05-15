package com.ndhzs.xposeddebugger.utils

import android.util.Log
import de.robv.android.xposed.XposedBridge

/**
 * .
 *
 * @author 985892345
 * 2023/5/12 22:10
 */

private val TAG = "ggg"

fun log(mes: String, tag: String = TAG) {
  XposedBridge.log("$tag: $mes")
  Log.d(tag, mes)
}

fun log(mes: String, e: Throwable, tag: String = TAG) {
  log("$mes\n${e.stackTraceToString()}", tag)
}

fun log(e: Throwable, tag: String = TAG) {
  log(e.javaClass.simpleName, e, tag)
}

object Log {
  fun e(e: Throwable) {
    val stack = e.stackTraceToString()
    XposedBridge.log("$TAG: $stack")
    Log.e(TAG, stack)
  }
  
  fun e(msg: String) {
    XposedBridge.log("$TAG: $msg")
    Log.e(TAG, msg)
  }
}
