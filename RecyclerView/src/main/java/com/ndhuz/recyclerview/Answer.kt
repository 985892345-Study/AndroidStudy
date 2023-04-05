package com.ndhuz.recyclerview

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 23:11
 */

/*
* 测量与布局：
* /// RV 的测量流程 ?
* 分三步走
* 1. 如果没有设置 LayoutManager，就直接根据父布局给的宽高和测量模式测量自身
* 2. 如果 LayoutManager 采用自动测量模式 (isAutoMeasureEnabled() 方法返回 true)
*   2.1. 先调用 LayoutManager.onMeasure()。但官方明确表示在自动测量模式中不应该重写它，调用他只是为了兼容旧代码
*   2.2. 在宽高测量模式都为精确值时就直接退出测量
*   2.3. 在宽高测量模式有一边不是精确值时将调用 dispatchLayoutStep1() 和 dispatchLayoutStep2()
*   2.4. 如果自身宽高测量模式都不为精确值，并且存在一个子 View 的宽高也不为精确值，则将以精确值的模式重新布局
* 3. 对于非自动测量模式的 LayoutManager，则会调用 LayoutManager.onMeasure() 进行自定义布局
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
* 3. 最后调用 dispatchLayoutStep3()
*   3.1. 该方法根据之前保存的信息只执行动画，不参与布局
* */