# LogCollector

## 概述

`LogCollector` 是日志收集器，继承自 `AbstractCollector`。它将线程池指标输出到专门的监控日志文件中。

## 核心作用

1. **指标输出**: 将线程池指标输出到监控日志
2. **JSON 格式**: 以 JSON 格式输出指标
3. **专用日志**: 使用专门的监控日志系统

## 核心方法

### collect(ThreadPoolStats threadPoolStats)

**作用**: 收集指标并输出到监控日志

**实现**:
```java
@Override
public void collect(ThreadPoolStats threadPoolStats) {
    String metrics = JsonUtil.toJson(threadPoolStats);
    if (LogHelper.getMonitorLogger() == null) {
        log.error("Cannot find monitor logger...");
        return;
    }
    LogHelper.getMonitorLogger().info("{}", metrics);
}
```

**说明**: 
- 将指标转换为 JSON 格式
- 检查监控日志器是否存在
- 输出到监控日志

---

### type()

**作用**: 获取收集器类型

**返回**: "logging"

## 使用场景

### 1. 配置收集器

```yaml
spring:
  dynamic:
    tp:
      collector-types: [logging]
```

### 2. 日志采集

监控日志可以被日志采集系统（如 ELK、Loki）采集和分析。

### 3. 指标分析

通过分析监控日志，了解线程池的运行状况。

## 设计特点

### 1. 专用日志

使用专门的监控日志系统，与业务日志分离。

### 2. JSON 格式

使用 JSON 格式，便于日志采集和分析。

### 3. 日志检查

检查监控日志器是否存在，避免空指针异常。

## 注意事项

1. **日志配置**: 需要配置监控日志系统
2. **日志量**: 监控间隔较短时，日志量可能较大
3. **日志采集**: 需要配置日志采集系统来收集监控日志

