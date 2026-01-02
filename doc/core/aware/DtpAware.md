# DtpAware

## 概述

`DtpAware` 是感知器的标记接口，用于标识框架中的感知器组件。它是一个空接口，主要用于类型标记和分类。

## 核心作用

1. **类型标记**: 标识框架中的感知器组件
2. **接口分类**: 作为感知器接口的父接口
3. **语义表达**: 表达"感知"的语义，基于观察者模式

## 接口定义

```java
public interface DtpAware {
}
```

## 设计理念

根据代码注释：

> 感知器接口，用于类似于 AOP 操作的功能增强
> 为什么要叫感知器呢，基于观察者模式，感知到事件发生了，进行响应

### 观察者模式

感知器采用观察者模式的设计：

- **观察者**: 感知器（Aware）
- **被观察者**: 线程池执行器（Executor）
- **事件**: 线程池生命周期事件（注册、刷新、执行、关闭等）
- **响应**: 感知器对事件做出响应（监控、告警、统计等）

### 与 AOP 的区别

| 特性 | Aware（感知器） | AOP |
|------|----------------|-----|
| **设计模式** | 观察者模式 | 代理模式 |
| **语义** | 感知事件并响应 | 拦截并增强 |
| **关注点** | 事件响应 | 方法拦截 |
| **耦合度** | 较低（事件驱动） | 较高（直接拦截） |

## 子接口

### ExecutorAware

执行器感知器接口，感知线程池生命周期事件：

```java
public interface ExecutorAware extends DtpAware {
    void register(ExecutorWrapper wrapper);
    void refresh(ExecutorWrapper wrapper, TpExecutorProps props);
    void execute(Executor executor, Runnable r);
    // ...
}
```

### MetricsAware

指标感知器接口，提供线程池指标：

```java
public interface MetricsAware extends DtpAware {
    ThreadPoolStats getPoolStats();
    List<ThreadPoolStats> getMultiPoolStats();
}
```

## 使用场景

### 1. 类型检查

用于检查对象是否为感知器：

```java
if (obj instanceof DtpAware) {
    // 是感知器
}
```

### 2. 接口继承

作为感知器接口的父接口：

```java
public interface MyAware extends DtpAware {
    // 自定义感知器
}
```

## 注意事项

1. **空接口**: `DtpAware` 是空接口，不定义任何方法
2. **标记作用**: 主要用于类型标记，不提供具体功能
3. **语义表达**: 通过接口名称表达"感知"的语义

## 总结

`DtpAware` 是感知器体系的根接口，虽然不定义任何方法，但它：

- ✅ 提供了类型标记
- ✅ 表达了设计理念（观察者模式）
- ✅ 作为感知器接口的父接口
- ✅ 体现了"感知事件并响应"的语义

