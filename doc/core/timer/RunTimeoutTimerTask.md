# RunTimeoutTimerTask

## 概述

`RunTimeoutTimerTask` 是执行超时定时任务，继承自 `AbstractTimeoutTimerTask`。它用于处理任务执行超时的情况。

## 核心作用

1. **执行超时处理**: 处理任务执行超时
2. **超时统计**: 增加执行超时计数
3. **告警触发**: 触发执行超时告警
4. **任务中断**: 可选地中断超时任务
5. **日志记录**: 记录详细的超时信息和堆栈跟踪

## 核心属性

```java
private final Thread thread;  // 执行任务的线程
```

## 核心方法

### RunTimeoutTimerTask(ExecutorWrapper executorWrapper, Runnable runnable, Thread thread)

**作用**: 构造方法

**说明**: 需要传入执行任务的线程，用于中断任务

---

### doRun()

**作用**: 执行超时处理逻辑

**实现**:
```java
@Override
protected void doRun() {
    val statProvider = executorWrapper.getThreadPoolStatProvider();
    ExecutorAdapter<?> executor = statProvider.getExecutorWrapper().getExecutor();
    val pair = getTaskNameAndTraceId();
    
    // 1. 增加执行超时计数
    statProvider.incRunTimeoutCount(1);
    
    // 2. 触发告警
    AlarmManager.tryAlarmAsync(executorWrapper, RUN_TIMEOUT, runnable);
    
    // 3. 记录详细日志（包含堆栈跟踪）
    String logMsg = CharSequenceUtil.format(
        "DynamicTp execute, run timeout, " +
        "tpName: {}, taskName: {}, traceId: {}, runTimeout: {}ms, " +
        "poolSize: {} (active: {}, core: {}, max: {}, largest: {}), " +
        "queueCapacity: {} (currSize: {}, remaining: {}), stackTrace: {}",
        // ... 详细信息
        traceToString(thread.getStackTrace())
    );
    log.warn(logMsg);
    
    // 4. 如果配置了 tryInterrupt，尝试中断任务
    if (statProvider.isTryInterrupt()) {
        thread.interrupt();
    }
}
```

**处理流程**:
1. 获取统计提供者和执行器
2. 获取任务名称和 traceId
3. 增加执行超时计数
4. 触发执行超时告警
5. 记录详细的超时日志（包含堆栈跟踪）
6. 如果配置了 `tryInterrupt`，尝试中断任务

---

### traceToString(StackTraceElement[] trace)

**作用**: 将堆栈跟踪转换为字符串

**实现**:
```java
public String traceToString(StackTraceElement[] trace) {
    StringBuilder builder = new StringBuilder(512);
    builder.append("\n");
    for (StackTraceElement traceElement : trace) {
        builder.append("\tat ").append(traceElement).append("\n");
    }
    return builder.toString();
}
```

## 日志内容

日志包含以下信息：
- 线程池名称
- 任务名称
- traceId
- 执行超时时间
- 线程池大小（当前、活跃、核心、最大、历史最大）
- 队列容量（当前大小、剩余容量）
- **堆栈跟踪**: 执行任务的线程的堆栈跟踪

## 使用场景

### 1. 执行超时监控

当任务执行时间超过配置的 `runTimeout` 时，会触发此任务。

### 2. 问题排查

通过详细的日志信息和堆栈跟踪快速定位问题。

### 3. 任务中断

如果配置了 `tryInterrupt: true`，会尝试中断超时任务。

### 4. 告警通知

执行超时时自动触发告警通知。

## 设计特点

### 1. 继承抽象类

继承 `AbstractTimeoutTimerTask`，复用基础功能。

### 2. 详细日志

记录详细的超时信息和堆栈跟踪，便于问题排查。

### 3. 任务中断

支持可选的任务中断功能。

### 4. 告警集成

集成告警功能，自动触发告警。

## 注意事项

1. **超时时间**: 由 `runTimeout` 配置决定
2. **任务中断**: 中断只是发送中断信号，任务需要检查中断状态
3. **堆栈跟踪**: 堆栈跟踪可能很长，注意日志大小
4. **告警频率**: 通过 `AlarmManager` 控制告警频率

