# BaseNoticeFilter

## 概述

`BaseNoticeFilter` 是基础通知过滤器，实现了 `NotifyFilter` 接口。它负责检查通知条件，只有满足条件时才继续传递。

## 核心作用

1. **条件检查**: 检查通知基础条件
2. **条件验证**: 验证通知是否启用、是否有平台配置

## 执行顺序

- **顺序**: 0（最先执行）

## 核心方法

### doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker)

**作用**: 执行过滤

**实现**:
```java
@Override
public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
    ExecutorWrapper executorWrapper = context.getExecutorWrapper();
    NotifyItem notifyItem = context.getNotifyItem();
    
    // 检查基础条件
    if (Objects.isNull(notifyItem) || !satisfyBaseCondition(notifyItem, executorWrapper)) {
        log.debug("DynamicTp notify, no platforms configured or notification is not enabled, threadPoolName: {}",
                executorWrapper.getThreadPoolName());
        return;  // 不满足条件，不发送通知
    }
    
    // 满足条件，继续传递
    nextInvoker.invoke(context);
}
```

**流程**:
1. 检查基础条件
2. 如果不满足条件，直接返回
3. 如果满足条件，继续传递

---

### satisfyBaseCondition(NotifyItem notifyItem, ExecutorWrapper executor)

**作用**: 检查基础条件

**实现**:
```java
private boolean satisfyBaseCondition(NotifyItem notifyItem, ExecutorWrapper executor) {
    return executor.isNotifyEnabled()
            && notifyItem.isEnabled()
            && CollectionUtils.isNotEmpty(notifyItem.getPlatformIds());
}
```

**条件**:
- 执行器通知已启用
- 通知项已启用
- 通知项配置了平台 ID

---

### getOrder()

**作用**: 获取执行顺序

**返回**: 0（最先执行）

## 使用场景

### 1. 通知责任链

作为通知责任链的第一个过滤器：

```java
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getCommonInvokerChain();
// BaseNoticeFilter 是第一个过滤器
```

## 设计特点

### 1. 条件检查

检查通知的基础条件，避免无效通知。

### 2. 简单实现

实现简单，只做条件检查。

### 3. 早期返回

不满足条件时早期返回，避免不必要的处理。

## 注意事项

1. **执行顺序**: 顺序为 0，最先执行
2. **条件检查**: 必须满足所有条件才继续传递
3. **调试日志**: 不满足条件时记录调试日志

