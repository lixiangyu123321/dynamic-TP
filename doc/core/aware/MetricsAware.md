# MetricsAware

## 概述

`MetricsAware` 是指标感知器接口，继承自 `DtpAware`。它提供了获取线程池指标的功能，用于暴露线程池的监控数据。

## 核心作用

1. **指标提供**: 提供线程池的指标数据
2. **监控支持**: 支持监控系统获取线程池指标
3. **数据查询**: 提供指标查询接口

## 接口定义

```java
public interface MetricsAware extends DtpAware {
    default ThreadPoolStats getPoolStats() {
        return null;
    }
    
    default List<ThreadPoolStats> getMultiPoolStats() {
        return Collections.emptyList();
    }
}
```

## 核心方法

### getPoolStats()

**作用**: 获取单个线程池的指标

**返回**: `ThreadPoolStats` 对象，包含线程池的各种指标

**默认实现**: 返回 `null`

---

### getMultiPoolStats()

**作用**: 获取多个线程池的指标

**返回**: `ThreadPoolStats` 列表

**默认实现**: 返回空列表

## 设计说明

根据代码注释：

> 这里弱化了感知属性，只是提供了指标的查询的功能

**说明**: 
- `MetricsAware` 并不是真正的"感知器"，它不感知事件
- 它只是一个数据提供接口，用于查询指标
- 与 `ExecutorAware` 不同，它不响应事件，只是提供数据

## 使用场景

### 1. 适配器实现

适配器可以实现 `MetricsAware` 接口来提供指标：

```java
public class MyAdapter implements DtpAdapter, MetricsAware {
    @Override
    public ThreadPoolStats getPoolStats() {
        // 返回线程池指标
        return ExecutorConverter.toMetrics(executorWrapper);
    }
    
    @Override
    public List<ThreadPoolStats> getMultiPoolStats() {
        // 返回多个线程池指标
        return executorWrappers.stream()
            .map(ExecutorConverter::toMetrics)
            .collect(Collectors.toList());
    }
}
```

### 2. 监控端点

监控端点可以使用 `MetricsAware` 获取指标：

```java
@RestController
public class MetricsEndpoint {
    @Autowired
    private MetricsAware metricsAware;
    
    @GetMapping("/metrics")
    public List<ThreadPoolStats> getMetrics() {
        return metricsAware.getMultiPoolStats();
    }
}
```

## 实现类

### DtpAdapter

`DtpAdapter` 接口继承了 `MetricsAware`：

```java
public interface DtpAdapter extends MetricsAware {
    // ...
}
```

所有适配器都需要实现 `MetricsAware` 接口。

## 设计特点

### 1. 数据提供模式

采用数据提供模式，而不是事件驱动模式。

### 2. 默认实现

提供默认实现，子类可以选择性实现。

### 3. 灵活查询

支持单个和多个线程池的指标查询。

## 注意事项

1. **非感知器**: `MetricsAware` 不是真正的感知器，只是数据提供接口
2. **按需查询**: 指标是按需查询的，不是事件驱动的
3. **性能考虑**: 指标查询可能涉及计算，需要注意性能

