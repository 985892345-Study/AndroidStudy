package com.ndhuz.sparsearray

/**
 * .
 *
 * @author 985892345
 * 2023/4/1 22:39
 */

/*
* https://juejin.cn/post/6844903442901630983
* https://github.com/zhpanvip/AndroidNote/wiki/SparseArray%E5%AE%9E%E7%8E%B0%E5%8E%9F%E7%90%86
*
* /// SparseArray 实现原理 ?
* 1、SparseArray 是一个键值对的 Map 容器，但 key 只能为 int，
* 2、在少量数据的情况下比 HashMap 更节省内存，
* 3、避免了 int 的装箱和拆箱
* 4、内部 key 是有序的，采用二分查找 key
* 5、具有延迟删除的特点，在删除时并不会直接移动数组，而是赋值为 DELETED 标记，在必要时才删除 (比如 put() 时空间不够，或者跟索引相关的操作会删除)
*
*
* /// 如何 gc 的 ?
* 1、在 remove() 时会将 mValues 数组对应的位置赋值为 DELETED 标记，
* 2、在下一次 put() 空间不够或者跟索引相关的操作时会调用 gc
* 3、gc 的整体逻辑就是把后面 value 不为 DELETED 的往前移动
* 4、因此 SparseArray 更适合频繁插入和频繁删除的情况
*
*
* /// 在 remove() 后直接采用反射获取 mSize，为什么会发现 mSize 并没有改变 ?
* 因为 SparseArray 具有延迟删除的特点，删除时只是将 mValues 数组对应的位置赋值为 DELETED 标记，
* 但并不会修改 mSize 大小，mSize 是在 gc() 时才会修改的
*
*
* /// append() 与 put() 的区别 ?
* append() 会对添加的元素进行判断，如果 key 大于 mKeys 数组中最后一个元素值，就会直接添加到队尾
* 在明确知道按递增顺序插入时使用 append() 方法性能更好，避免了二分查找
*
* */