# MetricsCollector

## 概述

`MetricsCollector` 是指标收集器接口，定义了指标收集的标准方法。它用于将线程池指标收集到不同的目标（日志、JMX、Micrometer 等）。

## 核心作用

1. **指标收集**: 收集线程池指标
2. **类型标识**: 标识收集器类型
3. **类型支持**: 检查是否支持指定的收集器类型

## 接口定义

```java
public interface MetricsCollector {
    void collect(ThreadPoolStats poolStats);  // 收集指标
    String type();                             // 获取收集器类型
    boolean support(String type);              // 检查是否支持类型
}
```

## 核心方法

### collect(ThreadPoolStats poolStats)

**作用**: 收集线程池指标

**参数**: `ThreadPoolStats` 对象，包含线程池的各种指标

**说明**: 将指标收集到目标系统（日志、JMX、Micrometer 等）

---

### type()

**作用**: 获取收集器类型

**返回**: 收集器类型名称（如 "log"、"jmx"、"micrometer" 等）

**说明**: 用于识别和选择收集器

---

### support(String type)

**作用**: 检查是否支持指定的收集器类型

**参数**: 收集器类型名称

**返回**: `true` 表示支持，`false` 表示不支持

**说明**: 用于判断收集器是否支持指定的类型

## 内置实现

框架提供了以下内置实现：

1. **LogCollector**: 日志收集器
2. **InternalLogCollector**: 内部日志收集器
3. **MicroMeterCollector**: Micrometer 收集器
4. **JMXCollector**: JMX 收集器

## 使用场景

### 1. 实现自定义收集器

```java
public class CustomCollector extends AbstractCollector {
    @Override
    public String type() {
        return "custom";
    }
    
    @Override
    public void collect(ThreadPoolStats poolStats) {
        // 收集指标到自定义系统
        sendToCustomSystem(poolStats);
    }
}
```

### 2. 通过 SPI 注册

```java
// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.monitor.collector.MetricsCollector
// com.example.CustomCollector
```

### 3. 配置收集器

```yaml
spring:
  dynamic:
    tp:
      collector-types: [log, micrometer, jmx]
```

## 设计特点

### 1. 接口抽象

通过接口抽象，支持多种收集器实现。

### 2. SPI 扩展

支持通过 SPI 机制扩展收集器。

### 3. 类型识别

通过类型名称识别和选择收集器。

## 注意事项

1. **类型唯一性**: 收集器类型应该唯一
2. **异常处理**: 收集器中的异常需要自行处理
3. **性能影响**: 收集器会在监控线程中执行，需要注意性能

