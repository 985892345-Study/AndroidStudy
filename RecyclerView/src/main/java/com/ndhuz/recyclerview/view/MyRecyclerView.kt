package com.ndhuz.recyclerview.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ndhuz.recyclerview.adapter.MyAdapter
import com.ndhuz.recyclerview.adapter.MyAdapterHelper
import com.ndhuz.recyclerview.animation.MyDefaultItemAnimator
import com.ndhuz.recyclerview.animation.MyItemAnimator
import com.ndhuz.recyclerview.layoutmanager.MyLayoutManager
import com.ndhuz.recyclerview.recycler.MyRecycler
import com.ndhuz.recyclerview.utils.MyChildHelper
import com.ndhuz.recyclerview.utils.MyLayoutParams
import com.ndhuz.recyclerview.utils.MyState
import com.ndhuz.recyclerview.utils.MyViewInfoStore
import com.ndhuz.recyclerview.viewholder.MyViewHolder

/**
 * [RecyclerView]
 *
 * 布局：
 * https://juejin.cn/post/6931894526160142350
 * https://juejin.cn/post/6910128327681474567
 *
 * @author 985892345
 * 2023/4/4 10:21
 */
class MyRecyclerView(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
  
  private lateinit var mAdapter: MyAdapter<*>
  
  private lateinit var mLayout: MyLayoutManager
  
  private val mRecycler = MyRecycler()
  
  private val mState = MyState()
  
  // 是否存在 item 内容发生了更新
  private var mItemsChanged = false
  
  internal val mTempRect = Rect()
  
  // 如果 RV 的大小不受 Adapter 影响则可以设置为 true 进行优化
  private var mHasFixedSize = false
  
  // 是否在测量期间发生了 Adapter 更新
  private var mAdapterUpdateDuringMeasure = false
  
  // item 动画
  private var mItemAnimator: MyItemAnimator? = MyDefaultItemAnimator()
  
  internal val mChildHelper = MyChildHelper(
    object : MyChildHelper.Callback {
      override fun getChildCount(): Int {
        return this@MyRecyclerView.childCount
      }
      
      override fun getChildAt(offset: Int): View {
        return this@MyRecyclerView.getChildAt(offset)
      }
    }
  )
  
  private val mAdapterHelper = MyAdapterHelper(
    object : MyAdapterHelper.Callback {
    
    }
  )
  
  private val mViewInfoStore = MyViewInfoStore()
  
  override fun onMeasure(widthSpec: Int, heightSpec: Int) {
    if (!this::mLayout.isInitialized) {
      /// 在未设置 mLayout 调用默认的测量
      defaultOnMeasure(widthSpec, heightSpec)
      return
    }
    if (mLayout.isAutoMeasureEnabled()) {
      /// 如果交给 RV 的自动测量机制
      // 常用的 LinearLayoutManager 就是自动测量的
      val widthMode = MeasureSpec.getMode(widthSpec)
      val heightMode = MeasureSpec.getMode(heightSpec)
      
      // 这里为了兼容旧代码才调用的 mLayout.onMeasure()
      // 官方源码中表示在 mLayout.isAutoMeasureEnabled() 返回 true 时不建议重写 LayoutManager#onMeasure 方法
      // 正确逻辑来说，这里应该是直接调用 defaultOnMeasure(int, int) 的
      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
      
      val measureSpecModeIsExactly =
        widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY
      if (measureSpecModeIsExactly || !this::mAdapter.isInitialized) {
        /// 如果宽和高都是精确值，则测量已经完成，后面将走 onLayout() 流程
        return
      }
      
      /// 走到这里说明宽和高有一边不为精确值
      
      if (mState.mLayoutStep == MyState.LayoutStep.STEP_START) {
        dispatchLayoutStep1()
      }
      // 在第二步中设置尺寸。为了保持一致性，应该使用旧尺寸进行预布局
      mLayout.setMeasureSpecs(widthSpec, heightSpec)
      mState.mIsMeasuring = true
      dispatchLayoutStep2()
      
      
      // 现在可以从子 View 中获取宽和高，从而获得自身边界值
      mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec)
      
      /// 如果 RecyclerView 的宽度和高度不准确，并且至少有一个孩子的宽度和高度也不准确，会重新测量
      if (mLayout.shouldMeasureTwice()) {
        mLayout.setMeasureSpecs(
          MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
        mState.mIsMeasuring = true
        dispatchLayoutStep2()
        mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec)
      }
    } else {
      /// 交给 LayoutManager 的自定义 onMeasure() 方法进行测量
      if (mHasFixedSize) {
        /// 如果 RV 宽高不受 Adapter 影响，则直接进行测量
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
        return
      }
      if (mAdapterUpdateDuringMeasure) {
        /// 如果 Adapter 发生了更新操作
        // 抑制布局
        startInterceptRequestLayout()
        /// 预加载动画
        processAdapterUpdatesAndSetAnimationFlags()
        
        if (mState.mRunPredictiveAnimations) {
          /// 如果需要预测动画，则会开启预布局
          mState.mInPreLayout = true
        } else {
          // 一次性应用所有更新
          mAdapterHelper.consumeUpdatesInOnePass()
          mState.mInPreLayout = false
        }
        // 抑制布局结束
        stopInterceptRequestLayout(false)
      } else if (mState.mRunPredictiveAnimations) {
        // 如果 mAdapterUpdateDuringMeasure 为假且 mRunPredictiveAnimations 为真：
        // 这意味着已经执行了一个 onMeasure() 调用来处理挂起的适配器更改，
        // 如果 RV 是 layout_width = MATCH_PARENT 的 LinearLayout 的子布局，
        // 则可能会发生两次 onMeasure() 调用。
        // RV 不能第二次调用 LM.onMeasure()，因为 LM 使用 child 测量时 getViewForPosition() 会崩溃。
        setMeasuredDimension(measuredWidth, measuredHeight)
        return
      }
      
      // ...
      // 抑制布局
      startInterceptRequestLayout()
      /// 前面处理了动画，最后调用 LayoutManager.onMeasure() 进行自定义布局
      /// 但值得注意的是这里仅仅是在 mLayout.isAutoMeasureEnabled() = false 时才会调用
      /// LinearLayoutManager 等官方 LM 都是使用了自动测量机制
      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec)
      // 抑制布局结束
      stopInterceptRequestLayout(false)
      mState.mInPreLayout = false
    }
  }
  
  
  /**
   * 使用适配器更新并计算我们要运行的动画类型。在 onMeasure 和 dispatchLayout 中调用。
   * 此方法可能仅处理更新的预布局状态或所有更新。
   */
  private fun processAdapterUpdatesAndSetAnimationFlags() {
    // ...
    if (predictiveItemAnimationsEnabled()) {
      // 如果支持 item 移动动画
      mAdapterHelper.preProcess()
    } else {
      // 不支持 item 移动动画
      // 一次性应用所有更新
      mAdapterHelper.consumeUpdatesInOnePass()
    }
    // 源码中这里有一堆判断，如果是添加、删除或修改就会执行动画
    mState.mRunSimpleAnimations = mItemAnimator != null
    // 源码中这里有一堆判断
    mState.mRunPredictiveAnimations = mState.mRunSimpleAnimations
      && predictiveItemAnimationsEnabled()
  }
  
  private fun predictiveItemAnimationsEnabled(): Boolean {
    // 是否支持 item 预测动画，官方的 LayoutManager 默认支持
    return mItemAnimator != null && mLayout.supportsPredictiveItemAnimations()
  }
  
  internal fun defaultOnMeasure(widthSpec: Int, heightSpec: Int) {
    val width = MyLayoutManager.chooseSize(
      widthSpec,
      paddingLeft + paddingRight,
      ViewCompat.getMinimumWidth(this)
    )
    val height = MyLayoutManager.chooseSize(
      heightSpec,
      paddingTop + paddingBottom,
      ViewCompat.getMinimumHeight(this)
    )
    setMeasuredDimension(width, height)
  }
  
  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    dispatchLayout()
  }
  
  /**
   * [RecyclerView.dispatchLayout]
   */
  private fun dispatchLayout() {
    if (this::mAdapter.isInitialized) return
    if (this::mLayout.isInitialized) return
    mState.mIsMeasuring = false
    if (mState.mLayoutStep == MyState.LayoutStep.STEP_START) {
      /// 在 mLayout.isAutoMeasureEnabled() = false，即不采用自动测量机制时会到这里面
      /// 一般发生在自定义 LayoutManager 中
      /// 或者在宽高都为精确值时也会走到这里，因为 onMeasure() 中没有调用 dispatchLayoutStep1()
      dispatchLayoutStep1()
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
      dispatchLayoutStep2()
    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != width || mLayout.getHeight() != height) {
      /// 这里说明 onMeasure() 中已经调用了 dispatchLayoutStep1() 和 dispatchLayoutStep2()
      /// 但因为有了新的更新或宽高发生改变，需要再次调用 dispatchLayoutStep2() 进行布局
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
      dispatchLayoutStep2()
    } else {
      setMeasuredDimension(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      )
    }
    dispatchLayoutStep3()
  }
  
  /**
   * //# 1、判断是否需要开启动画
   * //# 2、如果开启动画的话就保存旧的 ViewHolder 信息
   * //# 3、如果是 item 移动动画就调用 LayoutManager.onLayoutChildren() 进行预布局并记录新增的 ViewHolder
   *
   * [RecyclerView.dispatchLayoutStep1]
   */
  private fun dispatchLayoutStep1() {
    check(mState.mLayoutStep == MyState.LayoutStep.STEP_START)
    // ...
    mState.mIsMeasuring = false
    // 抑制布局
    startInterceptRequestLayout()
    // ...
    /// 判断是否需要开启动画
    processAdapterUpdatesAndSetAnimationFlags()
    mState.mTrackOldChangeHolders = mState.mRunSimpleAnimations && mItemsChanged
    mItemsChanged = false
    /// 记录预布局状态到 State 中
    mState.mInPreLayout = mState.mRunPredictiveAnimations
    // ...
    if (mState.mRunSimpleAnimations) {
      /*
      * //# 第0步：记录所有未删除的 item 信息
      * */
      for (i in 0 until mChildHelper.getChildCount()) {
        val holder = getChildViewHolderInt(mChildHelper.getChildAt(i))
        if (holder.shouldIgnore() || (holder.isInvalid() && !mAdapter.hasStableIds())) {
          // 如果 holder 需要被忽略掉或者 holder 无效并且 adapter 没有设置唯一 id 时就跳过保存 item 信息
          continue
        }
        /// 得到 item 旧的位置信息 (上次布局的信息)
        val animationInfo = mItemAnimator!!.recordPreLayoutInformation(
          mState, holder, MyItemAnimator.buildAdapterChangeFlagsForAnimations(holder),
          holder.getUnmodifiedPayloads()
        )
        /// 把 item 旧的位置信息保存进 mViewInfoStore
        mViewInfoStore.addToPreLayout(holder, animationInfo)
        if (mState.mTrackOldChangeHolders && holder.isUpdate() && !holder.isRemoved()
          && !holder.shouldIgnore() && !holder.isInvalid()
        ) {
          val key = getChangedHolderKey(holder)
          /// 对于需要发生更新的 holder，将单独保存
          // 因为更新的动画与增删移的动画是分开的。在 dispatchLayoutStep3 中会拿出来进行对比
          mViewInfoStore.addToOldChangeHolders(key, holder)
        }
      }
    }
    if (mState.mRunPredictiveAnimations) {
      /*
      * //# 第 1 步：运行预布局：这将使用项目的旧位置。
      * //# LayoutManager 应该对所有内容进行布局，甚至是删除的项目（尽管不会将删除的项目添加回容器）
      * */
      /// 动画执行前保存 holder 当前位置
      saveOldPositions()
      /// 调用 onLayoutChildren() 进行预布局
      mLayout.onLayoutChildren(mRecycler, mState)
      
      /// 遍历所有 item，寻找预布局后新增的 item
      for (i in 0 until mChildHelper.getChildCount()) {
        val holder = getChildViewHolderInt(mChildHelper.getChildAt(i))
        if (holder.shouldIgnore()) continue
        
        // 根据是否添加进 mViewInfoStore 的 pre layout 来判断
        if (!mViewInfoStore.isInPreLayout(holder)) {
          // 创建一个新的标志
          var flags = MyItemAnimator.buildAdapterChangeFlagsForAnimations(holder)
          val wasHidden = (holder.mFlags and MyViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST) != 0
          if (!wasHidden) {
            // 如果不是隐藏的 item，添加在预布局中出现的标志
            flags = flags or MyViewHolder.FLAG_APPEARED_IN_PRE_LAYOUT
          }
          val animationInfo = mItemAnimator!!.recordPreLayoutInformation(
            mState, holder, flags, holder.getUnmodifiedPayloads()
          )
          if (wasHidden) {
            // 如果是隐藏的 item，就取消标志然后添加进 mViewInfoStore
            recordAnimationInfoIfBouncedHiddenView(holder, animationInfo)
          } else {
            /// 新增的 item 以 Appeared 形式进行添加进 mViewInfoStore
            mViewInfoStore.addToAppearedInPreLayoutHolders(holder, animationInfo)
          }
        }
      }
      clearOldPositions()
    } else {
      clearOldPositions()
    }
    // 抑制布局结束
    stopInterceptRequestLayout(false)
    mState.mLayoutStep = MyState.LayoutStep.STEP_LAYOUT
  }
  
  /**
   * //# 真正用于布局的方法，会回调 LayoutManager.onLayoutChildren()
   * //# 正常布局填完屏幕后，如果一级缓存 Scrap 中有多的未被移除的 ViewHolder，仍然会布局上去 (由 LayoutManager 实现)
   *
   * [RecyclerView.dispatchLayoutStep2]
   */
  private fun dispatchLayoutStep2() {
    check(
      mState.mLayoutStep == MyState.LayoutStep.STEP_LAYOUT
        || mState.mLayoutStep == MyState.LayoutStep.STEP_ANIMATIONS
    )
    // 抑制布局
    startInterceptRequestLayout()
    // 一次性应用所有更新
    mAdapterHelper.consumeUpdatesInOnePass()
    // ...
    mState.mInPreLayout = false
    /// 正式布局
    mLayout.onLayoutChildren(mRecycler, mState)
    
    mState.mLayoutStep = MyState.LayoutStep.STEP_ANIMATIONS
    // ...
    // 抑制布局结束
    stopInterceptRequestLayout(false)
  }
  
  /**
   * //# 根据 dispatchLayoutStep1() 保存的信息执行动画
   * //# 执行动画后通知 LayoutManager 回收 dispatchLayoutStep2() 中多布局的 ViewHolder (会回收到二级缓存 mCacheViews 中)
   *
   * [RecyclerView.dispatchLayoutStep3]
   */
  private fun dispatchLayoutStep3() {
    check(mState.mLayoutStep == MyState.LayoutStep.STEP_ANIMATIONS)
    // 抑制布局
    startInterceptRequestLayout()
    mState.mLayoutStep = MyState.LayoutStep.STEP_START
    if (mState.mRunSimpleAnimations) {
      for (i in 0 until mChildHelper.getChildCount()) {
        val holder = getChildViewHolderInt(mChildHelper.getChildAt(i))
        if (holder.shouldIgnore()) continue
        val key = getChangedHolderKey(holder)
        val animationInfo = mItemAnimator!!.recordPostLayoutInformation(mState, holder)
        val oldChangeViewHolder = mViewInfoStore.getFromOldChangeHolders(key)
        if (oldChangeViewHolder != null && !oldChangeViewHolder.shouldIgnore()) {
          /// 如果是触发更新的 holder
          val oldDisappearing = mViewInfoStore.isDisappearing(oldChangeViewHolder)
          val newDisappearing = mViewInfoStore.isDisappearing(holder)
          if (oldDisappearing && oldChangeViewHolder === holder) {
            // 如果 holder 被移除了，就不再运行更新动画，直接运行 disappear 动画
            mViewInfoStore.addToPostLayout(holder, animationInfo)
          } else {
            val preInfo = mViewInfoStore.popFromPreLayout(oldChangeViewHolder)
            mViewInfoStore.addToPostLayout(holder, animationInfo)
            val postInfo = mViewInfoStore.popFromPostLayout(holder)!!
            if (preInfo == null) {
              // 源码中这里会抛出异常然后打印出来
            } else {
              /// 运行 holder 更新动画 (更新动画单独在这里执行)
              animateChange(
                oldChangeViewHolder, holder, preInfo, postInfo,
                oldDisappearing, newDisappearing
              )
            }
          }
        } else {
          /// 记录被移除和被移动的 holder 信息
          mViewInfoStore.addToPostLayout(holder, animationInfo)
        }
      }
      /// 开始执行动画 (这里面不包含更新动画，只包含增删移动画)
      mViewInfoStore.process(mViewInfoProcessCallback);
    }
    
    /// 回收一级缓存
    mLayout.removeAndRecycleScrapInt(mRecycler)
    mState.mRunSimpleAnimations = false
    mState.mRunPredictiveAnimations = false
    mRecycler.mChangedScrap.clear()
    
    /// 通知布局结束
    mLayout.onLayoutCompleted(mState)
    // 抑制布局结束
    stopInterceptRequestLayout(false)
    mViewInfoStore.clear()
  }
  
  
  // 供 MyLayoutManager 调用，因为 setMeasuredDimension() 是 protected
  internal fun setMeasuredDimensionInternal(measuredWidth: Int, measuredHeight: Int) {
    setMeasuredDimension(measuredWidth, measuredHeight)
  }
  
  // ViewHolder 得到 AdapterPosition
  internal fun getAdapterPositionFoe(holder: MyViewHolder): Int {
    if ((holder.mFlags and (MyViewHolder.FLAG_INVALID or MyViewHolder.FLAG_REMOVED) != 0)
      || !holder.isBound()
    ) {
      return RecyclerView.NO_POSITION
    }
    return mAdapterHelper.applyPendingUpdatesToPosition(holder.mPosition)
  }
  
  // 返回处理更改动画时要使用的唯一键。根据适配器类型，它可能是孩子的位置或稳定的 ID。
  internal fun getChangedHolderKey(holder: MyViewHolder): Long {
    return if (mAdapter.hasStableIds()) holder.getItemId() else holder.mPosition.toLong()
  }
  
  // 保存所有 holder 当前位置
  internal fun saveOldPositions() {
    repeat(mChildHelper.getUnfilteredChildCount()) {
      val holder = getChildViewHolderInt(mChildHelper.getChildAt(it))
      if (!holder.shouldIgnore()) {
        holder.saveOldPosition() // 保存 mPosition 到 mOldPosition
      }
    }
  }
  
  // 清理所有 holder 旧位置
  internal fun clearOldPositions() {
    repeat(mChildHelper.getUnfilteredChildCount()) {
      val holder = getChildViewHolderInt(mChildHelper.getChildAt(it))
      if (!holder.shouldIgnore()) {
        holder.clearOldPosition() // 清除 mOldPosition 和 mPreLayoutPosition
      }
    }
    mRecycler.clearOldPosition()
  }
  
  // 记录从隐藏列表弹回的视图持有者的动画信息。它还清除 FLAG_BOUNCED_FROM_HIDDEN_LIST 标志。
  internal fun recordAnimationInfoIfBouncedHiddenView(
    holder: MyViewHolder,
    animationInfo: MyItemAnimator.MyItemHolderInfo
  ) {
    // 清除 FLAG_BOUNCED_FROM_HIDDEN_LIST 标志
    holder.setFlags(0, MyViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST)
    if (mState.mTrackOldChangeHolders && holder.isUpdate() && !holder.isRemoved()
      && !holder.shouldIgnore()
    ) {
      val key = getChangedHolderKey(holder)
      mViewInfoStore.addToOldChangeHolders(key, holder)
    }
    mViewInfoStore.addToPreLayout(holder, animationInfo)
  }
  
  
  ////////////////////
  //
  //    抑制布局相关
  //
  ////////////////////
  
  // 拦截 requestLayout() 的计数器
  private var mInterceptRequestLayoutDepth = 0
  
  // 是否抑制布局，调用 suppressLayout 时传入的值
  private var mLayoutSuppressed = false
  
  // 抑制布局期间是否调用了 requestLayout 请求重新布局
  private var mLayoutWasDeferred = false
  
  // 拦截 requestLayout()，内部采取计数方式，可重入式
  private fun startInterceptRequestLayout() {
    mInterceptRequestLayoutDepth++
    if (mInterceptRequestLayoutDepth == 1 && !mLayoutSuppressed) {
      mLayoutWasDeferred = false // 初始化
    }
  }
  
  // 取消拦截 requestLayout()，performLayoutChildren: 是否执行子 View 布局
  private fun stopInterceptRequestLayout(performLayoutChildren: Boolean) {
    if (!performLayoutChildren && !mLayoutSuppressed) {
      mLayoutWasDeferred = false
    }
    if (mInterceptRequestLayoutDepth == 1) {
      if (performLayoutChildren && mLayoutWasDeferred && !mLayoutSuppressed
        && this::mLayout.isInitialized && this::mAdapter.isInitialized
      ) {
        dispatchLayout()
      }
      if (!mLayoutSuppressed) {
        mLayoutWasDeferred = false
      }
    }
    mInterceptRequestLayoutDepth--
  }
  
  override fun suppressLayout(suppress: Boolean) {
    if (suppress != mLayoutSuppressed) {
      if (!suppress) {
        mLayoutSuppressed = false
        if (mLayoutWasDeferred && this::mLayout.isInitialized && this::mAdapter.isInitialized) {
          requestLayout()
        }
        mLayoutWasDeferred = false
      } else {
        // 这里 rv 在布局被抑制时直接发送了一个 CANCEL 事件给自身，并且设置了 mIgnoreMotionEventTillDown 变量
        // 用于拦截后续事件，原因我猜测是因为布局抑制后不能滚动索性就直接把自身事件给取消算了
        // (注意这里只是调用 onTouchEvent 给自身，并不会影响子 View 的事件分发)
        //        val now = SystemClock.uptimeMillis()
        //        val cancelEvent = MotionEvent.obtain(
        //          now, now,
        //          MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0
        //        )
        //        onTouchEvent(cancelEvent)
        mLayoutSuppressed = true
        //        mIgnoreMotionEventTillDown = true
        //        stopScroll()
      }
    }
  }
  
  override fun requestLayout() {
    if (mInterceptRequestLayoutDepth == 0 && !mLayoutSuppressed) {
      super.requestLayout()
    } else {
      mLayoutWasDeferred = true
    }
  }
  
  
  //////////////////////
  //
  //      动画相关
  //
  //////////////////////
  
  private val mViewInfoProcessCallback = object : MyViewInfoStore.MyProcessCallback {
    override fun processDisappeared(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo,
      postInfo: MyItemAnimator.MyItemHolderInfo?
    ) {
      // 先从 recycler 中释放当前 holder
      mRecycler.unscrapView(holder)
      // 触发 mItemAnimator.animateAppearance()
    }
  
    override fun processAppeared(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo?,
      postInfo: MyItemAnimator.MyItemHolderInfo
    ) {
      // 触发 mItemAnimator.animateDisappearance()
    }
  
    override fun processPersistent(
      holder: MyViewHolder,
      preInfo: MyItemAnimator.MyItemHolderInfo,
      postInfo: MyItemAnimator.MyItemHolderInfo
    ) {
      // 会调用 mItemAnimator.animateChange() 或者 mItemAnimator.animatePersistence()
    }
  
    override fun unused(holder: MyViewHolder) {
      // 执行 mLayout.removeAndRecycleView(viewHolder.itemView, mRecycler)
    }
    
    /**
     * 上面虽然都调用了 mItemAnimator.animate**() 方法，但都不会立马执行动画，只用来保存信息，
     * 然后 rv 会调用
     * postAnimationRunner()
     *         ↓
     * ViewCompat.postOnAnimation(this, mItemAnimatorRunner)
     * /// 最后动画会在下一帧时才开始执行
     *
     * 动画执行后会回调 [RecyclerView.mItemAnimatorListener]，
     * /// 最后对 holder 进行回收，回收进二级缓存 mCachedViews 中
     */
  }
  
  private fun animateChange(
    oldHolder: MyViewHolder,
    newHolder: MyViewHolder,
    pre: MyItemAnimator.MyItemHolderInfo,
    post: MyItemAnimator.MyItemHolderInfo,
    oldHolderDisappearing: Boolean,
    newHolderDisappearing: Boolean
  ) {
    // 触发 mItemAnimator.animateChange()
  }
  
  companion object {
    
    // 返回视图的边界，包括它的装饰和边距
    fun getDecoratedBoundsWithMarginsInt(view: View, outBounds: Rect) {
      val lp = view.layoutParams as MyLayoutParams
      val insets = lp.mDecorInsets
      outBounds.set(
        view.left - insets.left - lp.leftMargin,
        view.top - insets.top - lp.topMargin,
        view.right + insets.right + lp.rightMargin,
        view.bottom + insets.bottom + lp.bottomMargin
      )
    }
    
    // 通过 view 得到 ViewHolder
    fun getChildViewHolderInt(child: View): MyViewHolder {
      return (child.layoutParams as MyLayoutParams).mViewHolder!!
    }
  }
}