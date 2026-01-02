# TaskQueue

## 概述

`TaskQueue` 是 `EagerDtpExecutor` 专用的任务队列，继承自 `VariableLinkedBlockingQueue`。它根据执行器的状态决定是否将任务放入队列，主要用于 IO 密集型场景。

## 核心作用

1. **智能入队**: 根据执行器状态决定是否入队
2. **快速响应**: 队列满时立即创建线程
3. **IO 密集型优化**: 适用于 IO 密集型场景

## 核心属性

```java
private transient EagerDtpExecutor executor;  // 关联的执行器
```

## 工作原理

### offer(Runnable runnable)

**作用**: 尝试将任务放入队列

**实现逻辑**:

1. **检查执行器**: 如果执行器为空，抛出异常
2. **检查最大线程数**: 如果已达到最大线程数，直接入队
3. **检查空闲线程**: 如果有空闲线程，将任务放入队列
4. **创建新线程**: 如果没有空闲线程且未达到最大线程数，返回 false，让执行器创建新线程

**实现**:
```java
@Override
public boolean offer(@NonNull Runnable runnable) {
    if (executor == null) {
        throw new RejectedExecutionException("The task queue does not have executor.");
    }
    // 如果已达到最大线程数，直接入队
    if (executor.getPoolSize() == executor.getMaximumPoolSize()) {
        return super.offer(runnable);
    }
    // 如果有空闲线程，将任务放入队列
    if (executor.getSubmittedTaskCount() <= executor.getPoolSize()) {
        return super.offer(runnable);
    }
    // 如果没有空闲线程且未达到最大线程数，返回 false，让执行器创建新线程
    if (executor.getPoolSize() < executor.getMaximumPoolSize()) {
        return false;
    }
    // 当前线程数 >= 最大线程数，入队
    return super.offer(runnable);
}
```

---

### force(Runnable o, long timeout, TimeUnit unit)

**作用**: 强制将任务放入队列

**实现**:
```java
public boolean force(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
    if (executor.isShutdown()) {
        throw new RejectedExecutionException("Executor is shutdown.");
    }
    return super.offer(o, timeout, unit);
}
```

## 使用场景

### 1. 配合 EagerDtpExecutor

```java
TaskQueue queue = new TaskQueue(200);
EagerDtpExecutor executor = new EagerDtpExecutor(10, 50, 60, TimeUnit.SECONDS,
    queue, threadFactory, handler);
queue.setExecutor(executor);  // 设置执行器
```

## 设计特点

### 1. 智能入队

根据执行器状态（线程数、已提交任务数）决定是否入队。

### 2. 快速响应

队列满时立即创建线程，而不是等待。

### 3. IO 密集型优化

适用于 IO 密集型场景，线程在等待 IO 时可以处理其他任务。

## 注意事项

1. **执行器关联**: 必须设置执行器
2. **配合使用**: 必须配合 `EagerDtpExecutor` 使用
3. **线程创建**: 返回 false 时会触发线程创建

