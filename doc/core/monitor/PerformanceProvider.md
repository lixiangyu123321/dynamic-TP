# PerformanceProvider

## 概述

`PerformanceProvider` 是性能提供者，用于收集和提供线程池的性能指标，包括 TPS（每秒事务数）、RT（响应时间）、TP 分位数等。

## 核心作用

1. **性能统计**: 统计任务的执行时间和数量
2. **性能快照**: 提供性能指标快照
3. **指标计算**: 计算 TPS、RT、TP 分位数等指标

## 核心属性

```java
private final AtomicLong lastRefreshMillis = new AtomicLong(System.currentTimeMillis());  // 上次刷新时间
private final MMAPCounter mmapCounter = new MMAPCounter();                                // 移动平均计数器
```

## 核心方法

### completeTask(long rt)

**作用**: 完成任务，记录响应时间

**实现**:
```java
public void completeTask(long rt) {
    mmapCounter.add(rt);
}
```

**说明**: 记录任务的响应时间（毫秒）

---

### getSnapshotAndReset()

**作用**: 获取性能快照并重置

**实现**:
```java
public PerformanceSnapshot getSnapshotAndReset() {
    long currentMillis = System.currentTimeMillis();
    int intervalTs = (int) (currentMillis - lastRefreshMillis.get()) / 1000;
    val performanceSnapshot = new PerformanceSnapshot(mmapCounter, intervalTs);
    reset(currentMillis);
    return performanceSnapshot;
}
```

**说明**:
- 计算监控间隔（秒）
- 创建性能快照
- 重置计数器

---

### reset(long currentMillis)

**作用**: 重置计数器

**实现**:
```java
private void reset(long currentMillis) {
    mmapCounter.reset();
    lastRefreshMillis.compareAndSet(lastRefreshMillis.get(), currentMillis);
}
```

---

### PerformanceSnapshot

**作用**: 性能快照，包含所有性能指标

**属性**:
- `tps`: 每秒事务数
- `maxRt`: 最大响应时间
- `minRt`: 最小响应时间
- `avg`: 平均响应时间
- `tp50`, `tp75`, `tp90`, `tp95`, `tp99`, `tp999`: TP 分位数

**实现**:
```java
@Getter
public static class PerformanceSnapshot {
    private final double tps;
    private final long maxRt;
    private final long minRt;
    private final double avg;
    private final double tp50;
    private final double tp75;
    private final double tp90;
    private final double tp95;
    private final double tp99;
    private final double tp999;
    
    public PerformanceSnapshot(MMAPCounter mmapCounter, int monitorInterval) {
        tps = BigDecimal.valueOf(mmapCounter.getMmaCounter().getCount())
                .divide(BigDecimal.valueOf(Math.max(monitorInterval, 1)), 1, RoundingMode.HALF_UP)
                .doubleValue();
        
        maxRt = mmapCounter.getMmaCounter().getMax();
        minRt = mmapCounter.getMmaCounter().getMin();
        avg = mmapCounter.getMmaCounter().getAvg();
        
        tp50 = mmapCounter.getSnapshot().getMedian();
        tp75 = mmapCounter.getSnapshot().get75thPercentile();
        tp90 = mmapCounter.getSnapshot().getValue(0.9);
        tp95 = mmapCounter.getSnapshot().get95thPercentile();
        tp99 = mmapCounter.getSnapshot().get99thPercentile();
        tp999 = mmapCounter.getSnapshot().get999thPercentile();
    }
}
```

## 使用场景

### 1. 性能监控

在任务完成时记录响应时间：

```java
PerformanceProvider provider = new PerformanceProvider();
// 任务执行
long startTime = System.currentTimeMillis();
// ... 任务逻辑
long rt = System.currentTimeMillis() - startTime;
provider.completeTask(rt);
```

### 2. 获取性能快照

在监控时获取性能快照：

```java
PerformanceSnapshot snapshot = provider.getSnapshotAndReset();
double tps = snapshot.getTps();
double avgRt = snapshot.getAvg();
double tp99 = snapshot.getTp99();
```

## 设计特点

### 1. 移动平均

使用 `MMAPCounter` 进行移动平均计算，减少内存占用。

### 2. 快照机制

获取快照后重置计数器，保证数据准确性。

### 3. 分位数计算

支持多种 TP 分位数计算（50、75、90、95、99、999）。

## 注意事项

1. **时间单位**: 响应时间单位为毫秒
2. **快照重置**: 获取快照后会重置计数器
3. **监控间隔**: TPS 计算依赖监控间隔

