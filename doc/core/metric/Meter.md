# Meter

## 概述

`Meter` 是计量器接口，定义了计量器的基本方法。它提供了重置功能，用于重置计量器的状态。

## 核心作用

1. **计量器定义**: 定义计量器的基本接口
2. **状态重置**: 提供重置功能

## 接口定义

```java
public interface Meter {
    void reset();
}
```

## 核心方法

### reset()

**作用**: 重置计量器

**说明**: 重置计量器的状态，清除所有数据

## 实现类

### Summary

`Summary` 接口继承了 `Meter` 接口：

```java
public interface Summary extends Meter {
    void add(long value);
}
```

### MMACounter

`MMACounter` 实现了 `Meter` 接口，提供移动平均计数功能。

### MMAPCounter

`MMAPCounter` 实现了 `Meter` 接口，提供移动平均百分比计数功能。

## 使用场景

### 1. 作为基接口

其他计量器接口可以继承 `Meter` 接口：

```java
public interface MyMeter extends Meter {
    // 自定义方法
}
```

### 2. 重置计量器

```java
Meter meter = ...;
meter.reset();  // 重置计量器
```

## 设计特点

### 1. 简单接口

接口简单，只定义重置功能。

### 2. 基础接口

作为其他计量器接口的基础接口。

### 3. 状态管理

通过 `reset()` 方法管理计量器状态。

## 注意事项

1. **重置影响**: 重置会清除所有数据
2. **实现要求**: 实现类必须实现 `reset()` 方法
3. **线程安全**: 实现类需要保证线程安全

