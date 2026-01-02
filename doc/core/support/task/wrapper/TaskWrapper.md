# TaskWrapper

## 概述

`TaskWrapper` 是任务包装器接口，用于增强任务功能。它提供了一种机制，可以在任务提交到线程池时对任务进行包装，实现上下文传递、链路追踪等功能。

## 核心作用

1. **任务增强**: 在任务执行前对任务进行包装，增强功能
2. **上下文传递**: 支持传递线程本地变量、MDC、链路追踪等上下文信息
3. **可扩展**: 通过 SPI 机制支持自定义包装器

## 接口定义

```java
@FunctionalInterface
public interface TaskWrapper {
    default String name() {
        return null;
    }
    
    Runnable wrap(Runnable runnable);
}
```

## 核心方法

### name()

**作用**: 返回包装器的名称

**返回**: 包装器名称（用于配置）

**默认实现**: 返回 `null`

**说明**: 用于在配置中指定需要使用的包装器

---

### wrap(Runnable runnable)

**作用**: 包装任务

**参数**: `runnable` - 原始任务

**返回**: 包装后的任务

**说明**: 这是函数式接口的核心方法，用于对任务进行包装

## 内置实现

### 1. TtlTaskWrapper

**名称**: `ttl`

**作用**: 支持 TransmittableThreadLocal（TTL）传递

**实现**:
```java
@Override
public Runnable wrap(Runnable runnable) {
    return TtlRunnable.get(runnable);
}
```

**使用场景**: 需要在异步任务中传递 ThreadLocal 值

---

### 2. MdcTaskWrapper

**名称**: `mdc`

**作用**: 支持 MDC（Mapped Diagnostic Context）传递

**实现**:
```java
@Override
public Runnable wrap(Runnable runnable) {
    return MdcRunnable.get(runnable);
}
```

**使用场景**: 需要在异步任务中传递日志上下文（如 traceId）

---

### 3. 扩展实现

框架还支持通过扩展实现其他包装器：

- **SkyWalking**: `SwTraceTaskWrapper`（在 extension-skywalking 模块中）
- **OpenTelemetry**: `OpenTelemetryTaskWrapper`（在 extension-opentelemetry 模块中）

## 使用方式

### 1. 配置方式

在线程池配置中指定任务包装器：

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          task-wrapper-names: [ttl, mdc]  # 指定使用的包装器
```

### 2. 编程方式

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .taskWrapper(new TtlTaskWrapper())
    .taskWrapper(new MdcTaskWrapper())
    .buildDynamic();
```

### 3. 链式包装

多个包装器会按顺序链式包装：

```java
Runnable task = ...;
task = ttlWrapper.wrap(task);      // 第一层包装
task = mdcWrapper.wrap(task);      // 第二层包装
executor.execute(task);
```

## 工作原理

### 1. 任务包装流程

```
原始任务
    │
    └─> TaskWrapper1.wrap()
        │
        └─> 包装后的任务1
            │
            └─> TaskWrapper2.wrap()
                │
                └─> 包装后的任务2
                    │
                    └─> 提交到线程池
```

### 2. 执行流程

```
线程池执行任务
    │
    └─> 包装后的任务2.run()
        │
        └─> 包装后的任务1.run()
            │
            └─> 原始任务.run()
```

## 自定义包装器

### 实现示例

```java
public class CustomTaskWrapper implements TaskWrapper {
    
    @Override
    public String name() {
        return "custom";
    }
    
    @Override
    public Runnable wrap(Runnable runnable) {
        return () -> {
            // 包装前逻辑
            try {
                // 设置上下文
                setContext();
                // 执行原始任务
                runnable.run();
            } finally {
                // 包装后逻辑
                clearContext();
            }
        };
    }
}
```

### SPI 注册

创建 `META-INF/services/org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper` 文件：

```
com.example.CustomTaskWrapper
```

## 设计特点

### 1. 函数式接口

`TaskWrapper` 是函数式接口，可以使用 Lambda 表达式：

```java
TaskWrapper wrapper = runnable -> {
    // 包装逻辑
    return new Runnable() {
        @Override
        public void run() {
            runnable.run();
        }
    };
};
```

### 2. 链式包装

支持多个包装器链式包装，每个包装器可以增强不同的功能。

### 3. 可配置

通过配置指定需要使用的包装器，灵活控制功能。

## 使用场景

### 1. 上下文传递

```java
// 主线程设置上下文
MDC.put("traceId", "123");

// 异步任务中获取上下文
executor.execute(() -> {
    String traceId = MDC.get("traceId");  // 需要 MdcTaskWrapper
    // 使用 traceId
});
```

### 2. 链路追踪

```java
// 主线程有链路上下文
Span span = tracer.nextSpan();

// 异步任务中传递链路上下文
executor.execute(() -> {
    // 需要相应的 TaskWrapper 传递上下文
    // 继续链路追踪
});
```

### 3. 线程本地变量传递

```java
// 主线程设置 ThreadLocal
ThreadLocal<String> context = new ThreadLocal<>();
context.set("value");

// 异步任务中获取 ThreadLocal（需要 TtlTaskWrapper）
executor.execute(() -> {
    String value = context.get();  // 需要 TTL 支持
});
```

## 注意事项

1. **包装顺序**: 包装器的顺序会影响执行顺序，需要注意
2. **性能影响**: 每个包装器都会增加一层调用，注意性能影响
3. **异常处理**: 包装器中的异常处理要谨慎，避免影响原始任务
4. **上下文清理**: 确保在任务执行后清理上下文，避免内存泄漏

## 与 Spring 的对比

| 特性 | TaskWrapper | Spring @Async |
|------|------------|---------------|
| **上下文传递** | 支持（通过包装器） | 部分支持 |
| **可扩展性** | 高（SPI 机制） | 较低 |
| **使用方式** | 配置或编程 | 注解 |
| **功能** | 更强大 | 基础功能 |

