# RejectHandlerAware

## 概述

`RejectHandlerAware` 是拒绝策略感知器接口，用于标识支持拒绝策略感知的执行器。它提供了获取和设置拒绝策略类型的功能。

## 核心作用

1. **拒绝策略感知**: 感知执行器的拒绝策略类型
2. **类型记录**: 记录拒绝策略的类型名称
3. **接口标识**: 标识支持拒绝策略感知的执行器

## 接口定义

```java
public interface RejectHandlerAware extends DtpAware {
    String getRejectHandlerType();
    default void setRejectHandlerType(String rejectHandlerType) { }
}
```

## 核心方法

### getRejectHandlerType()

**作用**: 获取拒绝策略类型

**返回**: 拒绝策略类型的名称（如 "AbortPolicy"、"CallerRunsPolicy" 等）

---

### setRejectHandlerType(String rejectHandlerType)

**作用**: 设置拒绝策略类型

**默认实现**: 空方法

**说明**: 子类可以重写此方法来记录拒绝策略类型

## 实现类

### DtpExecutor

`DtpExecutor` 实现了 `RejectHandlerAware` 接口：

```java
public class DtpExecutor extends ThreadPoolExecutor 
    implements TaskEnhanceAware, RejectHandlerAware {
    
    private String rejectHandlerType;
    
    @Override
    public String getRejectHandlerType() {
        return rejectHandlerType;
    }
    
    @Override
    public void setRejectHandlerType(String rejectHandlerType) {
        this.rejectHandlerType = rejectHandlerType;
    }
}
```

### ThreadPoolExecutorProxy

`ThreadPoolExecutorProxy` 也实现了 `RejectHandlerAware` 接口：

```java
public class ThreadPoolExecutorProxy extends ThreadPoolExecutor 
    implements TaskEnhanceAware, RejectHandlerAware {
    
    private String rejectHandlerType;
    
    @Override
    public String getRejectHandlerType() {
        return rejectHandlerType;
    }
    
    @Override
    public void setRejectHandlerType(String rejectHandlerType) {
        this.rejectHandlerType = rejectHandlerType;
    }
}
```

## 使用场景

### 1. 记录拒绝策略类型

在执行器创建或刷新时记录拒绝策略类型：

```java
RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
executor.setRejectHandler(handler);
// 自动记录拒绝策略类型为 "CallerRunsPolicy"
```

### 2. 获取拒绝策略类型

在监控或告警时获取拒绝策略类型：

```java
String rejectType = executor.getRejectHandlerType();
log.info("执行器拒绝策略类型: {}", rejectType);
```

### 3. 配置刷新

在配置刷新时更新拒绝策略类型：

```java
if (!Objects.equals(executor.getRejectHandlerType(), props.getRejectedHandlerType())) {
    RejectedExecutionHandler handler = RejectHandlerGetter.buildRejectedHandler(props.getRejectedHandlerType());
    executor.setRejectHandler(handler);
    // 自动更新拒绝策略类型
}
```

## 设计特点

### 1. 类型记录

记录拒绝策略的类型名称，而不是对象本身，便于序列化和传输。

### 2. 默认实现

`setRejectHandlerType()` 提供默认空实现，子类可以选择性实现。

### 3. 接口标识

通过接口标识支持拒绝策略感知的执行器。

## 注意事项

1. **类型名称**: 记录的是类名（如 "AbortPolicy"），不是完整类名
2. **自动更新**: 设置拒绝策略时应该同步更新类型
3. **线程安全**: 类型字段需要保证线程安全

