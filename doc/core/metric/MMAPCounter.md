# MMAPCounter

## 概述

`MMAPCounter` 是移动平均百分比计数器，实现了 `Summary` 接口。它结合了 `MMACounter` 和 `Histogram`，提供基础统计和分位数统计功能。

## 核心作用

1. **基础统计**: 提供总数、计数、最小值、最大值、平均值（通过 `MMACounter`）
2. **分位数统计**: 提供分位数统计（通过 `Histogram`）
3. **性能监控**: 用于性能监控中的响应时间统计

## 核心属性

```java
private final MMACounter mmaCounter;              // 移动平均计数器
private final LimitedUniformReservoir reservoir;  // 有限均匀采样器
private final Histogram histogram;                 // 直方图
```

## 核心方法

### MMAPCounter()

**作用**: 构造方法

**实现**:
```java
public MMAPCounter() {
    this.mmaCounter = new MMACounter();
    reservoir = new LimitedUniformReservoir();
    histogram = new Histogram(reservoir);
}
```

**说明**: 初始化 `MMACounter`、`LimitedUniformReservoir` 和 `Histogram`

---

### add(long value)

**作用**: 添加值

**实现**:
```java
@Override
public void add(long value) {
    mmaCounter.add(value);  // 添加到基础计数器
    histogram.update(value); // 更新直方图
}
```

**说明**: 同时更新基础计数器和直方图

---

### reset()

**作用**: 重置计数器

**实现**:
```java
@Override
public void reset() {
    mmaCounter.reset();
    reservoir.reset();
}
```

**说明**: 重置基础计数器和采样器

---

### getSnapshot()

**作用**: 获取快照

**实现**:
```java
public Snapshot getSnapshot() {
    return histogram.getSnapshot();
}
```

**返回**: `Snapshot` 对象，包含分位数统计信息

**说明**: 用于获取 TP 分位数（TP50、TP75、TP90、TP95、TP99、TP999）

---

### getMmaCounter()

**作用**: 获取基础计数器

**返回**: `MMACounter` 对象

**说明**: 用于获取基础统计信息（总数、计数、最小值、最大值、平均值）

## 使用场景

### 1. 性能监控

用于性能监控中的响应时间统计：

```java
MMAPCounter counter = new MMAPCounter();
counter.add(100);  // 响应时间 100ms
counter.add(200);  // 响应时间 200ms
counter.add(150);  // 响应时间 150ms

// 获取基础统计
MMACounter mmaCounter = counter.getMmaCounter();
double avg = mmaCounter.getAvg();
long min = mmaCounter.getMin();
long max = mmaCounter.getMax();

// 获取分位数统计
Snapshot snapshot = counter.getSnapshot();
double tp50 = snapshot.getMedian();
double tp90 = snapshot.get95thPercentile();
double tp99 = snapshot.get99thPercentile();
```

### 2. PerformanceProvider

`PerformanceProvider` 使用 `MMAPCounter` 统计性能指标：

```java
MMAPCounter mmapCounter = new MMAPCounter();
mmapCounter.add(rt);  // 添加响应时间
```

## 设计特点

### 1. 组合模式

组合 `MMACounter` 和 `Histogram`，提供完整的统计功能。

### 2. 双重统计

同时提供基础统计和分位数统计。

### 3. 快照机制

通过快照机制获取统计信息，保证数据一致性。

## 注意事项

1. **内存占用**: `LimitedUniformReservoir` 会占用一定内存
2. **采样精度**: 使用均匀采样，精度可能略有损失
3. **快照获取**: 获取快照后需要重置计数器

