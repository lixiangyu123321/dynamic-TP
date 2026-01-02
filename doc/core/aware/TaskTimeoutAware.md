# TaskTimeoutAware

## 概述

`TaskTimeoutAware` 是任务超时感知器，用于监控任务的队列等待超时和执行超时。它继承自 `TaskStatAware`，使用 `ThreadPoolStatProvider` 管理超时任务。

## 核心作用

1. **队列超时监控**: 监控任务在队列中的等待时间
2. **执行超时监控**: 监控任务的执行时间
3. **超时处理**: 超时时触发告警或中断任务

## 核心方法

### getOrder() / getName()

**顺序**: 2（`AwareTypeEnum.TASK_TIMEOUT_AWARE`）

**名称**: "timeout"

---

### refresh(TpExecutorProps props, ThreadPoolStatProvider statProvider)

**作用**: 刷新超时配置

**实现**:
```java
@Override
protected void refresh(TpExecutorProps props, ThreadPoolStatProvider statProvider) {
    super.refresh(props, statProvider);
    if (Objects.nonNull(props)) {
        statProvider.setRunTimeout(props.getRunTimeout());        // 执行超时时间
        statProvider.setQueueTimeout(props.getQueueTimeout());    // 队列等待超时时间
        statProvider.setTryInterrupt(props.isTryInterrupt());      // 是否尝试中断
    }
}
```

---

### execute(Executor executor, Runnable r)

**作用**: 任务提交时，开始监控队列超时

**实现**:
```java
@Override
public void execute(Executor executor, Runnable r) {
    if (TRUE_STR.equals(System.getProperty(DTP_EXECUTE_ENHANCED, TRUE_STR))) {
        Optional.ofNullable(statProviders.get(executor))
            .ifPresent(p -> p.startQueueTimeoutTask(r));
    }
}
```

**说明**:
- 检查系统属性 `DTP_EXECUTE_ENHANCED` 是否启用
- 如果启用，开始监控队列超时

---

### beforeExecute(Executor executor, Thread t, Runnable r)

**作用**: 任务执行前，取消队列超时监控，开始执行超时监控

**实现**:
```java
@Override
public void beforeExecute(Executor executor, Thread t, Runnable r) {
    Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> {
        p.cancelQueueTimeoutTask(r);  // 取消队列超时监控
        p.startRunTimeoutTask(t, r);  // 开始执行超时监控
    });
}
```

**说明**:
- 任务开始执行时，不再需要监控队列超时
- 开始监控执行超时

---

### afterExecute(Executor executor, Runnable r, Throwable t)

**作用**: 任务执行后，取消执行超时监控

**实现**:
```java
@Override
public void afterExecute(Executor executor, Runnable r, Throwable t) {
    Optional.ofNullable(statProviders.get(executor))
        .ifPresent(p -> p.cancelRunTimeoutTask(r));
}
```

---

### beforeReject(Runnable r, Executor executor)

**作用**: 任务拒绝前，取消队列超时监控

**实现**:
```java
@Override
public void beforeReject(Runnable r, Executor executor) {
    Optional.ofNullable(statProviders.get(executor))
        .ifPresent(p -> p.cancelQueueTimeoutTask(r));
}
```

**说明**: 任务被拒绝时，不再需要监控队列超时

## 工作流程

### 1. 任务提交阶段

```
任务提交
    │
    └─> execute()
        │
        └─> startQueueTimeoutTask()
            └─> 开始监控队列超时
```

### 2. 任务执行阶段

```
任务开始执行
    │
    ├─> beforeExecute()
    │   ├─> cancelQueueTimeoutTask()  // 取消队列超时监控
    │   └─> startRunTimeoutTask()     // 开始执行超时监控
    │
    └─> 任务执行中
        └─> 如果超时，触发告警或中断
```

### 3. 任务完成阶段

```
任务执行完成
    │
    └─> afterExecute()
        └─> cancelRunTimeoutTask()  // 取消执行超时监控
```

### 4. 任务拒绝阶段

```
任务被拒绝
    │
    └─> beforeReject()
        └─> cancelQueueTimeoutTask()  // 取消队列超时监控
```

## 配置说明

### 超时配置

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          run-timeout: 3000      # 执行超时时间（毫秒）
          queue-timeout: 1000    # 队列等待超时时间（毫秒）
          try-interrupt: true     # 超时时是否尝试中断任务
```

### 系统属性

- **DTP_EXECUTE_ENHANCED**: 是否启用执行增强（默认 "true"）

## 使用场景

### 1. 监控任务超时

自动监控任务的队列等待时间和执行时间，超时时触发告警。

### 2. 中断超时任务

如果配置了 `try-interrupt: true`，超时时会尝试中断任务。

### 3. 统计超时任务

统计队列超时和执行超时的任务数量。

## 注意事项

1. **性能影响**: 超时监控会有一定的性能开销
2. **系统属性**: 需要检查 `DTP_EXECUTE_ENHANCED` 系统属性
3. **超时精度**: 超时监控的精度取决于 `HashedWheelTimer` 的精度

