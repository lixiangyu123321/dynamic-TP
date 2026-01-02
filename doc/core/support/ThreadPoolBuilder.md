# ThreadPoolBuilder

## 概述

`ThreadPoolBuilder` 是线程池构建器类，采用建造者模式（Builder Pattern）提供链式 API 来创建和配置线程池。它支持创建多种类型的线程池，包括动态线程池、普通线程池、IO 密集型线程池、有序线程池、调度线程池、优先级线程池等。

## 核心作用

1. **链式构建**: 提供流畅的链式 API，方便配置线程池
2. **类型支持**: 支持创建多种类型的线程池
3. **默认值**: 提供合理的默认值，简化配置
4. **参数验证**: 对参数进行验证，确保配置正确

## 使用方式

### 基本使用

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .corePoolSize(10)
    .maximumPoolSize(20)
    .queueCapacity(200)
    .buildDynamic();
```

### 完整配置示例

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .corePoolSize(10)
    .maximumPoolSize(20)
    .keepAliveTime(60)
    .timeUnit(TimeUnit.SECONDS)
    .workQueue("VariableLinkedBlockingQueue", 200, false)
    .rejectedExecutionHandler("CallerRunsPolicy")
    .threadFactory("myThread")
    .allowCoreThreadTimeOut(false)
    .waitForTasksToCompleteOnShutdown(true)
    .awaitTerminationSeconds(5)
    .rejectEnhanced(true)
    .notifyEnabled(true)
    .runTimeout(3000)
    .queueTimeout(1000)
    .taskWrapper(new MdcTaskWrapper())
    .buildDynamic();
```

## 核心方法

### 创建方法

#### newBuilder()

**作用**: 创建新的构建器实例

```java
ThreadPoolBuilder builder = ThreadPoolBuilder.newBuilder();
```

### 配置方法

#### threadPoolName(String poolName)

**作用**: 设置线程池名称

**说明**: 线程池的唯一标识，用于注册和查找

#### corePoolSize(int corePoolSize)

**作用**: 设置核心线程数

**验证**: 必须 >= 0

#### maximumPoolSize(int maximumPoolSize)

**作用**: 设置最大线程数

**验证**: 必须 > 0

#### keepAliveTime(long keepAliveTime)

**作用**: 设置线程存活时间

**验证**: 必须 > 0

#### timeUnit(TimeUnit timeUnit)

**作用**: 设置时间单位

#### workQueue(String queueName, Integer capacity, Boolean fair, Integer maxFreeMemory)

**作用**: 创建工作队列

**参数**:
- `queueName`: 队列类型名称（如 "VariableLinkedBlockingQueue"）
- `capacity`: 队列容量
- `fair`: 是否公平（用于 SynchronousQueue）
- `maxFreeMemory`: 最大空闲内存（用于 MemorySafeLBQ）

#### queueCapacity(int queueCapacity)

**作用**: 设置队列容量

#### rejectedExecutionHandler(String rejectedName)

**作用**: 设置拒绝策略（通过名称）

**支持的类型**: 见 `RejectedTypeEnum`

#### rejectedExecutionHandler(RejectedExecutionHandler handler)

**作用**: 设置拒绝策略（直接传入处理器）

#### threadFactory(String prefix)

**作用**: 设置线程工厂（通过前缀）

**说明**: 会创建 `NamedThreadFactory`，线程名格式为 `{prefix}-{number}`

### 线程池类型方法

#### eager()

**作用**: 设置为 IO 密集型线程池（EagerDtpExecutor）

**说明**: 
- 适用于 IO 密集型任务
- 队列满时立即创建线程，而不是拒绝任务

#### ordered()

**作用**: 设置为有序线程池（OrderedDtpExecutor）

**说明**:
- 保证任务按提交顺序执行
- 适用于需要顺序执行的场景

#### scheduled()

**作用**: 设置为调度线程池（ScheduledDtpExecutor）

**说明**:
- 支持定时任务和周期性任务
- 继承自 `ScheduledThreadPoolExecutor`

#### priority()

**作用**: 设置为优先级线程池（PriorityDtpExecutor）

**说明**:
- 支持按优先级执行任务
- 使用 `PriorityBlockingQueue`

### 构建方法

#### build()

**作用**: 根据 `dynamic` 字段构建线程池

**说明**:
- 如果 `dynamic = true`，构建 `DtpExecutor`
- 如果 `dynamic = false`，构建普通 `ThreadPoolExecutor`

#### buildDynamic()

**作用**: 构建动态线程池（DtpExecutor）

**返回**: `DtpExecutor`

#### buildCommon()

**作用**: 构建普通线程池（ThreadPoolExecutor）

**返回**: `ThreadPoolExecutor`

#### buildScheduled()

**作用**: 构建调度线程池

**返回**: `ScheduledExecutorService`

#### buildOrdered()

**作用**: 构建有序线程池

**返回**: `OrderedDtpExecutor`

#### buildEager()

**作用**: 构建 IO 密集型线程池

**返回**: `EagerDtpExecutor`

#### buildPriority()

**作用**: 构建优先级线程池

**返回**: `PriorityDtpExecutor`

#### buildWithTtl()

**作用**: 构建带 TTL（TransmittableThreadLocal）支持的线程池

**说明**:
- 如果 `dynamic = true`，添加 `TtlTaskWrapper`
- 如果 `dynamic = false`，使用 `TtlExecutors.getTtlExecutorService()` 包装

## 默认值

如果不设置某些参数，构建器会使用以下默认值：

- **threadPoolName**: `"DynamicTp"`
- **corePoolSize**: `1`
- **maximumPoolSize**: `Runtime.getRuntime().availableProcessors()`（CPU 核心数）
- **keepAliveTime**: `60`
- **timeUnit**: `TimeUnit.SECONDS`
- **workQueue**: `VariableLinkedBlockingQueue`，容量 `1024`
- **queueCapacity**: `1024`
- **rejectedExecutionHandler**: `AbortPolicy`
- **threadFactory**: `NamedThreadFactory("dtp")`
- **allowCoreThreadTimeOut**: `false`
- **dynamic**: `true`
- **waitForTasksToCompleteOnShutdown**: `true`
- **awaitTerminationSeconds**: `3`
- **eager**: `false`
- **ordered**: `false`
- **scheduled**: `false`
- **priority**: `false`
- **preStartAllCoreThreads**: `false`
- **rejectEnhanced**: `true`
- **notifyEnabled**: `true`
- **runTimeout**: `0`
- **tryInterrupt**: `false`
- **queueTimeout**: `0`

## 线程池类型选择

### 1. 普通线程池（DtpExecutor）

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("normalPool")
    .buildDynamic();
```

**适用场景**: 通用场景，CPU 密集型任务

### 2. IO 密集型线程池（EagerDtpExecutor）

```java
EagerDtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("ioPool")
    .eager()
    .buildEager();
```

**适用场景**: IO 密集型任务，如网络请求、文件操作等

### 3. 有序线程池（OrderedDtpExecutor）

```java
OrderedDtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("orderedPool")
    .ordered()
    .buildOrdered();
```

**适用场景**: 需要保证任务执行顺序的场景

### 4. 调度线程池（ScheduledDtpExecutor）

```java
ScheduledDtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("scheduledPool")
    .scheduled()
    .buildScheduled();
```

**适用场景**: 定时任务、周期性任务

### 5. 优先级线程池（PriorityDtpExecutor）

```java
PriorityDtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("priorityPool")
    .priority()
    .buildPriority();
```

**适用场景**: 需要按优先级执行任务的场景

## 设计特点

### 1. 建造者模式

采用建造者模式，提供流畅的链式 API：

```java
ThreadPoolBuilder.newBuilder()
    .threadPoolName("pool")
    .corePoolSize(10)
    .maximumPoolSize(20)
    .buildDynamic();
```

### 2. 参数验证

对关键参数进行验证：

- `corePoolSize >= 0`
- `maximumPoolSize > 0`
- `keepAliveTime > 0`

### 3. 类型检查

通过 `checkExecutorType()` 确保只能设置一种线程池类型：

```java
private void checkExecutorType() {
    if (eager || ordered || scheduled || priority) {
        throw new IllegalArgumentException("More than one executor type is defined");
    }
}
```

### 4. 默认值合理

提供合理的默认值，简化配置：

- 最大线程数默认为 CPU 核心数
- 队列容量默认为 1024
- 线程存活时间默认为 60 秒

## 注意事项

1. **线程池名称**: 必须设置线程池名称，否则构建会失败
2. **类型互斥**: 只能设置一种线程池类型（eager、ordered、scheduled、priority）
3. **参数验证**: 某些参数有最小值或最大值限制
4. **动态 vs 普通**: `dynamic = true` 时构建的线程池会被框架管理，`false` 时不会

## 与 ThreadPoolCreator 的区别

| 特性 | ThreadPoolBuilder | ThreadPoolCreator |
|------|------------------|-------------------|
| **使用方式** | 链式 API，灵活配置 | 静态方法，快速创建 |
| **配置能力** | 支持所有配置项 | 使用默认值或少量参数 |
| **适用场景** | 需要详细配置的场景 | 快速创建简单线程池 |
| **返回类型** | 多种类型 | 固定类型 |

