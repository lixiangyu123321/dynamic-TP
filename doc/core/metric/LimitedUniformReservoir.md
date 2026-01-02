# LimitedUniformReservoir

## 概述

`LimitedUniformReservoir` 是有限均匀采样器，实现了 `Reservoir` 接口。它使用均匀采样算法，在有限的内存空间内采样数据，用于计算分位数。

## 核心作用

1. **均匀采样**: 使用均匀采样算法采样数据
2. **内存限制**: 限制内存占用（默认 4096）
3. **分位数计算**: 支持分位数计算

## 核心属性

```java
private static final int DEFAULT_SIZE = 4096;  // 默认大小
private static final int BITS_PER_LONG = 63;   // 每个 long 的位数
private final AtomicLong count = new AtomicLong();  // 计数
private volatile AtomicLongArray values = new AtomicLongArray(DEFAULT_SIZE);  // 值数组
```

## 核心方法

### size()

**作用**: 获取采样大小

**实现**:
```java
@Override
public int size() {
    final long c = count.get();
    if (c > values.length()) {
        return values.length();
    }
    return (int) c;
}
```

**说明**: 如果计数超过数组长度，返回数组长度

---

### update(long value)

**作用**: 更新采样值

**实现**:
```java
@Override
public void update(long value) {
    final long c = count.incrementAndGet();
    if (c <= values.length()) {
        values.set((int) c - 1, value);  // 直接存储
    } else {
        final long r = nextLong(c);  // 随机选择位置
        if (r < values.length()) {
            values.set((int) r, value);  // 替换
        }
    }
}
```

**采样算法**:
1. 如果计数 <= 数组长度，直接存储
2. 如果计数 > 数组长度，随机选择位置替换

---

### getSnapshot()

**作用**: 获取快照

**实现**:
```java
@Override
public Snapshot getSnapshot() {
    final int s = size();
    final List<Long> copy = new ArrayList<>(s);
    for (int i = 0; i < s; i++) {
        copy.add(values.get(i));
    }
    return new UniformSnapshot(copy);
}
```

**说明**: 创建值的副本，用于计算分位数

---

### reset()

**作用**: 重置采样器

**实现**:
```java
public void reset() {
    count.set(0);
    values = new AtomicLongArray(DEFAULT_SIZE);
}
```

**说明**: 重置计数和值数组

---

### nextLong(long n)

**作用**: 生成随机数（0 到 n-1）

**实现**:
```java
private static long nextLong(long n) {
    long bits;
    long val;
    do {
        bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
        val = bits % n;
    } while (bits - val + (n - 1) < 0L);
    return val;
}
```

**说明**: 使用 `ThreadLocalRandom` 生成随机数，保证均匀分布

## 使用场景

### 1. 分位数计算

用于计算响应时间的分位数：

```java
LimitedUniformReservoir reservoir = new LimitedUniformReservoir();
reservoir.update(100);
reservoir.update(200);
reservoir.update(150);
// ... 更多值

Snapshot snapshot = reservoir.getSnapshot();
double tp50 = snapshot.getMedian();
double tp90 = snapshot.get95thPercentile();
double tp99 = snapshot.get99thPercentile();
```

### 2. 性能监控

在性能监控中使用，限制内存占用：

```java
Histogram histogram = new Histogram(new LimitedUniformReservoir());
histogram.update(rt);
```

## 设计特点

### 1. 均匀采样

使用均匀采样算法，保证采样数据的代表性。

### 2. 内存限制

限制内存占用，默认 4096 个值。

### 3. 线程安全

使用 `AtomicLong` 和 `AtomicLongArray` 保证线程安全。

## 注意事项

1. **采样精度**: 使用均匀采样，精度可能略有损失
2. **内存占用**: 默认占用 4096 * 8 = 32KB 内存
3. **随机性**: 使用 `ThreadLocalRandom` 保证随机性

