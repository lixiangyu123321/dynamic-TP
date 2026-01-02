# AlarmCtx

## 概述

`AlarmCtx` 是告警上下文类，继承自 `BaseNotifyCtx`。它用于在告警责任链中传递告警相关的上下文信息。

## 核心作用

1. **告警上下文**: 存储告警相关的上下文信息
2. **告警信息**: 存储告警信息（`AlarmInfo`）
3. **责任链传递**: 在告警责任链中传递

## 核心属性

```java
private AlarmInfo alarmInfo;  // 告警信息
```

## 核心方法

### AlarmCtx(ExecutorWrapper wrapper, NotifyItem notifyItem)

**作用**: 构造方法

**实现**:
```java
public AlarmCtx(ExecutorWrapper wrapper, NotifyItem notifyItem) {
    super(wrapper, notifyItem);
}
```

**说明**: 调用父类构造方法，初始化基础上下文

## 使用场景

### 1. 创建告警上下文

```java
AlarmCtx context = new AlarmCtx(executorWrapper, notifyItem);
context.setAlarmInfo(alarmInfo);
DtpNotifyCtxHolder.set(context);
// 处理告警
DtpNotifyCtxHolder.remove();
```

### 2. 在责任链中使用

在告警责任链中传递上下文：

```java
AlarmCtx context = (AlarmCtx) DtpNotifyCtxHolder.get();
AlarmInfo alarmInfo = context.getAlarmInfo();
```

## 设计特点

### 1. 继承基类

继承 `BaseNotifyCtx`，复用基础功能。

### 2. 告警信息

扩展告警信息字段，用于存储告警统计。

### 3. 类型安全

通过类型区分告警上下文和通知上下文。

## 注意事项

1. **类型转换**: 在责任链中需要转换为 `AlarmCtx` 类型
2. **告警信息**: 告警信息需要单独设置
3. **上下文管理**: 需要在责任链结束后清理上下文

