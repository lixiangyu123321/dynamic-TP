# ScheduledThreadPoolExecutorProxy

## 概述

`ScheduledThreadPoolExecutorProxy` 是 `ScheduledThreadPoolExecutor` 的代理类，继承自 `ScheduledThreadPoolExecutor`，用于增强调度线程池的功能，使其支持框架的监控、告警、任务包装等功能。

## 核心作用

1. **功能增强**: 为 `ScheduledThreadPoolExecutor` 添加框架功能
2. **感知器支持**: 支持感知器功能（监控、超时、拒绝等）
3. **任务包装**: 支持任务包装功能
4. **调度方法增强**: 增强所有调度方法，支持任务包装

## 继承关系

```java
ScheduledThreadPoolExecutorProxy extends ScheduledThreadPoolExecutor 
    implements TaskEnhanceAware, RejectHandlerAware
```

## 核心方法

### 构造方法

```java
public ScheduledThreadPoolExecutorProxy(ScheduledThreadPoolExecutor executor) {
    super(executor.getCorePoolSize(), executor.getThreadFactory());
    this.rejectHandlerType = executor.getRejectedExecutionHandler().getClass().getSimpleName();
    setRejectedExecutionHandler(RejectHandlerGetter.getProxy(executor.getRejectedExecutionHandler()));
}
```

**说明**:
- 复制核心线程数和线程工厂
- 记录拒绝策略类型
- 使用代理拒绝策略

---

### execute(Runnable command)

**作用**: 执行任务

**实现**:
```java
@Override
public void execute(Runnable command) {
    command = getEnhancedTask(command);  // 任务包装
    super.execute(command);
}
```

**注意**: 调度线程池的 `execute()` 方法不支持队列超时监控

---

### schedule(Runnable command, long delay, TimeUnit unit)

**作用**: 延迟执行任务

**实现**:
```java
@Override
public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    command = getEnhancedTask(command);  // 任务包装
    return super.schedule(command, delay, unit);
}
```

**说明**: 支持任务包装，但不支持队列超时监控

---

### schedule(Runnable command, V result, long delay, TimeUnit unit)

**作用**: 延迟执行任务并返回结果

**实现**:
```java
public <V> ScheduledFuture<V> schedule(Runnable command, V result, long delay, TimeUnit unit) {
    command = getEnhancedTask(command);  // 任务包装
    return super.schedule(Executors.callable(command, result), delay, unit);
}
```

---

### scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)

**作用**: 按固定速率周期性执行任务

**实现**:
```java
@Override
public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    command = getEnhancedTask(command);  // 任务包装
    return super.scheduleAtFixedRate(command, initialDelay, period, unit);
}
```

---

### scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)

**作用**: 按固定延迟周期性执行任务

**实现**:
```java
@Override
public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    command = getEnhancedTask(command);  // 任务包装
    return super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
}
```

---

### beforeExecute(Thread t, Runnable r)

**作用**: 任务执行前处理

**实现**:
```java
@Override
protected void beforeExecute(Thread t, Runnable r) {
    AwareManager.beforeExecute(this, t, r);  // 感知器：任务执行前
    super.beforeExecute(t, r);
}
```

---

### afterExecute(Runnable r, Throwable t)

**作用**: 任务执行后处理

**实现**:
```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    AwareManager.afterExecute(this, r, t);  // 感知器：任务执行后
    ExecutorUtil.tryExecAfterExecute(r, t);
}
```

## 使用场景

### 1. 增强调度线程池

当框架识别到 `ScheduledThreadPoolExecutor` 时，会创建代理：

```java
ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
ScheduledThreadPoolExecutorProxy proxy = new ScheduledThreadPoolExecutorProxy(executor);
```

### 2. 定时任务增强

代理会自动为定时任务添加：

- 任务包装功能（MDC、TTL 等）
- 感知器支持（监控、超时等）
- 拒绝策略增强（告警）

## 设计特点

### 1. 调度方法增强

所有调度方法都支持任务包装：

- `schedule()`
- `scheduleAtFixedRate()`
- `scheduleWithFixedDelay()`

### 2. 队列超时限制

**注意**: 调度方法不支持队列超时监控，因为调度任务不经过队列。

### 3. 完全兼容

完全兼容 `ScheduledThreadPoolExecutor` 的 API。

## 注意事项

1. **队列超时**: 调度方法不支持队列超时监控
2. **任务包装**: 所有调度方法都支持任务包装
3. **感知器**: 支持感知器功能，但某些功能可能受限

