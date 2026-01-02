# RejectHandlerGetter

## 概述

`RejectHandlerGetter` 是拒绝策略获取器，负责创建和代理拒绝策略。它支持 JDK 内置的拒绝策略，也支持通过 SPI 扩展自定义拒绝策略。

## 核心作用

1. **拒绝策略创建**: 根据名称创建拒绝策略实例
2. **拒绝策略代理**: 为拒绝策略创建代理，支持增强功能
3. **SPI 扩展**: 支持通过 SPI 扩展自定义拒绝策略

## 核心方法

### buildRejectedHandler(String name)

**作用**: 根据名称创建拒绝策略

**实现**:
```java
public static RejectedExecutionHandler buildRejectedHandler(String name) {
    // JDK 内置策略
    if (Objects.equals(name, ABORT_POLICY.getName())) {
        return new ThreadPoolExecutor.AbortPolicy();
    } else if (Objects.equals(name, CALLER_RUNS_POLICY.getName())) {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    } else if (Objects.equals(name, DISCARD_OLDEST_POLICY.getName())) {
        return new ThreadPoolExecutor.DiscardOldestPolicy();
    } else if (Objects.equals(name, DISCARD_POLICY.getName())) {
        return new ThreadPoolExecutor.DiscardPolicy();
    }
    
    // SPI 扩展策略
    List<RejectedExecutionHandler> loadedHandlers = ExtensionServiceLoader.get(RejectedExecutionHandler.class);
    for (RejectedExecutionHandler handler : loadedHandlers) {
        String handlerName = handler.getClass().getSimpleName();
        if (name.equalsIgnoreCase(handlerName)) {
            return handler;
        }
    }
    
    log.error("Cannot find specified rejectedHandler {}", name);
    throw new DtpException("Cannot find specified rejectedHandler " + name);
}
```

**支持的策略**:
- `AbortPolicy`: 抛出异常
- `CallerRunsPolicy`: 调用者运行
- `DiscardOldestPolicy`: 丢弃最老的任务
- `DiscardPolicy`: 静默丢弃

---

### getProxy(String name)

**作用**: 获取代理拒绝策略（根据名称）

**实现**:
```java
public static RejectedExecutionHandler getProxy(String name) {
    return getProxy(buildRejectedHandler(name));
}
```

---

### getProxy(RejectedExecutionHandler handler)

**作用**: 获取代理拒绝策略（根据实例）

**实现**:
```java
public static RejectedExecutionHandler getProxy(RejectedExecutionHandler handler) {
    return (RejectedExecutionHandler) Proxy
            .newProxyInstance(handler.getClass().getClassLoader(),
                    new Class[]{RejectedExecutionHandler.class},
                    new RejectedInvocationHandler(handler));
}
```

**说明**: 使用 JDK 动态代理创建代理对象，支持增强功能（告警、日志等）

## 使用场景

### 1. 创建拒绝策略

```java
RejectedExecutionHandler handler = RejectHandlerGetter.buildRejectedHandler("AbortPolicy");
```

### 2. 创建代理拒绝策略

```java
RejectedExecutionHandler proxy = RejectHandlerGetter.getProxy("AbortPolicy");
// 代理策略支持告警、日志等增强功能
```

### 3. 扩展拒绝策略

通过 SPI 扩展自定义拒绝策略：

```java
// 实现 RejectedExecutionHandler 接口
public class CustomRejectHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 自定义拒绝逻辑
    }
}

// 创建 SPI 配置文件
// META-INF/services/java.util.concurrent.RejectedExecutionHandler
// com.example.CustomRejectHandler
```

## 设计特点

### 1. 策略模式

支持多种拒绝策略，通过名称选择。

### 2. 代理模式

使用 JDK 动态代理增强拒绝策略功能。

### 3. SPI 扩展

支持通过 SPI 机制扩展自定义拒绝策略。

## 注意事项

1. **策略名称**: 策略名称区分大小写
2. **代理增强**: 代理策略支持告警、日志等增强功能
3. **异常处理**: 如果找不到策略，抛出异常

