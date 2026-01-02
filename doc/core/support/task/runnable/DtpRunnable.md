# DtpRunnable

## 概述

`DtpRunnable` 是框架的任务包装类，用于包装原始任务，记录任务的相关信息（原始任务、包装后的任务、任务名称、traceId 等）。

> 💡 **MDC 说明**：想了解 `DtpRunnable` 中 MDC 的作用和使用方式？请查看 [DtpRunnable-MDC说明](DtpRunnable-MDC说明.md)

## 核心作用

1. **任务包装**: 包装原始任务和增强后的任务
2. **信息记录**: 记录任务名称、traceId 等信息
3. **任务追踪**: 用于任务追踪和调试

## 核心属性

```java
private final Runnable originRunnable;  // 原始任务
private final Runnable runnable;        // 包装后的任务
private final String taskName;         // 任务名称
private final String traceId;          // 追踪 ID（从 MDC 获取）
```

## 核心方法

### 构造方法

```java
public DtpRunnable(Runnable originRunnable, Runnable runnable, String taskName) {
    this.originRunnable = originRunnable;
    this.runnable = runnable;
    this.taskName = taskName;
    this.traceId = MDC.get(TRACE_ID);  // 从 MDC 获取 traceId
}
```

**说明**:
- 保存原始任务和包装后的任务
- 记录任务名称
- 从 MDC 中获取 traceId

---

### run()

**作用**: 执行包装后的任务

```java
@Override
public void run() {
    runnable.run();
}
```

**说明**: 执行的是包装后的任务，不是原始任务

---

### getter 方法

提供所有属性的 getter 方法（通过 `@Getter` 注解生成）：

- `getOriginRunnable()`: 获取原始任务
- `getRunnable()`: 获取包装后的任务
- `getTaskName()`: 获取任务名称
- `getTraceId()`: 获取 traceId

## 使用场景

### 1. 任务追踪

记录任务信息，用于追踪和调试：

```java
DtpRunnable dtpRunnable = new DtpRunnable(originalTask, wrappedTask, "myTask");
String traceId = dtpRunnable.getTraceId();  // 获取 traceId
```

### 2. 任务包装

在任务包装过程中使用：

```java
Runnable wrapped = taskWrappers.wrap(originalTask);
DtpRunnable dtpRunnable = new DtpRunnable(originalTask, wrapped, "taskName");
```

## 设计特点

### 1. 信息记录

记录任务的完整信息，包括原始任务、包装后的任务、名称、traceId。

### 2. traceId 自动获取

从 MDC 中自动获取 traceId，无需手动传递。

### 3. 不可变

所有属性都是 final，保证不可变。

## 注意事项

1. **traceId 获取**: traceId 在构造时从 MDC 获取，如果此时 MDC 中没有 traceId，则为 null
2. **任务执行**: 执行的是包装后的任务，不是原始任务
3. **信息用途**: 主要用于追踪和调试，不影响任务执行

