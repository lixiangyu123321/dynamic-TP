# NoticeManager

## 概述

`NoticeManager` 是通知管理器，负责管理配置变更通知的发送。它使用责任链模式处理通知，异步发送通知消息。

## 核心作用

1. **通知发送**: 异步发送配置变更通知
2. **责任链处理**: 使用责任链模式处理通知
3. **异步执行**: 异步执行通知，不阻塞主线程

## 核心属性

```java
private static final ExecutorService NOTICE_EXECUTOR;  // 通知执行器
private static final InvokerChain<BaseNotifyCtx> NOTICE_INVOKER_CHAIN;  // 通知责任链
```

## 初始化

在静态代码块中初始化：

```java
private static final ExecutorService NOTICE_EXECUTOR = ThreadPoolBuilder.newBuilder()
        .threadFactory("dtp-notify")
        .corePoolSize(1)
        .maximumPoolSize(1)
        .workQueue(LINKED_BLOCKING_QUEUE.getName(), 100)
        .rejectedExecutionHandler(RejectedTypeEnum.DISCARD_OLDEST_POLICY.getName())
        .buildCommon();

static {
    NOTICE_INVOKER_CHAIN = NotifyFilterBuilder.getCommonInvokerChain();
}
```

**通知执行器特点**:
- 单线程执行
- 队列容量 100
- 拒绝策略：丢弃最老的任务

## 核心方法

### tryNoticeAsync(ExecutorWrapper executor, TpMainFields oldFields, List<String> diffKeys)

**作用**: 异步尝试发送通知

**实现**:
```java
public static void tryNoticeAsync(ExecutorWrapper executor, TpMainFields oldFields, List<String> diffKeys) {
    NOTICE_EXECUTOR.execute(() -> doTryNotice(executor, oldFields, diffKeys));
}
```

**说明**: 异步执行通知，不阻塞调用线程

---

### doTryNotice(ExecutorWrapper executor, TpMainFields oldFields, List<String> diffKeys)

**作用**: 执行通知

**实现**:
```java
public static void doTryNotice(ExecutorWrapper executor, TpMainFields oldFields, List<String> diffKeys) {
    NotifyHelper.getNotifyItem(executor, CHANGE).ifPresent(notifyItem -> {
        val noticeCtx = new NoticeCtx(executor, notifyItem, oldFields, diffKeys);
        NOTICE_INVOKER_CHAIN.proceed(noticeCtx);
    });
}
```

**流程**:
1. 获取通知项
2. 创建通知上下文
3. 通过责任链处理通知

---

### destroy()

**作用**: 销毁管理器

**实现**:
```java
public static void destroy() {
    NOTICE_EXECUTOR.shutdownNow();
}
```

## 使用场景

### 1. 配置变更通知

在配置刷新时发送通知：

```java
NoticeManager.tryNoticeAsync(executorWrapper, oldFields, diffKeys);
```

## 设计特点

### 1. 异步执行

使用异步执行器，避免阻塞主线程。

### 2. 责任链模式

使用责任链模式处理通知，支持过滤器。

### 3. 简单实现

实现简单，专注于通知发送。

## 注意事项

1. **异步执行**: 通知是异步执行的，不会阻塞调用线程
2. **队列容量**: 通知队列容量为 100，超过会丢弃最老的任务
3. **通知项**: 只有配置了通知项才会发送通知

