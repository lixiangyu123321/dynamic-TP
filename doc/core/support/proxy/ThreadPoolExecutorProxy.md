# ThreadPoolExecutorProxy

## 概述

`ThreadPoolExecutorProxy` 是 `ThreadPoolExecutor` 的代理类，继承自 `ThreadPoolExecutor`，用于增强普通线程池的功能，使其支持框架的监控、告警、任务包装等功能。

## 核心作用

1. **功能增强**: 为普通 `ThreadPoolExecutor` 添加框架功能
2. **感知器支持**: 支持感知器功能（监控、超时、拒绝等）
3. **任务包装**: 支持任务包装功能
4. **拒绝策略增强**: 支持拒绝策略增强（告警等）

## 继承关系

```java
ThreadPoolExecutorProxy extends ThreadPoolExecutor 
    implements TaskEnhanceAware, RejectHandlerAware
```

## 核心属性

```java
private List<TaskWrapper> taskWrappers;      // 任务包装器列表
private String rejectHandlerType;            // 拒绝策略类型
```

## 核心方法

### 构造方法

```java
public ThreadPoolExecutorProxy(ThreadPoolExecutor executor) {
    super(executor.getCorePoolSize(), executor.getMaximumPoolSize(),
            executor.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS,
            executor.getQueue(), executor.getThreadFactory(),
            executor.getRejectedExecutionHandler());
    allowCoreThreadTimeOut(executor.allowsCoreThreadTimeOut());
    this.rejectHandlerType = getRejectedExecutionHandler().getClass().getSimpleName();
    setRejectedExecutionHandler(RejectHandlerGetter.getProxy(getRejectedExecutionHandler()));
}
```

**说明**:
- 复制原始执行器的所有参数
- 记录拒绝策略类型
- 使用代理拒绝策略替换原始拒绝策略（支持告警等功能）

---

### execute(Runnable command)

**作用**: 执行任务，支持任务包装和感知器

**实现**:
```java
@Override
public void execute(Runnable command) {
    command = getEnhancedTask(command);  // 任务包装
    AwareManager.execute(this, command);  // 感知器：任务提交
    super.execute(command);  // 执行任务
}
```

**流程**:
1. 任务包装：通过 `getEnhancedTask()` 应用任务包装器
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

**说明**: 触发感知器的 `beforeExecute` 事件

---

### afterExecute(Runnable r, Throwable t)

**作用**: 任务执行后处理

**实现**:
```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    AwareManager.afterExecute(this, r, t);  // 感知器：任务执行后
    ExecutorUtil.tryExecAfterExecute(r, t);  // 执行后处理
}
```

**说明**: 
- 先调用父类方法
- 然后触发感知器的 `afterExecute` 事件
- 执行后处理逻辑

---

### TaskEnhanceAware 接口实现

```java
@Override
public List<TaskWrapper> getTaskWrappers() {
    return taskWrappers;
}

@Override
public void setTaskWrappers(List<TaskWrapper> taskWrappers) {
    this.taskWrappers = taskWrappers;
}
```

**说明**: 支持设置和获取任务包装器列表

---

### RejectHandlerAware 接口实现

```java
@Override
public String getRejectHandlerType() {
    return rejectHandlerType;
}

@Override
public void setRejectHandlerType(String rejectHandlerType) {
    this.rejectHandlerType = rejectHandlerType;
}
```

**说明**: 支持设置和获取拒绝策略类型

## 使用场景

### 1. 增强普通线程池

当框架识别到 `ThreadPoolExecutor` Bean 时，会创建代理：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
ThreadPoolExecutorProxy proxy = new ThreadPoolExecutorProxy(executor);
```

### 2. 自动功能增强

代理会自动为线程池添加：

- 任务包装功能
- 感知器支持（监控、超时、拒绝）
- 拒绝策略增强（告警）

## 设计特点

### 1. 代理模式

使用代理模式增强原始执行器，不修改原始执行器。

### 2. 继承实现

继承 `ThreadPoolExecutor`，保持完全兼容。

### 3. 功能增强

通过重写关键方法，添加框架功能。

## 与 DtpExecutor 的区别

| 特性 | ThreadPoolExecutorProxy | DtpExecutor |
|------|------------------------|-------------|
| **基础类** | ThreadPoolExecutor | ThreadPoolExecutor |
| **创建方式** | 代理现有执行器 | 直接创建 |
| **功能** | 基础增强 | 完整功能 |
| **配置刷新** | 支持 | 支持 |
| **超时监控** | 不支持 | 支持 |

## 注意事项

1. **代理开销**: 代理会增加方法调用层次，但开销很小
2. **功能限制**: 某些功能（如超时监控）可能不支持
3. **兼容性**: 完全兼容 `ThreadPoolExecutor` 的 API

