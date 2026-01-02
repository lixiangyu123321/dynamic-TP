# DtpLifecycleSupport

## 概述

`DtpLifecycleSupport` 是线程池生命周期支持工具类，主要实现线程池的生命周期管理功能，包括初始化、优雅关闭等。它提供了静态方法来管理线程池的启动和关闭过程。

## 核心作用

1. **初始化管理**: 提供线程池初始化的统一入口
2. **优雅关闭**: 实现线程池的优雅关闭逻辑
3. **异步关闭**: 支持异步关闭线程池

## 核心方法

### 1. initialize(ExecutorWrapper executorWrapper)

**作用**: 初始化线程池包装器

**实现**:
```java
public static void initialize(ExecutorWrapper executorWrapper) {
    executorWrapper.initialize();
}
```

**说明**:
- 调用 `ExecutorWrapper.initialize()` 方法
- 如果是 `DtpExecutor`，会调用其 `initialize()` 方法
- 注册到 `AwareManager`，启用感知器功能

**使用场景**: 线程池注册到框架时调用

---

### 2. destroy(ExecutorWrapper executorWrapper)

**作用**: 销毁线程池包装器，执行优雅关闭

**实现**:
```java
public static void destroy(ExecutorWrapper executorWrapper) {
    if (executorWrapper.isExecutorService()) {
        ExecutorService executorService = (ExecutorService) executorWrapper.getExecutor().getOriginal();
        internalShutdown(executorService,
                executorWrapper.getThreadPoolName(),
                executorWrapper.isWaitForTasksToCompleteOnShutdown(),
                executorWrapper.getAwaitTerminationSeconds());
    }
}
```

**说明**:
- 检查是否为 `ExecutorService`
- 调用 `internalShutdown()` 执行关闭逻辑
- 使用包装器的配置（是否等待任务完成、等待时间等）

**使用场景**: Spring 容器关闭时，框架生命周期结束时

---

### 3. shutdownGracefulAsync(ExecutorService executor, String threadPoolName, int timeout)

**作用**: 异步优雅关闭线程池

**实现**:
```java
public static void shutdownGracefulAsync(ExecutorService executor,
                                         String threadPoolName,
                                         int timeout) {
    ExecutorService tmpExecutor = Executors.newSingleThreadExecutor();
    tmpExecutor.execute(() -> internalShutdown(executor, threadPoolName, true, timeout));
    tmpExecutor.shutdown();
}
```

**说明**:
- 创建临时单线程执行器
- 在临时执行器中异步执行关闭逻辑
- 关闭临时执行器

**使用场景**: 需要异步关闭线程池的场景

---

### 4. internalShutdown(ExecutorService executor, String threadPoolName, boolean waitForTasksToCompleteOnShutdown, int awaitTerminationSeconds)

**作用**: 执行线程池的内部关闭逻辑

**实现流程**:

1. **空值检查**:
   ```java
   if (Objects.isNull(executor)) {
       return;
   }
   ```

2. **记录日志**:
   ```java
   log.info("Shutting down ExecutorService, threadPoolName: {}", threadPoolName);
   ```

3. **选择关闭方式**:
   - 如果 `waitForTasksToCompleteOnShutdown = true`:
     ```java
     executor.shutdown();  // 优雅关闭，等待任务完成
     ```
   - 如果 `waitForTasksToCompleteOnShutdown = false`:
     ```java
     for (Runnable remainingTask : executor.shutdownNow()) {
         cancelRemainingTask(remainingTask);  // 立即关闭，取消未执行的任务
     }
     ```

4. **等待终止**:
   ```java
   awaitTerminationIfNecessary(executor, threadPoolName, awaitTerminationSeconds);
   ```

**参数说明**:
- `executor`: 要关闭的执行器
- `threadPoolName`: 线程池名称（用于日志）
- `waitForTasksToCompleteOnShutdown`: 是否等待任务完成
- `awaitTerminationSeconds`: 等待终止的最大秒数

---

### 5. cancelRemainingTask(Runnable task)

**作用**: 取消未执行的任务

**实现**:
```java
protected static void cancelRemainingTask(Runnable task) {
    if (task instanceof Future) {
        ((Future<?>) task).cancel(true);
    }
}
```

**说明**:
- 如果任务是 `Future` 类型，调用 `cancel(true)` 取消任务
- 参数 `true` 表示即使任务正在执行也尝试中断

**使用场景**: `shutdownNow()` 返回的任务列表中，取消未执行的任务

---

### 6. awaitTerminationIfNecessary(ExecutorService executor, String threadPoolName, int awaitTerminationSeconds)

**作用**: 等待执行器终止

**实现**:
```java
private static void awaitTerminationIfNecessary(ExecutorService executor,
                                                String threadPoolName,
                                                int awaitTerminationSeconds) {
    if (awaitTerminationSeconds <= 0) {
        return;  // 不等待
    }
    try {
        if (!executor.awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) {
            log.warn("Timed out while waiting for executor {} to terminate", threadPoolName);
        }
    } catch (InterruptedException ex) {
        log.warn("Interrupted while waiting for executor {} to terminate", threadPoolName);
        Thread.currentThread().interrupt();
    }
}
```

**说明**:
- 如果 `awaitTerminationSeconds <= 0`，不等待，直接返回
- 否则调用 `executor.awaitTermination()` 等待终止
- 如果超时，记录警告日志
- 如果被中断，恢复中断状态并记录警告

## 关闭策略

### 1. 优雅关闭（waitForTasksToCompleteOnShutdown = true）

**流程**:
1. 调用 `executor.shutdown()`，不再接受新任务
2. 等待已提交的任务执行完成
3. 等待队列中的任务执行完成
4. 在指定时间内等待线程池终止

**适用场景**: 
- 需要确保所有任务都执行完成
- 不能丢失任务

**优点**: 不丢失任务，数据一致性好

**缺点**: 关闭时间可能较长

---

### 2. 立即关闭（waitForTasksToCompleteOnShutdown = false）

**流程**:
1. 调用 `executor.shutdownNow()`，立即停止接受新任务
2. 尝试中断正在执行的任务
3. 返回未执行的任务列表
4. 取消未执行的任务（如果是 Future）
5. 在指定时间内等待线程池终止

**适用场景**:
- 需要快速关闭
- 可以接受任务丢失

**优点**: 关闭速度快

**缺点**: 可能丢失任务

## 使用场景

### 1. Spring 容器关闭

框架在 Spring 容器关闭时自动调用：

```java
@PreDestroy
public void destroy() {
    DtpLifecycleSupport.destroy(executorWrapper);
}
```

### 2. 框架生命周期管理

在 `DtpLifecycle` 中调用：

```java
@Override
public void stop() {
    DtpRegistry.getAllExecutors().forEach((k, v) -> 
        DtpLifecycleSupport.destroy(v)
    );
}
```

### 3. 手动关闭

```java
// 同步关闭
DtpLifecycleSupport.destroy(executorWrapper);

// 异步关闭
DtpLifecycleSupport.shutdownGracefulAsync(
    executorService, 
    "myPool", 
    5
);
```

## 配置说明

关闭行为由 `ExecutorWrapper` 的以下属性控制：

- **waitForTasksToCompleteOnShutdown**: 是否等待任务完成
- **awaitTerminationSeconds**: 等待终止的最大秒数

### 配置示例

```java
ExecutorWrapper wrapper = new ExecutorWrapper(executor);
wrapper.setWaitForTasksToCompleteOnShutdown(true);  // 等待任务完成
wrapper.setAwaitTerminationSeconds(10);              // 最多等待 10 秒
```

## 设计特点

### 1. 静态方法

所有方法都是静态方法，无需创建实例：

```java
DtpLifecycleSupport.initialize(wrapper);
DtpLifecycleSupport.destroy(wrapper);
```

### 2. 优雅关闭

支持优雅关闭，尽可能处理队列中的任务：

- 等待任务完成
- 设置超时时间
- 记录日志

### 3. 异常处理

对中断异常进行正确处理：

- 恢复中断状态
- 记录警告日志
- 不抛出异常

### 4. 日志记录

记录关闭过程的日志：

- 开始关闭
- 超时警告
- 中断警告

## 注意事项

1. **关闭顺序**: 建议在 Spring 容器关闭前关闭线程池
2. **超时设置**: 合理设置 `awaitTerminationSeconds`，避免等待时间过长
3. **任务丢失**: 如果使用立即关闭，可能丢失未执行的任务
4. **中断处理**: 被中断的任务需要正确处理中断异常

## 最佳实践

1. **生产环境**: 使用优雅关闭，确保任务完成
2. **测试环境**: 可以使用立即关闭，加快关闭速度
3. **超时设置**: 根据任务执行时间合理设置超时时间
4. **日志监控**: 关注关闭日志，及时发现超时问题

