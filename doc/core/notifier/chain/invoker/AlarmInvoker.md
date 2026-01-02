# AlarmInvoker

## 概述

`AlarmInvoker` 是告警调用器，实现了 `Invoker<BaseNotifyCtx>` 接口。它是告警责任链的最后一个节点，负责实际发送告警。

## 核心作用

1. **告警发送**: 实际发送告警消息
2. **计数重置**: 发送告警后重置告警计数
3. **上下文管理**: 管理通知上下文的设置和清理

## 核心方法

### invoke(BaseNotifyCtx context)

**作用**: 执行告警调用

**实现**:
```java
@Override
public void invoke(BaseNotifyCtx context) {
    val executorWrapper = context.getExecutorWrapper();
    val notifyItem = context.getNotifyItem();
    try {
        DtpNotifyCtxHolder.set(context);  // 设置上下文
        NotifierHandler.getInstance().sendAlarm(NotifyItemEnum.of(notifyItem.getType()));  // 发送告警
        AlarmCounter.reset(executorWrapper.getThreadPoolName(), notifyItem.getType());  // 重置计数
    } finally {
        DtpNotifyCtxHolder.remove();  // 清理上下文
    }
}
```

**流程**:
1. 设置通知上下文
2. 发送告警消息
3. 重置告警计数
4. 清理通知上下文

## 使用场景

### 1. 告警责任链

作为告警责任链的最后一个节点：

```java
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getAlarmInvokerChain();
AlarmCtx context = new AlarmCtx(executorWrapper, notifyItem);
chain.proceed(context);  // 最终会调用 AlarmInvoker.invoke()
```

## 设计特点

### 1. 责任链终点

作为责任链的最后一个节点，实际执行告警发送。

### 2. 上下文管理

负责上下文的设置和清理。

### 3. 异常安全

使用 `try-finally` 保证上下文清理。

## 注意事项

1. **上下文设置**: 需要设置上下文，通知器才能获取信息
2. **计数重置**: 发送告警后重置计数，避免重复告警
3. **上下文清理**: 必须在 `finally` 中清理上下文

