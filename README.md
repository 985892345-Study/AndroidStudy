# Android-Study
用于学习 Android 进阶知识，计划一周干掉一个，能手搓就手搓一遍 :)

## 目录
### 面试必问
- [x] [Handler](Handler)
  - Looper
    - [ThreadLocal](ThreadLocal)
  - MessageQueue
- [ ] [Activity](Activity)
  - 生命周期
  - 启动模式
  - 动态加载 Activity
- [ ] Fragment
  - 生命周期
- [ ] [Service](Service)
- [ ] [Binder](Binder) 
- [ ] 计网
  - TCP
  - UDP
  - Https

### 面试常问
- [x] [RecyclerView](RecyclerView)
  - 四级缓存
  - 测量与布局
  - LinearLayoutManager
- [ ] OkHttp
- [ ] Retrofit
- [ ] Glide
  - 生命周期
  - 三级缓存
  - Bitmap 优化
- [ ] 线程锁
  - synchronized
  - ReentrantLock
  - CAS
- [ ] 线程池
  - CachedThreadPool
  - FixesThreadPool
  - ScheduledThreadPool
  - SingleThreadExecutor
- [ ] JVM

### 集合方面
- [ ] HashMap
- [ ] ConcurrentHashMap
- [x] [SparseArray](SparseArray)
- [x] [ArrayMap](ArrayMap)

### jetpack
- [ ] LiveData
- [ ] ViewModel
- [ ] DataBinding

### View 方面
- [ ] onMeasure, onLayout
- [ ] onDraw
- [ ] 动画
  - 属性动画
  - 补间动画
  - 帧动画
- [ ] 事件分发
- [ ] CoordinatorLayout
- [ ] ConstraintLayout
- [ ] ViewRootImpl

### 其他框架
- [ ] LeakCanary
- [ ] ARouter
- [ ] Coroutines
- [ ] Rxjava
- [ ] EventBus

## 注意事项
### 注释
由于注释都是同一种颜色，不好写笔记，所以我使用了 [Better Highlights](https://plugins.jetbrains.com/plugin/12895-better-highlights) 插件用于给不同的注释显示不同的颜色。
23/5/15: Better Highlights 3.1 和 3.2 版本会导致 Flamingo|2022.2.1+ 无法打开设置，请去官网手动安装 2.10 版本

我的配置如下：
```
Comment
//#          FCAC00
///          FFD51D
注意          FF4302

并删掉官方自带的 *
```
在配置成功后你的注释应该是下面这种颜色(除了代码原有的字体颜色不一样外)：

<img src="doce/img/img_annotation_color.png" width="600" />

因为 cpp 文件未导入进行编译，所以需要开启此选项才能在 cpp 里面看到特殊注释

<img src="doce/img/img_annotation_color_cpp.png" width="360" />

### 如何白嫖?
如果你想白嫖的话，请查看每个项目 java 包下的 Answer.kt，里面包含一些常见的面试题答案 (因为 README 不如直接写注释方便)

