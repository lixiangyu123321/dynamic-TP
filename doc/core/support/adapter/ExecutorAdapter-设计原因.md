# ExecutorAdapter 设计原因

## 概述

`ExecutorAdapter` 是执行器适配器接口，用于统一不同执行器的操作接口。本文档详细说明为什么设计 `ExecutorAdapter` 接口，而不是直接继承 `ThreadPoolExecutor`。

## 核心问题

**为什么定义一个 `ExecutorAdapter` 接口，不嫌麻烦地抽取 `ThreadPoolExecutor` 中的方法，而不选择直接继承 `ThreadPoolExecutor`？**

## 设计原因

### 1. 需要适配多种不同类型的执行器

框架需要管理的执行器类型多种多样，不仅仅是 `ThreadPoolExecutor`：

#### 需要适配的执行器类型

1. **JUC 标准线程池**
   - `ThreadPoolExecutor`：Java 标准线程池

2. **框架自己的线程池**
   - `DtpExecutor`：框架的动态线程池（继承自 `ThreadPoolExecutor`）
   - `EagerDtpExecutor`：IO 密集型线程池
   - `OrderedDtpExecutor`：有序线程池
   - `ScheduledDtpExecutor`：调度线程池
   - `PriorityDtpExecutor`：优先级线程池

3. **中间件线程池**
   - **Undertow**：`EnhancedQueueExecutor`
   - **Tomcat**：`Executor`
   - **Jetty**：`ThreadPool.SizedThreadPool`
   - **Dubbo**：`ThreadPool`
   - **RocketMQ**：`ThreadPoolExecutor`

这些执行器类型完全不同，无法通过继承 `ThreadPoolExecutor` 来统一。

#### 代码证据

在 `ExecutorWrapper` 的构造方法中可以看到这种多样性：

```java
public ExecutorWrapper(String threadPoolName, Executor executor) {
    this.threadPoolName = threadPoolName;
    if (executor instanceof ThreadPoolExecutor) {
        // 将 ThreadPoolExecutor 包装为适配器
        this.executor = new ThreadPoolExecutorAdapter((ThreadPoolExecutor) executor);
    } else if (executor instanceof ExecutorAdapter<?>) {
        // 如果已经是适配器，直接使用（如 DtpExecutor）
        this.executor = (ExecutorAdapter<?>) executor;
    } else {
        throw new IllegalArgumentException("unsupported Executor type !");
    }
}
```

### 2. 适配器模式的优势

使用适配器模式（Adapter Pattern）可以：

#### 统一接口

为不同类型的执行器提供统一的操作接口，框架代码无需关心底层实现：

```java
// 框架代码统一使用 ExecutorAdapter 接口
ExecutorAdapter<?> adapter = executorWrapper.getExecutor();

// 无论底层是什么执行器，都用相同的 API 操作
int coreSize = adapter.getCorePoolSize();
adapter.setCorePoolSize(20);
int queueSize = adapter.getQueueSize();
```

#### 解耦

框架不直接依赖具体的执行器类型，而是依赖 `ExecutorAdapter` 接口：

```java
// 框架代码只依赖接口，不依赖具体实现
public void refreshExecutor(ExecutorAdapter<?> adapter, TpExecutorProps props) {
    adapter.setCorePoolSize(props.getCorePoolSize());
    adapter.setMaximumPoolSize(props.getMaximumPoolSize());
    // ...
}
```

#### 扩展性

可以为新的执行器类型创建适配器，无需修改框架代码：

```java
// 为新的执行器类型创建适配器
public class CustomExecutorAdapter implements ExecutorAdapter<CustomExecutor> {
    private final CustomExecutor executor;
    
    @Override
    public int getCorePoolSize() {
        return executor.getCoreSize();  // 映射到自定义方法
    }
    
    // 实现其他方法...
}
```

### 3. 语义更清晰

`ExecutorAdapter` 明确表示这是一个**适配器**，而不是执行器本身：

- **适配器**：用于适配和转换，将不同接口统一
- **执行器**：实际执行任务的组件

这种语义区分使代码更容易理解：

```java
// 清晰的语义：这是一个适配器
ExecutorAdapter<ThreadPoolExecutor> adapter = new ThreadPoolExecutorAdapter(executor);

// 而不是：这是一个执行器（但实际上它只是包装）
```

### 4. 灵活性

即使是 `DtpExecutor`（继承自 `ThreadPoolExecutor`），也实现了 `ExecutorAdapter`：

```java
public class DtpExecutor extends ThreadPoolExecutor 
    implements TaskEnhanceAware, ExecutorAdapter<ThreadPoolExecutor> {
    // ...
}
```

这样设计的好处：

#### 统一处理

`DtpExecutor` 和 `ThreadPoolExecutor` 都可以通过 `ExecutorAdapter` 统一处理：

```java
// 框架代码统一处理
ExecutorAdapter<?> adapter = executorWrapper.getExecutor();
adapter.setCorePoolSize(20);  // 无论是 DtpExecutor 还是 ThreadPoolExecutor
```

#### 类型安全

通过泛型 `ExecutorAdapter<ThreadPoolExecutor>` 保持类型信息：

```java
ThreadPoolExecutor original = adapter.getOriginal();  // 类型安全，无需强制转换
```

#### 避免类型丢失

`getOriginal()` 返回具体类型，不需要强制转换：

```java
// 如果直接返回 Executor，需要强制转换
Executor executor = adapter.getOriginal();
ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;  // 需要强制转换

// 使用泛型，类型安全
ThreadPoolExecutor tpe = adapter.getOriginal();  // 直接返回正确类型
```

### 5. 支持默认实现

接口提供了默认实现，适配器只需实现支持的方法：

```java
// 对于不支持的方法，返回默认值
default int getLargestPoolSize() {
    return -1;  // 默认不支持
}

default long getTaskCount() {
    return -1;  // 默认不支持
}
```

对于不支持某些方法的执行器（如 `ForkJoinPool`），可以返回默认值，而不需要抛出异常。

## 为什么不直接继承 ThreadPoolExecutor？

### 1. 无法适配非 ThreadPoolExecutor 类型

如果直接继承 `ThreadPoolExecutor`，只能处理 `ThreadPoolExecutor` 及其子类：

```java
// 如果框架只支持 ThreadPoolExecutor
public class FrameworkExecutor extends ThreadPoolExecutor {
    // ...
}

// 问题：无法适配 Undertow、Tomcat、Jetty 等中间件的执行器
// Undertow 的 EnhancedQueueExecutor 不是 ThreadPoolExecutor 的子类
// Tomcat 的 Executor 也不是 ThreadPoolExecutor 的子类
```

### 2. 无法统一管理框架自己的 DtpExecutor

`DtpExecutor` 已经继承自 `ThreadPoolExecutor`，如果框架也继承 `ThreadPoolExecutor`，会导致：

- 类型混乱：`DtpExecutor` 和框架的执行器类型不同
- 无法统一：框架代码需要区分 `DtpExecutor` 和框架执行器

### 3. 扩展性差

每次需要支持新的执行器类型，都需要修改框架代码，而不是通过适配器扩展。

## 设计目标

根据代码注释，`ExecutorAdapter` 的设计目标是：

> **尽可能兼容 `ThreadPoolExecutor` 的 API**

这意味着：

1. **统一 API**：将不同执行器的操作接口统一到 `ThreadPoolExecutor` 的 API 风格
2. **统一管理**：框架可以用统一的 API 管理不同类型的执行器
3. **向后兼容**：保持与 `ThreadPoolExecutor` API 的兼容性

## 实际应用场景

### 场景 1：统一配置刷新

```java
// 框架代码统一刷新配置
public void refreshExecutor(ExecutorAdapter<?> adapter, TpExecutorProps props) {
    adapter.setCorePoolSize(props.getCorePoolSize());
    adapter.setMaximumPoolSize(props.getMaximumPoolSize());
    adapter.setKeepAliveTime(props.getKeepAliveTime(), TimeUnit.SECONDS);
    // 无论是哪种执行器，都用相同的 API
}
```

### 场景 2：统一监控

```java
// 框架代码统一获取监控指标
public ThreadPoolStats getStats(ExecutorAdapter<?> adapter) {
    return ThreadPoolStats.builder()
        .corePoolSize(adapter.getCorePoolSize())
        .maximumPoolSize(adapter.getMaximumPoolSize())
        .poolSize(adapter.getPoolSize())
        .activeCount(adapter.getActiveCount())
        .queueSize(adapter.getQueueSize())
        // 无论是哪种执行器，都用相同的 API
        .build();
}
```

### 场景 3：适配中间件线程池

```java
// 为 Undertow 创建适配器
public class EnhancedQueueExecutorAdapter implements ExecutorAdapter<EnhancedQueueExecutor> {
    private final EnhancedQueueExecutor executor;
    
    @Override
    public int getCorePoolSize() {
        return executor.getCoreWorkerThreads();  // 映射到 Undertow 的方法
    }
    
    @Override
    public void setCorePoolSize(int corePoolSize) {
        executor.setCoreWorkerThreads(corePoolSize);  // 映射到 Undertow 的方法
    }
    
    // 实现其他方法...
}
```

## 总结

### 核心思想

**将不同执行器的操作接口统一到 `ThreadPoolExecutor` 的 API 风格**，而不是统一实现。

### 设计原因总结

1. **适配多种执行器类型**：不仅仅是 `ThreadPoolExecutor`，还包括中间件线程池
2. **适配器模式优势**：统一接口、解耦、扩展性强
3. **语义清晰**：明确表示这是适配器，不是执行器
4. **灵活性**：即使是 `DtpExecutor` 也实现接口，统一处理
5. **支持默认实现**：对于不支持的方法，返回默认值

### 设计优势

- ✅ **统一 API**：框架代码只需面向 `ExecutorAdapter` 接口
- ✅ **易于扩展**：新增执行器类型只需实现 `ExecutorAdapter`
- ✅ **向后兼容**：保持与 `ThreadPoolExecutor` API 的兼容性
- ✅ **类型安全**：通过泛型保持类型信息
- ✅ **解耦**：框架不依赖具体执行器类型

### 关键点

**统一的是操作接口（API），不是实现**。框架的目标是让不同类型的执行器都能用统一的 API 进行管理，以 `ThreadPoolExecutor` 的 API 为参考标准。

