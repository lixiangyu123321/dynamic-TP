# CollectorHandler

## 概述

`CollectorHandler` 是收集器处理器，负责管理指标收集器。它统一管理所有收集器，并提供指标收集功能。

## 核心作用

1. **收集器管理**: 统一管理所有指标收集器
2. **指标收集**: 将指标分发给指定的收集器
3. **SPI 扩展**: 支持通过 SPI 扩展收集器

## 核心属性

```java
private static final Map<String, MetricsCollector> COLLECTORS = Maps.newHashMap();  // 收集器映射
```

## 初始化

在构造方法中初始化收集器：

```java
private CollectorHandler() {
    // 1. 通过 SPI 加载收集器
    List<MetricsCollector> loadedCollectors = ExtensionServiceLoader.get(MetricsCollector.class);
    loadedCollectors.forEach(collector -> COLLECTORS.put(collector.type().toLowerCase(), collector));
    
    // 2. 添加内置收集器
    MetricsCollector microMeterCollector = new MicroMeterCollector();
    LogCollector logCollector = new LogCollector();
    InternalLogCollector internalLogCollector = new InternalLogCollector();
    JMXCollector jmxCollector = new JMXCollector();
    COLLECTORS.put(microMeterCollector.type(), microMeterCollector);
    COLLECTORS.put(logCollector.type(), logCollector);
    COLLECTORS.put(internalLogCollector.type(), internalLogCollector);
    COLLECTORS.put(jmxCollector.type(), jmxCollector);
}
```

**内置收集器**:
- `MicroMeterCollector`: Micrometer 收集器
- `LogCollector`: 日志收集器
- `InternalLogCollector`: 内部日志收集器
- `JMXCollector`: JMX 收集器

## 核心方法

### collect(ThreadPoolStats poolStats, List<String> types)

**作用**: 收集指标

**实现**:
```java
public void collect(ThreadPoolStats poolStats, List<String> types) {
    if (poolStats == null || CollectionUtils.isEmpty(types)) {
        return;
    }
    for (String collectorType : types) {
        MetricsCollector collector = COLLECTORS.get(collectorType.toLowerCase());
        if (collector != null) {
            collector.collect(poolStats);
        }
    }
}
```

**流程**:
1. 验证参数
2. 遍历收集器类型
3. 查找对应的收集器
4. 调用收集器的 `collect()` 方法

---

### getInstance()

**作用**: 获取单例实例

**实现**:
```java
public static CollectorHandler getInstance() {
    return CollectorHandlerHolder.INSTANCE;
}

private static class CollectorHandlerHolder {
    private static final CollectorHandler INSTANCE = new CollectorHandler();
}
```

## 使用场景

### 1. 收集指标

```java
CollectorHandler handler = CollectorHandler.getInstance();
ThreadPoolStats stats = ExecutorConverter.toMetrics(executorWrapper);
handler.collect(stats, Arrays.asList("log", "micrometer"));
```

### 2. 扩展收集器

通过 SPI 扩展收集器：

```java
// 实现 MetricsCollector 接口
public class CustomCollector extends AbstractCollector {
    @Override
    public String type() {
        return "custom";
    }
    
    @Override
    public void collect(ThreadPoolStats poolStats) {
        // 收集逻辑
    }
}

// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.monitor.collector.MetricsCollector
// com.example.CustomCollector
```

## 设计特点

### 1. 单例模式

使用静态内部类实现线程安全的单例。

### 2. SPI 扩展

支持通过 SPI 机制扩展收集器。

### 3. 多收集器支持

支持同时使用多个收集器收集指标。

## 注意事项

1. **收集器类型**: 类型名称不区分大小写
2. **空值处理**: 如果找不到收集器，会跳过
3. **异常处理**: 收集器中的异常需要自行处理

