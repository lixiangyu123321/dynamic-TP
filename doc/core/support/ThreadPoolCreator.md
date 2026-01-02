# ThreadPoolCreator

## 概述

`ThreadPoolCreator` 是线程池创建器类，提供快速创建线程池的静态方法。它使用默认值或少量参数快速创建线程池，适用于简单场景。对于需要详细配置的场景，建议使用 `ThreadPoolBuilder`。

## 核心作用

1. **快速创建**: 提供便捷的静态方法，快速创建线程池
2. **默认配置**: 使用合理的默认值，简化创建过程
3. **常用场景**: 覆盖常用的线程池创建场景

## 核心方法

### 1. createCommonFast(String threadPrefix)

**作用**: 快速创建普通 JUC 线程池

**参数**: `threadPrefix` - 线程名前缀

**默认配置**:
- corePoolSize: 1
- maximumPoolSize: CPU 核心数
- keepAliveTime: 60s
- workQueue: VariableLinkedBlockingQueue，容量 1024
- rejectedExecutionHandler: AbortPolicy

**返回**: `ThreadPoolExecutor`

**示例**:
```java
ThreadPoolExecutor executor = ThreadPoolCreator.createCommonFast("myThread");
```

---

### 2. createCommonWithTtl(String threadPrefix)

**作用**: 创建带 TTL 支持的普通线程池

**参数**: `threadPrefix` - 线程名前缀

**说明**: 使用 `TtlExecutors.getTtlExecutorService()` 包装线程池，支持 TransmittableThreadLocal

**返回**: `ExecutorService`

**示例**:
```java
ExecutorService executor = ThreadPoolCreator.createCommonWithTtl("myThread");
```

---

### 3. createDynamicFast(String poolName)

**作用**: 快速创建动态线程池（使用池名作为线程前缀）

**参数**: `poolName` - 线程池名称

**默认配置**: 同 `createCommonFast()`

**返回**: `DtpExecutor`

**示例**:
```java
DtpExecutor executor = ThreadPoolCreator.createDynamicFast("myPool");
```

---

### 4. createDynamicFast(String poolName, String threadPrefix)

**作用**: 快速创建动态线程池（指定线程前缀）

**参数**:
- `poolName` - 线程池名称
- `threadPrefix` - 线程名前缀

**返回**: `DtpExecutor`

**示例**:
```java
DtpExecutor executor = ThreadPoolCreator.createDynamicFast("myPool", "myThread");
```

---

### 5. createDynamicWithTtl(String poolName)

**作用**: 创建带 TTL 支持的动态线程池（使用池名作为线程前缀）

**参数**: `poolName` - 线程池名称

**说明**: 添加 `TtlTaskWrapper` 支持 TTL

**返回**: `ExecutorService`

**示例**:
```java
ExecutorService executor = ThreadPoolCreator.createDynamicWithTtl("myPool");
```

---

### 6. createDynamicWithTtl(String poolName, String threadPrefix)

**作用**: 创建带 TTL 支持的动态线程池（指定线程前缀）

**参数**:
- `poolName` - 线程池名称
- `threadPrefix` - 线程名前缀

**返回**: `ExecutorService`

---

### 7. newSingleThreadPool(String threadPrefix, int queueCapacity)

**作用**: 创建单线程线程池

**参数**:
- `threadPrefix` - 线程名前缀
- `queueCapacity` - 队列容量

**配置**:
- corePoolSize: 1
- maximumPoolSize: 1
- keepAliveTime: 0

**返回**: `ThreadPoolExecutor`

**示例**:
```java
ThreadPoolExecutor executor = ThreadPoolCreator.newSingleThreadPool("single", 100);
```

---

### 8. newFixedThreadPool(String threadPrefix, int poolSize, int queueCapacity)

**作用**: 创建固定大小的线程池

**参数**:
- `threadPrefix` - 线程名前缀
- `poolSize` - 线程池大小（核心线程数 = 最大线程数）
- `queueCapacity` - 队列容量

**配置**:
- corePoolSize: poolSize
- maximumPoolSize: poolSize
- keepAliveTime: 0

**返回**: `ThreadPoolExecutor`

**示例**:
```java
ThreadPoolExecutor executor = ThreadPoolCreator.newFixedThreadPool("fixed", 10, 200);
```

---

### 9. newThreadPool(String threadPrefix, int corePoolSize, int maximumPoolSize, int queueCapacity)

**作用**: 创建可配置的线程池

**参数**:
- `threadPrefix` - 线程名前缀
- `corePoolSize` - 核心线程数
- `maximumPoolSize` - 最大线程数
- `queueCapacity` - 队列容量

**配置**:
- keepAliveTime: 60s
- workQueue: VariableLinkedBlockingQueue

**返回**: `ThreadPoolExecutor`

**示例**:
```java
ThreadPoolExecutor executor = ThreadPoolCreator.newThreadPool("pool", 10, 20, 200);
```

---

### 10. newScheduledThreadPool(String threadPrefix, int corePoolSize)

**作用**: 创建调度线程池

**参数**:
- `threadPrefix` - 线程名前缀
- `corePoolSize` - 核心线程数

**配置**:
- maximumPoolSize: corePoolSize
- keepAliveTime: 0
- workQueue: DelayedWorkQueue

**返回**: `ScheduledExecutorService`

**示例**:
```java
ScheduledExecutorService executor = ThreadPoolCreator.newScheduledThreadPool("scheduled", 5);
```

---

### 11. newExecutorByBlockingCoefficient(float blockingCoefficient)

**作用**: 根据阻塞系数创建线程池

**参数**: `blockingCoefficient` - 阻塞系数（0-1 之间）

**计算公式**:
```
阻塞系数 = 阻塞时间 / (阻塞时间 + 使用CPU的时间)
建议线程数 = CPU可用核心数 / (1 - 阻塞系数)
```

**说明**:
- 阻塞系数为 0：计算密集型任务
- 阻塞系数接近 1：IO 密集型任务
- 阻塞系数越大，需要的线程数越多

**验证**: 阻塞系数必须在 [0, 1) 范围内

**返回**: `ThreadPoolExecutor`

**示例**:
```java
// IO 密集型任务（阻塞系数 0.8）
ThreadPoolExecutor executor = ThreadPoolCreator.newExecutorByBlockingCoefficient(0.8f);

// 计算密集型任务（阻塞系数 0）
ThreadPoolExecutor executor = ThreadPoolCreator.newExecutorByBlockingCoefficient(0f);
```

## 使用场景

### 1. 快速创建简单线程池

```java
// 快速创建普通线程池
ThreadPoolExecutor executor = ThreadPoolCreator.createCommonFast("worker");

// 快速创建动态线程池
DtpExecutor executor = ThreadPoolCreator.createDynamicFast("myPool");
```

### 2. 创建固定大小线程池

```java
// 创建 10 个线程的固定线程池
ThreadPoolExecutor executor = ThreadPoolCreator.newFixedThreadPool("fixed", 10, 200);
```

### 3. 创建单线程池

```java
// 创建单线程池
ThreadPoolExecutor executor = ThreadPoolCreator.newSingleThreadPool("single", 100);
```

### 4. 创建调度线程池

```java
// 创建调度线程池
ScheduledExecutorService executor = ThreadPoolCreator.newScheduledThreadPool("scheduled", 5);
```

### 5. 根据任务类型创建线程池

```java
// IO 密集型任务
ThreadPoolExecutor ioExecutor = ThreadPoolCreator.newExecutorByBlockingCoefficient(0.8f);

// 计算密集型任务
ThreadPoolExecutor cpuExecutor = ThreadPoolCreator.newExecutorByBlockingCoefficient(0f);
```

## 设计特点

### 1. 静态方法

所有方法都是静态方法，无需创建实例：

```java
ThreadPoolCreator.createCommonFast("thread");
```

### 2. 默认值合理

使用合理的默认值，简化创建：

- 最大线程数默认为 CPU 核心数
- 队列容量默认为 1024
- 线程存活时间默认为 60 秒

### 3. 内部使用 ThreadPoolBuilder

所有方法内部都使用 `ThreadPoolBuilder` 构建线程池，保证一致性：

```java
public static DtpExecutor createDynamicFast(String poolName) {
    return ThreadPoolBuilder.newBuilder()
            .threadPoolName(poolName)
            .threadFactory(poolName)
            .buildDynamic();
}
```

### 4. 阻塞系数计算

`newExecutorByBlockingCoefficient()` 方法根据阻塞系数自动计算线程数：

- **计算密集型**（阻塞系数 = 0）：线程数 = CPU 核心数
- **IO 密集型**（阻塞系数接近 1）：线程数 = CPU 核心数 / (1 - 阻塞系数)

## 与 ThreadPoolBuilder 的对比

| 特性 | ThreadPoolCreator | ThreadPoolBuilder |
|------|------------------|-------------------|
| **使用方式** | 静态方法，快速创建 | 链式 API，灵活配置 |
| **参数数量** | 少量参数或默认值 | 支持所有配置项 |
| **适用场景** | 简单场景，快速创建 | 需要详细配置的场景 |
| **灵活性** | 较低 | 高 |
| **代码量** | 少 | 多 |

## 注意事项

1. **默认值限制**: 使用默认值可能不适合所有场景，需要根据实际情况调整
2. **阻塞系数**: `newExecutorByBlockingCoefficient()` 的阻塞系数必须在 [0, 1) 范围内
3. **详细配置**: 如果需要详细配置，建议使用 `ThreadPoolBuilder`
4. **线程池管理**: 创建的动态线程池需要注册到框架才能被管理

## 最佳实践

1. **简单场景**: 使用 `ThreadPoolCreator` 快速创建
2. **复杂场景**: 使用 `ThreadPoolBuilder` 详细配置
3. **IO 密集型**: 使用 `newExecutorByBlockingCoefficient(0.8f)` 或更高
4. **计算密集型**: 使用 `newExecutorByBlockingCoefficient(0f)` 或接近 0 的值

