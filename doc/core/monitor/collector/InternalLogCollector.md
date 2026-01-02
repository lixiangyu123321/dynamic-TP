# InternalLogCollector

## 概述

`InternalLogCollector` 是内部日志收集器，继承自 `AbstractCollector`。它将线程池指标输出到应用日志中。

## 核心作用

1. **指标输出**: 将线程池指标输出到应用日志
2. **JSON 格式**: 以 JSON 格式输出指标
3. **内部日志**: 使用应用的标准日志系统

## 核心方法

### collect(ThreadPoolStats poolStats)

**作用**: 收集指标并输出到日志

**实现**:
```java
@Override
public void collect(ThreadPoolStats poolStats) {
    log.info("dynamic.tp metrics: {}", JsonUtil.toJson(poolStats));
}
```

**说明**: 将指标转换为 JSON 格式，输出到 INFO 级别日志

---

### type()

**作用**: 获取收集器类型

**返回**: "internal_logging"

## 使用场景

### 1. 配置收集器

```yaml
spring:
  dynamic:
    tp:
      collector-types: [internal_logging]
```

### 2. 查看指标

在应用日志中查看线程池指标：

```
INFO  - dynamic.tp metrics: {"poolName":"dtpExecutor1","corePoolSize":10,...}
```

## 设计特点

### 1. 简单实现

实现简单，直接将指标输出到日志。

### 2. JSON 格式

使用 JSON 格式，便于解析和处理。

### 3. 标准日志

使用应用的标准日志系统，无需额外配置。

## 注意事项

1. **日志级别**: 使用 INFO 级别，确保日志不会被过滤
2. **日志量**: 监控间隔较短时，日志量可能较大
3. **性能影响**: 日志输出会有一定的性能开销

