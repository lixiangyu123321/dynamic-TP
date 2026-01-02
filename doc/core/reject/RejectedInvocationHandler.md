# RejectedInvocationHandler

## 概述

`RejectedInvocationHandler` 是拒绝策略调用处理器，实现了 `InvocationHandler` 接口。它用于代理拒绝策略，在拒绝任务前后执行增强逻辑（如告警、日志等）。

## 核心作用

1. **拒绝策略代理**: 代理拒绝策略的执行
2. **拒绝前处理**: 在拒绝任务前执行增强逻辑
3. **拒绝后处理**: 在拒绝任务后执行增强逻辑

## 核心属性

```java
private final Object target;  // 目标拒绝策略
```

## 核心方法

### RejectedInvocationHandler(Object target)

**作用**: 构造方法

**说明**: 传入目标拒绝策略对象

---

### invoke(Object proxy, Method method, Object[] args)

**作用**: 代理方法调用

**实现**:
```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    beforeReject((Runnable) args[0], (Executor) args[1]);
    try {
        return method.invoke(target, args);
    } catch (InvocationTargetException ex) {
        throw ex.getCause();
    } finally {
        afterReject((Runnable) args[0], (Executor) args[1]);
    }
}
```

**流程**:
1. 调用 `beforeReject()` 执行拒绝前处理
2. 调用目标拒绝策略的方法
3. 处理异常（抛出原始异常）
4. 调用 `afterReject()` 执行拒绝后处理

---

### beforeReject(Runnable runnable, Executor executor)

**作用**: 拒绝前处理

**实现**:
```java
private void beforeReject(Runnable runnable, Executor executor) {
    AwareManager.beforeReject(runnable, executor);
}
```

**说明**: 调用 `AwareManager.beforeReject()` 触发感知器的拒绝前处理

---

### afterReject(Runnable runnable, Executor executor)

**作用**: 拒绝后处理

**实现**:
```java
private void afterReject(Runnable runnable, Executor executor) {
    AwareManager.afterReject(runnable, executor);
}
```

**说明**: 调用 `AwareManager.afterReject()` 触发感知器的拒绝后处理

## 使用场景

### 1. 代理拒绝策略

`RejectHandlerGetter.getProxy()` 使用此处理器创建代理：

```java
public static RejectedExecutionHandler getProxy(RejectedExecutionHandler handler) {
    return (RejectedExecutionHandler) Proxy
            .newProxyInstance(handler.getClass().getClassLoader(),
                    new Class[]{RejectedExecutionHandler.class},
                    new RejectedInvocationHandler(handler));
}
```

### 2. 增强拒绝策略

通过代理增强拒绝策略功能：

```java
RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();
RejectedExecutionHandler proxy = RejectHandlerGetter.getProxy(handler);
// 代理策略支持告警、日志等增强功能
```

## 设计特点

### 1. JDK 动态代理

使用 JDK 动态代理实现拒绝策略的增强。

### 2. 前后处理

在拒绝前后执行增强逻辑，不影响原有拒绝策略的行为。

### 3. 异常处理

正确处理异常，抛出原始异常。

## 注意事项

1. **参数类型**: 假设第一个参数是 `Runnable`，第二个参数是 `Executor`
2. **异常处理**: 正确处理 `InvocationTargetException`，抛出原始异常
3. **性能影响**: 代理会增加一层调用，但开销很小

