package com.ndhuz.recyclerview

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 23:11
 */

/*
* https://juejin.cn/post/6931894526160142350
*
* /// RV 的测量流程 ?
* 分三步走
* 1. 如果没有设置 LayoutManager，就直接根据父布局给的宽高和测量模式测量自身
* 2. 如果 LayoutManager 采用自动测量模式 (isAutoMeasureEnabled() 方法返回 true)
*   2.1. 先调用 LayoutManager.onMeasure()。(但官方明确表示在自动测量模式中不应该重写它，调用他只是为了兼容旧代码)
*   2.2. 在宽高测量模式都为精确值时就直接退出测量
*   2.3. 在宽高测量模式有一边不是精确值时将调用 dispatchLayoutStep1() 和 dispatchLayoutStep2()
*   2.4. 如果自身宽高测量模式都不为精确值，并且存在一个子 View 的宽高也不为精确值，则将以精确值的模式重新布局
* 3. 对于非自动测量模式的 LayoutManager，则会调用 LayoutManager.onMeasure() 进行自定义测量
*
* 精简回答：
* onMeasure方法中，
* 首先如果没有设置 LayoutManager，则直接根据父布局给出的宽高和测量模式测量自身，
* 在设置了 LayoutManager 时，则会根据是否采取自动测量模式来进行测量，
* 如果不是自动测量模式时，则会直接调用 LayoutManager.onMeasure() 进行测量，一般出现在自定义 LayoutManager 中
* 官方的 LayoutManager 默认采用自动测量模式，
* 在自动测量模式中，如果 rv 自身的宽高测量模式有一边不为 EXACTLY 时则将调用 dispatchLayoutStep1() 进行预布局
* 然后调用 dispatchLayoutStep2() 进行实际的布局。
* 以上是 rv 的 onMeasure() 过程
*
*
* /// RV 的布局流程 ?
* 也分三步
* 1. 如果 onMeasure() 中没有调用过 dispatchLayoutStep1()，则会调用一次 dispatchLayoutStep1()
*   1.1. 判断是否需要开启动画
*   1.2. 如果开启动画的话就保存旧的 ViewHolder 信息
*   1.3. 如果是 item 移动动画就调用 LayoutManager.onLayoutChildren() 进行预布局并记录新增的 ViewHolder
* 2. 然后调用 dispatchLayoutStep2()
*   2.1. 他直接调用 LayoutManager.onLayoutChildren() 进行正式布局
*   2.2. 正常布局填完屏幕后，如果一级缓存 Scrap 中有多的未被移除的 ViewHolder，仍然会布局上去 (由 LayoutManager 实现)
* 3. 最后调用 dispatchLayoutStep3()
*   3.1. 该方法根据之前保存的信息只执行动画
*   3.2. 执行动画后通知 LayoutManager 回收 dispatchLayoutStep2() 中多布局的 ViewHolder (会回收到二级缓存 mCacheViews 中)
*
* 精简回答：
* rv 的布局有三个方法，dispatchLayoutStep1/2/3，
* dispatchLayoutStep1() 用于预布局，在开启动画时会调用 LayoutManager.onLayoutChildren() 进行预布局并记录新增的 ViewHolder
* dispatchLayoutStep2() 用于实际布局，调用 LayoutManager.onLayoutChildren()
* dispatchLayoutStep3() 用于开启动画
*
*
* /// RV 动画的实现 ?
* 1. Rv 中有一个 ViewInfoStore 工具类专门记录 item 的位置信息
* 2. 首先在 dispatchLayoutStep1() 中保存旧的 item 位置信息和新增的 item 的信息
* 3. 然后在 dispatchLayoutStep3() 中保存布局后的 item 信息，
* 4. 最后调用 mItemAnimator 执行动画
* 官方默认的 ItemAnimator 通过调用 view 自带的 animate() 设置 translationX、translationY 来实现动画
* (view.animate() 是官方封装的一种 属性 动画，可以用于更方便的实现动画)
*
* 5. 在动画执行完时对于需要回收的 holder 会回收进二级缓存 mCachedViews 中
*
*
* /// RV 四级缓存 ?
*
*
* /// RV hasStableIds 的作用 ?
*
*
* */