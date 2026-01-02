# MMACounter

## 概述

`MMACounter` 是移动平均计数器，实现了 `Summary` 接口。它用于统计值的总数、计数、最小值、最大值和平均值。

## 核心作用

1. **值统计**: 统计值的总数、计数、最小值、最大值
2. **平均值计算**: 计算平均值
3. **线程安全**: 使用原子类保证线程安全

## 核心属性

```java
private final AtomicLong total = new AtomicLong();        // 总值
private final AtomicLong count = new AtomicLong();        // 计数
private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);  // 最小值
private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);  // 最大值
```

## 核心方法

### add(long value)

**作用**: 添加值

**实现**:
```java
@Override
public void add(long value) {
    total.addAndGet(value);
    count.incrementAndGet();
    setMin(value);
    setMax(value);
}
```

**流程**:
1. 增加总值
2. 增加计数
3. 更新最小值
4. 更新最大值

---

### reset()

**作用**: 重置计数器

**实现**:
```java
@Override
public void reset() {
    total.set(0);
    count.set(0);
    min.set(Long.MAX_VALUE);
    max.set(Long.MIN_VALUE);
}
```

---

### getTotal() / getCount()

**作用**: 获取总值和计数

---

### getMin() / getMax()

**作用**: 获取最小值和最大值

**实现**:
```java
public long getMin() {
    long current = min.get();
    return (current == Long.MAX_VALUE) ? 0 : current;
}

public long getMax() {
    long current = max.get();
    return (current == Long.MIN_VALUE) ? 0 : current;
}
```

**说明**: 如果未初始化，返回 0

---

### getAvg()

**作用**: 获取平均值

**实现**:
```java
public double getAvg() {
    long currentCount = count.get();
    long currentTotal = total.get();
    if (currentCount > 0) {
        double avgLatency = currentTotal / (double) currentCount;
        BigDecimal bg = new BigDecimal(avgLatency);
        return bg.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
    return 0;
}
```

**说明**: 
- 如果计数为 0，返回 0
- 保留 4 位小数，四舍五入

---

### setMin(long value) / setMax(long value)

**作用**: 设置最小值和最大值

**实现**:
```java
private void setMax(long value) {
    long current;
    while (value > (current = max.get()) && !max.compareAndSet(current, value)) {
        // no op
    }
}

private void setMin(long value) {
    long current;
    while (value < (current = min.get()) && !min.compareAndSet(current, value)) {
        // no op
    }
}
```

**说明**: 使用 CAS 操作保证线程安全

## 使用场景

### 1. 性能统计

用于统计任务的响应时间：

```java
MMACounter counter = new MMACounter();
counter.add(100);  // 响应时间 100ms
counter.add(200);  // 响应时间 200ms
counter.add(150);  // 响应时间 150ms

long total = counter.getTotal();  // 450
long count = counter.getCount();  // 3
double avg = counter.getAvg();    // 150.0
long min = counter.getMin();      // 100
long max = counter.getMax();      // 200
```

## 设计特点

### 1. 线程安全

使用 `AtomicLong` 保证线程安全。

### 2. CAS 操作

使用 CAS 操作更新最小值和最大值。

### 3. 精度控制

平均值保留 4 位小数。

## 注意事项

1. **初始化**: 最小值和最大值使用特殊值初始化
2. **线程安全**: 所有操作都是线程安全的
3. **精度**: 平均值保留 4 位小数

