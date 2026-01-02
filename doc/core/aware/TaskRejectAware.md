# TaskRejectAware

## 概述

`TaskRejectAware` 是任务拒绝感知器，用于处理任务被拒绝的情况。它继承自 `TaskStatAware`，在任务被拒绝时触发告警和记录详细日志。

## 核心作用

1. **拒绝统计**: 统计被拒绝的任务数量
2. **拒绝告警**: 任务被拒绝时触发告警
3. **详细日志**: 记录任务拒绝时的详细信息

## 核心方法

### getOrder() / getName()

**顺序**: 3（`AwareTypeEnum.TASK_REJECT_AWARE`）

**名称**: "reject"

---

### beforeReject(Runnable runnable, Executor executor)

**作用**: 任务拒绝前的处理

**实现流程**:

1. **获取统计提供者**:
   ```java
   ThreadPoolStatProvider statProvider = statProviders.get(executor);
   if (Objects.isNull(statProvider)) {
       return;
   }
   ```

2. **增加拒绝计数**:
   ```java
   statProvider.incRejectCount(1);
   ```

3. **触发告警**:
   ```java
   AlarmManager.tryAlarmAsync(statProvider.getExecutorWrapper(), REJECT, runnable);
   ```

4. **记录详细日志**:
   ```java
   String logMsg = CharSequenceUtil.format(
       "DynamicTp execute, thread pool is exhausted, tpName: {}, traceId: {}, " +
       "poolSize: {} (active: {}, core: {}, max: {}, largest: {}), " +
       "task: {} (completed: {}), queueCapacity: {}, (currSize: {}, remaining: {}) ," +
       "executorStatus: (isShutdown: {}, isTerminated: {}, isTerminating: {})",
       // ... 详细信息
   );
   log.warn(logMsg);
   ```

**日志内容**:
- 线程池名称
- traceId（从 MDC 获取）
- 线程池大小（当前、活跃、核心、最大、历史最大）
- 任务统计（总数、已完成）
- 队列信息（容量、当前大小、剩余容量）
- 执行器状态（是否关闭、是否终止、是否正在终止）

## 工作流程

```
任务被拒绝
    │
    └─> beforeReject()
        │
        ├─> incRejectCount(1)        // 增加拒绝计数
        │
        ├─> tryAlarmAsync()          // 触发告警
        │   └─> 发送拒绝告警通知
        │
        └─> log.warn()               // 记录详细日志
            └─> 包含线程池的完整状态信息
```

## 使用场景

### 1. 拒绝监控

自动监控任务拒绝情况，触发告警：

```java
// 当任务被拒绝时，自动触发
// 1. 增加拒绝计数
// 2. 发送告警通知
// 3. 记录详细日志
```

### 2. 问题排查

通过详细日志快速定位问题：

```java
// 日志包含：
// - 线程池状态
// - 队列状态
// - 任务统计
// - traceId（用于追踪）
```

### 3. 告警通知

拒绝时自动发送告警通知：

```java
// 通过 AlarmManager 发送告警
// 支持多种通知渠道（钉钉、企微、飞书等）
```

## 配置说明

### 告警配置

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          notify-items:
            - type: reject
              enabled: true
              threshold: 1      # 拒绝次数阈值
              interval: 120     # 告警间隔（秒）
```

## 注意事项

1. **拒绝计数**: 每次拒绝都会增加计数
2. **告警频率**: 通过 `AlarmManager` 控制告警频率
3. **日志级别**: 使用 WARN 级别记录拒绝日志
4. **详细信息**: 日志包含线程池的完整状态信息

## 与其他组件的关系

```
TaskRejectAware
    │
    ├─> ThreadPoolStatProvider
    │   └─> 获取统计信息和执行器包装器
    │
    └─> AlarmManager
        └─> 发送拒绝告警通知
```

