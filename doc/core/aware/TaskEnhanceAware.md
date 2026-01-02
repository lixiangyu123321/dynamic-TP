# TaskEnhanceAware

## 概述

`TaskEnhanceAware` 是任务增强感知器接口，用于标识支持任务增强的执行器。它提供了任务包装的功能，可以将任务包装为 `DtpRunnable`，并应用任务包装器。

## 核心作用

1. **任务增强**: 提供任务增强功能
2. **任务包装**: 将任务包装为 `DtpRunnable`，并应用任务包装器
3. **接口标识**: 标识支持任务增强的执行器

## 接口定义

```java
public interface TaskEnhanceAware extends DtpAware {
    Runnable getEnhancedTask(Runnable command, List<TaskWrapper> taskWrappers);
    Runnable getEnhancedTask(Runnable command);
    List<TaskWrapper> getTaskWrappers();
    void setTaskWrappers(List<TaskWrapper> taskWrappers);
}
```

## 核心方法

### getEnhancedTask(Runnable command, List<TaskWrapper> taskWrappers)

**作用**: 增强任务，应用任务包装器

**实现**:
```java
default Runnable getEnhancedTask(Runnable command, List<TaskWrapper> taskWrappers) {
    Runnable wrapRunnable = command;
    String taskName = (wrapRunnable instanceof NamedRunnable) 
        ? ((NamedRunnable) wrapRunnable).getName() 
        : null;
    
    // 应用任务包装器
    if (CollectionUtils.isNotEmpty(taskWrappers)) {
        for (TaskWrapper t : taskWrappers) {
            wrapRunnable = t.wrap(wrapRunnable);
        }
    }
    
    // 包装为 DtpRunnable
    return new DtpRunnable(command, wrapRunnable, taskName);
}
```

**流程**:
1. 获取任务名称（如果是 `NamedRunnable`）
2. 应用所有任务包装器（链式包装）
3. 包装为 `DtpRunnable`

---

### getEnhancedTask(Runnable command)

**作用**: 增强任务（使用执行器的任务包装器）

**实现**:
```java
default Runnable getEnhancedTask(Runnable command) {
    return getEnhancedTask(command, getTaskWrappers());
}
```

**说明**: 使用执行器自己的任务包装器列表

---

### getTaskWrappers() / setTaskWrappers(List<TaskWrapper> taskWrappers)

**作用**: 获取/设置任务包装器列表

## 实现类

### DtpExecutor

`DtpExecutor` 实现了 `TaskEnhanceAware` 接口：

```java
public class DtpExecutor extends ThreadPoolExecutor 
    implements TaskEnhanceAware, ExecutorAdapter<ThreadPoolExecutor> {
    
    private List<TaskWrapper> taskWrappers = Lists.newArrayList();
    
    @Override
    public List<TaskWrapper> getTaskWrappers() {
        return taskWrappers;
    }
    
    @Override
    public void setTaskWrappers(List<TaskWrapper> taskWrappers) {
        this.taskWrappers = taskWrappers;
    }
    
    @Override
    public void execute(Runnable command) {
        command = getEnhancedTask(command);  // 增强任务
        AwareManager.execute(this, command);
        super.execute(command);
    }
}
```

### ThreadPoolExecutorProxy

`ThreadPoolExecutorProxy` 也实现了 `TaskEnhanceAware` 接口：

```java
public class ThreadPoolExecutorProxy extends ThreadPoolExecutor 
    implements TaskEnhanceAware, RejectHandlerAware {
    
    private List<TaskWrapper> taskWrappers;
    
    @Override
    public void execute(Runnable command) {
        command = getEnhancedTask(command);  // 增强任务
        AwareManager.execute(this, command);
        super.execute(command);
    }
}
```

## 使用场景

### 1. 任务包装

在执行器执行任务前，自动应用任务包装器：

```java
// 设置任务包装器
executor.setTaskWrappers(Arrays.asList(
    new TtlTaskWrapper(),
    new MdcTaskWrapper()
));

// 执行任务（自动应用包装器）
executor.execute(() -> {
    // 任务会被 TTL 和 MDC 包装
});
```

### 2. 任务追踪

通过 `DtpRunnable` 追踪任务：

```java
Runnable enhanced = executor.getEnhancedTask(originalTask);
DtpRunnable dtpRunnable = (DtpRunnable) enhanced;
String traceId = dtpRunnable.getTraceId();  // 获取 traceId
```

## 设计特点

### 1. 链式包装

任务包装器按顺序链式包装：

```java
Runnable task = originalTask;
for (TaskWrapper t : taskWrappers) {
    task = t.wrap(task);  // 链式包装
}
```

### 2. 统一包装

所有任务最终都包装为 `DtpRunnable`，便于追踪和管理。

### 3. 任务名称保留

如果任务是 `NamedRunnable`，保留任务名称。

## 注意事项

1. **包装顺序**: 任务包装器的顺序会影响执行顺序
2. **性能影响**: 每个包装器都会增加一层调用
3. **任务类型**: 最终返回的是 `DtpRunnable`，不是原始任务

