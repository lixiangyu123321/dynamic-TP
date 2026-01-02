# PerformanceMonitorAware

## 概述

`PerformanceMonitorAware` 是性能监控感知器，继承自 `TaskStatAware`。它负责监控线程池的性能指标，包括 TPS（每秒事务数）、RT（响应时间）、TP 分位数等。

## 核心作用

1. **性能监控**: 监控任务的执行时间和数量
2. **指标统计**: 统计 TPS、RT、TP 分位数等指标
3. **数据提供**: 为监控系统提供性能数据

## 执行顺序

- **顺序**: 1（`AwareTypeEnum.PERFORMANCE_MONITOR_AWARE`）
- **名称**: "monitor"

## 核心方法

### execute(Executor executor, Runnable r)

**作用**: 任务提交时，开始记录任务

**实现**:
```java
@Override
public void execute(Executor executor, Runnable r) {
    Optional.ofNullable(statProviders.get(executor))
        .ifPresent(p -> p.startTask(r));
}
```

**说明**: 任务提交时，通知统计提供者开始记录任务

---

### afterExecute(Executor executor, Runnable r, Throwable t)

**作用**: 任务执行后，完成记录并统计响应时间

**实现**:
```java
@Override
public void afterExecute(Executor executor, Runnable r, Throwable t) {
    Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> {
        long rt = p.completeTask(r);
        // rt 是任务的响应时间（毫秒）
    });
}
```

**说明**: 任务执行完成后，通知统计提供者完成任务，并记录响应时间

## 工作流程

```
任务提交
    │
    └─> execute()
        └─> startTask()
            └─> 记录任务开始时间

任务执行
    │
    └─> 任务执行中...

任务完成
    │
    └─> afterExecute()
        └─> completeTask()
            ├─> 计算响应时间
            └─> 更新性能指标
```

## 性能指标

通过 `PerformanceProvider` 提供以下性能指标：

- **TPS**: 每秒事务数
- **RT**: 响应时间（平均、最大、最小）
- **TP 分位数**: TP50、TP75、TP90、TP95、TP99、TP999

## 使用场景

### 1. 性能监控

自动监控线程池的性能指标，无需手动配置。

### 2. 性能分析

通过性能指标分析线程池的性能瓶颈。

### 3. 告警触发

基于性能指标触发告警。

## 设计特点

### 1. 自动监控

框架自动集成，无需手动配置。

### 2. 低开销

使用移动平均算法，减少内存占用。

### 3. 快照机制

通过快照机制获取性能数据，保证数据准确性。

## 注意事项

1. **性能影响**: 性能监控会有一定的性能开销
2. **数据精度**: 使用移动平均算法，数据精度可能略有损失
3. **快照重置**: 获取快照后会重置计数器

## 相关文档

- 详细注释文档: [PerformanceMonitorAware-详细注释.md](../PerformanceMonitorAware-详细注释.md)

