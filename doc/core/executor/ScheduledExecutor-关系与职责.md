# ScheduledFuture、ScheduledExecutorService 与 ScheduledThreadPoolExecutor 的关系与职责

## 概述

本文档详细说明 Java 并发包中三个核心类/接口的关系和职责：
- `ScheduledFuture<V>` - 调度任务的 Future 接口
- `ScheduledExecutorService` - 调度执行器服务接口
- `ScheduledThreadPoolExecutor` - 调度线程池执行器实现类

## 类图关系

```
┌─────────────────────────────────┐
│      ScheduledFuture<V>          │
│  (extends Delayed, Future<V>)    │
└─────────────────────────────────┘
              ▲
              │
              │ 返回
              │
┌─────────────────────────────────┐
│   ScheduledExecutorService      │
│  (extends ExecutorService)      │
└─────────────────────────────────┘
              ▲
              │
              │ 实现
              │
┌─────────────────────────────────┐
│ ScheduledThreadPoolExecutor     │
│ (extends ThreadPoolExecutor)    │
└─────────────────────────────────┘
```

## 详细说明

### 1. ScheduledFuture<V>

#### 定义

```java
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}
```

#### 核心职责

1. **继承 Delayed 接口**: 提供延迟时间计算功能
   - `getDelay(TimeUnit unit)`: 获取剩余延迟时间
   - 用于延迟队列（DelayQueue）的排序和调度

2. **继承 Future<V> 接口**: 提供异步任务结果获取功能
   - `get()`: 获取任务执行结果（阻塞）
   - `get(long timeout, TimeUnit unit)`: 带超时的获取结果
   - `cancel(boolean mayInterruptIfRunning)`: 取消任务
   - `isCancelled()`: 判断是否已取消
   - `isDone()`: 判断是否已完成

#### 主要特点

- **延迟执行**: 任务可以在指定延迟后执行
- **可取消**: 支持取消尚未执行或正在执行的任务
- **结果获取**: 可以获取任务执行的结果（如果是 Callable）

#### 使用示例

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

// 延迟执行任务，返回 ScheduledFuture
ScheduledFuture<?> future = executor.schedule(() -> {
    System.out.println("延迟执行");
}, 5, TimeUnit.SECONDS);

// 获取剩余延迟时间
long delay = future.getDelay(TimeUnit.SECONDS);

// 取消任务
future.cancel(true);

// 等待任务完成
future.get();
```

---

### 2. ScheduledExecutorService

#### 定义

```java
public interface ScheduledExecutorService extends ExecutorService {
    // 调度方法
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
```

#### 核心职责

1. **延迟执行任务**: 在指定延迟后执行一次任务
2. **周期性任务**: 支持固定频率和固定延迟的周期性任务
3. **任务管理**: 继承 `ExecutorService` 的所有功能（提交、执行、关闭等）

#### 核心方法

##### schedule(Runnable command, long delay, TimeUnit unit)

**作用**: 延迟执行一次任务

**参数**:
- `command`: 要执行的任务
- `delay`: 延迟时间
- `unit`: 时间单位

**返回值**: `ScheduledFuture<?>` - 可用于取消任务或等待完成

**示例**:
```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
ScheduledFuture<?> future = executor.schedule(() -> {
    System.out.println("5秒后执行");
}, 5, TimeUnit.SECONDS);
```

##### schedule(Callable<V> callable, long delay, TimeUnit unit)

**作用**: 延迟执行 Callable 任务，可获取返回值

**返回值**: `ScheduledFuture<V>` - 可获取任务执行结果

**示例**:
```java
ScheduledFuture<String> future = executor.schedule(() -> {
    return "执行结果";
}, 3, TimeUnit.SECONDS);

String result = future.get(); // 获取返回值
```

##### scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)

**作用**: 固定频率的周期性任务

**特点**:
- 从 `initialDelay` 后开始执行
- 之后每隔 `period` 时间执行一次
- **固定频率**: 无论任务执行时间多长，都会按固定频率调度

**执行时间线**:
```
初始延迟: initialDelay
第1次: initialDelay
第2次: initialDelay + period
第3次: initialDelay + 2*period
...
```

**注意**: 如果任务执行时间超过 `period`，下一次任务会立即执行（不会等待）

**示例**:
```java
// 初始延迟1秒，之后每2秒执行一次
executor.scheduleAtFixedRate(() -> {
    System.out.println("固定频率执行");
}, 1, 2, TimeUnit.SECONDS);
```

##### scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)

**作用**: 固定延迟的周期性任务

**特点**:
- 从 `initialDelay` 后开始执行
- 上一次任务**完成后**，延迟 `delay` 时间再执行下一次
- **固定延迟**: 保证任务之间有固定的时间间隔

**执行时间线**:
```
初始延迟: initialDelay
第1次: initialDelay
第1次完成: initialDelay + task1Time
第2次: initialDelay + task1Time + delay
第2次完成: initialDelay + task1Time + delay + task2Time
第3次: initialDelay + task1Time + delay + task2Time + delay
...
```

**示例**:
```java
// 初始延迟1秒，每次任务完成后延迟2秒再执行下一次
executor.scheduleWithFixedDelay(() -> {
    System.out.println("固定延迟执行");
}, 1, 2, TimeUnit.SECONDS);
```

#### 方法对比

| 方法 | 执行次数 | 执行时机 | 适用场景 |
|------|---------|---------|---------|
| `schedule()` | 1次 | 延迟后执行 | 延迟执行、超时处理 |
| `scheduleAtFixedRate()` | 多次 | 固定频率 | 定时采集、定期报告 |
| `scheduleWithFixedDelay()` | 多次 | 固定延迟 | 轮询检查、定期清理 |

---

### 3. ScheduledThreadPoolExecutor

#### 定义

```java
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor 
    implements ScheduledExecutorService {
    // 实现类
}
```

#### 核心职责

1. **线程池管理**: 继承 `ThreadPoolExecutor` 的所有功能
2. **任务调度**: 实现 `ScheduledExecutorService` 接口的调度功能
3. **延迟队列**: 使用 `DelayedWorkQueue` 作为工作队列
4. **任务包装**: 将任务包装为 `ScheduledFutureTask` 进行调度

#### 核心特点

##### 1. 使用 DelayedWorkQueue

`ScheduledThreadPoolExecutor` 使用特殊的延迟队列 `DelayedWorkQueue`：
- 基于堆（Heap）实现
- 按延迟时间排序
- 自动处理延迟到期的任务

##### 2. 任务包装

所有调度任务都被包装为 `ScheduledFutureTask`：
- 实现 `ScheduledFuture` 接口
- 包含延迟时间、执行周期等信息
- 支持任务取消和结果获取

##### 3. 线程池特性

- **核心线程数**: 默认等于传入的 `corePoolSize`
- **最大线程数**: 通常等于核心线程数（调度任务不需要动态扩展）
- **队列**: 使用 `DelayedWorkQueue`（无界队列）
- **拒绝策略**: 支持自定义拒绝策略

#### 构造方法

```java
// 最常用
public ScheduledThreadPoolExecutor(int corePoolSize)

// 完整参数
public ScheduledThreadPoolExecutor(int corePoolSize, 
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler)
```

#### 在项目中的使用

在 DynamicTP 项目中，`ScheduledDtpExecutor` 使用 `ScheduledThreadPoolExecutor` 作为底层实现：

```44:66:core/src/main/java/org/dromara/dynamictp/core/executor/ScheduledDtpExecutor.java
public class ScheduledDtpExecutor extends DtpExecutor implements ScheduledExecutorService {

    /**
     * 可调度的任务线程池代理类
     */
    private final ScheduledThreadPoolExecutorProxy delegate;

    public ScheduledDtpExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                ThreadFactory threadFactory,
                                RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        // 如果是JDK8, corePoolSize为0时, ScheduledThreadPoolExecutor会导致"死循环", CPU100%
        // https://bugs.openjdk.org/browse/JDK-8065320
        if (JreEnum.JAVA_8.isCurrentVersion()) {
            corePoolSize = corePoolSize == 0 ? 1 : corePoolSize;
        }
        delegate = new ScheduledThreadPoolExecutorProxy(new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, handler));
        delegate.setTaskWrappers(getTaskWrappers());
    }
```

**说明**:
- 使用 `ScheduledThreadPoolExecutorProxy` 作为代理
- 修复了 JDK8 的 bug（corePoolSize 为 0 时会导致死循环）
- 支持任务增强和感知器功能

---

## 三者关系总结

### 1. 接口层次关系

```
Executor
  └── ExecutorService
      └── ScheduledExecutorService  (定义调度方法)
          └── ScheduledThreadPoolExecutor  (实现类)

Delayed
  └── ScheduledFuture<V>  (调度任务的 Future)
      └── Future<V>
```

### 2. 使用流程

```
1. 创建 ScheduledThreadPoolExecutor
   ↓
2. 调用 schedule/scheduleAtFixedRate/scheduleWithFixedDelay
   ↓
3. 返回 ScheduledFuture
   ↓
4. 使用 ScheduledFuture 取消任务或获取结果
```

### 3. 职责划分

| 类/接口 | 主要职责 |
|---------|---------|
| `ScheduledFuture<V>` | 表示调度任务的结果，提供延迟时间查询和任务取消功能 |
| `ScheduledExecutorService` | 定义调度任务的接口方法（延迟执行、周期性任务） |
| `ScheduledThreadPoolExecutor` | 实现调度功能，管理线程池，执行调度任务 |

---

## 使用示例

### 示例 1: 延迟执行

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

// 延迟5秒执行
ScheduledFuture<?> future = executor.schedule(() -> {
    System.out.println("延迟执行");
}, 5, TimeUnit.SECONDS);

// 等待完成
future.get();
```

### 示例 2: 固定频率任务

```java
// 初始延迟1秒，之后每2秒执行一次
ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
    System.out.println("固定频率执行: " + System.currentTimeMillis());
}, 1, 2, TimeUnit.SECONDS);

// 10秒后取消
Thread.sleep(10000);
future.cancel(true);
```

### 示例 3: 固定延迟任务

```java
// 初始延迟1秒，每次任务完成后延迟2秒再执行
ScheduledFuture<?> future = executor.scheduleWithFixedDelay(() -> {
    System.out.println("固定延迟执行: " + System.currentTimeMillis());
    Thread.sleep(1000); // 模拟任务执行时间
}, 1, 2, TimeUnit.SECONDS);
```

### 示例 4: 获取返回值

```java
ScheduledFuture<String> future = executor.schedule(() -> {
    return "任务执行结果";
}, 3, TimeUnit.SECONDS);

String result = future.get(); // 阻塞等待结果
System.out.println(result);
```

---

## 注意事项

### 1. 线程池大小

- **建议**: 根据任务类型和数量合理设置核心线程数
- **注意**: 调度任务通常不需要大量线程，核心线程数一般较小（1-10）

### 2. 任务执行时间

- **scheduleAtFixedRate**: 如果任务执行时间超过周期，下一次会立即执行
- **scheduleWithFixedDelay**: 保证任务之间有固定延迟，更适合执行时间不确定的任务

### 3. 任务取消

- 调用 `ScheduledFuture.cancel(true)` 可以取消任务
- 已执行的任务无法取消
- 周期性任务取消后不会再次执行

### 4. 异常处理

- 任务中的未捕获异常不会影响线程池
- 建议在任务内部处理异常
- 可以使用 `Future.get()` 捕获 `ExecutionException`

### 5. 资源关闭

```java
// 优雅关闭
executor.shutdown();
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
}
```

---

## 在 DynamicTP 项目中的应用

### ScheduledDtpExecutor

`ScheduledDtpExecutor` 是 DynamicTP 对 `ScheduledThreadPoolExecutor` 的封装：

1. **功能增强**: 支持任务包装、监控、告警等功能
2. **代理模式**: 使用 `ScheduledThreadPoolExecutorProxy` 作为代理
3. **Bug 修复**: 修复 JDK8 中 corePoolSize=0 时的死循环问题
4. **动态配置**: 支持运行时动态调整线程池参数

### 使用场景

- 定时任务执行
- 周期性数据采集
- 延迟任务处理
- 超时控制

---

## 总结

1. **ScheduledFuture<V>**: 调度任务的 Future，提供延迟查询和取消功能
2. **ScheduledExecutorService**: 定义调度任务的接口，包括延迟执行和周期性任务
3. **ScheduledThreadPoolExecutor**: 实现调度功能的线程池，使用延迟队列管理任务

三者协同工作，提供了完整的任务调度功能，适用于各种定时和周期性任务场景。

