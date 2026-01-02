# BaseNotifyCtx

## 概述

`BaseNotifyCtx` 是基础通知上下文类，包含执行器包装器和通知项。它是 `AlarmCtx` 和 `NoticeCtx` 的基类。

## 核心作用

1. **上下文基础**: 提供通知上下文的基础信息
2. **执行器捕获**: 捕获执行器的快照
3. **通知项存储**: 存储通知项信息

## 核心属性

```java
private ExecutorWrapper executorWrapper;  // 执行器包装器（捕获的快照）
private NotifyItem notifyItem;            // 通知项
```

## 核心方法

### BaseNotifyCtx(ExecutorWrapper wrapper, NotifyItem notifyItem)

**作用**: 构造方法

**实现**:
```java
public BaseNotifyCtx(ExecutorWrapper wrapper, NotifyItem notifyItem) {
    this.executorWrapper = wrapper.capture();  // 捕获快照
    this.notifyItem = notifyItem;
}
```

**说明**: 使用 `wrapper.capture()` 捕获执行器的快照，避免执行器状态变化影响通知内容

---

### getNotifyItemEnum()

**作用**: 获取通知项枚举

**实现**:
```java
public NotifyItemEnum getNotifyItemEnum() {
    return NotifyItemEnum.of(notifyItem.getType());
}
```

## 使用场景

### 1. 作为基类

子类继承 `BaseNotifyCtx`：

```java
public class AlarmCtx extends BaseNotifyCtx {
    private AlarmInfo alarmInfo;
    // ...
}

public class NoticeCtx extends BaseNotifyCtx {
    private TpMainFields oldFields;
    private List<String> diffs;
    // ...
}
```

### 2. 在责任链中使用

在责任链中传递上下文：

```java
BaseNotifyCtx context = new AlarmCtx(executorWrapper, notifyItem);
DtpNotifyCtxHolder.set(context);
// 处理告警
DtpNotifyCtxHolder.remove();
```

## 设计特点

### 1. 快照机制

使用 `capture()` 方法捕获执行器快照，保证数据一致性。

### 2. 基类设计

作为基类，提供通用的上下文信息。

### 3. 数据封装

封装执行器包装器和通知项，便于传递。

## 注意事项

1. **快照捕获**: 使用 `capture()` 捕获快照，避免状态变化
2. **线程安全**: 上下文对象本身不是线程安全的
3. **内存管理**: 需要在责任链结束后清理上下文

