# Core 模块文档

`core` 模块是框架的核心模块，包含了线程池管理、监控、告警、通知等核心功能。

## 目录结构

```
core/
├── DtpRegistry.java                    # 核心注册中心
├── aware/                              # 感知器模块
│   ├── ExecutorAware.java             # 执行器感知器接口
│   ├── AwareManager.java              # 感知器管理器
│   └── ...
├── executor/                           # 执行器模块
│   ├── DtpExecutor.java               # 动态线程池执行器
│   ├── OrderedDtpExecutor.java        # 有序线程池执行器
│   └── ...
├── monitor/                            # 监控模块
│   ├── DtpMonitor.java                # 监控管理器
│   ├── PerformanceProvider.java       # 性能提供者
│   └── collector/                     # 指标收集器
├── notifier/                           # 通知器模块
│   ├── DtpNotifier.java               # 通知器接口
│   ├── manager/                       # 管理器
│   └── ...
├── handler/                            # 处理器模块
│   ├── ConfigHandler.java             # 配置处理器
│   ├── CollectorHandler.java          # 收集器处理器
│   └── NotifierHandler.java           # 通知器处理器
├── lifecycle/                          # 生命周期模块
│   ├── DtpLifecycle.java              # 生命周期管理器
│   └── LifeCycleManagement.java       # 生命周期管理接口
├── converter/                          # 转换器模块
│   └── ExecutorConverter.java         # 执行器转换器
├── refresher/                          # 刷新器模块
│   ├── Refresher.java                 # 刷新器接口
│   └── AbstractRefresher.java        # 抽象刷新器
├── reject/                             # 拒绝策略模块
│   ├── RejectHandlerGetter.java       # 拒绝策略获取器
│   └── RejectedInvocationHandler.java # 拒绝策略调用处理器
├── system/                             # 系统指标模块
│   ├── SystemMetricManager.java       # 系统指标管理器
│   └── ...
├── timer/                              # 定时器模块
│   ├── AbstractTimeoutTimerTask.java  # 抽象超时定时任务
│   └── ...
├── metric/                             # 指标模块
│   ├── Meter.java                      # 计量器
│   ├── Summary.java                    # 摘要
│   └── ...
└── support/                            # 支持模块
    ├── ExecutorWrapper.java           # 执行器包装器
    ├── ThreadPoolBuilder.java         # 线程池构建器
    └── ...
```

## 模块说明

### 核心组件

- **[DtpRegistry](DtpRegistry.md)**: 核心注册中心，管理所有线程池执行器

### 功能模块

- **[aware](aware/README.md)**: 感知器模块，提供线程池生命周期增强
- **[executor](executor/README.md)**: 执行器模块，提供各种线程池执行器
- **[monitor](monitor/README.md)**: 监控模块，提供指标收集和性能监控
- **[notifier](notifier/README.md)**: 通知器模块，提供告警和通知功能
- **[handler](handler/README.md)**: 处理器模块，提供配置、收集器、通知器处理
- **[lifecycle](lifecycle/)**: 生命周期模块，管理框架生命周期
- **[converter](converter/)**: 转换器模块，提供执行器转换功能
- **[refresher](refresher/)**: 刷新器模块，提供配置刷新功能
- **[reject](reject/README.md)**: 拒绝策略模块，提供拒绝策略管理
- **[system](system/README.md)**: 系统指标模块，提供系统指标监控
- **[timer](timer/README.md)**: 定时器模块，提供超时定时任务
- **[metric](metric/)**: 指标模块，提供指标计算功能
- **[support](support/README.md)**: 支持模块，提供各种支持类和工具

## 快速导航

- 想了解核心注册中心？查看 [DtpRegistry](DtpRegistry.md)
- 想了解执行器？查看 [executor](executor/README.md)
- 想了解监控？查看 [monitor](monitor/README.md)
- 想了解通知？查看 [notifier](notifier/README.md)
- 想了解支持类？查看 [support](support/README.md)

