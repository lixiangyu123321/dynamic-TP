# ScheduledDtpExecutor

## 概述

`ScheduledDtpExecutor` 是调度线程池执行器，继承自 `DtpExecutor`，实现了 `ScheduledExecutorService` 接口。它支持定时任务和周期性任务的执行。

## 核心作用

1. **定时任务**: 支持延迟执行任务
2. **周期性任务**: 支持固定延迟和固定频率的周期性任务
3. **任务增强**: 支持框架的任务增强功能

## 核心属性

```java
private final ScheduledThreadPoolExecutorProxy delegate;  // 代理执行器
```

## 工作原理

`ScheduledDtpExecutor` 使用委托模式，将调度功能委托给 `ScheduledThreadPoolExecutorProxy`：

```java
public ScheduledDtpExecutor(...) {
    super(...);
    // JDK8 的 bug 修复
    if (JreEnum.JAVA_8.isCurrentVersion()) {
        corePoolSize = corePoolSize == 0 ? 1 : corePoolSize;
    }
    delegate = new ScheduledThreadPoolExecutorProxy(
        new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, handler));
    delegate.setTaskWrappers(getTaskWrappers());
}
```

**说明**: 
- 使用 `ScheduledThreadPoolExecutorProxy` 作为代理
- 代理支持任务增强和感知器
- 修复了 JDK8 的 bug（corePoolSize 为 0 时会导致死循环）

## 核心方法

### schedule(Runnable command, long delay, TimeUnit unit)

**作用**: 延迟执行任务

**实现**:
```java
@Override
public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(command, delay, unit);
}
```

---

### schedule(Callable<V> callable, long delay, TimeUnit unit)

**作用**: 延迟执行 Callable 任务

---

### scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)

**作用**: 固定延迟的周期性任务

**说明**: 上一次执行完成后，延迟指定时间再执行下一次

---

### scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)

**作用**: 固定频率的周期性任务

**说明**: 按固定频率执行，不考虑上一次执行时间

---

### execute(Runnable command)

**作用**: 立即执行任务

**实现**:
```java
@Override
public void execute(Runnable command) {
    schedule(command, 0, NANOSECONDS);  // 延迟 0 纳秒，即立即执行
}
```

## 使用场景

### 1. 延迟执行

```java
ScheduledDtpExecutor executor = new ScheduledDtpExecutor(10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(200), threadFactory, handler);

// 延迟 5 秒执行
executor.schedule(() -> {
    System.out.println("延迟执行");
}, 5, TimeUnit.SECONDS);
```

### 2. 固定延迟周期性任务

```java
// 每 10 秒执行一次（上一次执行完成后 10 秒）
executor.scheduleWithFixedDelay(() -> {
    System.out.println("周期性任务");
}, 0, 10, TimeUnit.SECONDS);
```

### 3. 固定频率周期性任务

```java
// 每 10 秒执行一次（固定频率）
executor.scheduleAtFixedRate(() -> {
    System.out.println("周期性任务");
}, 0, 10, TimeUnit.SECONDS);
```

## 设计特点

### 1. 委托模式

使用委托模式，将调度功能委托给 `ScheduledThreadPoolExecutorProxy`。

### 2. 任务增强

通过代理支持框架的任务增强功能（MDC、TTL、链路追踪等）。

### 3. JDK8 兼容

修复了 JDK8 的 bug（corePoolSize 为 0 时会导致死循环）。

## 注意事项

1. **核心线程数**: JDK8 下，corePoolSize 不能为 0
2. **最大线程数**: `ScheduledThreadPoolExecutor` 不支持动态调整最大线程数
3. **队列类型**: 使用 `DelayedWorkQueue`，不支持动态调整容量

