# ThreadPoolExecutorAdapter

## 概述

`ThreadPoolExecutorAdapter` 是 `ThreadPoolExecutor` 的适配器实现，完整实现了 `ExecutorAdapter` 接口的所有方法，使得框架可以统一管理 JUC 的 `ThreadPoolExecutor`。

## 核心作用

1. **适配 ThreadPoolExecutor**: 将 `ThreadPoolExecutor` 适配为 `ExecutorAdapter`
2. **完整实现**: 实现所有接口方法，提供完整功能
3. **委托模式**: 所有操作都委托给原始 `ThreadPoolExecutor`

## 实现方式

### 委托模式

所有方法都直接委托给原始 `ThreadPoolExecutor`：

```java
@Override
public int getCorePoolSize() {
    return this.executor.getCorePoolSize();
}

@Override
public void setCorePoolSize(int corePoolSize) {
    this.executor.setCorePoolSize(corePoolSize);
}
```

## 核心方法实现

### 构造方法

```java
public ThreadPoolExecutorAdapter(ThreadPoolExecutor executor) {
    this.executor = executor;
}
```

**说明**: 接收一个 `ThreadPoolExecutor` 实例，保存为内部字段

---

### getOriginal()

```java
@Override
public ThreadPoolExecutor getOriginal() {
    return this.executor;
}
```

**作用**: 返回原始的 `ThreadPoolExecutor` 实例

---

### 参数获取和设置

所有参数方法都直接委托：

```java
@Override
public int getCorePoolSize() {
    return this.executor.getCorePoolSize();
}

@Override
public void setCorePoolSize(int corePoolSize) {
    this.executor.setCorePoolSize(corePoolSize);
}
```

**支持的方法**:
- `getCorePoolSize()` / `setCorePoolSize()`
- `getMaximumPoolSize()` / `setMaximumPoolSize()`
- `getPoolSize()`
- `getActiveCount()`
- `getLargestPoolSize()`
- `getTaskCount()`
- `getCompletedTaskCount()`

---

### 队列操作

```java
@Override
public BlockingQueue<Runnable> getQueue() {
    return this.executor.getQueue();
}
```

**说明**: 直接返回 `ThreadPoolExecutor` 的队列

---

### 拒绝策略

```java
@Override
public RejectedExecutionHandler getRejectedExecutionHandler() {
    return this.executor.getRejectedExecutionHandler();
}

@Override
public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
    this.executor.setRejectedExecutionHandler(handler);
}

@Override
public String getRejectHandlerType() {
    if (executor instanceof RejectHandlerAware) {
        return ((RejectHandlerAware) executor).getRejectHandlerType();
    }
    return getRejectedExecutionHandler().getClass().getSimpleName();
}
```

**说明**: 
- 获取和设置拒绝策略直接委托
- 获取拒绝策略类型时，如果执行器实现了 `RejectHandlerAware`，优先使用其方法

---

### 线程管理

```java
@Override
public boolean allowsCoreThreadTimeOut() {
    return this.executor.allowsCoreThreadTimeOut();
}

@Override
public void allowCoreThreadTimeOut(boolean value) {
    this.executor.allowCoreThreadTimeOut(value);
}

@Override
public void preStartAllCoreThreads() {
    this.executor.prestartAllCoreThreads();
}

@Override
public long getKeepAliveTime(TimeUnit unit) {
    return this.executor.getKeepAliveTime(unit);
}

@Override
public void setKeepAliveTime(long time, TimeUnit unit) {
    this.executor.setKeepAliveTime(time, unit);
}
```

---

### 状态查询

```java
@Override
public boolean isShutdown() {
    return this.executor.isShutdown();
}

@Override
public boolean isTerminated() {
    return this.executor.isTerminated();
}

@Override
public boolean isTerminating() {
    return this.executor.isTerminating();
}
```

## 使用场景

### 1. 包装普通线程池

当框架识别到 `ThreadPoolExecutor` Bean 时，会创建适配器：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
ThreadPoolExecutorAdapter adapter = new ThreadPoolExecutorAdapter(executor);
ExecutorWrapper wrapper = new ExecutorWrapper("myPool", adapter);
```

### 2. 统一管理

通过适配器，框架可以统一管理 `ThreadPoolExecutor`：

```java
ExecutorAdapter<?> adapter = wrapper.getExecutor();
adapter.setCorePoolSize(20);  // 动态调整参数
```

## 设计特点

### 1. 完全委托

所有操作都委托给原始 `ThreadPoolExecutor`，不进行任何转换或增强。

### 2. 零开销

适配器只是简单的委托，没有额外的性能开销。

### 3. 类型安全

通过泛型 `ExecutorAdapter<ThreadPoolExecutor>` 保证类型安全。

## 与其他组件的关系

```
ThreadPoolExecutor
    │
    └─> ThreadPoolExecutorAdapter (适配)
        │
        └─> ExecutorWrapper (包装)
            │
            └─> DtpRegistry (注册)
```

## 注意事项

1. **线程安全**: `ThreadPoolExecutor` 本身是线程安全的，适配器也是线程安全的
2. **原始引用**: 可以通过 `getOriginal()` 获取原始执行器，但建议通过适配器操作
3. **功能完整**: 由于 `ThreadPoolExecutor` 功能完整，适配器实现了所有接口方法

