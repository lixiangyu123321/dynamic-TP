# EagerDtpExecutor

## 概述

`EagerDtpExecutor` 是 IO 密集型线程池执行器，继承自 `DtpExecutor`。当核心线程都在忙碌时，它会创建新线程而不是将任务放入阻塞队列，主要用于 IO 密集型场景。

## 核心作用

1. **快速响应**: 队列满时立即创建线程，而不是等待
2. **IO 密集型优化**: 适用于 IO 密集型场景
3. **任务计数**: 跟踪已提交但未完成的任务数

## 核心属性

```java
private final AtomicInteger submittedTaskCount = new AtomicInteger(0);  // 已提交但未完成的任务数
```

## 工作原理

### 1. 任务提交计数

任务提交时增加计数：

```java
@Override
public void execute(Runnable command) {
    submittedTaskCount.incrementAndGet();
    try {
        super.execute(command);
    } catch (RejectedExecutionException rx) {
        // 处理拒绝异常
    }
}
```

### 2. 任务完成计数

任务完成时减少计数：

```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    submittedTaskCount.decrementAndGet();
    super.afterExecute(r, t);
}
```

### 3. TaskQueue 配合

需要配合 `TaskQueue` 使用，`TaskQueue` 的 `offer()` 方法会检查 `submittedTaskCount`：

```java
// 如果有空闲线程，将任务放入队列
if (executor.getSubmittedTaskCount() <= executor.getPoolSize()) {
    return super.offer(runnable);
}
// 否则返回 false，让执行器创建新线程
if (executor.getPoolSize() < executor.getMaximumPoolSize()) {
    return false;
}
```

## 核心方法

### getSubmittedTaskCount()

**作用**: 获取已提交但未完成的任务数

**实现**:
```java
public int getSubmittedTaskCount() {
    return submittedTaskCount.get();
}
```

---

### execute(Runnable command)

**作用**: 执行任务，处理拒绝异常

**实现**:
```java
@Override
public void execute(Runnable command) {
    if (command == null) {
        throw new NullPointerException();
    }
    submittedTaskCount.incrementAndGet();
    try {
        super.execute(command);
    } catch (RejectedExecutionException rx) {
        if (getQueue() instanceof TaskQueue) {
            // 如果队列是 TaskQueue，尝试强制入队
            final TaskQueue queue = (TaskQueue) getQueue();
            try {
                if (!queue.force(command, 0, TimeUnit.MILLISECONDS)) {
                    submittedTaskCount.decrementAndGet();
                    throw new RejectedExecutionException("Queue capacity is full.", rx);
                }
            } catch (InterruptedException x) {
                submittedTaskCount.decrementAndGet();
                throw new RejectedExecutionException(x);
            }
        } else {
            submittedTaskCount.decrementAndGet();
            throw rx;
        }
    }
}
```

## 使用场景

### 1. IO 密集型任务

```java
EagerDtpExecutor executor = new EagerDtpExecutor(10, 50, 60, TimeUnit.SECONDS,
    new TaskQueue(200), threadFactory, handler);

// IO 密集型任务
executor.execute(() -> {
    // 读取文件、网络请求等 IO 操作
    readFile();
});
```

### 2. 快速响应场景

需要快速响应的场景，队列满时立即创建线程。

## 设计特点

### 1. 任务计数

通过 `submittedTaskCount` 跟踪已提交但未完成的任务数。

### 2. TaskQueue 配合

需要配合 `TaskQueue` 使用，`TaskQueue` 根据 `submittedTaskCount` 决定是否入队。

### 3. 拒绝处理

如果任务被拒绝，尝试强制入队。

## 注意事项

1. **队列类型**: 必须使用 `TaskQueue`
2. **任务计数**: 需要准确跟踪任务提交和完成
3. **IO 密集型**: 适用于 IO 密集型场景，CPU 密集型场景不适用

