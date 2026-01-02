# DtpNotifyCtxHolder

## 概述

`DtpNotifyCtxHolder` 是通知上下文持有者，使用 `ThreadLocal` 存储通知上下文。它用于在线程中传递通知上下文信息。

## 核心作用

1. **上下文存储**: 使用 `ThreadLocal` 存储通知上下文
2. **线程隔离**: 保证线程间的上下文隔离
3. **上下文传递**: 在责任链中传递上下文

## 核心属性

```java
private static final ThreadLocal<BaseNotifyCtx> CONTEXT = new ThreadLocal<>();
```

## 核心方法

### set(BaseNotifyCtx dtpContext)

**作用**: 设置上下文

**实现**:
```java
public static void set(BaseNotifyCtx dtpContext) {
    CONTEXT.set(dtpContext);
}
```

**说明**: 将上下文存储到 `ThreadLocal` 中

---

### get()

**作用**: 获取上下文

**实现**:
```java
public static BaseNotifyCtx get() {
    return CONTEXT.get();
}
```

**返回**: 当前线程的上下文，如果不存在返回 `null`

---

### remove()

**作用**: 移除上下文

**实现**:
```java
public static void remove() {
    CONTEXT.remove();
}
```

**说明**: 从 `ThreadLocal` 中移除上下文，避免内存泄漏

## 使用场景

### 1. 设置上下文

在发送告警或通知前设置上下文：

```java
AlarmCtx context = new AlarmCtx(executorWrapper, notifyItem, ...);
DtpNotifyCtxHolder.set(context);
// 发送告警
DtpNotifyCtxHolder.remove();
```

### 2. 获取上下文

在通知器中获取上下文：

```java
BaseNotifyCtx context = DtpNotifyCtxHolder.get();
ExecutorWrapper executorWrapper = context.getExecutorWrapper();
```

## 设计特点

### 1. ThreadLocal

使用 `ThreadLocal` 保证线程隔离。

### 2. 静态方法

提供静态方法，便于使用。

### 3. 内存管理

需要手动调用 `remove()` 避免内存泄漏。

## 注意事项

1. **内存泄漏**: 使用后必须调用 `remove()` 移除上下文
2. **线程隔离**: 每个线程有独立的上下文
3. **空值检查**: `get()` 可能返回 `null`，需要检查

