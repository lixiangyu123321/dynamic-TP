# Monitor 模块文档

`core/monitor` 目录包含了框架的监控相关功能，包括指标收集、性能监控等。

## 目录结构

```
monitor/
├── DtpMonitor.java                    # 监控管理器
├── PerformanceProvider.java          # 性能提供者
└── collector/
    ├── AbstractCollector.java        # 抽象收集器
    ├── MetricsCollector.java         # 指标收集器接口
    ├── InternalLogCollector.java     # 内部日志收集器
    ├── LogCollector.java             # 日志收集器
    ├── MicroMeterCollector.java      # Micrometer 收集器
    └── jmx/
        ├── JMXCollector.java         # JMX 收集器
        ├── ThreadPoolStatsJMX.java   # JMX 统计对象
        └── ThreadPoolStatsMXBean.java # JMX MBean 接口
```

## 模块说明

### 核心类

- **[DtpMonitor](DtpMonitor.md)**: 监控管理器，负责定时收集指标和检查告警
- **[PerformanceProvider](PerformanceProvider.md)**: 性能提供者，提供性能指标（TPS、RT、TP 分位数等）

### 收集器

- **[AbstractCollector](collector/AbstractCollector.md)**: 抽象收集器基类
- **[MetricsCollector](collector/MetricsCollector.md)**: 指标收集器接口
- **[InternalLogCollector](collector/InternalLogCollector.md)**: 内部日志收集器
- **[LogCollector](collector/LogCollector.md)**: 日志收集器
- **[MicroMeterCollector](collector/MicroMeterCollector.md)**: Micrometer 收集器
- **[JMXCollector](collector/jmx/JMXCollector.md)**: JMX 收集器

## 快速导航

- 想了解监控管理？查看 [DtpMonitor](DtpMonitor.md)
- 想了解性能指标？查看 [PerformanceProvider](PerformanceProvider.md)
- 想了解指标收集？查看 [MetricsCollector](collector/MetricsCollector.md)

