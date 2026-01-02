# MdcTaskWrapper

## 概述

`MdcTaskWrapper` 是 MDC（Mapped Diagnostic Context）任务包装器，用于在异步任务中传递 MDC 上下文信息。它确保在异步任务执行时能够访问到主线程的 MDC 上下文。

## 核心作用

1. **MDC 传递**: 将主线程的 MDC 上下文传递到异步任务
2. **日志追踪**: 支持在异步任务中保持 traceId 等日志上下文
3. **自动清理**: 任务执行后自动清理 MDC 上下文

## 实现

```java
public class MdcTaskWrapper implements TaskWrapper {
    
    private static final String NAME = "mdc";
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public Runnable wrap(Runnable runnable) {
        return MdcRunnable.get(runnable);
    }
}
```

## 工作原理

### 1. 包装任务

`MdcTaskWrapper` 将原始任务包装为 `MdcRunnable`：

```java
Runnable wrapped = MdcRunnable.get(originalRunnable);
```

### 2. MdcRunnable 实现

`MdcRunnable` 会：

1. **捕获 MDC 上下文**: 在包装时捕获当前线程的 MDC 上下文
2. **传递上下文**: 在任务执行时设置 MDC 上下文
3. **清理上下文**: 任务执行后清理 MDC 上下文

## 使用场景

### 1. 日志追踪

```java
// 主线程设置 traceId
MDC.put("traceId", "123-456");

// 异步任务中自动传递 traceId
executor.execute(() -> {
    // 可以获取到 traceId
    String traceId = MDC.get("traceId");
    log.info("处理任务, traceId: {}", traceId);
});
```

### 2. 分布式追踪

在微服务场景下，保持请求的 traceId：

```java
// 接收到请求，设置 traceId
String traceId = request.getHeader("X-Trace-Id");
MDC.put("traceId", traceId);

// 异步处理请求
executor.execute(() -> {
    // traceId 自动传递
    processRequest();
});
```

## 配置方式

### 1. 配置文件

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          task-wrapper-names: [mdc]
```

### 2. 编程方式

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .taskWrapper(new MdcTaskWrapper())
    .buildDynamic();
```

## 注意事项

1. **MDC 实现**: 需要日志框架支持 MDC（如 Logback、Log4j2）
2. **上下文清理**: `MdcRunnable` 会自动清理上下文，避免内存泄漏
3. **性能影响**: MDC 传递会有一定的性能开销，但通常可以忽略
4. **线程安全**: MDC 是线程本地的，每个线程有独立的上下文

## 与其他包装器的配合

可以与其他包装器配合使用：

```yaml
task-wrapper-names: [mdc, ttl]  # 同时使用 MDC 和 TTL
```

包装顺序：
1. 先应用 TTL 包装
2. 再应用 MDC 包装

执行顺序：
1. MDC 包装的任务执行
2. TTL 包装的任务执行
3. 原始任务执行

