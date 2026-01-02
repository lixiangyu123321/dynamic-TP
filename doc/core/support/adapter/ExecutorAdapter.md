# ExecutorAdapter

## 概述

`ExecutorAdapter` 是执行器适配器接口，用于统一不同执行器的操作接口。它的目标是尽可能兼容 `ThreadPoolExecutor` 的 API，使得框架可以统一处理不同类型的执行器。

> 💡 **设计原因**：想了解为什么设计 `ExecutorAdapter` 接口而不是直接继承 `ThreadPoolExecutor`？请查看 [ExecutorAdapter-设计原因](ExecutorAdapter-设计原因.md)

## 核心作用

1. **统一接口**: 为不同类型的执行器提供统一的操作接口
2. **兼容性**: 尽可能兼容 `ThreadPoolExecutor` 的 API
3. **适配器模式**: 使用适配器模式适配不同的执行器实现

## 接口定义

```java
public interface ExecutorAdapter<E extends Executor> extends Executor {
    E getOriginal();
    // ... 其他方法
}
```

## 核心方法

### 1. getOriginal()

**作用**: 获取原始执行器

**返回**: 原始的执行器实例（如 `ThreadPoolExecutor`）

**说明**: 用于访问原始执行器的特定功能

---

### 2. execute(Runnable command)

**作用**: 执行任务

**默认实现**: 委托给原始执行器的 `execute()` 方法

---

### 3. 线程池参数方法

#### getCorePoolSize() / setCorePoolSize(int corePoolSize)

**作用**: 获取/设置核心线程数

#### getMaximumPoolSize() / setMaximumPoolSize(int maximumPoolSize)

**作用**: 获取/设置最大线程数

#### getPoolSize()

**作用**: 获取当前线程池大小

#### getActiveCount()

**作用**: 获取活跃线程数

#### getLargestPoolSize()

**作用**: 获取历史最大线程池大小

**默认值**: `-1`（不支持）

---

### 4. 任务统计方法

#### getTaskCount()

**作用**: 获取总任务数

**默认值**: `-1`（不支持）

#### getCompletedTaskCount()

**作用**: 获取已完成任务数

**默认值**: `-1`（不支持）

---

### 5. 队列相关方法

#### getQueue()

**作用**: 获取任务队列

**默认实现**: 返回 `UnsupportedBlockingQueue`（不支持操作）

#### getQueueType()

**作用**: 获取队列类型

**默认实现**: 返回队列类的简单名称

#### getQueueSize()

**作用**: 获取队列大小

**默认实现**: `getQueue().size()`

#### getQueueRemainingCapacity()

**作用**: 获取队列剩余容量

**默认实现**: `getQueue().remainingCapacity()`

#### getQueueCapacity()

**作用**: 获取队列容量

**默认实现**: `queueSize + remainingCapacity`

#### onRefreshQueueCapacity(int capacity)

**作用**: 刷新队列容量时调用

**默认实现**: 空操作

---

### 6. 拒绝策略方法

#### getRejectedExecutionHandler()

**作用**: 获取拒绝策略

**默认值**: `null`（不支持）

#### setRejectedExecutionHandler(RejectedExecutionHandler handler)

**作用**: 设置拒绝策略

**默认实现**: 空操作（不支持）

#### getRejectHandlerType()

**作用**: 获取拒绝策略类型名称

**默认实现**: 从拒绝策略获取类名，如果为 null 返回 "unknown"

---

### 7. 线程管理方法

#### allowsCoreThreadTimeOut() / allowCoreThreadTimeOut(boolean value)

**作用**: 获取/设置是否允许核心线程超时

**默认值**: `false`（不支持）

#### preStartAllCoreThreads()

**作用**: 预启动所有核心线程

**默认实现**: 空操作（不支持）

#### getKeepAliveTime(TimeUnit unit) / setKeepAliveTime(long time, TimeUnit unit)

**作用**: 获取/设置线程存活时间

**默认值**: `-1`（不支持）

---

### 8. 状态查询方法

#### isShutdown()

**作用**: 判断是否已关闭

**默认值**: `false`（不支持）

#### isTerminated()

**作用**: 判断是否已终止

**默认值**: `false`（不支持）

#### isTerminating()

**作用**: 判断是否正在终止

**默认值**: `false`（不支持）

---

### 9. UnsupportedBlockingQueue

**作用**: 不支持操作的阻塞队列实现

**说明**: 当执行器不支持队列操作时，返回此队列。所有操作都会抛出 `UnsupportedOperationException`。

## 实现类

### ThreadPoolExecutorAdapter

`ThreadPoolExecutor` 的适配器实现，完整实现了所有方法。

## 设计特点

### 1. 默认实现

接口方法都提供了默认实现，适配器只需实现支持的方法：

```java
default int getLargestPoolSize() {
    return -1;  // 默认不支持
}
```

### 2. 兼容性

尽可能兼容 `ThreadPoolExecutor` 的 API，使得框架可以统一处理：

- 参数获取和设置
- 状态查询
- 队列操作
- 拒绝策略

### 3. 扩展性

通过适配器模式，可以轻松适配新的执行器类型：

```java
public class CustomExecutorAdapter implements ExecutorAdapter<CustomExecutor> {
    // 实现接口方法
}
```

## 使用场景

### 1. 统一管理不同执行器

框架通过 `ExecutorAdapter` 统一管理不同类型的执行器：

```java
ExecutorAdapter<?> adapter = executorWrapper.getExecutor();
int coreSize = adapter.getCorePoolSize();
adapter.setCorePoolSize(20);
```

### 2. 适配器实现

为新的执行器类型创建适配器：

```java
public class MyExecutorAdapter implements ExecutorAdapter<MyExecutor> {
    private final MyExecutor executor;
    
    @Override
    public int getCorePoolSize() {
        return executor.getCoreSize();
    }
    
    // 实现其他方法
}
```

## 注意事项

1. **不支持的方法**: 如果执行器不支持某些方法，返回默认值（如 `-1`、`false`、`null`）
2. **队列操作**: 如果执行器不支持队列操作，返回 `UnsupportedBlockingQueue`
3. **线程安全**: 适配器方法需要保证线程安全

