package com.ndhuz.arraymap

/**
 * .
 *
 * @author 985892345
 * 2023/4/3 16:47
 */

/*
* http://gityuan.com/2019/01/13/arraymap/
*
* /// ArrayMap 的原理 ?
* 1、ArrayMap 是一个键值对的 Map 容器
* 2、采用二分查找键值对
* 3、内部有两个数组，一个 mHashes 数组值保存 key 的 hash 值，一个 mArray 数组保存 key 和 value (两个为一组的保存，所以 mArray 长度是 mHashes 的两倍)
*
*
* /// ArrayMap 是怎么解决 hash 冲突的 ?
* ArrayMap 遇到 hash 冲突时将保存到下一个为空的位置
* HashMap 采用链表 + 红黑树解决 hash 冲突 (jdk7为只有链表 + 头插法，jdk8/sdk26 采用先链表 + 尾插法，链表超过长度 8 后使用红黑树)
* https://juejin.cn/post/7163985718417555487
*
* /// ArrayMap 的存储结构 ?
* 两个数组
* 一个 mHashes 数组只保存 key 的 hash 值
* 另一个 mArray 数组保存 key 和 value 值 (mArray 数组长度是 mHashes 的两倍)
* 先二分查找 mHashes 中的 hash 值找到索引，再通过该索引左移一位就是 mArray 中的 key 位置，再 +1 就是 value 值
*
*
* /// ArrayMap 的缓存机制 ?
* ArrayMap 带有两个静态变量用于保存长度为 4 和长度为 8 的缓存池
* 该缓存池的 array[0] 保存着下一个数组，array[1] 保存着对应的 mHashes 数组
* - 初始长度设置为 4 或 8 时使用缓存池
* - 默认初始长度，在 put 时会使用长度为 4 的缓存，put 元素超过 4 时将使用长度为 8 的缓存，超过 8 时将以 1.5 倍扩容
* - 在 ArrayMap 收缩容量时也会考虑替换为缓存
*
*
* /// ArrayMap 的容量收缩机制 ?
* 在 mHashes 的长度大于 8，但 mSize 小于 mHashes 实际长度的 3 分之 1 时会触发容量收缩机制
* - 如果当前长度大于 8，则收缩为当前长度的 1.5 倍
* - 如果小于等于 8，则收缩为 8，然后使用缓存
* 比如：mHashes.size = 9，mSize = 2，此时会收缩容量为 8，刚好可以使用缓存
*
*
* /// ArrayMap 的扩容机制 ?
* - mSize < 4，则扩容为 4，使用缓存
* - 4 <= mSize < 8，则扩容为 8 ，使用缓存
* - mSize >= 8，则扩容为 1.5 倍
*
*
* /// ArrayMap、HashMap、SparseArray 比较 ?
* http://gityuan.com/2019/01/13/arraymap/#%E4%BA%94%E6%80%BB%E7%BB%93
* //# 数据结构
* - ArrayMap 采用两个数组，减少了内存消耗
* - SparseArray 也采用两个数组，保存 key 的是基本类型数组，更节省内存
* - HashMap 采用数组 + 链表 + 红黑树，并且每个数据都会被单独包裹，更消耗内存
*
* //# 性能方面
* - ArrayMap 和 SparseArray 查找复杂度为 O(logn)
* - ArrayMap 添加、删除将移动成员，时间复杂度 O(n)
* - SparseArray 有延迟删除特点，适合频繁添加和删除
* - HashMap 查找、添加、删除都是 O(1)
*
* //# 缓存机制
* - ArrayMap 缓存了长度为 4 和 8 的数组
* - SparseArray 有延迟删除的特点，更适合频繁添加和删除
* - HashMap 无缓存
*
* 综上：
* 少量数据，int 为 key 时首选 SparseArray，其次选择 ArrayMap
* */