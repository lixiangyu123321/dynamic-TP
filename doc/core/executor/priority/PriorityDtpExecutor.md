# PriorityDtpExecutor

## 概述

`PriorityDtpExecutor` 是优先级线程池执行器，继承自 `DtpExecutor`。它使用 `PriorityBlockingQueue` 作为任务队列，支持按优先级执行任务。

## 核心作用

1. **优先级执行**: 支持按优先级执行任务
2. **任务排序**: 使用优先级队列对任务进行排序
3. **灵活提交**: 支持多种提交方式

## 工作原理

### 1. 优先级队列

使用 `PriorityBlockingQueue` 作为任务队列：

```java
public PriorityDtpExecutor(..., int capacity) {
    this(..., new PriorityBlockingQueue<>(capacity), ...);
}
```

### 2. 任务包装

任务会被包装为 `PriorityRunnable` 或 `PriorityCallable`：

```java
public void execute(Runnable command, int priority) {
    super.execute(PriorityRunnable.of(command, priority));
}
```

### 3. 优先级比较

使用 `getRunnableComparator()` 比较任务优先级：

```java
public static Comparator<Runnable> getRunnableComparator() {
    return (o1, o2) -> {
        if (!(o1 instanceof DtpRunnable) || !(o2 instanceof DtpRunnable)) {
            return 0;
        }
        Runnable po1 = ((DtpRunnable) o1).getOriginRunnable();
        Runnable po2 = ((DtpRunnable) o2).getOriginRunnable();
        if (po1 instanceof Priority && po2 instanceof Priority) {
            return Integer.compare(((Priority) po1).getPriority(), ((Priority) po2).getPriority());
        }
        return 0;
    };
}
```

**说明**: 优先级值越小，优先级越高。

## 核心方法

### execute(Runnable command, int priority)

**作用**: 执行任务（指定优先级）

**实现**:
```java
public void execute(Runnable command, int priority) {
    super.execute(PriorityRunnable.of(command, priority));
}
```

---

### submit(Runnable task, int priority)

**作用**: 提交任务（指定优先级）

---

### submit(Callable<T> task, int priority)

**作用**: 提交 Callable 任务（指定优先级）

---

### newTaskFor(Runnable runnable, T value)

**作用**: 创建优先级 FutureTask

**实现**:
```java
@Override
protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new PriorityFutureTask<>(runnable, value);
}
```

## 使用场景

### 1. 优先级任务

```java
PriorityDtpExecutor executor = new PriorityDtpExecutor(10, 20, 60, TimeUnit.SECONDS, 200);

// 高优先级任务
executor.execute(() -> {
    System.out.println("高优先级任务");
}, Priority.HIGHEST_PRECEDENCE);

// 低优先级任务
executor.execute(() -> {
    System.out.println("低优先级任务");
}, Priority.LOWEST_PRECEDENCE);
```

### 2. 使用 Priority 接口

```java
public class PriorityTask implements Runnable, Priority {
    @Override
    public int getPriority() {
        return 10;  // 优先级值
    }
    
    @Override
    public void run() {
        // 任务逻辑
    }
}

executor.execute(new PriorityTask());
```

## 设计特点

### 1. 优先级队列

使用 `PriorityBlockingQueue` 对任务进行排序。

### 2. 任务包装

任务被包装为 `PriorityRunnable` 或 `PriorityCallable`。

### 3. 灵活提交

支持多种提交方式，可以指定优先级。

## 注意事项

1. **优先级值**: 值越小，优先级越高
2. **队列类型**: 必须使用 `PriorityBlockingQueue`
3. **任务包装**: 任务会被包装，原始任务需要实现 `Priority` 接口

