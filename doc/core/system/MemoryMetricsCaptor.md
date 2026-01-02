# MemoryMetricsCaptor

## 概述

`MemoryMetricsCaptor` 是内存指标捕获器，实现了 `Runnable` 接口。它定时捕获老年代内存的使用率。

## 核心作用

1. **内存监控**: 定时捕获老年代内存使用率
2. **数据计算**: 计算老年代内存使用率
3. **数据提供**: 提供老年代内存使用率数据

## 核心属性

```java
private double max = -1;   // 最大内存
private double used = -1;  // 已使用内存
```

## 核心方法

### getLongLivedMemoryUsage()

**作用**: 获取老年代内存使用率

**返回**: 老年代内存使用率（0.0 - 1.0，-1 表示未初始化）

**实现**:
```java
public double getLongLivedMemoryUsage() {
    if (max == -1 || used == -1) {
        return -1;
    }
    return used / max;
}
```

---

### run()

**作用**: 执行内存指标捕获

**实现**:
```java
@Override
public void run() {
    try {
        val memoryPoolBeans = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
        if (CollectionUtils.isEmpty(memoryPoolBeans)) {
            return;
        }
        for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
            String name = memoryPoolBean.getName();
            boolean isLongLivedPool = isLongLivedPool(name);
            if (isLongLivedPool) {
                used = getUsageValue(memoryPoolBean, MemoryUsage::getUsed);
                max = getUsageValue(memoryPoolBean, MemoryUsage::getMax);
                break;
            }
        }
    } catch (Exception e) {
        log.warn("MemoryMetricsCaptor run failed.", e);
    }
}
```

**流程**:
1. 获取所有内存池
2. 查找老年代内存池
3. 获取已使用内存和最大内存
4. 计算使用率

---

### isLongLivedPool(String name)

**作用**: 判断是否为老年代内存池

**实现**:
```java
private boolean isLongLivedPool(String name) {
    return StringUtils.isNotBlank(name) && (
        name.endsWith("Old Gen") ||
        name.endsWith("Tenured Gen") ||
        "ZHeap".equals(name) ||
        "Shenandoah".equals(name) ||
        name.endsWith("balanced-old") ||
        name.contains("tenured") ||
        "JavaHeap".equals(name)
    );
}
```

**支持的内存池名称**:
- `*Old Gen`: HotSpot 老年代
- `*Tenured Gen`: HotSpot 老年代（旧名称）
- `ZHeap`: ZGC 堆
- `Shenandoah`: Shenandoah GC 堆
- `*balanced-old`: G1 老年代
- `*tenured*`: 包含 "tenured" 的名称
- `JavaHeap`: Java 堆

---

### getUsageValue(MemoryPoolMXBean memoryPoolMXBean, ToLongFunction<MemoryUsage> getter)

**作用**: 获取内存使用值

**实现**:
```java
private double getUsageValue(MemoryPoolMXBean memoryPoolMXBean, ToLongFunction<MemoryUsage> getter) {
    MemoryUsage usage = getUsage(memoryPoolMXBean);
    if (usage == null) {
        return -1;
    }
    return getter.applyAsLong(usage);
}
```

---

### getUsage(MemoryPoolMXBean memoryPoolMXBean)

**作用**: 获取内存使用情况

**实现**:
```java
private MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
    try {
        return memoryPoolMXBean.getUsage();
    } catch (InternalError e) {
        return null;
    }
}
```

## 使用场景

### 1. 系统指标监控

由 `SystemMetricManager` 定时调用：

```java
EXECUTOR.scheduleAtFixedRate(MEMORY_METRICS_CAPTOR, 0, 2, TimeUnit.SECONDS);
```

### 2. 获取内存使用率

```java
double memoryUsage = SystemMetricManager.getLongLivedMemoryUsage();
```

## 设计特点

### 1. 定时捕获

定时捕获内存指标，保证数据实时性。

### 2. 多 GC 支持

支持多种 GC 算法的老年代内存池识别。

### 3. 异常处理

捕获异常，避免影响系统运行。

## 注意事项

1. **捕获间隔**: 每 2 秒捕获一次
2. **初始化**: 第一次调用时返回 -1
3. **GC 兼容**: 支持多种 GC 算法，但可能不覆盖所有情况

