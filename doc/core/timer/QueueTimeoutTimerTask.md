# QueueTimeoutTimerTask

## 概述

`QueueTimeoutTimerTask` 是队列超时定时任务，继承自 `AbstractTimeoutTimerTask`。它用于处理任务在队列中等待超时的情况。

## 核心作用

1. **队列超时处理**: 处理任务在队列中等待超时
2. **超时统计**: 增加队列超时计数
3. **告警触发**: 触发队列超时告警
4. **日志记录**: 记录详细的超时信息

## 核心方法

### doRun()

**作用**: 执行超时处理逻辑

**实现**:
```java
@Override
protected void doRun() {
    val statProvider = executorWrapper.getThreadPoolStatProvider();
    ExecutorAdapter<?> executor = statProvider.getExecutorWrapper().getExecutor();
    val pair = getTaskNameAndTraceId();
    
    // 1. 增加队列超时计数
    statProvider.incQueueTimeoutCount(1);
    
    // 2. 触发告警
    AlarmManager.tryAlarmAsync(executorWrapper, QUEUE_TIMEOUT, runnable);
    
    // 3. 记录详细日志
    String logMsg = CharSequenceUtil.format(
        "DynamicTp execute, queue timeout, " +
        "tpName: {}, taskName: {}, traceId: {}, queueTimeout: {}ms, " +
        "poolSize: {} (active: {}, core: {}, max: {}, largest: {}), " +
        "queueCapacity: {} (currSize: {}, remaining: {})",
        // ... 详细信息
    );
    log.warn(logMsg);
}
```

**处理流程**:
1. 获取统计提供者和执行器
2. 获取任务名称和 traceId
3. 增加队列超时计数
4. 触发队列超时告警
5. 记录详细的超时日志

## 日志内容

日志包含以下信息：
- 线程池名称
- 任务名称
- traceId
- 队列超时时间
- 线程池大小（当前、活跃、核心、最大、历史最大）
- 队列容量（当前大小、剩余容量）

## 使用场景

### 1. 队列超时监控

当任务在队列中等待时间超过配置的 `queueTimeout` 时，会触发此任务。

### 2. 问题排查

通过详细的日志信息快速定位问题。

### 3. 告警通知

队列超时时自动触发告警通知。

## 设计特点

### 1. 继承抽象类

继承 `AbstractTimeoutTimerTask`，复用基础功能。

### 2. 详细日志

记录详细的超时信息，便于问题排查。

### 3. 告警集成

集成告警功能，自动触发告警。

## 注意事项

1. **超时时间**: 由 `queueTimeout` 配置决定
2. **任务信息**: 只有 `DtpRunnable` 类型的任务才能获取名称和 traceId
3. **告警频率**: 通过 `AlarmManager` 控制告警频率

