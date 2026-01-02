# EnhancedRunnable

## 概述

`EnhancedRunnable` 是增强的 Runnable 实现，用于为没有 `beforeExecute` 和 `afterExecute` 方法的线程池提供感知器支持。它会在任务执行前后触发感知器事件。

## 核心作用

1. **感知器支持**: 为不支持 `beforeExecute`/`afterExecute` 的线程池提供感知器功能
2. **任务增强**: 在任务执行前后触发感知器事件
3. **异常处理**: 正确处理异常并传递给感知器

## 核心属性

```java
private final Runnable runnable;  // 原始任务
private final Executor executor;  // 执行器
```

## 核心方法

### 构造方法

```java
public EnhancedRunnable(Runnable runnable, Executor executor) {
    this.runnable = runnable;
    this.executor = executor;
}
```

---

### of(Runnable runnable, Executor executor)

**作用**: 创建 `EnhancedRunnable` 的静态工厂方法

```java
public static EnhancedRunnable of(Runnable runnable, Executor executor) {
    return new EnhancedRunnable(runnable, executor);
}
```

---

### run()

**作用**: 执行任务，触发感知器事件

**实现**:
```java
@Override
public void run() {
    if (Objects.isNull(runnable)) {
        return;  // 任务为空，直接返回
    }
    
    // 触发任务执行前事件
    AwareManager.beforeExecute(executor, Thread.currentThread(), runnable);
    
    Throwable t = null;
    try {
        runnable.run();  // 执行任务
    } catch (Exception e) {
        t = e;  // 记录异常
        throw e;  // 重新抛出异常
    } finally {
        // 触发任务执行后事件
        AwareManager.afterExecute(executor, runnable, t);
        ExecutorUtil.tryExecAfterExecute(runnable, t);
    }
}
```

**执行流程**:

1. **空值检查**: 如果任务为空，直接返回

2. **任务执行前**: 调用 `AwareManager.beforeExecute()` 触发感知器事件

3. **执行任务**: 在 try 块中执行任务

4. **异常处理**: 如果发生异常，记录异常并重新抛出

5. **任务执行后**: 在 finally 块中调用 `AwareManager.afterExecute()` 触发感知器事件

6. **后处理**: 调用 `ExecutorUtil.tryExecAfterExecute()` 执行后处理逻辑

## 使用场景

### 1. 增强不支持 beforeExecute/afterExecute 的线程池

某些线程池（如 `ForkJoinPool`）不支持 `beforeExecute`/`afterExecute`，可以使用 `EnhancedRunnable` 提供感知器支持：

```java
Executor executor = new ForkJoinPool();
Runnable task = () -> processData();
EnhancedRunnable enhancedTask = EnhancedRunnable.of(task, executor);
executor.execute(enhancedTask);
```

### 2. 自定义执行器

为自定义执行器提供感知器支持：

```java
public class MyExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        // 使用 EnhancedRunnable 提供感知器支持
        EnhancedRunnable enhanced = EnhancedRunnable.of(command, this);
        // 执行增强后的任务
        doExecute(enhanced);
    }
}
```

## 设计特点

### 1. 异常传递

正确处理异常，将异常传递给 `afterExecute`：

```java
Throwable t = null;
try {
    runnable.run();
} catch (Exception e) {
    t = e;
    throw e;
} finally {
    AwareManager.afterExecute(executor, runnable, t);
}
```

### 2. 感知器支持

通过 `AwareManager` 触发感知器事件，支持监控、超时等功能。

### 3. 后处理支持

调用 `ExecutorUtil.tryExecAfterExecute()` 执行后处理逻辑。

## 注意事项

1. **性能影响**: 会增加方法调用层次，但开销很小
2. **异常处理**: 异常会被重新抛出，不会吞掉异常
3. **感知器功能**: 支持感知器的所有功能（监控、超时等）

