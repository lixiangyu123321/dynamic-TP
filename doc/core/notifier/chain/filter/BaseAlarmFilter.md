# BaseAlarmFilter

## 概述

`BaseAlarmFilter` 是基础告警过滤器，实现了 `NotifyFilter` 接口。它负责检查告警条件、统计告警次数，只有满足条件时才继续传递。

## 核心作用

1. **条件检查**: 检查告警基础条件
2. **告警计数**: 统计告警次数
3. **阈值判断**: 判断告警次数是否达到阈值

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
    
    // 1. 检查基础条件
    if (Objects.isNull(notifyItem) || !satisfyBaseCondition(notifyItem, executorWrapper)) {
        return;
    }
    
    // 2. 增加告警计数
    String threadPoolName = executorWrapper.getThreadPoolName();
    AlarmCounter.incAlarmCount(threadPoolName, notifyItem.getType());
    
    // 3. 获取告警信息
    AlarmInfo alarmInfo = AlarmCounter.getAlarmInfo(threadPoolName, notifyItem.getType());
    if (Objects.isNull(alarmInfo)) {
        return;
    }
    
    // 4. 判断是否达到阈值
    if (alarmInfo.getCount() < notifyItem.getCount()) {
        if (log.isDebugEnabled()) {
            log.debug("DynamicTp notify, alarm count not reached, current count: {}, threshold: {}",
                    alarmInfo.getCount(), notifyItem.getCount());
        }
        return;  // 未达到阈值，不发送告警
    }
    
    // 5. 设置告警信息并继续传递
    ((AlarmCtx) context).setAlarmInfo(alarmInfo);
    nextInvoker.invoke(context);
}
```

**流程**:
1. 检查基础条件（通知是否启用、是否有平台配置）
2. 增加告警计数
3. 获取告警信息
4. 判断告警次数是否达到阈值
5. 如果达到阈值，设置告警信息并继续传递

---

### satisfyBaseCondition(NotifyItem notifyItem, ExecutorWrapper executorWrapper)

**作用**: 检查基础条件

**实现**:
```java
private boolean satisfyBaseCondition(NotifyItem notifyItem, ExecutorWrapper executorWrapper) {
    return executorWrapper.isNotifyEnabled()
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

### 1. 告警责任链

作为告警责任链的第一个过滤器：

```java
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getAlarmInvokerChain();
// BaseAlarmFilter 是第一个过滤器
```

## 设计特点

### 1. 条件检查

检查告警的基础条件，避免无效告警。

### 2. 计数统计

统计告警次数，只有达到阈值才发送。

### 3. 阈值控制

通过阈值控制告警频率。

## 注意事项

1. **执行顺序**: 顺序为 0，最先执行
2. **阈值判断**: 只有达到阈值才继续传递
3. **告警信息**: 需要设置告警信息到上下文

