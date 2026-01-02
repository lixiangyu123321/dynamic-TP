# ThreadPoolStatProvider 作用说明

## 概述

`ThreadPoolStatProvider` 是线程池统计信息提供者，为每个线程池提供统一的统计功能。它是框架中任务统计、性能监控、超时监控等功能的底层支撑组件。

## 核心作用

`ThreadPoolStatProvider` 的主要作用包括：

1. **任务性能统计**: 记录任务的执行时间，计算 TPS、响应时间分布等性能指标
2. **超时监控**: 监控任务在队列中的等待时间和执行时间，支持超时告警和中断
3. **拒绝统计**: 统计被拒绝的任务数量
4. **超时统计**: 统计执行超时和队列等待超时的任务数量

## 类结构

### 核心属性

```java
public class ThreadPoolStatProvider {
    // 关联的线程池包装器
    private final ExecutorWrapper executorWrapper;
    
    // 超时配置
    private long runTimeout = 0;           // 任务执行超时时间（毫秒）
    private long queueTimeout = 0;          // 任务队列等待超时时间（毫秒）
    private boolean tryInterrupt = false;   // 超时时是否尝试中断任务
    
    // 统计计数器
    private final LongAdder rejectCount = new LongAdder();        // 拒绝任务计数
    private final LongAdder runTimeoutCount = new LongAdder();    // 执行超时计数
    private final LongAdder queueTimeoutCount = new LongAdder();  // 队列等待超时计数
    
    // 任务跟踪映射
    private final Map<Runnable, SoftReference<Timeout>> runTimeoutMap;     // 执行超时任务映射
    private final Map<Runnable, SoftReference<Timeout>> queueTimeoutMap;   // 队列等待超时任务映射
    private final Map<Runnable, Long> stopWatchMap;                        // 任务计时映射
    
    // 性能提供者
    private final PerformanceProvider performanceProvider;
}
```

## 功能模块

### 1. 任务性能统计

#### startTask(Runnable r)

**作用**: 开始任务计时，记录任务提交时间

**实现**:
```java
public void startTask(Runnable r) {
    stopWatchMap.put(r, System.currentTimeMillis());
}
```

**说明**:
- 将任务和当前时间戳存入 `stopWatchMap`
- 用于后续计算任务的响应时间（RT）
- 由 `PerformanceMonitorAware.execute()` 调用

**使用场景**: 任务提交到线程池时

---

#### completeTask(Runnable r)

**作用**: 完成任务计时，计算响应时间并记录到性能统计

**实现**:
```java
public void completeTask(Runnable r) {
    Optional.ofNullable(stopWatchMap.remove(r))
            .ifPresent(millis -> {
                long rt = System.currentTimeMillis() - millis;
                performanceProvider.completeTask(rt);
            });
}
```

**说明**:
- 从 `stopWatchMap` 中移除任务，获取开始时间
- 计算响应时间：`rt = 当前时间 - 开始时间`
- 将响应时间记录到 `PerformanceProvider`
- `PerformanceProvider` 使用 `MMAPCounter` 统计响应时间分布
- 由 `PerformanceMonitorAware.afterExecute()` 或 `afterReject()` 调用

**统计的指标**:
- TPS（每秒任务数）
- 最大/最小/平均响应时间
- TP50/TP75/TP90/TP95/TP99/TP999（响应时间百分位数）

**使用场景**: 任务执行完成或被拒绝后

---

### 2. 队列等待超时监控

#### startQueueTimeoutTask(Runnable r)

**作用**: 启动任务队列等待超时监控

**实现**:
```java
public void startQueueTimeoutTask(Runnable r) {
    if (queueTimeout <= 0) {
        return;  // 未配置队列超时，不监控
    }
    HashedWheelTimer timer = ContextManagerHelper.getBean(HashedWheelTimer.class);
    QueueTimeoutTimerTask timerTask = new QueueTimeoutTimerTask(executorWrapper, r);
    queueTimeoutMap.put(r, new SoftReference<>(timer.newTimeout(timerTask, queueTimeout, TimeUnit.MILLISECONDS)));
}
```

**说明**:
- 检查是否配置了队列超时时间（`queueTimeout > 0`）
- 使用 `HashedWheelTimer`（时间轮定时器）创建超时任务
- 创建 `QueueTimeoutTimerTask`，用于处理队列超时
- 将任务和超时对象存入 `queueTimeoutMap`（使用 `SoftReference` 避免内存泄漏）
- 由 `TaskTimeoutAware.execute()` 调用

**超时处理**: 如果任务在队列中等待超过 `queueTimeout` 时间，会触发超时处理：
- 记录队列等待超时计数
- 触发告警
- 记录日志

**使用场景**: 任务提交到线程池时（任务可能在队列中等待）

---

#### cancelQueueTimeoutTask(Runnable r)

**作用**: 取消任务队列等待超时监控

**实现**:
```java
public void cancelQueueTimeoutTask(Runnable r) {
    Optional.ofNullable(queueTimeoutMap.remove(r))
            .map(SoftReference::get)
            .ifPresent(Timeout::cancel);
}
```

**说明**:
- 从 `queueTimeoutMap` 中移除任务
- 获取超时对象并取消定时任务
- 由 `TaskTimeoutAware.beforeExecute()` 或 `beforeReject()` 调用

**使用场景**: 
- 任务开始执行时（不再需要监控队列等待时间）
- 任务被拒绝时（不再需要监控）

---

### 3. 任务执行超时监控

#### startRunTimeoutTask(Thread t, Runnable r)

**作用**: 启动任务执行超时监控

**实现**:
```java
public void startRunTimeoutTask(Thread t, Runnable r) {
    if (runTimeout <= 0) {
        return;  // 未配置执行超时，不监控
    }
    HashedWheelTimer timer = ContextManagerHelper.getBean(HashedWheelTimer.class);
    RunTimeoutTimerTask timerTask = new RunTimeoutTimerTask(executorWrapper, r, t);
    runTimeoutMap.put(r, new SoftReference<>(timer.newTimeout(timerTask, runTimeout, TimeUnit.MILLISECONDS)));
}
```

**说明**:
- 检查是否配置了执行超时时间（`runTimeout > 0`）
- 使用 `HashedWheelTimer` 创建超时任务
- 创建 `RunTimeoutTimerTask`，用于处理执行超时
- 将任务和超时对象存入 `runTimeoutMap`
- 由 `TaskTimeoutAware.beforeExecute()` 调用

**超时处理**: 如果任务执行时间超过 `runTimeout`，会触发超时处理：
- 记录执行超时计数
- 如果 `tryInterrupt = true`，尝试中断执行任务的线程
- 触发告警
- 记录日志

**使用场景**: 任务开始执行时

---

#### cancelRunTimeoutTask(Runnable r)

**作用**: 取消任务执行超时监控

**实现**:
```java
public void cancelRunTimeoutTask(Runnable r) {
    Optional.ofNullable(runTimeoutMap.remove(r))
            .map(SoftReference::get)
            .ifPresent(Timeout::cancel);
}
```

**说明**:
- 从 `runTimeoutMap` 中移除任务
- 获取超时对象并取消定时任务
- 由 `TaskTimeoutAware.afterExecute()` 调用

**使用场景**: 任务执行完成时

---

### 4. 拒绝统计

#### incRejectCount(int count)

**作用**: 增加拒绝任务计数

**实现**:
```java
public void incRejectCount(int count) {
    rejectCount.add(count);
}
```

**说明**:
- 使用 `LongAdder` 实现高并发场景下的计数
- 由 `TaskRejectAware.beforeReject()` 调用

**使用场景**: 任务被拒绝时

---

#### getRejectedTaskCount()

**作用**: 获取拒绝任务总数

**实现**:
```java
public long getRejectedTaskCount() {
    return rejectCount.sum();
}
```

**说明**:
- 返回累计的拒绝任务数量
- 用于监控和告警

---

### 5. 超时统计

#### incRunTimeoutCount(int count)

**作用**: 增加执行超时任务计数

**说明**:
- 记录执行超时的任务数量
- 由 `RunTimeoutTimerTask` 触发超时时调用

---

#### incQueueTimeoutCount(int count)

**作用**: 增加队列等待超时任务计数

**说明**:
- 记录队列等待超时的任务数量
- 由 `QueueTimeoutTimerTask` 触发超时时调用

---

## 创建和使用

### 创建方式

```java
public static ThreadPoolStatProvider of(ExecutorWrapper executorWrapper) {
    ThreadPoolStatProvider provider = new ThreadPoolStatProvider(executorWrapper);
    if (executorWrapper.isDtpExecutor()) {
        DtpExecutor dtpExecutor = (DtpExecutor) executorWrapper.getExecutor();
        // 从 DtpExecutor 中获取超时配置
        provider.setRunTimeout(dtpExecutor.getRunTimeout());
        provider.setQueueTimeout(dtpExecutor.getQueueTimeout());
        provider.setTryInterrupt(dtpExecutor.isTryInterrupt());
    }
    return provider;
}
```

**说明**:
- 通过静态工厂方法创建
- 如果是 `DtpExecutor`，会从执行器中获取超时配置
- 由 `ExecutorWrapper` 创建并持有

### 使用方式

`ThreadPoolStatProvider` 由 `ExecutorWrapper` 持有，通过以下方式使用：

1. **感知器使用**: 各种 `ExecutorAware` 实现通过 `TaskStatAware` 获取 `ThreadPoolStatProvider`
2. **统计查询**: 通过 `ExecutorWrapper.getThreadPoolStatProvider()` 获取统计信息
3. **指标转换**: 通过 `ExecutorConverter.toMetrics()` 将统计信息转换为 `ThreadPoolStats`

## 数据流向

```
ThreadPoolStatProvider
    │
    ├─> 性能统计
    │   ├─> startTask() → stopWatchMap
    │   └─> completeTask() → PerformanceProvider → MMAPCounter
    │       └─> 定期采集 → PerformanceSnapshot (TPS, RT, TPXX)
    │
    ├─> 队列超时监控
    │   ├─> startQueueTimeoutTask() → HashedWheelTimer
    │   └─> cancelQueueTimeoutTask() → 取消定时任务
    │
    ├─> 执行超时监控
    │   ├─> startRunTimeoutTask() → HashedWheelTimer
    │   └─> cancelRunTimeoutTask() → 取消定时任务
    │
    └─> 统计计数
        ├─> rejectCount → 拒绝任务数
        ├─> runTimeoutCount → 执行超时数
        └─> queueTimeoutCount → 队列等待超时数
```

## 设计特点

### 1. 使用 SoftReference

超时映射使用 `SoftReference<Timeout>` 存储超时对象：

```java
private final Map<Runnable, SoftReference<Timeout>> runTimeoutMap;
```

**优势**:
- 避免内存泄漏：任务完成后，如果内存紧张，GC 可以回收超时对象
- 自动清理：不需要手动清理已完成任务的超时对象

### 2. 使用 LongAdder

统计计数使用 `LongAdder` 而不是 `AtomicLong`：

```java
private final LongAdder rejectCount = new LongAdder();
```

**优势**:
- 高并发性能更好：`LongAdder` 使用分段锁，在高并发场景下性能优于 `AtomicLong`
- 适合统计场景：统计场景下读多写少，`LongAdder` 更合适

### 3. 使用 ConcurrentHashMap

所有映射都使用 `ConcurrentHashMap`：

**优势**:
- 线程安全：支持并发读写
- 高性能：分段锁机制，性能优于 `Hashtable`

### 4. 条件检查

超时监控方法都有条件检查：

```java
if (queueTimeout <= 0) {
    return;  // 未配置，不监控
}
```

**优势**:
- 避免不必要的开销：如果未配置超时，直接返回
- 性能优化：减少不必要的对象创建和定时任务

## 与感知器的关系

`ThreadPoolStatProvider` 是感知器功能的底层支撑：

| 感知器 | 使用的功能 |
|--------|-----------|
| **PerformanceMonitorAware** | `startTask()`, `completeTask()` |
| **TaskTimeoutAware** | `startQueueTimeoutTask()`, `cancelQueueTimeoutTask()`, `startRunTimeoutTask()`, `cancelRunTimeoutTask()` |
| **TaskRejectAware** | `incRejectCount()` |

## 总结

`ThreadPoolStatProvider` 是框架中任务统计和监控的核心组件，它：

1. **统一管理**: 为每个线程池提供统一的统计功能
2. **性能统计**: 记录任务响应时间，计算性能指标
3. **超时监控**: 监控任务队列等待和执行超时
4. **拒绝统计**: 统计被拒绝的任务数量
5. **高并发支持**: 使用 `LongAdder` 和 `ConcurrentHashMap` 支持高并发场景
6. **内存优化**: 使用 `SoftReference` 避免内存泄漏

它是连接感知器和底层统计功能的桥梁，为框架的监控、告警、性能分析等功能提供数据支撑。

