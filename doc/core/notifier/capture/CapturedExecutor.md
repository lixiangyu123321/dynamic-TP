# CapturedExecutor

## 概述

`CapturedExecutor` 是捕获的执行器，实现了 `ExecutorAdapter` 接口。它用于在构建告警上下文时捕获执行器的状态快照，确保告警内容中的线程池状态与触发告警时的状态一致。

## 核心作用

1. **状态快照**: 捕获执行器的状态快照
2. **数据一致性**: 保证告警内容中的状态与触发时一致
3. **只读访问**: 提供只读访问，不支持修改操作

## 设计目的

根据代码注释：

> 在 `AlarmManager#doTryAlarm` 构建 `BaseNotifyCtx` 时捕获 `DtpExecutor` 的状态。
> 这样可以确保触发告警阈值时的线程池状态与 `AbstractDtpNotifier#buildAlarmContent` 中的内容一致。

## 核心属性

```java
private final ExecutorAdapter<?> originExecutor;  // 原始执行器
private final int corePoolSize;                    // 核心线程数（快照）
private final int maximumPoolSize;                 // 最大线程数（快照）
private final int activeCount;                     // 活跃线程数（快照）
private final int poolSize;                        // 当前线程数（快照）
private final int largestPoolSize;                 // 历史最大线程数（快照）
private final long taskCount;                      // 任务总数（快照）
private final long completedTaskCount;             // 已完成任务数（快照）
private final long keepAliveTime;                  // 线程存活时间（快照）
private final boolean allowCoreThreadTimeOut;      // 是否允许核心线程超时（快照）
private final RejectedExecutionHandler rejectedExecutionHandler;  // 拒绝策略（快照）
private final String rejectHandlerType;            // 拒绝策略类型（快照）
private final CapturedBlockingQueue blockingQueue;  // 捕获的队列（快照）
```

## 核心方法

### CapturedExecutor(ExecutorAdapter<?> executorAdapter)

**作用**: 构造方法，捕获执行器状态

**实现**:
```java
public CapturedExecutor(ExecutorAdapter<?> executorAdapter) {
    this.originExecutor = executorAdapter;
    this.corePoolSize = executorAdapter.getCorePoolSize();
    this.maximumPoolSize = executorAdapter.getMaximumPoolSize();
    this.activeCount = executorAdapter.getActiveCount();
    this.poolSize = executorAdapter.getPoolSize();
    this.largestPoolSize = executorAdapter.getLargestPoolSize();
    this.taskCount = executorAdapter.getTaskCount();
    this.completedTaskCount = executorAdapter.getCompletedTaskCount();
    this.keepAliveTime = executorAdapter.getKeepAliveTime(TimeUnit.SECONDS);
    this.allowCoreThreadTimeOut = executorAdapter.allowsCoreThreadTimeOut();
    this.rejectedExecutionHandler = executorAdapter.getRejectedExecutionHandler();
    this.rejectHandlerType = executorAdapter.getRejectHandlerType();
    this.blockingQueue = new CapturedBlockingQueue(executorAdapter);
}
```

**说明**: 在构造时捕获所有状态，后续访问返回快照值

---

### getOriginal()

**作用**: 获取原始执行器

**返回**: 原始执行器适配器

---

### 所有 getter 方法

**作用**: 返回快照值

**说明**: 所有 getter 方法都返回构造时捕获的值

---

### 所有 setter 方法

**作用**: 不支持修改操作

**实现**: 所有 setter 方法都抛出 `UnsupportedOperationException`

---

### execute(Runnable command)

**作用**: 不支持执行操作

**实现**: 抛出 `UnsupportedOperationException`

## 使用场景

### 1. 告警上下文构建

在构建告警上下文时使用：

```java
ExecutorWrapper wrapper = executorWrapper.capture();  // 捕获快照
AlarmCtx context = new AlarmCtx(wrapper, notifyItem);
```

### 2. 告警消息构建

在构建告警消息时使用：

```java
BaseNotifyCtx context = DtpNotifyCtxHolder.get();
ExecutorWrapper wrapper = context.getExecutorWrapper();
ExecutorAdapter<?> executor = wrapper.getExecutor();  // 获取的是 CapturedExecutor
// 获取的状态是触发告警时的快照
```

## 设计特点

### 1. 快照机制

在构造时捕获所有状态，后续访问返回快照值。

### 2. 只读访问

提供只读访问，不支持修改操作。

### 3. 数据一致性

保证告警内容中的状态与触发时一致。

## 注意事项

1. **快照时机**: 在构造时捕获状态，后续状态变化不会反映
2. **只读访问**: 不支持修改操作，所有 setter 都抛出异常
3. **数据一致性**: 保证告警内容中的状态与触发时一致

