# DtpLifecycle

## 概述

`DtpLifecycle` 是框架的生命周期管理器，实现了 `LifeCycleManagement` 接口。它负责管理框架的启动和关闭，包括执行器初始化、监控启动、告警启动等。

## 核心作用

1. **生命周期管理**: 管理框架的启动和关闭
2. **执行器初始化**: 初始化所有注册的执行器
3. **组件关闭**: 关闭监控、告警、通知等组件

## 核心属性

```java
private final AtomicBoolean running = new AtomicBoolean(false);  // 运行状态
```

## 核心方法

### start()

**作用**: 启动框架

**实现**:
```java
@Override
public void start() {
    if (this.running.compareAndSet(false, true)) {
        DtpRegistry.getAllExecutors().forEach((k, v) -> 
            DtpLifecycleSupport.initialize(v));  // 初始化所有执行器
    }
}
```

**说明**: 初始化所有注册的执行器

---

### stop()

**作用**: 停止框架

**实现**:
```java
@Override
public void stop() {
    if (this.running.compareAndSet(true, false)) {
        shutdownInternal();  // 关闭内部组件
        DtpRegistry.getAllExecutors().forEach((k, v) -> 
            DtpLifecycleSupport.destroy(v));  // 销毁所有执行器
    }
}
```

---

### shutdownInternal()

**作用**: 关闭内部组件

**实现**:
```java
@Override
public void shutdownInternal() {
    DtpMonitor.destroy();           // 关闭监控
    AlarmManager.destroy();          // 关闭告警管理器
    NoticeManager.destroy();         // 关闭通知管理器
    SystemMetricManager.destroy();   // 关闭系统指标管理器
    EventBusManager.destroy();       // 关闭事件总线
}
```

---

### isRunning()

**作用**: 检查是否运行中

**实现**:
```java
@Override
public boolean isRunning() {
    return this.running.get();
}
```

---

### isAutoStartup() / getPhase()

**作用**: 自动启动和阶段

**实现**:
```java
@Override
public boolean isAutoStartup() {
    return true;  // 自动启动
}

@Override
public int getPhase() {
    return Integer.MAX_VALUE;  // 最后阶段
}
```

## 使用场景

### 1. Spring 集成

在 Spring 环境中，`DtpLifecycle` 会被自动管理：

```java
@Bean
public DtpLifecycle dtpLifecycle() {
    return new DtpLifecycle();
}
```

### 2. 手动管理

```java
DtpLifecycle lifecycle = new DtpLifecycle();
lifecycle.start();   // 启动
// ...
lifecycle.stop();    // 停止
```

## 设计特点

### 1. 原子操作

使用 `AtomicBoolean` 保证线程安全。

### 2. 组件关闭

按顺序关闭各个组件，确保资源正确释放。

### 3. 最后阶段

在 Spring 中设置为最后阶段，确保其他组件都已初始化。

## 注意事项

1. **启动顺序**: 在 Spring 中会在最后阶段启动
2. **关闭顺序**: 先关闭内部组件，再销毁执行器
3. **线程安全**: 使用原子操作保证线程安全

