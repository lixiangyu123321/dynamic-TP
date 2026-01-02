# DtpExecutor

## 概述

`DtpExecutor` 是动态线程池执行器，继承自 `ThreadPoolExecutor`，是框架的核心执行器。它在 `ThreadPoolExecutor` 的基础上增加了动态配置、任务增强、监控告警等功能。

## 核心作用

1. **动态配置**: 支持运行时动态调整线程池参数
2. **任务增强**: 支持任务包装（MDC、TTL、链路追踪等）
3. **监控告警**: 集成监控和告警功能
4. **感知器支持**: 支持感知器功能增强

## 继承关系

```java
DtpExecutor extends ThreadPoolExecutor 
    implements TaskEnhanceAware, ExecutorAdapter<ThreadPoolExecutor>
```

## 核心属性

### 基本信息

```java
protected String threadPoolName;           // 线程池名称
private String threadPoolAliasName;        // 线程池别名
```

### 通知配置

```java
private boolean notifyEnabled = true;      // 是否启用通知
private List<NotifyItem> notifyItems;      // 通知项列表
private List<String> platformIds;          // 通知平台 ID 列表
```

### 任务增强

```java
private List<TaskWrapper> taskWrappers = Lists.newArrayList();  // 任务包装器列表
private Set<String> pluginNames = Sets.newHashSet();            // 插件名称集合
private Set<String> awareNames = Sets.newHashSet();             // 感知器名称集合
```

### 拒绝策略

```java
private String rejectHandlerType;         // 拒绝策略类型
private boolean rejectEnhanced = true;     // 是否增强拒绝策略
```

### 超时配置

```java
private long runTimeout = 0;              // 任务执行超时时间（毫秒）
private boolean tryInterrupt = false;     // 超时时是否尝试中断任务
private long queueTimeout = 0;            // 任务队列等待超时时间（毫秒）
```

### 生命周期配置

```java
private boolean preStartAllCoreThreads;   // 是否预启动所有核心线程
protected boolean waitForTasksToCompleteOnShutdown = false;  // 关闭时是否等待任务完成
protected int awaitTerminationSeconds = 0;  // 等待终止的最大秒数
```

## 核心方法

### 构造方法

提供多个构造方法，最终调用父类构造方法：

```java
public DtpExecutor(int corePoolSize, int maximumPoolSize, 
                   long keepAliveTime, TimeUnit unit,
                   BlockingQueue<Runnable> workQueue,
                   ThreadFactory threadFactory,
                   RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, 
          workQueue, threadFactory, handler);
}
```

---

### execute(Runnable command)

**作用**: 执行任务，支持任务增强和感知器

**实现**:
```java
@Override
public void execute(Runnable command) {
    command = getEnhancedTask(command);      // 任务增强
    AwareManager.execute(this, command);     // 感知器：任务提交
    super.execute(command);                  // 执行任务
}
```

**流程**:
1. 任务增强：通过 `getEnhancedTask()` 应用任务包装器
2. 感知器执行：调用 `AwareManager.execute()` 触发任务提交事件
3. 执行任务：调用父类的 `execute()` 方法

---

### beforeExecute(Thread t, Runnable r)

**作用**: 任务执行前处理

**实现**:
```java
@Override
protected void beforeExecute(Thread t, Runnable r) {
    AwareManager.beforeExecute(this, t, r);  // 感知器：任务执行前
    super.beforeExecute(t, r);
}
```

---

### afterExecute(Runnable r, Throwable t)

**作用**: 任务执行后处理

**实现**:
```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    AwareManager.afterExecute(this, r, t);   // 感知器：任务执行后
    ExecutorUtil.tryExecAfterExecute(r, t); // 执行后处理
}
```

---

### shutdown() / shutdownNow()

**作用**: 关闭执行器，通知感知器

**实现**:
```java
@Override
public void shutdown() {
    super.shutdown();
    AwareManager.shutdown(this);
}

@Override
public List<Runnable> shutdownNow() {
    val tasks = super.shutdownNow();
    AwareManager.shutdownNow(this, tasks);
    return tasks;
}
```

---

### terminated()

**作用**: 执行器终止时通知感知器

**实现**:
```java
@Override
protected void terminated() {
    super.terminated();
    AwareManager.terminated(this);
}
```

---

### initialize()

**作用**: 初始化执行器

**实现**:
```java
public void initialize() {
    NotifyHelper.initNotify(this);  // 初始化通知
    if (preStartAllCoreThreads) {
        prestartAllCoreThreads();   // 预启动核心线程
    }
    // 根据 rejectEnhanced 设置拒绝策略
    setRejectHandler(RejectHandlerGetter.buildRejectedHandler(getRejectHandlerType()));
}
```

---

### setRejectHandler(RejectedExecutionHandler handler)

**作用**: 设置拒绝策略

**实现**:
```java
public void setRejectHandler(RejectedExecutionHandler handler) {
    this.rejectHandlerType = handler.getClass().getSimpleName();
    if (!isRejectEnhanced()) {
        setRejectedExecutionHandler(handler);  // 直接设置
        return;
    }
    setRejectedExecutionHandler(RejectHandlerGetter.getProxy(handler));  // 使用代理
}
```

**说明**:
- 如果 `rejectEnhanced = true`，使用代理拒绝策略（支持告警等功能）
- 否则直接设置拒绝策略

---

### ExecutorAdapter 接口实现

`DtpExecutor` 实现了 `ExecutorAdapter<ThreadPoolExecutor>` 接口，所有方法都直接委托给自身（因为 `DtpExecutor` 本身就是 `ThreadPoolExecutor`）：

```java
@Override
public ThreadPoolExecutor getOriginal() {
    return this;  // 返回自身
}

@Override
public int getCorePoolSize() {
    return super.getCorePoolSize();
}

// 其他方法类似...
```

## 使用场景

### 1. 创建动态线程池

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .corePoolSize(10)
    .maximumPoolSize(20)
    .queueCapacity(200)
    .buildDynamic();
```

### 2. 动态调整参数

```java
// 通过配置刷新动态调整
DtpExecutorProps props = new DtpExecutorProps();
props.setThreadPoolName("myPool");
props.setCorePoolSize(15);
props.setMaximumPoolSize(25);
DtpRegistry.refresh(props);
```

### 3. 任务增强

```java
executor.setTaskWrappers(Arrays.asList(
    new TtlTaskWrapper(),
    new MdcTaskWrapper()
));
```

## 设计特点

### 1. 继承 ThreadPoolExecutor

完全兼容 `ThreadPoolExecutor` 的 API，可以无缝替换。

### 2. 功能增强

通过重写关键方法，添加框架功能：
- 任务增强
- 感知器支持
- 监控告警

### 3. 动态配置

支持运行时动态调整参数，无需重启应用。

## 与其他执行器的关系

```
DtpExecutor (基础执行器)
    │
    ├─> OrderedDtpExecutor (有序执行器)
    ├─> ScheduledDtpExecutor (调度执行器)
    └─> PriorityDtpExecutor (优先级执行器)

EagerDtpExecutor (IO 密集型执行器，独立实现)
```

## 注意事项

1. **初始化**: 创建后需要调用 `initialize()` 方法
2. **注册**: 需要注册到 `DtpRegistry` 才能被框架管理
3. **任务包装**: 任务会被包装为 `DtpRunnable`
4. **感知器**: 自动支持所有注册的感知器

