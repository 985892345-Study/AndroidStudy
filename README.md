# Android-Study
用于学习 Android 进阶知识，计划一周干掉一个，能手搓就手搓一遍 :)

## 目录
### 面试必问
- [x] [Handler](Handler)
  - Looper
    - [ThreadLocal](ThreadLocal)
  - MessageQueue
- [ ] Service
- [ ] [Binder](Binder) 
- [ ] Activity
  - 生命周期
  - 启动模式
- [ ] Fragment
  - 生命周期
- [ ] 计网
  - TCP
  - UDP
  - Https

### 面试常问
- [ ] [RecyclerView](RecyclerView)
  - 四级缓存
  - 测量与布局
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
由于注释都是同一种颜色，不好写笔记，所以我使用了 [Better Highlights](https://plugins.jetbrains.com/plugin/12895-better-highlights) 插件用于给不同的注释显示不同的颜色。

我的配置如下：
```
Comment
//#          FCAC00
///          FFD51D
注意          FF4302
```
在配置成功后你的注释应该是下面这种颜色(除了代码原有的字体颜色不一样外)：

<img src="doce/img/img_annotation_color.png" width="600" />

因为 cpp 文件未导入进行编译，所以需要开启此选项才能在 cpp 里面看到特殊注释

<img src="doce/img/img_annotation_color_cpp.png" width="360" />
