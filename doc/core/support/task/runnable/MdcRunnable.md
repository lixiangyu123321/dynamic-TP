# MdcRunnable

## 概述

`MdcRunnable` 是 MDC（Mapped Diagnostic Context）Runnable 实现，用于在异步任务中传递 MDC 上下文。它会在任务执行时设置主线程的 MDC 上下文，执行后清理。

## 核心作用

1. **MDC 传递**: 将主线程的 MDC 上下文传递到异步任务
2. **自动清理**: 任务执行后自动清理 MDC 上下文（保留 traceId）
3. **线程检查**: 如果任务在主线程执行，不进行 MDC 操作

## 核心属性

```java
private final Runnable runnable;           // 原始任务
private final Thread parentThread;         // 父线程
private final Map<String, String> parentMdc; // 父线程的 MDC 上下文
```

## 核心方法

### 构造方法

```java
public MdcRunnable(Runnable runnable) {
    this.runnable = runnable;
    this.parentMdc = MDC.getCopyOfContextMap();  // 捕获当前线程的 MDC
    this.parentThread = Thread.currentThread();   // 记录父线程
}
```

**说明**:
- 捕获当前线程的 MDC 上下文副本
- 记录父线程引用

---

### get(Runnable runnable)

**作用**: 创建 `MdcRunnable` 实例的静态工厂方法

```java
public static MdcRunnable get(Runnable runnable) {
    return new MdcRunnable(runnable);
}
```

---

### run()

**作用**: 执行任务，传递 MDC 上下文

**实现流程**:

1. **线程检查**:
   ```java
   if (MapUtils.isEmpty(parentMdc) || Objects.equals(Thread.currentThread(), parentThread)) {
       runnable.run();
       return;  // 如果 MDC 为空或仍在主线程，直接执行
   }
   ```

2. **设置 MDC**:
   ```java
   for (Map.Entry<String, String> entry : parentMdc.entrySet()) {
       MDC.put(entry.getKey(), entry.getValue());
   }
   ```

3. **执行任务**:
   ```java
   try {
       runnable.run();
   } finally {
       // 清理 MDC（保留 traceId）
       for (Map.Entry<String, String> entry : parentMdc.entrySet()) {
           if (!TRACE_ID.equals(entry.getKey())) {
               MDC.remove(entry.getKey());
           }
       }
   }
   ```

**特殊处理**: 
- 保留 `traceId`，不清理
- 其他 MDC 键值对会被清理

## 使用场景

### 1. 日志追踪

```java
// 主线程设置 traceId
MDC.put("traceId", "123-456");

// 异步任务中传递 traceId
executor.execute(MdcRunnable.get(() -> {
    String traceId = MDC.get("traceId");  // 可以获取到
    log.info("处理任务, traceId: {}", traceId);
}));
```

### 2. 分布式追踪

在微服务场景下保持 traceId：

```java
String traceId = request.getHeader("X-Trace-Id");
MDC.put("traceId", traceId);

executor.execute(MdcRunnable.get(() -> {
    // traceId 自动传递
    processRequest();
}));
```

## 设计特点

### 1. 线程检查

如果任务在主线程执行，不进行 MDC 操作，避免不必要的开销。

### 2. 保留 traceId

清理 MDC 时保留 `traceId`，因为 traceId 通常需要在任务执行后继续使用。

### 3. 自动清理

使用 `try-finally` 确保 MDC 上下文被清理，避免内存泄漏。

## 注意事项

1. **MDC 实现**: 需要日志框架支持 MDC（如 Logback、Log4j2）
2. **traceId 保留**: traceId 不会被清理，需要注意
3. **线程检查**: 在主线程执行时不会进行 MDC 操作

