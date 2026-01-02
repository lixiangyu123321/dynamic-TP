# ExecutorWrapper

## 概述

`ExecutorWrapper` 是线程池包装器类，用于统一封装线程池的元数据、配置信息和功能增强。它是框架中线程池的统一表示，为框架的监控、告警、配置刷新等功能提供统一的接口。

## 核心作用

1. **统一封装**: 将不同类型的线程池（DtpExecutor、ThreadPoolExecutor 等）统一封装
2. **元数据管理**: 管理线程池的名称、别名、通知配置等元数据
3. **功能增强**: 提供任务包装、拒绝策略增强等功能
4. **统计支持**: 持有 `ThreadPoolStatProvider`，提供统计功能

## 核心属性

### 基本信息

- **threadPoolName**: 线程池名称
- **threadPoolAliasName**: 线程池别名（用于通知显示）
- **executor**: 执行器适配器（`ExecutorAdapter`）

### 通知配置

- **notifyItems**: 通知项列表（活性、容量、拒绝等）
- **platformIds**: 通知平台 ID 列表
- **notifyEnabled**: 是否启用通知

### 功能配置

- **rejectEnhanced**: 是否增强拒绝策略
- **awareNames**: 感知器名称集合
- **taskWrappers**: 任务包装器列表（通过 `setTaskWrappers()` 设置）

### 生命周期配置

- **waitForTasksToCompleteOnShutdown**: 关闭时是否等待任务完成
- **awaitTerminationSeconds**: 等待终止的最大秒数

### 统计支持

- **threadPoolStatProvider**: 线程池统计提供者

## 核心方法

### 构造方法

#### ExecutorWrapper(DtpExecutor executor)

**作用**: 从 `DtpExecutor` 创建包装器

**说明**:
- 从 `DtpExecutor` 中提取所有配置信息
- 创建 `ThreadPoolStatProvider`
- 适用于框架管理的动态线程池

#### ExecutorWrapper(String threadPoolName, Executor executor)

**作用**: 从普通 `Executor` 创建包装器

**说明**:
- 将 `ThreadPoolExecutor` 适配为 `ThreadPoolExecutorAdapter`
- 初始化默认通知配置
- 创建 `ThreadPoolStatProvider`
- 适用于手动管理的线程池

### 静态工厂方法

#### of(DtpExecutor executor)

**作用**: 创建包装器的便捷方法

```java
ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);
```

### 初始化方法

#### initialize()

**作用**: 初始化线程池包装器

**说明**:
- 如果是 `DtpExecutor`，调用其 `initialize()` 方法
- 注册到 `AwareManager`，启用感知器功能

### 类型判断方法

#### isDtpExecutor()

**作用**: 判断是否为 `DtpExecutor`

#### isThreadPoolExecutor()

**作用**: 判断是否为 `ThreadPoolExecutorAdapter`

#### isExecutorService()

**作用**: 判断是否为 `ExecutorService`

### 功能设置方法

#### setTaskWrappers(List<TaskWrapper> taskWrappers)

**作用**: 设置任务包装器列表

**说明**:
- 如果执行器实现了 `TaskEnhanceAware` 接口，设置任务包装器
- 任务包装器用于增强任务功能（如 MDC、TTL、链路追踪等）

#### setRejectHandler(RejectedExecutionHandler handler)

**作用**: 设置拒绝策略

**说明**:
- 如果执行器实现了 `RejectHandlerAware` 接口，记录拒绝策略类型
- 如果 `rejectEnhanced = true`，使用代理拒绝策略（支持告警等功能）
- 否则直接设置拒绝策略

### 捕获方法

#### capture()

**作用**: 创建捕获版本的包装器

**说明**:
- 复制当前包装器的所有属性
- 使用 `CapturedExecutor` 包装执行器
- 用于告警通知时捕获线程池状态

## 使用场景

### 1. 框架自动创建

框架在以下场景自动创建 `ExecutorWrapper`：

- 从配置中心加载配置创建 `DtpExecutor` 时
- Spring Bean 后处理器识别到线程池 Bean 时
- 适配器发现中间件线程池时

### 2. 手动创建

```java
// 从 DtpExecutor 创建
DtpExecutor dtpExecutor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .buildDynamic();
ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);

// 从普通线程池创建
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
ExecutorWrapper wrapper = new ExecutorWrapper("myPool", executor);
```

### 3. 注册到框架

```java
ExecutorWrapper wrapper = ...;
wrapper.initialize();  // 初始化并注册到框架
DtpRegistry.registerExecutor(wrapper, "manual");
```

## 设计特点

### 1. 统一接口

通过 `ExecutorAdapter` 统一不同线程池的接口，使得框架可以统一处理：
- `DtpExecutor`
- `ThreadPoolExecutor`
- 其他类型的执行器

### 2. 元数据管理

集中管理线程池的元数据，包括：
- 名称和别名
- 通知配置
- 功能开关
- 生命周期配置

### 3. 功能增强

提供功能增强的入口：
- 任务包装（上下文传递等）
- 拒绝策略增强（告警等）
- 感知器支持（监控、超时等）

### 4. 统计支持

持有 `ThreadPoolStatProvider`，提供：
- 性能统计
- 超时监控
- 拒绝统计

## 与其他组件的关系

```
ExecutorWrapper
    │
    ├─> ExecutorAdapter (执行器适配器)
    │   └─> 统一不同执行器的接口
    │
    ├─> ThreadPoolStatProvider (统计提供者)
    │   └─> 提供统计和监控功能
    │
    ├─> AwareManager (感知管理器)
    │   └─> 注册感知器
    │
    └─> DtpRegistry (注册中心)
        └─> 注册到框架进行管理
```

## 注意事项

1. **线程安全**: `ExecutorWrapper` 本身不是线程安全的，但持有的执行器是线程安全的
2. **生命周期**: 需要正确管理包装器的生命周期，确保资源正确释放
3. **初始化**: 创建后需要调用 `initialize()` 方法才能启用框架功能
4. **统计提供者**: 每个包装器都有独立的统计提供者，统计数据不会共享

