# Delayed

## 概述

`Delayed` 是 Java 并发包 (`java.util.concurrent`) 中的一个接口，用于标记那些应该在给定延迟时间之后执行的对象。它继承自 `Comparable<Delayed>` 接口，使得延迟对象可以按照延迟时间进行排序。

## 核心作用

1. **延迟执行标记**: 标记需要在指定延迟时间后执行的对象
2. **延迟时间获取**: 提供获取剩余延迟时间的方法
3. **可排序性**: 继承 `Comparable`，支持按延迟时间排序

## 接口定义

```java
public interface Delayed extends Comparable<Delayed> {

    /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
    long getDelay(TimeUnit unit);
}
```

## 核心方法

### getDelay(TimeUnit unit)

**作用**: 获取与此对象关联的剩余延迟时间

**参数**:
- `unit`: 时间单位

**返回值**: 
- 剩余延迟时间（以指定时间单位表示）
- 零或负值表示延迟时间已经过去（可以执行）

**说明**: 
- 该方法用于查询对象的剩余延迟时间
- 返回值为零或负数时，表示延迟已过期，对象可以执行
- 时间单位由 `TimeUnit` 枚举指定（如 `TimeUnit.MILLISECONDS`、`TimeUnit.SECONDS` 等）

## 使用场景

### 1. 延迟队列 (DelayQueue)

`Delayed` 接口主要用于 `DelayQueue`，这是一个无界阻塞队列，只有在延迟期满时才能从中提取元素：

```java
DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();

// 添加延迟任务
delayQueue.offer(new DelayedTask("task1", 5, TimeUnit.SECONDS));
delayQueue.offer(new DelayedTask("task2", 3, TimeUnit.SECONDS));

// 获取到期的任务（会阻塞直到有任务到期）
DelayedTask task = delayQueue.take();
```

### 2. 定时任务调度

实现定时任务调度，在指定时间后执行：

```java
public class ScheduledTask implements Delayed {
    private final long executeTime;
    private final Runnable task;
    
    public ScheduledTask(Runnable task, long delay, TimeUnit unit) {
        this.task = task;
        this.executeTime = System.currentTimeMillis() + unit.toMillis(delay);
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.executeTime, ((ScheduledTask) o).executeTime);
    }
    
    public void execute() {
        task.run();
    }
}
```

### 3. 缓存过期

实现带过期时间的缓存：

```java
public class CacheItem implements Delayed {
    private final String key;
    private final Object value;
    private final long expireTime;
    
    public CacheItem(String key, Object value, long ttl, TimeUnit unit) {
        this.key = key;
        this.value = value;
        this.expireTime = System.currentTimeMillis() + unit.toMillis(ttl);
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = expireTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.expireTime, ((CacheItem) o).expireTime);
    }
}
```

## 实现要求

### 1. 必须实现 Comparable

由于 `Delayed` 继承自 `Comparable<Delayed>`，实现类必须提供 `compareTo()` 方法：

```java
@Override
public int compareTo(Delayed o) {
    // 通常按照延迟时间进行比较
    return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), 
                       o.getDelay(TimeUnit.NANOSECONDS));
}
```

### 2. getDelay() 返回值说明

- **正数**: 表示还有剩余延迟时间
- **零或负数**: 表示延迟已过期，对象可以执行或处理

### 3. 时间单位转换

`getDelay()` 方法需要根据传入的 `TimeUnit` 参数返回相应单位的延迟时间，通常需要从内部存储的时间（如毫秒）转换为请求的单位。

## 设计特点

### 1. 延迟时间抽象

将延迟时间的概念抽象为接口，使得不同的延迟对象可以统一处理。

### 2. 可排序性

通过继承 `Comparable`，支持延迟对象按照延迟时间排序，便于在队列中按顺序处理。

### 3. 时间单位灵活性

通过 `TimeUnit` 参数，支持多种时间单位的查询，提高了接口的灵活性。

## 注意事项

1. **线程安全**: `getDelay()` 方法可能被多个线程调用，实现时需要考虑线程安全性
2. **时间精度**: 注意时间精度问题，避免因精度丢失导致的计算错误
3. **负数处理**: `getDelay()` 返回零或负数时表示延迟已过期，实现逻辑需要正确处理
4. **compareTo 一致性**: `compareTo()` 方法的实现应该与 `getDelay()` 保持一致，确保排序正确

## 相关类

- **DelayQueue**: 使用 `Delayed` 接口的无界阻塞队列
- **ScheduledExecutorService**: 定时任务执行器（内部可能使用 `Delayed`）
- **TimeUnit**: 时间单位枚举类

