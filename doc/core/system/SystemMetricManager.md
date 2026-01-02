# SystemMetricManager

## 概述

`SystemMetricManager` 是系统指标管理器，负责管理 CPU 和内存指标。它定时捕获系统指标，并提供获取系统指标的方法。

## 核心作用

1. **系统指标管理**: 管理 CPU 和内存指标捕获器
2. **定时捕获**: 定时捕获系统指标
3. **指标获取**: 提供获取系统指标的方法

## 核心属性

```java
private static final CpuMetricsCaptor CPU_METRICS_CAPTOR;        // CPU 指标捕获器
private static final MemoryMetricsCaptor MEMORY_METRICS_CAPTOR;  // 内存指标捕获器
private static final ScheduledExecutorService EXECUTOR;          // 定时执行器
```

## 初始化

在静态代码块中初始化：

```java
static {
    CPU_METRICS_CAPTOR = new CpuMetricsCaptor();
    MEMORY_METRICS_CAPTOR = new MemoryMetricsCaptor();
    EXECUTOR = ThreadPoolCreator.newScheduledThreadPool("dtp-system-metric", 1);
    EXECUTOR.scheduleAtFixedRate(CPU_METRICS_CAPTOR, 0, 2, TimeUnit.SECONDS);
    EXECUTOR.scheduleAtFixedRate(MEMORY_METRICS_CAPTOR, 0, 2, TimeUnit.SECONDS);
}
```

**说明**: 
- 每 2 秒捕获一次 CPU 和内存指标
- 使用单线程定时执行器

## 核心方法

### getSystemMetric()

**作用**: 获取系统指标字符串

**实现**:
```java
public static String getSystemMetric() {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    double systemAvgLoad = osBean.getSystemLoadAverage();
    double systemCpuUsage = OperatingSystemBeanManager.getSystemCpuUsage();
    int cpuCores = osBean.getAvailableProcessors();
    return String.format("SystemMetric{sAvgLoad=%.2f, sCpuUsage=%.2f, pCpuUsage=%.2f, cpuCores=%d, oldMemUsage=%.2f}",
            systemAvgLoad, systemCpuUsage, getProcessCpuUsage(), cpuCores, getLongLivedMemoryUsage());
}
```

**指标内容**:
- `sAvgLoad`: 系统平均负载
- `sCpuUsage`: 系统 CPU 使用率
- `pCpuUsage`: 进程 CPU 使用率
- `cpuCores`: CPU 核心数
- `oldMemUsage`: 老年代内存使用率

---

### getProcessCpuUsage()

**作用**: 获取进程 CPU 使用率

**实现**:
```java
public static double getProcessCpuUsage() {
    return CPU_METRICS_CAPTOR.getProcessCpuUsage();
}
```

---

### getLongLivedMemoryUsage()

**作用**: 获取老年代内存使用率

**实现**:
```java
public static double getLongLivedMemoryUsage() {
    return MEMORY_METRICS_CAPTOR.getLongLivedMemoryUsage();
}
```

---

### destroy()

**作用**: 销毁管理器

**实现**:
```java
public static void destroy() {
    EXECUTOR.shutdown();
}
```

## 使用场景

### 1. 获取系统指标

```java
String systemMetric = SystemMetricManager.getSystemMetric();
// 输出: SystemMetric{sAvgLoad=0.50, sCpuUsage=25.00, pCpuUsage=10.00, cpuCores=4, oldMemUsage=60.00}
```

### 2. 获取 CPU 使用率

```java
double cpuUsage = SystemMetricManager.getProcessCpuUsage();
```

### 3. 获取内存使用率

```java
double memoryUsage = SystemMetricManager.getLongLivedMemoryUsage();
```

## 设计特点

### 1. 定时捕获

定时捕获系统指标，保证数据实时性。

### 2. 单例管理

使用静态方法管理，全局唯一。

### 3. 参考 Sentinel

参考 Sentinel 的 `SystemStatusListener` 实现。

## 注意事项

1. **捕获间隔**: 每 2 秒捕获一次
2. **资源消耗**: 定时捕获会消耗一定的系统资源
3. **线程安全**: 指标捕获器需要保证线程安全

