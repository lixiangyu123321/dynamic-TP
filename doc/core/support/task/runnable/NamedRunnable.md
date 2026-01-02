# NamedRunnable

## 概述

`NamedRunnable` 是带名称的 Runnable 实现，用于为任务指定名称，便于任务识别和追踪。

## 核心作用

1. **任务命名**: 为任务指定名称
2. **任务识别**: 通过名称识别任务
3. **自动命名**: 如果未指定名称，自动生成名称

## 核心属性

```java
private final Runnable runnable;  // 原始任务
private final String name;        // 任务名称
```

## 核心方法

### 构造方法

```java
public NamedRunnable(Runnable runnable, String name) {
    this.runnable = runnable;
    this.name = name;
}
```

---

### run()

**作用**: 执行原始任务

```java
@Override
public void run() {
    this.runnable.run();
}
```

---

### getName()

**作用**: 获取任务名称

```java
public String getName() {
    return name;
}
```

---

### of(Runnable runnable, String name)

**作用**: 创建 `NamedRunnable` 的静态工厂方法

**实现**:
```java
public static NamedRunnable of(Runnable runnable, String name) {
    if (StringUtils.isBlank(name)) {
        name = runnable.getClass().getSimpleName() + "-" + UUID.randomUUID();
    }
    return new NamedRunnable(runnable, name);
}
```

**说明**:
- 如果名称为空，自动生成名称
- 名称格式：`{类名}-{UUID}`

## 使用场景

### 1. 任务命名

为任务指定名称，便于识别：

```java
Runnable task = () -> processData();
NamedRunnable namedTask = new NamedRunnable(task, "dataProcessTask");
executor.execute(namedTask);
```

### 2. 自动命名

不指定名称，自动生成：

```java
Runnable task = () -> processData();
NamedRunnable namedTask = NamedRunnable.of(task, null);
// 名称自动生成，如 "DataProcessor-123e4567-e89b-12d3-a456-426614174000"
```

### 3. 任务追踪

通过名称追踪任务：

```java
NamedRunnable task = NamedRunnable.of(runnable, "myTask");
String taskName = task.getName();  // 获取任务名称
```

## 设计特点

### 1. 自动命名

如果未指定名称，自动生成唯一名称。

### 2. 简单包装

只是简单包装，不改变任务执行逻辑。

### 3. 名称获取

提供 `getName()` 方法获取任务名称。

## 注意事项

1. **名称唯一性**: 自动生成的名称包含 UUID，保证唯一性
2. **性能影响**: 名称生成会有一定的性能开销，但通常可以忽略
3. **用途**: 主要用于任务识别和追踪

