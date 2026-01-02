# PriorityFutureTask

## 概述

`PriorityFutureTask` 是优先级 FutureTask，继承自 `FutureTask`，实现了 `Priority` 接口。它用于支持优先级任务的异步执行。

## 核心作用

1. **优先级支持**: 为 `FutureTask` 添加优先级支持
2. **任务执行**: 支持异步任务执行
3. **优先级获取**: 提供优先级值获取

## 核心属性

```java
private final Priority obj;  // 优先级对象（PriorityRunnable 或 PriorityCallable）
private final int priority;  // 优先级值
```

## 核心方法

### PriorityFutureTask(Runnable runnable, V result)

**作用**: 构造方法（Runnable 任务）

**实现**:
```java
public PriorityFutureTask(Runnable runnable, V result) {
    super(runnable, result);
    this.obj = (PriorityRunnable) runnable;
    this.priority = this.obj.getPriority();
}
```

**说明**: 
- 要求 `runnable` 必须是 `PriorityRunnable` 类型
- 从 `PriorityRunnable` 获取优先级值

---

### PriorityFutureTask(Callable<V> callable)

**作用**: 构造方法（Callable 任务）

**实现**:
```java
public PriorityFutureTask(Callable<V> callable) {
    super(callable);
    this.obj = (PriorityCallable<V>) callable;
    this.priority = this.obj.getPriority();
}
```

**说明**: 
- 要求 `callable` 必须是 `PriorityCallable` 类型
- 从 `PriorityCallable` 获取优先级值

---

### getPriority()

**作用**: 获取优先级值

**实现**:
```java
@Override
public int getPriority() {
    return this.priority;
}
```

## 使用场景

### 1. 优先级任务执行

`PriorityDtpExecutor` 内部使用 `PriorityFutureTask`：

```java
@Override
protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new PriorityFutureTask<>(runnable, value);
}

@Override
protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new PriorityFutureTask<>(callable);
}
```

### 2. 优先级队列排序

`PriorityBlockingQueue` 使用 `PriorityFutureTask` 的优先级进行排序。

## 设计特点

### 1. 继承 FutureTask

继承 `FutureTask`，保持原有的异步执行功能。

### 2. 优先级提取

从包装的任务中提取优先级值。

### 3. 类型要求

要求任务必须是 `PriorityRunnable` 或 `PriorityCallable` 类型。

## 注意事项

1. **类型要求**: 任务必须是 `PriorityRunnable` 或 `PriorityCallable` 类型
2. **优先级值**: 值越小，优先级越高
3. **队列排序**: 优先级用于 `PriorityBlockingQueue` 的排序

