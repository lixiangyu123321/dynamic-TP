# TransmittableThreadLocal (TTL) 原理和使用

## 概述

`TransmittableThreadLocal`（TTL）是阿里巴巴开源的一个 Java 库，用于解决 `ThreadLocal` 在线程池场景下无法传递的问题。它允许将父线程的 `ThreadLocal` 值传递到子线程（包括线程池中的线程）。

## 为什么需要 TTL？

### ThreadLocal 的问题

`ThreadLocal` 是 Java 提供的线程本地变量，每个线程有独立的变量副本。但在使用线程池时，`ThreadLocal` 会遇到以下问题：

#### 问题 1: 线程复用导致上下文丢失

```java
// 定义 ThreadLocal
ThreadLocal<String> context = new ThreadLocal<>();

// 主线程设置值
context.set("value1");

// 提交任务到线程池
executor.execute(() -> {
    String value = context.get();  // null！获取不到值
    // 因为线程池中的线程是复用的，可能之前被其他任务使用过
});
```

#### 问题 2: 线程切换导致上下文丢失

```java
// 主线程
ThreadLocal<String> context = new ThreadLocal<>();
context.set("value1");

// 异步执行
executor.execute(() -> {
    // 此时运行在线程池的线程中，不是主线程
    String value = context.get();  // null！获取不到值
    // 因为 ThreadLocal 是线程本地的，不同线程无法访问
});
```

### TTL 的解决方案

`TransmittableThreadLocal` 通过以下方式解决这些问题：

1. **捕获上下文**: 在任务提交时捕获父线程的所有 TTL 值
2. **传递上下文**: 在任务执行时恢复 TTL 值到子线程
3. **清理上下文**: 任务执行后清理 TTL 值，避免污染

## TTL 的原理

### 核心概念

#### 1. TransmittableThreadLocal

`TransmittableThreadLocal` 继承自 `InheritableThreadLocal`，但提供了额外的传递能力：

```java
public class TransmittableThreadLocal<T> extends InheritableThreadLocal<T> {
    // ...
}
```

#### 2. TtlRunnable / TtlCallable

用于包装 `Runnable` 和 `Callable`，实现上下文传递：

```java
// 包装 Runnable
Runnable ttlRunnable = TtlRunnable.get(originalRunnable);

// 包装 Callable
Callable<T> ttlCallable = TtlCallable.get(originalCallable);
```

#### 3. TtlExecutors

用于包装 `ExecutorService`，自动处理所有提交的任务：

```java
ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
```

### 工作原理

#### 1. 任务包装阶段

```java
// 主线程中
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
context.set("value1");

// 包装任务
Runnable ttlRunnable = TtlRunnable.get(originalRunnable);
// 此时会捕获当前线程的所有 TTL 值
```

**内部实现**:
- `TtlRunnable.get()` 会调用 `Transmitter.capture()` 捕获所有 TTL 值
- 将捕获的值存储在 `TtlRunnable` 对象中

#### 2. 任务执行阶段

```java
// 在线程池的线程中执行
ttlRunnable.run();
// 此时会恢复父线程的 TTL 值
```

**内部实现**:
- `TtlRunnable.run()` 会调用 `Transmitter.replay()` 恢复 TTL 值
- 在子线程中设置 TTL 值
- 执行原始任务
- 调用 `Transmitter.restore()` 清理 TTL 值

#### 3. 执行流程

```
主线程
  │
  ├─> 设置 TTL 值: context.set("value1")
  │
  ├─> 包装任务: TtlRunnable.get(task)
  │   │
  │   └─> 捕获 TTL 值: Transmitter.capture()
  │       └─> 保存到 TtlRunnable 对象
  │
  └─> 提交任务: executor.execute(ttlRunnable)
      │
      └─> 线程池线程执行
          │
          ├─> 恢复 TTL 值: Transmitter.replay()
          │   └─> 在子线程中设置 TTL 值
          │
          ├─> 执行任务: originalRunnable.run()
          │   └─> 此时可以获取到 TTL 值: context.get() = "value1"
          │
          └─> 清理 TTL 值: Transmitter.restore()
              └─> 恢复子线程的原始 TTL 值
```

### 关键机制

#### 1. 捕获机制（Capture）

```java
// 捕获当前线程的所有 TTL 值
Map<TransmittableThreadLocal<?>, Object> captured = Transmitter.capture();
```

**作用**: 在任务提交时，捕获父线程的所有 TTL 值，保存为快照。

#### 2. 回放机制（Replay）

```java
// 在子线程中恢复 TTL 值
Object backup = Transmitter.replay(captured);
```

**作用**: 在任务执行时，将捕获的 TTL 值恢复到子线程中。

#### 3. 恢复机制（Restore）

```java
// 恢复子线程的原始 TTL 值
Transmitter.restore(backup);
```

**作用**: 在任务执行后，清理恢复的 TTL 值，恢复子线程的原始状态。

## 在项目中的使用

### 方式 1: 使用 TtlTaskWrapper

#### 配置方式

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          task-wrapper-names: [ttl]
```

#### 编程方式

```java
DtpExecutor executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .taskWrapper(new TtlTaskWrapper())
    .buildDynamic();
```

#### 实现原理

```java
public class TtlTaskWrapper implements TaskWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
        return TtlRunnable.get(runnable);  // 包装为 TtlRunnable
    }
}
```

---

### 方式 2: 使用 buildWithTtl()

#### 动态线程池

```java
ExecutorService executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .buildWithTtl();  // 自动添加 TtlTaskWrapper
```

**实现**:
```java
public ExecutorService buildWithTtl() {
    if (dynamic) {
        taskWrappers.add(TtlRunnable::get);  // 添加 TTL 包装器
        return buildDtpExecutor(this);
    } else {
        return TtlExecutors.getTtlExecutorService(buildCommonExecutor(this));
    }
}
```

#### 普通线程池

```java
ExecutorService executor = ThreadPoolCreator.createCommonWithTtl("thread");
// 或
ExecutorService executor = ThreadPoolBuilder.newBuilder()
    .dynamic(false)
    .buildWithTtl();  // 使用 TtlExecutors 包装
```

**实现**:
```java
return TtlExecutors.getTtlExecutorService(buildCommonExecutor(this));
```

---

### 方式 3: 直接使用 TtlRunnable

```java
// 定义 TTL
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 主线程设置值
context.set("value1");

// 包装任务
Runnable task = () -> {
    String value = context.get();  // 可以获取到 "value1"
    // 使用 value
};

Runnable ttlTask = TtlRunnable.get(task);

// 提交任务
executor.execute(ttlTask);
```

---

### 方式 4: 使用 TtlExecutors 包装线程池

```java
// 创建普通线程池
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);

// 包装为支持 TTL 的线程池
ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);

// 使用 TTL
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
context.set("value1");

// 提交任务（自动支持 TTL）
ttlExecutor.execute(() -> {
    String value = context.get();  // 可以获取到 "value1"
});
```

## 使用示例

### 示例 1: 用户上下文传递

```java
// 定义用户上下文
TransmittableThreadLocal<User> userContext = new TransmittableThreadLocal<>();

// 在请求处理中设置用户
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable String id) {
        // 设置当前用户
        User currentUser = getCurrentUser();
        userContext.set(currentUser);
        
        // 异步处理（使用支持 TTL 的线程池）
        executor.execute(() -> {
            // 可以获取到用户信息
            User user = userContext.get();
            processUser(user);
        });
        
        return user;
    }
}
```

### 示例 2: 请求 ID 传递

```java
// 定义请求 ID 上下文
TransmittableThreadLocal<String> requestIdContext = new TransmittableThreadLocal<>();

// 在请求入口设置请求 ID
@RestController
public class ApiController {
    @GetMapping("/api")
    public Response api(HttpServletRequest request) {
        // 从请求头获取或生成请求 ID
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        requestIdContext.set(requestId);
        
        try {
            // 异步处理
            executor.execute(() -> {
                String id = requestIdContext.get();  // 获取请求 ID
                log.info("处理请求, requestId: {}", id);
                processRequest();
            });
            
            return response;
        } finally {
            requestIdContext.remove();  // 清理
        }
    }
}
```

### 示例 3: 配置线程池支持 TTL

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    public DtpExecutor asyncExecutor() {
        return ThreadPoolBuilder.newBuilder()
            .threadPoolName("asyncExecutor")
            .corePoolSize(10)
            .maximumPoolSize(20)
            .taskWrapper(new TtlTaskWrapper())  // 添加 TTL 支持
            .buildDynamic();
    }
    
    // 或使用 buildWithTtl()
    @Bean
    public ExecutorService ttlExecutor() {
        return ThreadPoolBuilder.newBuilder()
            .threadPoolName("ttlExecutor")
            .buildWithTtl();  // 自动支持 TTL
    }
}
```

## 与 ThreadLocal 的对比

| 特性 | ThreadLocal | TransmittableThreadLocal |
|------|------------|-------------------------|
| **线程本地** | ✅ 支持 | ✅ 支持 |
| **线程池传递** | ❌ 不支持 | ✅ 支持 |
| **异步传递** | ❌ 不支持 | ✅ 支持 |
| **性能** | 更高 | 略低（有传递开销） |
| **使用方式** | 直接使用 | 需要包装任务或线程池 |
| **内存泄漏风险** | 有 | 有（需要正确清理） |

## 注意事项

### 1. 必须使用 TransmittableThreadLocal

不能使用普通的 `ThreadLocal`，必须使用 `TransmittableThreadLocal`：

```java
// ❌ 错误：普通 ThreadLocal 无法传递
ThreadLocal<String> context = new ThreadLocal<>();

// ✅ 正确：使用 TransmittableThreadLocal
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
```

### 2. 需要包装任务或线程池

TTL 不会自动工作，需要包装任务或线程池：

```java
// ❌ 错误：直接提交任务，TTL 无法传递
executor.execute(() -> {
    String value = context.get();  // null
});

// ✅ 正确：包装任务
Runnable ttlTask = TtlRunnable.get(() -> {
    String value = context.get();  // 可以获取到值
});
executor.execute(ttlTask);

// ✅ 正确：包装线程池
ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
ttlExecutor.execute(() -> {
    String value = context.get();  // 可以获取到值
});
```

### 3. 及时清理上下文

虽然 TTL 会自动清理，但也要注意及时清理不需要的上下文：

```java
try {
    context.set("value");
    // 使用 context
} finally {
    context.remove();  // 清理
}
```

### 4. 性能影响

TTL 传递会有一定的性能开销：

- **捕获开销**: 在任务提交时捕获 TTL 值
- **恢复开销**: 在任务执行时恢复 TTL 值
- **清理开销**: 在任务执行后清理 TTL 值

对于高频任务，需要考虑性能影响。

### 5. 内存泄漏风险

如果 TTL 值持有大对象或引用，可能导致内存泄漏：

```java
// 注意：避免持有大对象
TransmittableThreadLocal<LargeObject> context = new TransmittableThreadLocal<>();
context.set(largeObject);  // 可能导致内存泄漏

// 建议：只传递必要的轻量级数据
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
context.set("lightweight-data");
```

## 最佳实践

### 1. 明确使用场景

只有在需要传递线程本地变量时才使用 TTL：

- ✅ **适合**: 用户上下文、请求 ID、追踪 ID 等轻量级数据
- ❌ **不适合**: 大对象、频繁访问的数据

### 2. 统一配置

在框架层面统一配置 TTL 支持：

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: asyncExecutor
          task-wrapper-names: [ttl]  # 统一支持 TTL
```

### 3. 配合其他包装器使用

可以与其他包装器配合使用：

```yaml
task-wrapper-names: [ttl, mdc]  # 同时支持 TTL 和 MDC
```

### 4. 使用 buildWithTtl() 简化配置

对于新创建的线程池，使用 `buildWithTtl()` 简化配置：

```java
ExecutorService executor = ThreadPoolBuilder.newBuilder()
    .threadPoolName("myPool")
    .buildWithTtl();  // 自动支持 TTL
```

### 5. 注意清理

在请求处理完成后清理 TTL 值：

```java
try {
    context.set("value");
    // 处理请求
} finally {
    context.remove();  // 确保清理
}
```

## 实现细节

### TtlRunnable 的实现

```java
public final class TtlRunnable implements Runnable {
    private final Runnable runnable;
    private final Object captured;  // 捕获的 TTL 值
    
    public static TtlRunnable get(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        if (runnable instanceof TtlRunnable) {
            return (TtlRunnable) runnable;
        }
        return new TtlRunnable(runnable, Transmitter.capture());
    }
    
    @Override
    public void run() {
        Object backup = Transmitter.replay(captured);  // 恢复 TTL 值
        try {
            runnable.run();  // 执行任务
        } finally {
            Transmitter.restore(backup);  // 清理 TTL 值
        }
    }
}
```

### TtlExecutors 的实现

```java
public class TtlExecutors {
    public static ExecutorService getTtlExecutorService(ExecutorService executorService) {
        if (executorService == null) {
            return null;
        }
        if (executorService instanceof TtlExecutorService) {
            return executorService;
        }
        return new TtlExecutorService(executorService);
    }
}

// TtlExecutorService 会自动包装所有提交的任务
class TtlExecutorService implements ExecutorService {
    private final ExecutorService executorService;
    
    @Override
    public void execute(Runnable command) {
        executorService.execute(TtlRunnable.get(command));  // 自动包装
    }
    
    // 其他方法类似...
}
```

## 总结

### TTL 的核心价值

1. **解决线程池上下文传递问题**: 让 `ThreadLocal` 在线程池场景下也能正常工作
2. **简化异步编程**: 无需手动传递上下文，自动传递 TTL 值
3. **支持分布式追踪**: 可以在异步任务中保持追踪 ID 等上下文信息

### 使用要点

- ✅ 使用 `TransmittableThreadLocal` 而不是 `ThreadLocal`
- ✅ 包装任务（`TtlRunnable`）或线程池（`TtlExecutors`）
- ✅ 及时清理不需要的上下文
- ✅ 注意性能影响，避免传递大对象

### 在项目中的应用

- **TtlTaskWrapper**: 框架提供的任务包装器，自动支持 TTL
- **buildWithTtl()**: 快速创建支持 TTL 的线程池
- **统一配置**: 通过配置统一启用 TTL 支持

TTL 是解决线程池场景下上下文传递问题的优秀方案，在分布式追踪、用户上下文传递等场景中非常有用。

