# NoticeInvoker

## 概述

`NoticeInvoker` 是通知调用器，实现了 `Invoker<BaseNotifyCtx>` 接口。它是通知责任链的最后一个节点，负责实际发送通知。

## 核心作用

1. **通知发送**: 实际发送配置变更通知
2. **上下文管理**: 管理通知上下文的设置和清理

## 核心方法

### invoke(BaseNotifyCtx context)

**作用**: 执行通知调用

**实现**:
```java
@Override
public void invoke(BaseNotifyCtx context) {
    try {
        DtpNotifyCtxHolder.set(context);  // 设置上下文
        val noticeCtx = (NoticeCtx) context;
        NotifierHandler.getInstance().sendNotice(noticeCtx.getOldFields(), noticeCtx.getDiffs());  // 发送通知
    } finally {
        DtpNotifyCtxHolder.remove();  // 清理上下文
    }
}
```

**流程**:
1. 设置通知上下文
2. 转换为 `NoticeCtx` 类型
3. 发送通知消息
4. 清理通知上下文

## 使用场景

### 1. 通知责任链

作为通知责任链的最后一个节点：

```java
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getCommonInvokerChain();
NoticeCtx context = new NoticeCtx(executorWrapper, notifyItem, oldFields, diffs);
chain.proceed(context);  // 最终会调用 NoticeInvoker.invoke()
```

## 设计特点

### 1. 责任链终点

作为责任链的最后一个节点，实际执行通知发送。

### 2. 上下文管理

负责上下文的设置和清理。

### 3. 异常安全

使用 `try-finally` 保证上下文清理。

## 注意事项

1. **类型转换**: 需要转换为 `NoticeCtx` 类型
2. **上下文设置**: 需要设置上下文，通知器才能获取信息
3. **上下文清理**: 必须在 `finally` 中清理上下文

