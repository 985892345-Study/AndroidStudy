package com.ndhuz.threadlocal

/**
 * .
 *
 * @author 985892345
 * 2023/3/24 0:24
 */

/**
 * /// ThreadLocal 有什么作用 ? 为什么要使用 ThreadLocal ?
 * 1、ThreadLocal 是线程局部变量，他在每个线程中都会创建一个不同的副本，线程之间不会互相影响
 *
 *
 * /// ThreadLocal 是怎么实现的 ?
 * 1、Thread 中包含了一个类型为 ThreadLocalMap 的成员变量 (ThreadLocal 的静态内部类)，
 * 2、ThreadLocalMap 包含一个类型为 Entry 的数组，
 * 3、Entry 继承于 WeakReference，保存了 ThreadLocal 对象和 Value 值
 * 4、在调用 ThreadLocal.get() 时，会通过当前所处的 Thread 对象获取 ThreadLocalMap 中 Entry 数组，然后得到 Value 值
 * (在定位 Entry 索引时使用 ThreadLocal 的一个自定义 hash 值来取模获得索引，如果 hash 冲突则调用 nextInt() 搜索下一个位置)
 *
 *
 * /// 为什么使用弱引用 ?
 * 1、主要是防止内存泄漏，因为如果设置成强引用，ThreadLocal 对象就会被 ThreadLocalMap 强持有，ThreadLocalMap 又被 Thread 强持有，
 *   最后导致 ThreadLocal 内存泄漏
 * 2、但是设置成弱引用并不能完全解决内存泄漏问题，因为 ThreadLocalMap 只会在自身方法被调用时才会去检查并移除被回收了的 Entry 对象，
 *   如果一直没有方法被调用的话，就会导致 Value 内存泄漏
 * 3、可以在使用完 ThreadLocal 后手动调用 remove() 方法进行移除
 *
 * 扩展：
 * 软引用: 内存不充足时才会被回收，比弱引用强一点
 * 虚引用: 必须与一个引用队列关联，get() 方法时总会返回 null。
 *   主要用于跟踪垃圾回收过程，在垃圾回收时会把虚引用的对象放进队列中，在出队前不会彻底销毁该对象。
 *   这时就可以调用一些清理资源的工作 (在 hotspot 的 jvm 中对于堆外内存使用到了虚引用 https://zhuanlan.zhihu.com/p/408186038)
 *
 *
 * /// 既然 Value 会导致内存泄漏，那为什么不把 Value 一起用弱引用保存呢 ?
 * 如果 Value 作为弱引用保存，在没有其他地方强引用 Value 对象的时候会被直接 GC 掉，导致出现异常，所以 Value 只能使用强引用保存
 *
 *
 *
 *
 */