# Support 模块文档

`core/support` 目录包含了框架的核心支撑类和工具类，为框架提供基础功能支持。

## 目录结构

```
support/
├── adapter/              # 适配器相关
│   ├── ExecutorAdapter.java
│   └── ThreadPoolExecutorAdapter.java
├── binder/               # 配置绑定相关
│   ├── BinderHelper.java
│   └── PropertiesBinder.java
├── init/                 # 初始化相关
│   ├── DtpInitializer.java
│   └── DtpInitializerExecutor.java
├── proxy/                # 代理相关
│   ├── ScheduledThreadPoolExecutorProxy.java
│   └── ThreadPoolExecutorProxy.java
├── selector/             # 选择器相关
│   ├── ExecutorSelector.java
│   ├── HashedExecutorSelector.java
│   └── RandomExecutorSelector.java
├── task/                 # 任务相关
│   ├── callable/
│   │   └── OrderedCallable.java
│   ├── runnable/
│   │   ├── DtpRunnable.java
│   │   ├── EnhancedRunnable.java
│   │   ├── MdcRunnable.java
│   │   ├── NamedRunnable.java
│   │   └── OrderedRunnable.java
│   ├── wrapper/
│   │   ├── MdcTaskWrapper.java
│   │   ├── TaskWrapper.java
│   │   ├── TaskWrappers.java
│   │   └── TtlTaskWrapper.java
│   └── Ordered.java
├── ExecutorWrapper.java          # 执行器包装器
├── ThreadPoolBuilder.java        # 线程池构建器
├── ThreadPoolCreator.java        # 线程池创建器
├── ThreadPoolStatProvider.java   # 线程池统计提供者
├── DynamicTp.java                # 动态线程池注解
├── DtpBannerPrinter.java        # Banner 打印器
└── DtpLifecycleSupport.java     # 生命周期支持
```

## 模块说明

### 核心类

- **[ExecutorWrapper](ExecutorWrapper.md)**: 线程池包装器，统一封装线程池的元数据和功能
- **[ThreadPoolBuilder](ThreadPoolBuilder.md)**: 线程池构建器，提供链式构建线程池
- **[ThreadPoolCreator](ThreadPoolCreator.md)**: 线程池创建器，提供快速创建线程池的静态方法
- **[ThreadPoolStatProvider](ThreadPoolStatProvider.md)**: 线程池统计提供者，提供任务统计和监控功能

### 适配器

- **[ExecutorAdapter](adapter/ExecutorAdapter.md)**: 执行器适配器接口，统一不同执行器的操作接口
- **[ThreadPoolExecutorAdapter](adapter/ThreadPoolExecutorAdapter.md)**: ThreadPoolExecutor 适配器实现

### 任务相关

#### 任务包装器

- **[TaskWrapper](task/wrapper/TaskWrapper.md)**: 任务包装器接口，用于增强任务功能
- **[TaskWrappers](task/wrapper/TaskWrappers.md)**: 任务包装器管理器
- **[MdcTaskWrapper](task/wrapper/MdcTaskWrapper.md)**: MDC 任务包装器
- **[TtlTaskWrapper](task/wrapper/TtlTaskWrapper.md)**: TTL 任务包装器

#### Runnable 实现

- **[DtpRunnable](task/runnable/DtpRunnable.md)**: 框架任务包装类
- **[MdcRunnable](task/runnable/MdcRunnable.md)**: MDC Runnable 实现
- **[NamedRunnable](task/runnable/NamedRunnable.md)**: 带名称的 Runnable
- **[OrderedRunnable](task/runnable/OrderedRunnable.md)**: 有序 Runnable
- **[EnhancedRunnable](task/runnable/EnhancedRunnable.md)**: 增强的 Runnable

#### Callable 实现

- **[OrderedCallable](task/callable/OrderedCallable.md)**: 有序 Callable

#### 其他

- **[Ordered](task/Ordered.md)**: 有序接口

### 代理

- **[ThreadPoolExecutorProxy](proxy/ThreadPoolExecutorProxy.md)**: ThreadPoolExecutor 代理
- **[ScheduledThreadPoolExecutorProxy](proxy/ScheduledThreadPoolExecutorProxy.md)**: ScheduledThreadPoolExecutor 代理

### 选择器

- **[ExecutorSelector](selector/ExecutorSelector.md)**: 执行器选择器接口
- **[HashedExecutorSelector](selector/HashedExecutorSelector.md)**: 哈希执行器选择器
- **[RandomExecutorSelector](selector/RandomExecutorSelector.md)**: 随机执行器选择器

### 配置绑定

- **[PropertiesBinder](binder/PropertiesBinder.md)**: 配置属性绑定器接口
- **[BinderHelper](binder/BinderHelper.md)**: 配置绑定辅助类

### 初始化

- **[DtpInitializer](init/DtpInitializer.md)**: 动态线程池初始化器接口
- **[DtpInitializerExecutor](init/DtpInitializerExecutor.md)**: 初始化器执行器

### 其他

- **[DynamicTp](DynamicTp.md)**: 动态线程池注解
- **[DtpLifecycleSupport](DtpLifecycleSupport.md)**: 生命周期支持工具类
- **[DtpBannerPrinter](DtpBannerPrinter.md)**: Banner 打印器

## 快速导航

- 想了解如何包装线程池？查看 [ExecutorWrapper](ExecutorWrapper.md)
- 想了解如何构建线程池？查看 [ThreadPoolBuilder](ThreadPoolBuilder.md)
- 想了解如何快速创建线程池？查看 [ThreadPoolCreator](ThreadPoolCreator.md)
- 想了解任务包装功能？查看 [TaskWrapper](task/wrapper/TaskWrapper.md)

