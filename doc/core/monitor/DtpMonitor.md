# DtpMonitor

## 概述

`DtpMonitor` 是监控管理器，负责定时收集线程池指标和检查告警。它使用定时任务定期执行监控逻辑。

## 核心作用

1. **定时监控**: 定时收集线程池指标
2. **告警检查**: 定时检查告警条件
3. **事件发布**: 发布监控和告警检查事件

## 核心属性

```java
private static ScheduledExecutorService monitorExecutor;  // 监控执行器
private final DtpProperties dtpProperties;               // 配置属性
private ScheduledFuture<?> monitorFuture;                 // 监控任务
private int monitorInterval;                               // 监控间隔
```

## 核心方法

### onContextRefreshedEvent(CustomContextRefreshedEvent event)

**作用**: 处理上下文刷新事件，启动监控任务

**实现**:
```java
@Subscribe
public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
    // 如果监控间隔相同，不处理
    if (monitorInterval == dtpProperties.getMonitorInterval()) {
        return;
    }
    // 取消旧的监控任务
    if (monitorFuture != null) {
        monitorFuture.cancel(true);
    }
    // 兼容 Spring Boot DevTools 重启场景
    if (monitorExecutor == null || monitorExecutor.isShutdown() || monitorExecutor.isTerminated()) {
        monitorExecutor = ThreadPoolCreator.newScheduledThreadPool("dtp-monitor", 1);
    }
    monitorInterval = dtpProperties.getMonitorInterval();
    monitorFuture = monitorExecutor.scheduleWithFixedDelay(this::run, 0, monitorInterval, TimeUnit.SECONDS);
}
```

**说明**:
- 如果监控间隔变化，重新启动监控任务
- 兼容 Spring Boot DevTools 重启场景

---

### run()

**作用**: 执行监控逻辑

**实现**:
```java
private void run() {
    Set<String> executorNames = DtpRegistry.getAllExecutorNames();
    try {
        checkAlarm(executorNames);      // 检查告警
        collectMetrics(executorNames);  // 收集指标
    } catch (Exception e) {
        log.error("DynamicTp monitor, run error", e);
    }
}
```

---

### checkAlarm(Set<String> executorNames)

**作用**: 检查告警

**实现**:
```java
private void checkAlarm(Set<String> executorNames) {
    executorNames.forEach(name -> {
        ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper(name);
        AlarmManager.checkAndTryAlarmAsync(wrapper, SCHEDULE_NOTIFY_ITEMS);
    });
    publishAlarmCheckEvent();
}
```

**说明**: 对每个执行器检查告警条件

---

### collectMetrics(Set<String> executorNames)

**作用**: 收集指标

**实现**:
```java
private void collectMetrics(Set<String> executorNames) {
    if (!dtpProperties.isEnabledCollect()) {
        return;
    }
    executorNames.forEach(x -> {
        ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper(x);
        doCollect(ExecutorConverter.toMetrics(wrapper));
    });
    publishCollectEvent();
}
```

**说明**: 
- 如果未启用收集，直接返回
- 对每个执行器收集指标

---

### doCollect(ThreadPoolStats threadPoolStats)

**作用**: 执行指标收集

**实现**:
```java
private void doCollect(ThreadPoolStats threadPoolStats) {
    try {
        CollectorHandler.getInstance().collect(threadPoolStats, dtpProperties.getCollectorTypes());
    } catch (Exception e) {
        log.error("DynamicTp monitor, metrics collect error.", e);
    }
}
```

---

### publishCollectEvent() / publishAlarmCheckEvent()

**作用**: 发布事件

**实现**:
```java
private void publishCollectEvent() {
    CollectEvent event = new CollectEvent(this, dtpProperties);
    EventBusManager.post(event);
}

private void publishAlarmCheckEvent() {
    AlarmCheckEvent event = new AlarmCheckEvent(this, dtpProperties);
    EventBusManager.post(event);
}
```

---

### destroy()

**作用**: 销毁监控器

**实现**:
```java
public static void destroy() {
    monitorExecutor.shutdownNow();
}
```

## 使用场景

### 1. 自动监控

框架启动后自动开始监控：

```java
DtpMonitor monitor = new DtpMonitor(dtpProperties);
// 框架会自动启动监控任务
```

### 2. 配置监控间隔

```yaml
spring:
  dynamic:
    tp:
      monitor-interval: 5  # 监控间隔（秒）
```

## 设计特点

### 1. 定时任务

使用 `ScheduledExecutorService` 定时执行监控逻辑。

### 2. 事件驱动

监控和告警检查后发布事件，其他组件可以监听。

### 3. 异常隔离

监控异常不会影响其他功能。

## 注意事项

1. **监控间隔**: 通过配置设置监控间隔
2. **资源消耗**: 监控会消耗一定的系统资源
3. **异常处理**: 监控异常会被捕获，不会影响框架运行

