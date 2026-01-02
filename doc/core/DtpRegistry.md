# DtpRegistry

## 概述

`DtpRegistry` 是框架的核心注册中心，负责管理所有注册的动态线程池执行器。它是框架中线程池的统一管理中心，提供注册、获取、刷新等功能。

## 核心作用

1. **线程池注册**: 管理所有自动注册和手动注册的执行器
2. **线程池获取**: 提供根据名称获取执行器的方法
3. **配置刷新**: 监听配置变化，动态刷新线程池参数
4. **统一管理**: 作为框架中线程池的统一入口

## 核心属性

```java
// 执行器注册表，线程安全的 Map
private static final Map<String, ExecutorWrapper> EXECUTOR_REGISTRY = new ConcurrentHashMap<>();

// 用于比较两个 TpMainFields 的 Equator
private static final Equator EQUATOR = new GetterBaseEquator();

// 框架配置属性
private static DtpProperties dtpProperties;
```

## 核心方法

### 1. 注册相关方法

#### registerExecutor(ExecutorWrapper wrapper, String source)

**作用**: 注册执行器到注册中心

**实现**:
```java
public static void registerExecutor(ExecutorWrapper wrapper, String source) {
    log.info("DynamicTp register executor: {}, source: {}", ExecutorConverter.toMainFields(wrapper), source);
    EXECUTOR_REGISTRY.putIfAbsent(wrapper.getThreadPoolName(), wrapper);
}
```

**说明**:
- 使用 `putIfAbsent()` 确保线程安全
- 如果已存在同名执行器，不会覆盖
- 记录注册来源（source）用于日志追踪

---

#### unregisterExecutor(String name)

**作用**: 注销执行器

**实现**:
```java
public static ExecutorWrapper unregisterExecutor(String name) {
    ExecutorWrapper executorWrapper = getExecutorWrapper(name);
    log.info("DynamicTp unregister executor: {}", executorWrapper);
    return EXECUTOR_REGISTRY.remove(name);
}
```

---

### 2. 获取相关方法

#### getAllExecutorNames()

**作用**: 获取所有执行器名称

**返回**: 不可修改的执行器名称集合

---

#### getAllExecutors()

**作用**: 获取所有执行器

**返回**: 执行器注册表（Map）

---

#### getExecutor(String name)

**作用**: 根据名称获取执行器

**返回**: `Executor` 实例

**说明**: 如果找不到执行器，抛出 `DtpException`

---

#### getDtpExecutor(String name)

**作用**: 根据名称获取 DtpExecutor

**返回**: `DtpExecutor` 实例

**说明**: 
- 如果执行器不是 `DtpExecutor`，抛出异常
- 用于需要访问 `DtpExecutor` 特定功能的场景

---

#### getExecutorWrapper(String name)

**作用**: 根据名称获取 ExecutorWrapper

**返回**: `ExecutorWrapper` 实例

**说明**: 这是最通用的获取方法，返回包装器对象

---

### 3. 刷新相关方法

#### refresh(DtpProperties dtpProperties)

**作用**: 刷新所有执行器的配置

**实现**:
```java
public static void refresh(DtpProperties dtpProperties) {
    if (Objects.isNull(dtpProperties) || CollectionUtils.isEmpty(dtpProperties.getExecutors())) {
        log.debug("DynamicTp refresh, empty thread pool properties.");
        return;
    }
    dtpProperties.getExecutors().forEach(DtpRegistry::refresh);
}
```

**说明**: 遍历配置中的所有执行器，逐个刷新

---

#### refresh(DtpExecutorProps props)

**作用**: 刷新指定执行器的配置

**实现流程**:

1. **参数验证**: 检查配置是否有效
2. **查找执行器**: 从注册表中查找执行器
3. **执行刷新**: 调用 `refresh(ExecutorWrapper, DtpExecutorProps)`

---

#### refresh(ExecutorWrapper executorWrapper, DtpExecutorProps props)

**作用**: 刷新执行器的配置

**实现流程**:

1. **参数验证**: 检查核心参数是否有效
2. **记录旧值**: 使用 `ExecutorConverter.toMainFields()` 记录旧配置
3. **执行刷新**: 调用 `doRefresh()` 更新配置
4. **记录新值**: 记录新配置
5. **比较差异**: 使用 `Equator` 比较新旧配置，找出变化的字段
6. **发送通知**: 如果有变化，通过 `NoticeManager` 发送通知
7. **记录日志**: 记录刷新日志，包含变化的字段

---

#### doRefresh(ExecutorWrapper executorWrapper, DtpExecutorProps props)

**作用**: 执行实际的刷新操作

**刷新内容**:

1. **线程池大小**: `doRefreshPoolSize()`
2. **线程存活时间**: `setKeepAliveTime()`
3. **核心线程超时**: `allowCoreThreadTimeOut()`
4. **队列属性**: `updateQueueProps()`
5. **执行器特定配置**: 
   - 如果是 `DtpExecutor`，调用 `doRefreshDtp()`
   - 否则调用 `doRefreshCommon()`

---

#### doRefreshPoolSize(ExecutorAdapter<?> executor, DtpExecutorProps props)

**作用**: 刷新线程池大小

**特殊处理**: 

由于 JDK 的 bug（JDK-7153400），需要确保 `corePoolSize <= maximumPoolSize`：

```java
// 如果新最大值 < 旧最大值，先调整核心线程数，再调整最大线程数
if (props.getMaximumPoolSize() < executor.getMaximumPoolSize()) {
    executor.setCorePoolSize(props.getCorePoolSize());
    executor.setMaximumPoolSize(props.getMaximumPoolSize());
    return;
}
// 否则先调整最大线程数，再调整核心线程数
executor.setMaximumPoolSize(props.getMaximumPoolSize());
executor.setCorePoolSize(props.getCorePoolSize());
```

---

#### updateQueueProps(ExecutorAdapter<?> executor, DtpExecutorProps props)

**作用**: 更新队列属性

**支持的队列类型**:

1. **MemorySafeLinkedBlockingQueue**: 更新最大空闲内存
2. **VariableLinkedBlockingQueue**: 更新队列容量

**说明**: 其他类型的队列不支持动态调整容量

---

### 4. 事件监听

#### onContextRefreshedEvent(CustomContextRefreshedEvent event)

**作用**: 处理上下文刷新事件

**实现流程**:

1. **获取配置的执行器**: 从 `dtpProperties` 获取
2. **获取已注册的执行器**: 从注册表获取
3. **分类执行器**:
   - `remoteExecutors`: 配置中心和本地都有的执行器
   - `localExecutors`: 只在本地注册的执行器
4. **刷新非 DTP 执行器**: 刷新 `autoCreate = false` 的执行器
5. **记录日志**: 记录初始化信息

## 使用场景

### 1. 注册执行器

```java
// 创建执行器
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .buildDynamic();

// 注册到框架
DtpRegistry.registerExecutor(ExecutorWrapper.of(executor), "manual");
```

### 2. 获取执行器

```java
// 获取执行器
Executor executor = DtpRegistry.getExecutor("myPool");

// 获取 DtpExecutor（需要访问特定功能）
DtpExecutor dtpExecutor = DtpRegistry.getDtpExecutor("myPool");

// 获取包装器
ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper("myPool");
```

### 3. 刷新配置

```java
// 刷新所有执行器
DtpProperties properties = loadFromConfigCenter();
DtpRegistry.refresh(properties);

// 刷新指定执行器
DtpExecutorProps props = new DtpExecutorProps();
props.setThreadPoolName("myPool");
props.setCorePoolSize(20);
DtpRegistry.refresh(props);
```

### 4. 获取所有执行器

```java
// 获取所有执行器名称
Set<String> names = DtpRegistry.getAllExecutorNames();

// 获取所有执行器
Map<String, ExecutorWrapper> executors = DtpRegistry.getAllExecutors();
```

## 设计特点

### 1. 线程安全

使用 `ConcurrentHashMap` 保证线程安全：

```java
private static final Map<String, ExecutorWrapper> EXECUTOR_REGISTRY = new ConcurrentHashMap<>();
```

### 2. 配置比较

使用 `Equator` 比较配置变化，只通知有变化的字段：

```java
List<FieldInfo> diffFields = EQUATOR.getDiffFields(oldFields, newFields);
List<String> diffKeys = StreamUtil.fetchProperty(diffFields, FieldInfo::getFieldName);
```

### 3. 智能刷新

根据执行器类型选择不同的刷新策略：

- **DtpExecutor**: 支持更多配置项（超时、任务包装等）
- **普通执行器**: 支持基础配置项

### 4. 事件驱动

通过事件总线监听上下文刷新事件，自动处理配置同步。

## 注意事项

1. **线程安全**: 注册表是线程安全的，可以在多线程环境中使用
2. **名称唯一性**: 执行器名称必须唯一，重复注册不会覆盖
3. **配置验证**: 刷新时会验证配置的有效性
4. **变化通知**: 只有配置真正变化时才会发送通知

## 与其他组件的关系

```
DtpRegistry (注册中心)
    │
    ├─> ExecutorWrapper (执行器包装器)
    │   └─> ExecutorAdapter (执行器适配器)
    │
    ├─> AwareManager (感知管理器)
    │   └─> 刷新时更新感知器
    │
    ├─> NoticeManager (通知管理器)
    │   └─> 配置变化时发送通知
    │
    └─> ExecutorConverter (执行器转换器)
        └─> 转换为配置对象进行比较
```

