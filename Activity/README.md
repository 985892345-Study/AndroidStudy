# Activity

## Lifecycle
### 点击跳转然后返回
```mermaid
sequenceDiagram
Activity1->>Activity1: onCreate()、onStart()、onResume()
Activity1->>Activity2: 点击跳转
Activity1->>Activity1: onPause()
Activity2->>Activity2: onCreate()、onStart()、onResume()
Activity1->>Activity1: onStop()
Activity2->>Activity1: 返回
Activity2->>Activity2: onPause()
Activity1->>Activity1: onRestart()、onStart()、onResume()
Activity2->>Activity2: onStop()、onDestory()
```

### 在 onCreate() 中直接跳转
```mermaid
sequenceDiagram
Activity1->>Activity1: onCreate()、onStart()、onResume()、onPause()
Activity2->>Activity2: onCreate()、onStart()、onResume()
Activity1->>Activity1: onStop()
```

### 在 onCreate() 中先跳转后直接 finish()
```mermaid
sequenceDiagram
Activity1->>Activity1: onCreate()
Activity2->>Activity2: onCreate()、onStart()、onResume()
Activity1->>Activity1: onDestory()
```

### 直接在 onCreate() 中 finish()
```mermaid
sequenceDiagram
Activity1->>Activity1: onCreate()
Activity1->>Activity1: onDestory()
```

### 旋转重建的生命周期
其实跟 finish() 后重新打开是一致的，走完了全部生命周期
```mermaid
sequenceDiagram
Activity->>Activity: onCreate()、onStart()、onResume()
Activity->>Activity: onPause
Activity->>Activity: onStop
Activity->>Activity: onDestory
Activity->>Activity: onCreate()、onStart()、onResume()
```

## startActivity() 过程分析
```mermaid
sequenceDiagram
Activity1->>Instrumentation: startActivityForResult
alt Target <= API28
    Instrumentation->>ActivityManagerService: startActivity
else Target > API28
    Instrumentation->>ActivityTaskManager: startActivity
end
ActivityManagerService->>ActivityThread: scheduleTransaction
ActivityTaskManager->>ActivityThread: scheduleTransaction
ActivityThread->>ActivityThread#H: sendMessage
ActivityThread#H->>ActivityThread#H: handleMessage
ActivityThread#H->>ActivityThread: performLaunchActivity
ActivityThread->>Instrumentation: newActivity
Instrumentation->>Instrumentation: callActivityOnCreate
Instrumentation->>Activity2: performCreate
```
