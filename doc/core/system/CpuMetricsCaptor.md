# CpuMetricsCaptor

## 概述

`CpuMetricsCaptor` 是 CPU 指标捕获器，实现了 `Runnable` 接口。它定时捕获进程的 CPU 使用率。

## 核心作用

1. **CPU 监控**: 定时捕获进程的 CPU 使用率
2. **数据计算**: 计算进程 CPU 使用率
3. **数据提供**: 提供进程 CPU 使用率数据

## 核心属性

```java
private double currProcessCpuUsage = -1;  // 当前进程 CPU 使用率
private long prevProcessCpuTime = 0;      // 上次进程 CPU 时间
private long prevUpTime = 0;              // 上次运行时间
```

## 核心方法

### getProcessCpuUsage()

**作用**: 获取进程 CPU 使用率

**返回**: 进程 CPU 使用率（0.0 - 1.0，-1 表示未初始化）

---

### run()

**作用**: 执行 CPU 指标捕获

**实现**:
```java
@Override
public void run() {
    try {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        int cpuCores = osBean.getAvailableProcessors();
        
        long newProcessCpuTime = OperatingSystemBeanManager.getProcessCpuTime();
        RuntimeMXBean runtimeBean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
        long newUpTime = runtimeBean.getUptime();
        
        // 计算 CPU 使用率
        long elapsedCpu = TimeUnit.NANOSECONDS.toMillis(newProcessCpuTime - prevProcessCpuTime);
        long elapsedTime = newUpTime - prevUpTime;
        double processCpuUsage = (double) elapsedCpu / elapsedTime / cpuCores;
        
        // 更新状态
        prevProcessCpuTime = newProcessCpuTime;
        prevUpTime = newUpTime;
        currProcessCpuUsage = processCpuUsage;
    } catch (Throwable e) {
        log.error("Get system metrics error.", e);
    }
}
```

**计算逻辑**:
1. 获取 CPU 核心数
2. 获取当前进程 CPU 时间和运行时间
3. 计算时间差
4. 计算 CPU 使用率：`(CPU 时间差 / 运行时间差) / CPU 核心数`

## 使用场景

### 1. 系统指标监控

由 `SystemMetricManager` 定时调用：

```java
EXECUTOR.scheduleAtFixedRate(CPU_METRICS_CAPTOR, 0, 2, TimeUnit.SECONDS);
```

### 2. 获取 CPU 使用率

```java
double cpuUsage = SystemMetricManager.getProcessCpuUsage();
```

## 设计特点

### 1. 定时捕获

定时捕获 CPU 指标，保证数据实时性。

### 2. 增量计算

通过增量计算 CPU 使用率，避免累计误差。

### 3. 异常处理

捕获异常，避免影响系统运行。

## 注意事项

1. **捕获间隔**: 每 2 秒捕获一次
2. **初始化**: 第一次调用时返回 -1
3. **精度**: CPU 使用率精度取决于捕获间隔

