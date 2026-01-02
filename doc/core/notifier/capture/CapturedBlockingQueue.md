# CapturedBlockingQueue

## 概述

`CapturedBlockingQueue` 是捕获的阻塞队列，实现了 `BlockingQueue` 接口。它用于在构建告警上下文时捕获队列的状态快照，确保告警内容中的队列状态与触发告警时的状态一致。

## 核心作用

1. **队列快照**: 捕获队列的状态快照
2. **数据一致性**: 保证告警内容中的队列状态与触发时一致
3. **只读访问**: 提供只读访问，不支持修改操作

## 核心属性

```java
private final int size;                // 队列大小（快照）
private final int remainingCapacity;  // 队列剩余容量（快照）
private final int queueCapacity;      // 队列容量（快照）
private final String queueType;       // 队列类型（快照）
private final BlockingQueue<Runnable> originQueue;  // 原始队列
```

## 核心方法

### CapturedBlockingQueue(ExecutorAdapter<?> executorAdapter)

**作用**: 构造方法，捕获队列状态

**实现**:
```java
public CapturedBlockingQueue(ExecutorAdapter<?> executorAdapter) {
    this.size = executorAdapter.getQueueSize();
    this.remainingCapacity = executorAdapter.getQueueRemainingCapacity();
    this.queueCapacity = executorAdapter.getQueueCapacity();
    this.queueType = executorAdapter.getQueueType();
    this.originQueue = executorAdapter.getQueue();
}
```

**说明**: 在构造时捕获所有队列状态，后续访问返回快照值

---

### size()

**作用**: 获取队列大小

**返回**: 构造时捕获的队列大小

---

### remainingCapacity()

**作用**: 获取队列剩余容量

**返回**: 构造时捕获的剩余容量

---

### getQueueCapacity()

**作用**: 获取队列容量

**返回**: 构造时捕获的队列容量

---

### getQueueType()

**作用**: 获取队列类型

**返回**: 构造时捕获的队列类型

---

### getOriginQueue()

**作用**: 获取原始队列

**返回**: 原始队列对象

---

### 其他方法

**作用**: 不支持操作

**实现**: 所有其他方法都抛出 `UnsupportedOperationException`

## 使用场景

### 1. 告警上下文构建

在构建告警上下文时使用：

```java
CapturedExecutor capturedExecutor = new CapturedExecutor(executorAdapter);
CapturedBlockingQueue queue = capturedExecutor.getQueue();  // 获取的是 CapturedBlockingQueue
// 获取的状态是触发告警时的快照
```

## 设计特点

### 1. 快照机制

在构造时捕获所有状态，后续访问返回快照值。

### 2. 只读访问

提供只读访问，不支持修改操作。

### 3. 数据一致性

保证告警内容中的队列状态与触发时一致。

## 注意事项

1. **快照时机**: 在构造时捕获状态，后续状态变化不会反映
2. **只读访问**: 不支持修改操作，所有修改方法都抛出异常
3. **数据一致性**: 保证告警内容中的队列状态与触发时一致

