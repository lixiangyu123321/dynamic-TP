# DtpRunnable 中的 MDC 说明

## 概述

本文档详细说明 `DtpRunnable` 中使用的 MDC（Mapped Diagnostic Context，映射诊断上下文）是什么，以及它在框架中的作用。

## MDC 是什么？

### 定义

**MDC（Mapped Diagnostic Context）** 是 SLF4J（Simple Logging Facade for Java）提供的一个功能，用于在日志中存储诊断信息。它是一个线程本地的键值对存储，可以在同一个线程的日志输出中自动包含这些信息。

### 核心特点

1. **线程本地存储**: MDC 是线程本地的，每个线程有独立的 MDC 上下文
2. **键值对存储**: 以键值对的形式存储信息
3. **自动输出**: 日志框架（如 Logback、Log4j2）可以自动将 MDC 中的信息输出到日志中
4. **上下文传递**: 可以在线程间传递上下文信息（需要特殊处理）

### MDC 的 API

```java
// 设置 MDC 值
MDC.put("key", "value");

// 获取 MDC 值
String value = MDC.get("key");

// 获取所有 MDC 值
Map<String, String> context = MDC.getCopyOfContextMap();

// 清除 MDC 值
MDC.remove("key");
MDC.clear();
```

## MDC 在 DtpRunnable 中的使用

### 代码位置

在 `DtpRunnable` 的构造方法中：

```java
public DtpRunnable(Runnable originRunnable, Runnable runnable, String taskName) {
    this.originRunnable = originRunnable;
    this.runnable = runnable;
    this.taskName = taskName;
    this.traceId = MDC.get(TRACE_ID);  // 从 MDC 获取 traceId
}
```

### 关键代码

```java
import org.slf4j.MDC;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.TRACE_ID;

// TRACE_ID 常量定义为 "traceId"
public static final String TRACE_ID = "traceId";

// 在构造方法中获取 traceId
this.traceId = MDC.get(TRACE_ID);
```

## MDC 的作用

### 1. 分布式追踪

MDC 中的 `traceId` 用于分布式追踪，可以在整个请求链路中追踪请求：

```java
// 在请求入口设置 traceId
MDC.put("traceId", "123-456-789");

// 在异步任务中获取 traceId（通过 DtpRunnable）
DtpRunnable dtpRunnable = new DtpRunnable(...);
String traceId = dtpRunnable.getTraceId();  // 获取 "123-456-789"

// 日志中自动包含 traceId
log.info("处理任务");  // 日志输出: [traceId: 123-456-789] 处理任务
```

### 2. 日志关联

通过 `traceId` 可以将同一请求的所有日志关联起来：

```
[traceId: 123-456-789] 接收请求
[traceId: 123-456-789] 提交任务到线程池
[traceId: 123-456-789] 任务开始执行
[traceId: 123-456-789] 任务执行完成
```

### 3. 问题排查

当出现问题时，可以通过 `traceId` 快速定位相关日志：

```java
// 通过 traceId 搜索日志
grep "123-456-789" application.log
```

## DtpRunnable 中 MDC 的工作流程

### 1. 任务创建时

```java
// 主线程中，MDC 已经设置了 traceId
MDC.put("traceId", "123-456-789");

// 创建 DtpRunnable 时，从 MDC 获取 traceId
DtpRunnable dtpRunnable = new DtpRunnable(originalTask, wrappedTask, "myTask");
// dtpRunnable.traceId = "123-456-789"
```

### 2. 任务执行时

```java
// 在异步线程中执行任务
executor.execute(dtpRunnable);

// 可以通过 DtpRunnable 获取 traceId
String traceId = dtpRunnable.getTraceId();  // "123-456-789"
```

### 3. 日志输出

```java
// 在任务执行过程中记录日志
log.info("任务执行中, traceId: {}", dtpRunnable.getTraceId());
// 输出: [traceId: 123-456-789] 任务执行中, traceId: 123-456-789
```

## MDC 与 MdcRunnable 的关系

### 区别

| 特性 | DtpRunnable | MdcRunnable |
|------|------------|-------------|
| **作用** | 记录 traceId | 传递 MDC 上下文 |
| **时机** | 构造时获取 | 执行时传递 |
| **方式** | 从 MDC 读取并保存 | 复制并传递 MDC |
| **用途** | 任务追踪 | 上下文传递 |

### 配合使用

```java
// 1. 主线程设置 MDC
MDC.put("traceId", "123-456-789");
MDC.put("userId", "user123");

// 2. 使用 MdcTaskWrapper 传递 MDC（自动使用 MdcRunnable）
executor.execute(task);  // MdcRunnable 会传递所有 MDC 值

// 3. 在异步任务中
// - MDC 中已经有 traceId 和 userId
// - 可以通过 DtpRunnable 获取 traceId（如果任务被包装为 DtpRunnable）
```

## 实际应用场景

### 场景 1: 分布式追踪

```java
// 请求入口
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable String id) {
        // 设置 traceId
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        
        // 异步处理
        executor.execute(() -> {
            // 通过 DtpRunnable 获取 traceId
            // 或通过 MdcRunnable 传递的 MDC 获取
            String currentTraceId = MDC.get("traceId");
            processUser(id);
        });
        
        return user;
    }
}
```

### 场景 2: 日志关联

```java
// 主线程
MDC.put("traceId", "123-456-789");
log.info("开始处理请求");  // [traceId: 123-456-789] 开始处理请求

// 异步任务（通过 MdcRunnable 传递 MDC）
executor.execute(() -> {
    log.info("处理任务");  // [traceId: 123-456-789] 处理任务
    // 所有日志都包含相同的 traceId
});
```

### 场景 3: 问题排查

```java
// 当出现问题时，通过 traceId 查找相关日志
// 1. 从错误日志中获取 traceId
// 2. 使用 traceId 搜索所有相关日志
grep "123-456-789" application.log

// 输出所有相关日志：
// [traceId: 123-456-789] 接收请求
// [traceId: 123-456-789] 提交任务
// [traceId: 123-456-789] 任务执行失败: ...
```

## 日志配置示例

### Logback 配置

在 `logback.xml` 中配置 MDC 输出：

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

**输出示例**:
```
2024-01-01 10:00:00 [123-456-789] INFO  com.example.Service - 处理任务
```

### Log4j2 配置

在 `log4j2.xml` 中配置 MDC 输出：

```xml
<PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n"/>
```

## 注意事项

### 1. MDC 是线程本地的

MDC 是线程本地的，不同线程的 MDC 是独立的：

```java
// 主线程
MDC.put("traceId", "123");

// 异步线程（如果没有传递 MDC）
executor.execute(() -> {
    String traceId = MDC.get("traceId");  // null，因为异步线程的 MDC 是空的
});
```

**解决方案**: 使用 `MdcRunnable` 或 `MdcTaskWrapper` 传递 MDC。

### 2. DtpRunnable 只获取，不传递

`DtpRunnable` 只在构造时从 MDC 获取 `traceId`，不会传递 MDC 到异步线程：

```java
// 主线程
MDC.put("traceId", "123");

// 创建 DtpRunnable
DtpRunnable dtpRunnable = new DtpRunnable(...);
// dtpRunnable.traceId = "123"（已保存）

// 异步线程中
executor.execute(dtpRunnable);
// MDC.get("traceId") 可能是 null（如果没有使用 MdcRunnable）
// 但可以通过 dtpRunnable.getTraceId() 获取
```

### 3. 需要配合 MdcRunnable 使用

如果需要 MDC 在异步线程中可用，需要配合 `MdcRunnable` 或 `MdcTaskWrapper` 使用：

```java
// 配置任务包装器
executor.setTaskWrappers(Arrays.asList(new MdcTaskWrapper()));

// 或手动使用 MdcRunnable
executor.execute(MdcRunnable.get(task));
```

### 4. traceId 的获取时机

`DtpRunnable` 在构造时获取 `traceId`，此时必须在主线程中，且 MDC 中必须有 `traceId`：

```java
// 必须在创建 DtpRunnable 之前设置 traceId
MDC.put("traceId", "123-456-789");
DtpRunnable dtpRunnable = new DtpRunnable(...);  // 此时获取 traceId
```

## 最佳实践

### 1. 在请求入口设置 traceId

```java
@RestController
public class ApiController {
    @GetMapping("/api")
    public Response api() {
        // 从请求头获取或生成 traceId
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        
        // 设置到 MDC
        MDC.put("traceId", traceId);
        
        try {
            // 处理请求
            return process();
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}
```

### 2. 使用 MdcTaskWrapper 传递 MDC

```java
// 配置线程池时添加 MdcTaskWrapper
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .taskWrapper(new MdcTaskWrapper())  // 自动传递 MDC
    .buildDynamic();
```

### 3. 在日志中输出 traceId

配置日志框架，自动在日志中输出 `traceId`：

```xml
<!-- Logback -->
<pattern>%d [%X{traceId}] %-5level %logger - %msg%n</pattern>
```

### 4. 通过 DtpRunnable 获取 traceId

如果需要在不使用 MDC 的情况下获取 `traceId`，可以通过 `DtpRunnable`：

```java
// 如果任务被包装为 DtpRunnable
if (runnable instanceof DtpRunnable) {
    DtpRunnable dtpRunnable = (DtpRunnable) runnable;
    String traceId = dtpRunnable.getTraceId();
    // 使用 traceId
}
```

## 总结

### MDC 的作用

1. **分布式追踪**: 通过 `traceId` 追踪整个请求链路
2. **日志关联**: 将同一请求的所有日志关联起来
3. **问题排查**: 通过 `traceId` 快速定位相关日志

### DtpRunnable 中 MDC 的使用

1. **获取 traceId**: 在构造时从 MDC 获取 `traceId` 并保存
2. **任务追踪**: 通过保存的 `traceId` 追踪任务
3. **日志关联**: 在日志中输出 `traceId`，关联任务日志

### 关键点

- ✅ MDC 是线程本地的，需要特殊处理才能在异步线程中使用
- ✅ `DtpRunnable` 只获取 `traceId`，不传递 MDC
- ✅ 需要配合 `MdcRunnable` 或 `MdcTaskWrapper` 才能传递 MDC
- ✅ `traceId` 必须在创建 `DtpRunnable` 之前设置到 MDC 中

