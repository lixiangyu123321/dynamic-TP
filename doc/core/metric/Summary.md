# Summary

## 概述

`Summary` 是摘要接口，继承自 `Meter` 接口。它提供了值添加功能，用于统计数据的摘要信息。

## 核心作用

1. **值添加**: 添加值到摘要
2. **状态重置**: 提供重置功能（继承自 `Meter`）

## 接口定义

```java
public interface Summary extends Meter {
    void add(long value);
}
```

## 核心方法

### add(long value)

**作用**: 添加值到摘要

**参数**: `value` - 要添加的值

**说明**: 将值添加到摘要中，用于统计计算

---

### reset()

**作用**: 重置摘要（继承自 `Meter`）

**说明**: 重置摘要的状态，清除所有数据

## 实现类

### LimitedUniformReservoir

`LimitedUniformReservoir` 实现了 `Summary` 接口，提供有限均匀采样功能。

## 使用场景

### 1. 统计摘要

用于统计数据的摘要信息：

```java
Summary summary = new LimitedUniformReservoir();
summary.add(100);
summary.add(200);
summary.add(150);
// 计算摘要统计信息
```

### 2. 性能监控

用于性能监控中的响应时间统计：

```java
Summary summary = ...;
long startTime = System.currentTimeMillis();
// 执行任务
long rt = System.currentTimeMillis() - startTime;
summary.add(rt);
```

## 设计特点

### 1. 继承 Meter

继承 `Meter` 接口，提供重置功能。

### 2. 值添加

提供值添加功能，支持统计计算。

### 3. 抽象接口

作为抽象接口，由具体实现类提供统计功能。

## 注意事项

1. **值类型**: 值类型为 `long`
2. **实现要求**: 实现类必须实现 `add()` 和 `reset()` 方法
3. **线程安全**: 实现类需要保证线程安全

