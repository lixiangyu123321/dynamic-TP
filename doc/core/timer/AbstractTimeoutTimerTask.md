# AbstractTimeoutTimerTask

## 概述

`AbstractTimeoutTimerTask` 是抽象超时定时任务基类，实现了 `TimerTask` 接口。它提供了超时任务的基础功能，包括执行器包装器和任务的获取。

## 核心作用

1. **超时任务基类**: 为超时任务提供基础实现
2. **任务信息获取**: 提供任务名称和 traceId 的获取
3. **抽象方法**: 定义 `doRun()` 抽象方法，由子类实现

## 核心属性

```java
protected final ExecutorWrapper executorWrapper;  // 执行器包装器
protected final Runnable runnable;                 // 任务
```

## 核心方法

### run(Timeout timeout)

**作用**: 执行超时任务

**实现**:
```java
@Override
public void run(Timeout timeout) throws Exception {
    val statProvider = executorWrapper.getThreadPoolStatProvider();
    if (Objects.isNull(statProvider)) {
        return;
    }
    doRun();
}
```

**流程**:
1. 获取统计提供者
2. 如果统计提供者为空，直接返回
3. 调用 `doRun()` 方法（由子类实现）

---

### getTaskNameAndTraceId()

**作用**: 获取任务名称和 traceId

**实现**:
```java
protected Pair<String, String> getTaskNameAndTraceId() {
    String taskName = StringUtils.EMPTY;
    String traceId = StringUtils.EMPTY;
    if (runnable instanceof DtpRunnable) {
        DtpRunnable dtpRunnable = (DtpRunnable) runnable;
        taskName = dtpRunnable.getTaskName();
        traceId = dtpRunnable.getTraceId();
    }
    return Pair.of(taskName, traceId);
}
```

**说明**: 如果任务是 `DtpRunnable`，获取任务名称和 traceId

---

### doRun()

**作用**: 执行超时逻辑（抽象方法）

**说明**: 由子类实现具体的超时处理逻辑

## 使用场景

### 1. 作为基类

子类继承 `AbstractTimeoutTimerTask` 实现超时任务：

```java
public class CustomTimeoutTask extends AbstractTimeoutTimerTask {
    public CustomTimeoutTask(ExecutorWrapper executorWrapper, Runnable runnable) {
        super(executorWrapper, runnable);
    }
    
    @Override
    protected void doRun() {
        // 超时处理逻辑
        Pair<String, String> nameAndTraceId = getTaskNameAndTraceId();
        String taskName = nameAndTraceId.getLeft();
        String traceId = nameAndTraceId.getRight();
        // 处理超时
    }
}
```

## 设计特点

### 1. 模板方法模式

定义超时任务的骨架，子类实现具体逻辑。

### 2. 任务信息提取

提供任务名称和 traceId 的提取方法。

### 3. 空值检查

在执行前检查统计提供者是否为空。

## 注意事项

1. **抽象方法**: 子类必须实现 `doRun()` 方法
2. **任务类型**: 只有 `DtpRunnable` 类型的任务才能获取名称和 traceId
3. **统计提供者**: 需要确保统计提供者不为空

