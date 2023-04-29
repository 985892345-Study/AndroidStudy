package com.ndhuz.android.study

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * onTouchEvent: ACTION_MOVE
 *   ↓
 * ValueAnimator
 *   ↓
 * postOnAnimation
 *   ↓
 * View 测量布局绘制
 *
 *
 * @author 985892345
 * 2023/4/22 20:46
 */
class TestView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
  
  val mValueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
    addUpdateListener {
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "ValueAnimator")
    }
    duration = 100 * 1000
  }
  
  val mRunnable = object : Runnable {
    override fun run() {
      android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
        "postOnAnimation")
      postOnAnimation(this)
    }
  }
  
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "onMeasure")
  }
  
  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "onLayout")
  }
  
  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "onDraw")
  }
  
  override fun onTouchEvent(event: MotionEvent): Boolean {
    android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
      "onTouchEvent")
    requestLayout()
    invalidate()
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
          "onTouchEvent: ACTION_DOWN")
        mValueAnimator.start()
        postOnAnimation(mRunnable)
      }
      MotionEvent.ACTION_POINTER_DOWN -> {
        android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
          "onTouchEvent: ACTION_POINTER_DOWN")
      }
      MotionEvent.ACTION_MOVE -> {
        android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
          "onTouchEvent: ACTION_MOVE")
      }
      MotionEvent.ACTION_POINTER_UP -> {
        android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
          "onTouchEvent: ACTION_POINTER_UP")
      }
      MotionEvent.ACTION_UP -> {
        android.util.Log.d("ggg", "(${Exception().stackTrace[0].run { "$fileName:$lineNumber" }}) -> " +
          "onTouchEvent: ACTION_UP")
        mValueAnimator.cancel()
        removeCallbacks(mRunnable)
      }
    }
    return true
  }
}