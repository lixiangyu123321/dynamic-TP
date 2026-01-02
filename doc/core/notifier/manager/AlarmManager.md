# AlarmManager

## 概述

`AlarmManager` 是告警管理器，负责管理告警的发送。它使用责任链模式处理告警，支持告警限流和计数。

## 核心作用

1. **告警发送**: 异步发送告警消息
2. **告警检查**: 定时检查告警条件
3. **告警限流**: 控制告警频率
4. **告警计数**: 统计告警次数

## 核心属性

```java
private static final ExecutorService ALARM_EXECUTOR;  // 告警执行器
private static final InvokerChain<BaseNotifyCtx> ALARM_INVOKER_CHAIN;  // 告警责任链
```

## 初始化

在静态代码块中初始化：

```java
private static final ExecutorService ALARM_EXECUTOR = ThreadPoolBuilder.newBuilder()
        .threadFactory("dtp-alarm")
        .corePoolSize(1)
        .maximumPoolSize(1)
        .workQueue(LINKED_BLOCKING_QUEUE.getName(), 2000)
        .rejectedExecutionHandler(RejectedTypeEnum.DISCARD_OLDEST_POLICY.getName())
        .rejectEnhanced(false)
        .taskWrappers(TaskWrappers.getInstance().getByNames(Sets.newHashSet("mdc")))
        .buildDynamic();

static {
    ALARM_INVOKER_CHAIN = NotifyFilterBuilder.getAlarmInvokerChain();
}
```

**告警执行器特点**:
- 单线程执行
- 队列容量 2000
- 使用 MDC 任务包装器
- 拒绝策略：丢弃最老的任务

## 核心方法

### initAlarm(String poolName, List<NotifyItem> notifyItems)

**作用**: 初始化告警（多个通知项）

**实现**:
```java
public static void initAlarm(String poolName, List<NotifyItem> notifyItems) {
    notifyItems.forEach(x -> initAlarm(poolName, x));
}
```

---

### initAlarm(String poolName, NotifyItem notifyItem)

**作用**: 初始化告警（单个通知项）

**实现**:
```java
public static void initAlarm(String poolName, NotifyItem notifyItem) {
    AlarmLimiter.initAlarmLimiter(poolName, notifyItem);
    AlarmCounter.initAlarmCounter(poolName, notifyItem);
}
```

**说明**: 初始化告警限流器和计数器

---

### tryAlarmAsync(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType, Runnable runnable)

**作用**: 异步尝试发送告警

**实现**:
```java
public static void tryAlarmAsync(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType, Runnable runnable) {
    preAlarm(runnable);
    try {
        ALARM_EXECUTOR.execute(() -> doTryAlarm(executorWrapper, notifyType));
    } finally {
        postAlarm(runnable);
    }
}
```

**流程**:
1. 告警前处理
2. 异步执行告警
3. 告警后处理

---

### checkAndTryAlarmAsync(ExecutorWrapper executorWrapper, List<NotifyItemEnum> notifyTypes)

**作用**: 检查并尝试发送告警（多个类型）

**实现**:
```java
public static void checkAndTryAlarmAsync(ExecutorWrapper executorWrapper, List<NotifyItemEnum> notifyTypes) {
    ALARM_EXECUTOR.execute(() -> notifyTypes.forEach(x -> doCheckAndTryAlarm(executorWrapper, x)));
}
```

---

### doTryAlarm(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType)

**作用**: 执行告警

**说明**: 创建告警上下文，通过责任链处理告警

---

### doCheckAndTryAlarm(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType)

**作用**: 检查并执行告警

**说明**: 检查告警条件，如果满足则发送告警

## 使用场景

### 1. 任务拒绝告警

```java
AlarmManager.tryAlarmAsync(executorWrapper, REJECT, runnable);
```

### 2. 定时检查告警

```java
AlarmManager.checkAndTryAlarmAsync(executorWrapper, Arrays.asList(REJECT, CAPACITY, LIVENESS));
```

## 设计特点

### 1. 异步执行

使用异步执行器，避免阻塞主线程。

### 2. 责任链模式

使用责任链模式处理告警，支持过滤器。

### 3. 限流和计数

支持告警限流和计数，避免告警风暴。

## 注意事项

1. **异步执行**: 告警是异步执行的，不会阻塞调用线程
2. **队列容量**: 告警队列容量为 2000，超过会丢弃最老的任务
3. **MDC 支持**: 告警任务支持 MDC，可以追踪 traceId

