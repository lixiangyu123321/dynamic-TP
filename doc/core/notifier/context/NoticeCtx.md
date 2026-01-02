# NoticeCtx

## 概述

`NoticeCtx` 是通知上下文类，继承自 `BaseNotifyCtx`。它用于在通知责任链中传递配置变更相关的上下文信息。

## 核心作用

1. **通知上下文**: 存储通知相关的上下文信息
2. **配置变更**: 存储旧的配置字段和变化的键
3. **责任链传递**: 在通知责任链中传递

## 核心属性

```java
private TpMainFields oldFields;  // 旧的配置字段
private List<String> diffs;      // 变化的配置键列表
```

## 核心方法

### NoticeCtx(ExecutorWrapper wrapper, NotifyItem notifyItem, TpMainFields oldFields, List<String> diffs)

**作用**: 构造方法

**实现**:
```java
public NoticeCtx(ExecutorWrapper wrapper, NotifyItem notifyItem, TpMainFields oldFields, List<String> diffs) {
    super(wrapper, notifyItem);
    this.oldFields = oldFields;
    this.diffs = diffs;
}
```

**说明**: 初始化通知上下文，包含旧的配置字段和变化的键

## 使用场景

### 1. 创建通知上下文

```java
NoticeCtx context = new NoticeCtx(executorWrapper, notifyItem, oldFields, diffKeys);
DtpNotifyCtxHolder.set(context);
// 处理通知
DtpNotifyCtxHolder.remove();
```

### 2. 在责任链中使用

在通知责任链中传递上下文：

```java
NoticeCtx context = (NoticeCtx) DtpNotifyCtxHolder.get();
TpMainFields oldFields = context.getOldFields();
List<String> diffs = context.getDiffs();
```

## 设计特点

### 1. 继承基类

继承 `BaseNotifyCtx`，复用基础功能。

### 2. 配置变更

扩展配置变更信息，用于通知消息构建。

### 3. 类型安全

通过类型区分告警上下文和通知上下文。

## 注意事项

1. **类型转换**: 在责任链中需要转换为 `NoticeCtx` 类型
2. **变化键**: 只包含变化的配置键，不包含所有配置
3. **上下文管理**: 需要在责任链结束后清理上下文

