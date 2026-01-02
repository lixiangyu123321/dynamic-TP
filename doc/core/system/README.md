# System 模块文档

`core/system` 目录包含了框架的系统指标相关功能，包括 CPU 指标、内存指标等。

## 目录结构

```
system/
├── SystemMetricManager.java        # 系统指标管理器
├── CpuMetricsCaptor.java            # CPU 指标捕获器
├── MemoryMetricsCaptor.java         # 内存指标捕获器
└── OperatingSystemBeanManager.java  # 操作系统 Bean 管理器
```

## 模块说明

### 核心类

- **[SystemMetricManager](SystemMetricManager.md)**: 系统指标管理器，负责管理 CPU 和内存指标
- **[CpuMetricsCaptor](CpuMetricsCaptor.md)**: CPU 指标捕获器，负责捕获 CPU 使用率
- **[MemoryMetricsCaptor](MemoryMetricsCaptor.md)**: 内存指标捕获器，负责捕获内存使用率
- **[OperatingSystemBeanManager](OperatingSystemBeanManager.md)**: 操作系统 Bean 管理器，提供系统信息

## 快速导航

- 想了解系统指标管理？查看 [SystemMetricManager](SystemMetricManager.md)
- 想了解 CPU 指标？查看 [CpuMetricsCaptor](CpuMetricsCaptor.md)
- 想了解内存指标？查看 [MemoryMetricsCaptor](MemoryMetricsCaptor.md)

