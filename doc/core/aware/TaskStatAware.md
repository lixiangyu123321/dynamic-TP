# TaskStatAware

## 概述

`TaskStatAware` 是任务统计感知器的抽象基类，提供了与 `ThreadPoolStatProvider` 交互的基础功能。它是 `PerformanceMonitorAware`、`TaskTimeoutAware`、`TaskRejectAware` 的父类。

## 核心作用

1. **统计提供者管理**: 管理执行器与 `ThreadPoolStatProvider` 的映射关系
2. **生命周期管理**: 提供注册、刷新、移除的基础实现
3. **代码复用**: 为子类提供通用的统计提供者管理功能

## 核心属性

```java
// 执行器与统计提供者的映射
protected final Map<Executor, ThreadPoolStatProvider> statProviders = new ConcurrentHashMap<>();
```

**说明**: 
- 使用 `ConcurrentHashMap` 保证线程安全
- 同时存储 `ExecutorAdapter` 和原始执行器的映射

## 核心方法

### register(ExecutorWrapper wrapper)

**作用**: 注册执行器，建立执行器与统计提供者的映射

**实现**:
```java
@Override
public void register(ExecutorWrapper wrapper) {
    ThreadPoolStatProvider statProvider = wrapper.getThreadPoolStatProvider();
    statProviders.put(wrapper.getExecutor(), statProvider);
    statProviders.put(wrapper.getExecutor().getOriginal(), statProvider);
}
```

**说明**:
- 从 `ExecutorWrapper` 获取 `ThreadPoolStatProvider`
- 同时存储 `ExecutorAdapter` 和原始执行器的映射
- 这样可以通过执行器或原始执行器都能找到统计提供者

---

### refresh(ExecutorWrapper wrapper, TpExecutorProps props)

**作用**: 刷新执行器配置

**实现**:
```java
@Override
public void refresh(ExecutorWrapper wrapper, TpExecutorProps props) {
    if (Objects.isNull(statProviders.get(wrapper.getExecutor()))) {
        register(wrapper);  // 如果未注册，先注册
    }
    ThreadPoolStatProvider statProvider = wrapper.getThreadPoolStatProvider();
    refresh(props, statProvider);  // 调用子类实现
}
```

**说明**:
- 如果未注册，先注册
- 调用 `refresh(TpExecutorProps, ThreadPoolStatProvider)` 方法，由子类实现

---

### remove(ExecutorWrapper wrapper)

**作用**: 移除执行器

**实现**:
```java
@Override
public void remove(ExecutorWrapper wrapper) {
    statProviders.remove(wrapper.getExecutor());
    statProviders.remove(wrapper.getExecutor().getOriginal());
}
```

**说明**: 同时移除两个映射

---

### refresh(TpExecutorProps props, ThreadPoolStatProvider statProvider)

**作用**: 刷新统计提供者配置（由子类实现）

**默认实现**: 空方法

**说明**: 子类可以重写此方法来刷新特定的配置

## 设计特点

### 1. 双重映射

同时存储 `ExecutorAdapter` 和原始执行器的映射：

```java
statProviders.put(wrapper.getExecutor(), statProvider);           // ExecutorAdapter
statProviders.put(wrapper.getExecutor().getOriginal(), statProvider);  // 原始执行器
```

**原因**: 在不同的场景下，可能传入的是 `ExecutorAdapter` 或原始执行器，双重映射确保都能找到统计提供者。

### 2. 模板方法模式

使用模板方法模式，定义骨架算法：

- **固定步骤**: `register()`、`refresh()`、`remove()` 的流程是固定的
- **可变步骤**: `refresh(TpExecutorProps, ThreadPoolStatProvider)` 由子类实现

### 3. 线程安全

使用 `ConcurrentHashMap` 保证线程安全。

## 子类实现

### PerformanceMonitorAware

```java
public class PerformanceMonitorAware extends TaskStatAware {
    @Override
    public void execute(Executor executor, Runnable r) {
        Optional.ofNullable(statProviders.get(executor))
            .ifPresent(p -> p.startTask(r));
    }
    
    @Override
    public void afterExecute(Executor executor, Runnable r, Throwable t) {
        Optional.ofNullable(statProviders.get(executor))
            .ifPresent(p -> p.completeTask(r));
    }
}
```

### TaskTimeoutAware

```java
public class TaskTimeoutAware extends TaskStatAware {
    @Override
    protected void refresh(TpExecutorProps props, ThreadPoolStatProvider statProvider) {
        statProvider.setRunTimeout(props.getRunTimeout());
        statProvider.setQueueTimeout(props.getQueueTimeout());
        statProvider.setTryInterrupt(props.isTryInterrupt());
    }
    
    @Override
    public void execute(Executor executor, Runnable r) {
        Optional.ofNullable(statProviders.get(executor))
            .ifPresent(p -> p.startQueueTimeoutTask(r));
    }
    // ...
}
```

### TaskRejectAware

```java
public class TaskRejectAware extends TaskStatAware {
    @Override
    public void beforeReject(Runnable runnable, Executor executor) {
        ThreadPoolStatProvider statProvider = statProviders.get(executor);
        statProvider.incRejectCount(1);
        // ...
    }
}
```

## 使用场景

### 1. 作为基类

子类继承 `TaskStatAware` 来复用统计提供者管理功能：

```java
public class CustomStatAware extends TaskStatAware {
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public void execute(Executor executor, Runnable r) {
        ThreadPoolStatProvider provider = statProviders.get(executor);
        if (provider != null) {
            // 使用统计提供者
        }
    }
}
```

## 注意事项

1. **双重映射**: 需要同时维护两个映射，确保都能找到统计提供者
2. **线程安全**: 使用 `ConcurrentHashMap` 保证线程安全
3. **子类实现**: 子类需要实现 `getOrder()` 和 `getName()` 方法

