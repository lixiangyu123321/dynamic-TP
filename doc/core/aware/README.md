# Aware 模块文档

`core/aware` 目录包含了框架的感知器（Aware）相关接口和实现，用于在线程池生命周期中提供功能增强。

## 目录结构

```
aware/
├── DtpAware.java                    # 感知器标记接口
├── ExecutorAware.java                # 执行器感知器接口
├── MetricsAware.java                 # 指标感知器接口
├── AwareManager.java                 # 感知器管理器
├── AwareTypeEnum.java                # 感知器类型枚举
├── PerformanceMonitorAware.java      # 性能监控感知器
├── TaskTimeoutAware.java             # 任务超时感知器
├── TaskRejectAware.java              # 任务拒绝感知器
├── TaskStatAware.java                # 任务统计感知器基类
├── TaskEnhanceAware.java             # 任务增强感知器接口
└── RejectHandlerAware.java           # 拒绝策略感知器接口
```

## 模块说明

### 核心接口

- **[DtpAware](DtpAware.md)**: 感知器标记接口
- **[ExecutorAware](ExecutorAware.md)**: 执行器感知器接口，定义线程池生命周期钩子
- **[MetricsAware](MetricsAware.md)**: 指标感知器接口，提供线程池指标

### 管理器

- **[AwareManager](AwareManager.md)**: 感知器管理器，统一管理所有感知器

### 内置实现

- **[PerformanceMonitorAware](PerformanceMonitorAware.md)**: 性能监控感知器
- **[TaskTimeoutAware](TaskTimeoutAware.md)**: 任务超时感知器
- **[TaskRejectAware](TaskRejectAware.md)**: 任务拒绝感知器

### 辅助接口

- **[TaskStatAware](TaskStatAware.md)**: 任务统计感知器基类
- **[TaskEnhanceAware](TaskEnhanceAware.md)**: 任务增强感知器接口
- **[RejectHandlerAware](RejectHandlerAware.md)**: 拒绝策略感知器接口

### 枚举

- **[AwareTypeEnum](AwareTypeEnum.md)**: 感知器类型枚举

## 快速导航

- 想了解感知器的设计理念？查看 [ExecutorAware-观察者模式](../../ExecutorAware-观察者模式.md)
- 想了解如何管理感知器？查看 [AwareManager](AwareManager.md)
- 想了解性能监控？查看 [PerformanceMonitorAware](PerformanceMonitorAware.md)

