# PriorityRunnable

## 概述

`PriorityRunnable` 是优先级 Runnable 包装类，实现了 `Priority` 和 `Runnable` 接口。它用于为普通 `Runnable` 任务添加优先级支持。

## 核心作用

1. **优先级包装**: 为 `Runnable` 任务添加优先级
2. **任务代理**: 代理原始任务的执行
3. **优先级支持**: 实现 `Priority` 接口，提供优先级值

## 核心属性

```java
private final Runnable runnable;  // 原始任务
@Getter
private final int priority;       // 优先级值
```

## 核心方法

### PriorityRunnable(Runnable runnable, int priority)

**作用**: 构造方法（私有）

**说明**: 使用静态工厂方法创建实例

---

### of(Runnable runnable, int priority)

**作用**: 静态工厂方法，创建 `PriorityRunnable` 实例

**实现**:
```java
public static PriorityRunnable of(Runnable runnable, int priority) {
    return new PriorityRunnable(runnable, priority);
}
```

---

### run()

**作用**: 执行任务

**实现**:
```java
@Override
public void run() {
    this.runnable.run();
}
```

**说明**: 直接调用原始任务的 `run()` 方法

---

### getPriority()

**作用**: 获取优先级值

**说明**: 实现 `Priority` 接口，返回优先级值

## 使用场景

### 1. 提交优先级任务

```java
PriorityDtpExecutor executor = new PriorityDtpExecutor(...);

// 提交高优先级任务
executor.execute(PriorityRunnable.of(() -> {
    System.out.println("高优先级任务");
}, Priority.HIGHEST_PRECEDENCE));

// 提交低优先级任务
executor.execute(PriorityRunnable.of(() -> {
    System.out.println("低优先级任务");
}, Priority.LOWEST_PRECEDENCE));
```

### 2. 使用 execute 方法

```java
executor.execute(() -> {
    System.out.println("任务");
}, 10);  // 指定优先级
```

## 设计特点

### 1. 包装模式

使用包装模式，为任务添加优先级功能。

### 2. 工厂方法

使用静态工厂方法创建实例，简化使用。

### 3. 透明代理

对原始任务进行透明代理，不影响任务执行。

## 注意事项

1. **优先级值**: 值越小，优先级越高
2. **任务类型**: 只能包装 `Runnable` 任务
3. **执行顺序**: 优先级只影响队列中的任务顺序

