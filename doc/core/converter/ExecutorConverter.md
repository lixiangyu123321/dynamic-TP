# ExecutorConverter

## 概述

`ExecutorConverter` 是执行器转换器，提供将 `ExecutorWrapper` 转换为配置对象和指标对象的功能。

## 核心作用

1. **配置转换**: 将 `ExecutorWrapper` 转换为 `TpMainFields`（配置对象）
2. **指标转换**: 将 `ExecutorWrapper` 转换为 `ThreadPoolStats`（指标对象）

## 核心方法

### toMainFields(ExecutorWrapper executorWrapper)

**作用**: 转换为配置对象

**实现**:
```java
public static TpMainFields toMainFields(ExecutorWrapper executorWrapper) {
    TpMainFields mainFields = new TpMainFields();
    mainFields.setThreadPoolName(executorWrapper.getThreadPoolName());
    val executor = executorWrapper.getExecutor();
    mainFields.setCorePoolSize(executor.getCorePoolSize());
    mainFields.setMaxPoolSize(executor.getMaximumPoolSize());
    mainFields.setKeepAliveTime(executor.getKeepAliveTime(TimeUnit.SECONDS));
    mainFields.setQueueType(executor.getQueueType());
    mainFields.setQueueCapacity(executor.getQueueCapacity());
    mainFields.setAllowCoreThreadTimeOut(executor.allowsCoreThreadTimeOut());
    mainFields.setRejectType(executor.getRejectHandlerType());
    return mainFields;
}
```

**转换字段**:
- 线程池名称
- 核心线程数
- 最大线程数
- 线程存活时间
- 队列类型
- 队列容量
- 是否允许核心线程超时
- 拒绝策略类型

---

### toMetrics(ExecutorWrapper wrapper)

**作用**: 转换为指标对象

**实现**:
```java
public static ThreadPoolStats toMetrics(ExecutorWrapper wrapper) {
    ExecutorAdapter<?> executor = wrapper.getExecutor();
    ThreadPoolStatProvider provider = wrapper.getThreadPoolStatProvider();
    PerformanceProvider performanceProvider = provider.getPerformanceProvider();
    val performanceSnapshot = performanceProvider.getSnapshotAndReset();
    
    ThreadPoolStats poolStats = convertCommon(executor);
    poolStats.setPoolName(wrapper.getThreadPoolName());
    poolStats.setPoolAliasName(wrapper.getThreadPoolAliasName());
    poolStats.setRunTimeoutCount(provider.getRunTimeoutCount());
    poolStats.setQueueTimeoutCount(provider.getQueueTimeoutCount());
    poolStats.setRejectCount(provider.getRejectedTaskCount());
    poolStats.setDynamic(executor instanceof DtpExecutor);
    
    // 性能指标
    poolStats.setTps(performanceSnapshot.getTps());
    poolStats.setAvg(performanceSnapshot.getAvg());
    poolStats.setMaxRt(performanceSnapshot.getMaxRt());
    poolStats.setMinRt(performanceSnapshot.getMinRt());
    poolStats.setTp50(performanceSnapshot.getTp50());
    poolStats.setTp75(performanceSnapshot.getTp75());
    poolStats.setTp90(performanceSnapshot.getTp90());
    poolStats.setTp95(performanceSnapshot.getTp95());
    poolStats.setTp99(performanceSnapshot.getTp99());
    poolStats.setTp999(performanceSnapshot.getTp999());
    return poolStats;
}
```

**转换内容**:
- 基础指标（线程数、队列大小等）
- 超时统计（执行超时、队列超时）
- 拒绝统计
- 性能指标（TPS、RT、TP 分位数）

---

### convertCommon(ExecutorAdapter<?> executor)

**作用**: 转换基础指标

**实现**:
```java
private static ThreadPoolStats convertCommon(ExecutorAdapter<?> executor) {
    ThreadPoolStats poolStats = new ThreadPoolStats();
    poolStats.setCorePoolSize(executor.getCorePoolSize());
    poolStats.setMaximumPoolSize(executor.getMaximumPoolSize());
    poolStats.setPoolSize(executor.getPoolSize());
    poolStats.setActiveCount(executor.getActiveCount());
    poolStats.setLargestPoolSize(executor.getLargestPoolSize());
    poolStats.setQueueType(executor.getQueueType());
    poolStats.setQueueCapacity(executor.getQueueCapacity());
    poolStats.setQueueSize(executor.getQueueSize());
    poolStats.setQueueRemainingCapacity(executor.getQueueRemainingCapacity());
    poolStats.setTaskCount(executor.getTaskCount());
    poolStats.setCompletedTaskCount(executor.getCompletedTaskCount());
    poolStats.setWaitTaskCount(executor.getQueueSize());
    poolStats.setRejectHandlerName(executor.getRejectHandlerType());
    poolStats.setKeepAliveTime(executor.getKeepAliveTime(TimeUnit.MILLISECONDS));
    return poolStats;
}
```

## 使用场景

### 1. 配置比较

在配置刷新时，使用 `toMainFields()` 比较配置变化：

```java
TpMainFields oldFields = ExecutorConverter.toMainFields(executorWrapper);
// 刷新配置
TpMainFields newFields = ExecutorConverter.toMainFields(executorWrapper);
// 比较变化
```

### 2. 指标收集

在监控时，使用 `toMetrics()` 收集指标：

```java
ThreadPoolStats stats = ExecutorConverter.toMetrics(executorWrapper);
// 上报指标
```

## 设计特点

### 1. 静态方法

所有方法都是静态方法，无需实例化。

### 2. 数据快照

`toMetrics()` 会获取性能快照并重置，保证数据准确性。

### 3. 统一转换

提供统一的转换接口，便于使用。

## 注意事项

1. **性能快照**: `toMetrics()` 会重置性能快照
2. **线程安全**: 转换方法需要保证线程安全
3. **数据准确性**: 转换时获取的是当前时刻的快照

