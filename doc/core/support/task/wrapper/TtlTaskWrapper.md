# TtlTaskWrapper

## 概述

`TtlTaskWrapper` 是 TTL（TransmittableThreadLocal）任务包装器，用于在异步任务中传递 TransmittableThreadLocal 的值。它基于阿里巴巴的 TransmittableThreadLocal 库实现。

> 💡 **TTL 原理和使用**: 想深入了解 TransmittableThreadLocal 的原理、工作机制和使用方式？请查看 [TransmittableThreadLocal-原理和使用](TransmittableThreadLocal-原理和使用.md)

## 核心作用

1. **TTL 传递**: 将主线程的 TransmittableThreadLocal 值传递到异步任务
2. **线程本地变量传递**: 支持在异步任务中访问主线程的 ThreadLocal 值
3. **自动清理**: 任务执行后自动清理 TTL 上下文

## 实现

```java
public class TtlTaskWrapper implements TaskWrapper {
    
    private static final String NAME = "ttl";
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public Runnable wrap(Runnable runnable) {
        return TtlRunnable.get(runnable);
    }
}
```

## 工作原理

### 1. 包装任务

`TtlTaskWrapper` 将原始任务包装为 `TtlRunnable`：

```java
Runnable wrapped = TtlRunnable.get(originalRunnable);
```

### 2. TtlRunnable 实现

`TtlRunnable` 会：

1. **捕获 TTL 上下文**: 在包装时捕获当前线程的所有 TransmittableThreadLocal 值
2. **传递上下文**: 在任务执行时恢复 TTL 上下文
3. **清理上下文**: 任务执行后清理 TTL 上下文

## 使用场景

### 1. 线程本地变量传递

```java
// 定义 TransmittableThreadLocal
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 主线程设置值
context.set("value");

// 异步任务中获取值
executor.execute(() -> {
    String value = context.get();  // 可以获取到 "value"
    // 使用 value
});
```

### 2. 用户上下文传递

```java
// 用户上下文
TransmittableThreadLocal<User> userContext = new TransmittableThreadLocal<>();

// 设置当前用户
userContext.set(currentUser);

// 异步任务中获取用户
executor.execute(() -> {
    User user = userContext.get();  // 可以获取到当前用户
    // 使用用户信息
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
          task-wrapper-names: [ttl]
```

### 2. 编程方式

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .taskWrapper(new TtlTaskWrapper())
    .buildDynamic();
```

### 3. 使用 buildWithTtl()

```java
ExecutorService executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .buildWithTtl();  // 自动添加 TtlTaskWrapper
```

## 与普通 ThreadLocal 的区别

| 特性 | ThreadLocal | TransmittableThreadLocal |
|------|------------|-------------------------|
| **异步传递** | 不支持 | 支持 |
| **线程池传递** | 不支持 | 支持 |
| **使用方式** | 直接使用 | 需要 TtlTaskWrapper |
| **性能** | 更高 | 略低 |

## 注意事项

1. **依赖**: 需要引入 `transmittable-thread-local` 依赖
2. **类型**: 必须使用 `TransmittableThreadLocal`，不能使用普通 `ThreadLocal`
3. **性能影响**: TTL 传递会有一定的性能开销
4. **上下文清理**: `TtlRunnable` 会自动清理上下文

## 最佳实践

1. **明确需求**: 只有在需要传递线程本地变量时才使用
2. **合理使用**: 避免传递大量数据，影响性能
3. **及时清理**: 虽然会自动清理，但也要注意及时清理不需要的上下文
4. **配合使用**: 可以与其他包装器配合使用（如 MDC）

