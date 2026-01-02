# SilentCheckFilter

## 概述

`SilentCheckFilter` 是静默检查过滤器，实现了 `NotifyFilter` 接口。它负责检查告警是否在静默期内，如果在静默期内则不发送告警。

## 核心作用

1. **静默检查**: 检查告警是否在静默期内
2. **限流控制**: 通过静默期控制告警频率
3. **线程安全**: 使用锁保证线程安全

## 执行顺序

- **顺序**: 5

## 核心属性

```java
private static final Map<String, Lock> LOCK_MAP = new ConcurrentHashMap<>();  // 锁映射
```

## 核心方法

### doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker)

**作用**: 执行过滤

**实现**:
```java
@Override
public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
    if (isSilent(context)) {
        return;  // 在静默期内，不发送告警
    }
    nextInvoker.invoke(context);  // 不在静默期内，继续传递
}
```

---

### isSilent(BaseNotifyCtx context)

**作用**: 检查是否在静默期内

**实现**:
```java
protected boolean isSilent(BaseNotifyCtx context) {
    NotifyItem notifyItem = context.getNotifyItem();
    
    // 如果静默期 <= 0，不进行静默检查
    if (notifyItem.getSilencePeriod() <= 0) {
        return false;
    }
    
    ExecutorWrapper executorWrapper = context.getExecutorWrapper();
    String lockKey = executorWrapper.getThreadPoolName();
    Lock lock = LOCK_MAP.computeIfAbsent(lockKey, k -> new ReentrantLock());
    
    lock.lock();
    try {
        // 检查是否允许发送告警
        boolean isAllowed = AlarmLimiter.isAllowed(executorWrapper.getThreadPoolName(), notifyItem.getType());
        if (!isAllowed) {
            if (log.isDebugEnabled()) {
                log.debug("DynamicTp notify, trigger rate limit, threadPoolName: {}, notifyItem: {}",
                        executorWrapper.getThreadPoolName(), notifyItem.getType());
            }
            return true;  // 在静默期内
        }
        // 不在静默期内，放入限流器，开始静默期
        AlarmLimiter.putVal(executorWrapper.getThreadPoolName(), notifyItem.getType());
    } finally {
        lock.unlock();
    }
    return false;  // 不在静默期内
}
```

**流程**:
1. 检查静默期配置（如果 <= 0，不进行静默检查）
2. 获取线程池对应的锁
3. 加锁
4. 检查是否允许发送告警
5. 如果不允许，返回 `true`（在静默期内）
6. 如果允许，放入限流器，开始静默期
7. 解锁

---

### getOrder()

**作用**: 获取执行顺序

**返回**: 5

## 使用场景

### 1. 告警限流

在告警责任链中控制告警频率：

```java
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getAlarmInvokerChain();
// SilentCheckFilter 在责任链中执行
```

## 设计特点

### 1. 锁机制

使用锁保证线程安全，避免并发问题。

### 2. 静默期控制

通过 `AlarmLimiter` 控制静默期。

### 3. 可配置

支持配置静默期，如果 <= 0 则不进行静默检查。

## 注意事项

1. **执行顺序**: 顺序为 5，在 `BaseAlarmFilter` 之后执行
2. **线程安全**: 使用锁保证线程安全
3. **静默期**: 通过 `notifyItem.getSilencePeriod()` 配置

