package com.ndhuz.recyclerview

/**
 * .
 *
 * @author 985892345
 * 2023/4/4 23:11
 */

/*
* https://juejin.cn/post/6844903910553944078
* https://juejin.cn/post/6931894526160142350
* https://juejin.cn/post/6844904164359667720
* https://blog.csdn.net/qjyws/article/details/123237071
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
* 详细可以看 MyRecycler 中的注释
*
* 一级缓存：由 mAttachScrap 和 mChangedScrap 两个 list 组成，只负责布局期间的缓存，
*   mAttachScrap 负责被移除和带有 payload 更新的 holder，
*   mChangedScrap 负责 mAttachScrap 以外的 holder，通常是不带有 payload 更新的 holder。
*   每次布局时，LayoutManager 会将屏幕上的 view 暂时脱离 RV，放进一级缓存里，
*   然后在布局期间进行查找
* 二级缓存：由一个叫 mCachedView 的 list 负责，
*   只缓存最近离开屏幕且不带有任何 移除、更新、非法标志的 holder，默认容量为 2 个，
*   它采取 position 进行定位，用于上下短距离来回滑动的时候快速找到对应位置的 holder，不会触发重新绑定
* 三级缓存：由开发者自己实现，需要继承于 ViewCacheExtension，
*   三级缓存可以用于拦截 onCreateViewHolder 和 四级缓存，但是在回收 holder 并不会通知三级缓存，
*   三级缓存返回的是 View，而不是 ViewHolder，仍然需要在 Adapter 中添加新的 itemType 才能使用
*   所以三级缓存的使用场景很少，唯一想到的是配合 setIsRecyclable 一起使用，因为设置不可回收后，在离开屏幕后的 holder 会被直接丢弃
* 四级缓存：由 RecyclerViewPool 实现，
*   每种 itemType 默认保存 5 个，且这里面的 holder 需要重新绑定，即调用 onBindViewHolder，
*   RV 提供了方法用于设置自定义的四级缓存，一般用于多个 RV 之间的 ViewHolder 复用
*
*
* /// 调用 notifyDataSetChanged() 会发生什么 ?
* 调用时会回调 AdapterDataObserver 的 onChanged() 方法，RV 在 setAdapter 时就注册了一个对 adapter 的监听，
* 由 RecyclerViewDataObserver 实现，里面的 onChanged() 方法会将在屏幕上和二级缓存中的 ViewHolder 添加 UPDATE 和 INVALID 标志，
*   (RV 布局期间调用刷新会直接抛异常，一级缓存只在布局期间使用，所以没设置一级缓存)
* 如果 Adapter 没有设置 hasStableIds，还会回收二级缓存到四级缓存中，
* 然后调用 requestLayout() 重新布局
* 整个流程后屏幕上的 holder 失效，二级缓存被回收，但四级缓存仍然有效，但是需要重新绑定
*
*
* /// RV hasStableIds 的作用 ?
* 用于表示一个 item 的唯一 id，从一级缓存和二级缓存中查找 holder 时使用
* 这个一般出现在 holder 带有 INVALID 标志时，INVALID 标志一般由 notifyDataSetChanged() 触发，
* 在调用 notifyDataSetChanged() 后，虽然 holder 带有了 INVALID 标志，但是 RV 会根据唯一 id 来查找 holder,
* 这样就可以避免 notifyDataSetChanged() 导致的一、二级缓存失效，并且此时还可以带有移动动画，
*
*
* /// 重新设置 Adapter 会发生什么 ?
* 重新设置 Adapter 时将回收屏幕上和一、二级缓存中所有 ViewHolder 到四级缓存，
* 如果四级缓存 RecycledViewPool 只关联了当前一个 adapter，则四级缓存失效  (如果关联了多个，但你却没有重新设置四级缓存，在 holder 类型不相同时就会导致异常)。
* 如果 ViewHolder 不会发生改变的话，更推荐使用 swapAdapter() 方法重新设置 adapter，该方法可以不导致缓存全部失效
*
*
* /// ViewHolder 的 position ?
* ViewHolder 有三个变量：
* mPosition: onBind 后的位置、移动后位置
* mOldPosition: 在发生位置移动时和预布局前记录 mPosition，只会在没有记录时才记录，预布局(或者 dispatchLayoutStep1)后失效
* mPreLayoutPosition: 预布局中当前 holder 该显示的位置，在发生位置移动时记录 mPosition，只会在没有记录时才记录，预布局(或者 dispatchLayoutStep1)后失效
*
* getOldPosition()              就是返回 mOldPosition，但这个在预布局后就会失效
* getLayoutPosition()           上一次布局时的位置，在预布局前会得到 mPreLayoutPosition，其他时候得到 mPosition
* getAdapterPosition()          1.2.0 后被废弃，因为官方添加了 ConcatAdapter，用于结合多个 adapter，该方法会引起歧义
* getBindingAdapterPosition()   返回绑定的 adapter 布局后的位置，如果调用了 notifyDataSetChanged() 会返回 -1
*                               (原理是通过遍历还没有执行的增删移操作来计算出布局后位置)
* getAbsoluteAdapterPosition()  返回绝对的布局后的位置
*
*
*
* /// RV 的预取操作 ?
* https://juejin.cn/post/6911107137661829128
* 预取就是把将要显示的 ViewHolder 预先放置到缓存中，以优化 RecyclerView 滑动流畅度，在 SDK21 后加入
*
* 由 GapWorker 实现，官方的 LayoutManager 支持
* 基本原理为：rv 通过在手指触摸和滑动中通知 GapWorker 发送一个 Runnable 来提前加载附近的 holder 到缓存中
*
*
* /// LinearLayoutManager 的 holder 加载流程 ?
* 这里以单个 holder 占一页来记录 (就像 VP2 一样)，一页如果有多个 holder，其实逻辑是一样的
*
* //# 第 1 个 holder 的创建在 dispatchLayoutStep2 时触发，记为 A
* onLayout -> dispatchLayoutStep2 -> LayoutManager.onLayoutChildren ->
* 调用 fill 方法布局，布局时会调用 Recycler.tryGetViewHolderForPositionByDeadline ->
* 因为是第 1 个没缓存所以回调 createViewHolder
*
* 当前处于第 0 页
*
* onCreate(A)
* onBind(A)
* 二级缓存: []
*
*
* //# 第 2 个 holder 是在滑动时触发，记为 B
* onTouchEvent -> scrollByInternal -> LayoutManager.scrollHorizontallyBy ->
* 仍然是调用 fill 方法布局，布局时会调用 Recycler.tryGetViewHolderForPositionByDeadline ->
* 因为没缓存所以回调 createViewHolder
*
* 滑进部分第 1 页时
*
* onCreate(B)
* onBind(B)
* 二级缓存: []
*
*
* //# 第 3 个 holder 也是在滑动时触发，记为 C
*
* 预取
*
* onCreate(C)
* 二级缓存: [C]
*
* 滑进部分第 2 页时
*
* onBind(C)
* 二级缓存: []
*
* //# 第 4 个 holder 也是在滑动时触发，记为 D
*
* 预取
*
* 二级缓存: [A]
* onCreate(D)
* 二级缓存: [A, D]
*
* 滑进部分第 3 页时
*
* onBind(D)
* 二级缓存: [A]
*
* //# 第 5 个 holder 也是在滑动时触发，记为 E
*
* 预取
*
* 二级缓存: [A, B]
* onCreate(E)
* 二级缓存: [A, B, E]
*
* 滑进部分第 4 页时
*
* onBind(E)
* 二级缓存: [A, B]
*
* 隔了一段时间
*
* 四级缓存: [A]
* onBind(A) position = 5，注意此时并没有滑进第 5 页
* 二级缓存: [B, C]
*
* 滑进第 5 页
*
* 四级缓存: [B]
* onBind(B) position = 6
* 二级缓存: [C, D]
*
* //# 上面这些原因在 LinearLayoutManager 中，后面有时间再来详细分析
* */