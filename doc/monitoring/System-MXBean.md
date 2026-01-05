# 系统提供的 MXBean

## 概述

Java 平台提供了多个内置的 MXBean（Management Extension Bean），用于监控和管理 JVM 和系统资源。这些 MXBean 通过 `ManagementFactory` 获取，提供了丰富的 JVM 和系统信息。

### 核心定位

- **JVM 监控**: 监控 JVM 内存、线程、运行时等信息
- **系统监控**: 监控操作系统 CPU、内存等信息
- **标准接口**: Java 平台标准的管理接口
- **无需配置**: 开箱即用，无需额外配置

### 主要特性

1. **开箱即用**: Java 平台内置，无需额外依赖
2. **丰富信息**: 提供 JVM 和系统的详细信息
3. **标准接口**: 遵循 JMX 标准
4. **工具支持**: 支持 JConsole、VisualVM 等工具查看

## 获取 MXBean 的方式

### ManagementFactory

`ManagementFactory` 是获取所有平台 MXBean 的工厂类：

```java
import java.lang.management.ManagementFactory;

// 获取单个 MXBean
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

// 获取平台 MXBean（推荐）
OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

// 获取多个 MXBean
List<MemoryPoolMXBean> memoryPools = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
```

## 系统提供的 MXBean

### 1. MemoryMXBean（内存管理）

用于监控 JVM 内存使用情况。

#### 获取方式

```java
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
```

#### 核心方法

```java
// 获取堆内存使用情况
MemoryUsage getHeapMemoryUsage();

// 获取非堆内存使用情况
MemoryUsage getNonHeapMemoryUsage();

// 获取已使用的堆内存
long getHeapMemoryUsed();

// 获取堆内存最大值
long getHeapMemoryMax();

// 触发垃圾回收
void gc();
```

#### 使用示例

```java
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

// 获取堆内存使用情况
MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
System.out.println("堆内存已使用: " + heapUsage.getUsed());
System.out.println("堆内存最大值: " + heapUsage.getMax());
System.out.println("堆内存已提交: " + heapUsage.getCommitted());
System.out.println("堆内存初始值: " + heapUsage.getInit());

// 获取非堆内存使用情况
MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
System.out.println("非堆内存已使用: " + nonHeapUsage.getUsed());

// 触发垃圾回收
memoryBean.gc();
```

#### MemoryUsage 对象

`MemoryUsage` 包含内存使用的详细信息：

```java
public class MemoryUsage {
    private long init;      // 初始内存大小
    private long used;      // 已使用内存大小
    private long committed; // 已提交内存大小
    private long max;       // 最大内存大小（-1 表示无限制）
}
```

### 2. ThreadMXBean（线程管理）

用于监控 JVM 线程信息。

#### 获取方式

```java
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
```

#### 核心方法

```java
// 获取当前线程数
int getThreadCount();

// 获取峰值线程数
int getPeakThreadCount();

// 获取总启动线程数
long getTotalStartedThreadCount();

// 获取所有线程 ID
long[] getAllThreadIds();

// 获取线程信息
ThreadInfo getThreadInfo(long id);

// 获取所有线程信息
ThreadInfo[] getThreadInfo(long[] ids);

// 获取死锁线程
long[] findDeadlockedThreads();

// 是否支持线程 CPU 时间测量
boolean isThreadCpuTimeSupported();

// 获取线程 CPU 时间
long getThreadCpuTime(long id);
```

#### 使用示例

```java
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

// 获取当前线程数
int threadCount = threadBean.getThreadCount();
System.out.println("当前线程数: " + threadCount);

// 获取峰值线程数
int peakThreadCount = threadBean.getPeakThreadCount();
System.out.println("峰值线程数: " + peakThreadCount);

// 获取总启动线程数
long totalStarted = threadBean.getTotalStartedThreadCount();
System.out.println("总启动线程数: " + totalStarted);

// 获取所有线程信息
long[] threadIds = threadBean.getAllThreadIds();
ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);
for (ThreadInfo info : threadInfos) {
    if (info != null) {
        System.out.println("线程名: " + info.getThreadName());
        System.out.println("线程状态: " + info.getThreadState());
        System.out.println("CPU 时间: " + threadBean.getThreadCpuTime(info.getThreadId()));
    }
}

// 检测死锁
long[] deadlockedThreads = threadBean.findDeadlockedThreads();
if (deadlockedThreads != null) {
    System.out.println("发现死锁线程: " + Arrays.toString(deadlockedThreads));
}
```

#### ThreadInfo 对象

`ThreadInfo` 包含线程的详细信息：

```java
public class ThreadInfo {
    String getThreadName();           // 线程名称
    long getThreadId();               // 线程 ID
    Thread.State getThreadState();    // 线程状态
    long getBlockedTime();            // 阻塞时间
    long getWaitedTime();             // 等待时间
    String getLockName();             // 锁名称
    StackTraceElement[] getStackTrace(); // 堆栈跟踪
}
```

### 3. RuntimeMXBean（运行时管理）

用于监控 JVM 运行时信息。

#### 获取方式

```java
RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
```

#### 核心方法

```java
// 获取 JVM 名称
String getVmName();

// 获取 JVM 版本
String getVmVersion();

// 获取 JVM 供应商
String getVmVendor();

// 获取 JVM 启动时间
long getStartTime();

// 获取 JVM 运行时间
long getUptime();

// 获取系统属性
String getSystemProperty(String key);

// 获取所有系统属性
Map<String, String> getSystemProperties();

// 获取 JVM 输入参数
List<String> getInputArguments();

// 获取类路径
String getClassPath();

// 获取库路径
String getLibraryPath();
```

#### 使用示例

```java
RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

// 获取 JVM 信息
System.out.println("JVM 名称: " + runtimeBean.getVmName());
System.out.println("JVM 版本: " + runtimeBean.getVmVersion());
System.out.println("JVM 供应商: " + runtimeBean.getVmVendor());

// 获取运行时间
long uptime = runtimeBean.getUptime();
System.out.println("JVM 运行时间: " + uptime + " ms");

// 获取启动时间
long startTime = runtimeBean.getStartTime();
Date startDate = new Date(startTime);
System.out.println("JVM 启动时间: " + startDate);

// 获取系统属性
String javaVersion = runtimeBean.getSystemProperty("java.version");
System.out.println("Java 版本: " + javaVersion);

// 获取所有系统属性
Map<String, String> props = runtimeBean.getSystemProperties();
props.forEach((key, value) -> System.out.println(key + " = " + value));

// 获取 JVM 输入参数
List<String> args = runtimeBean.getInputArguments();
System.out.println("JVM 参数: " + args);
```

### 4. OperatingSystemMXBean（操作系统管理）

用于监控操作系统信息。

#### 获取方式

```java
OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
// 或使用平台方法（推荐，支持扩展接口）
OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
```

#### 核心方法

```java
// 获取操作系统名称
String getName();

// 获取操作系统架构
String getArch();

// 获取操作系统版本
String getVersion();

// 获取可用处理器数
int getAvailableProcessors();

// 获取系统平均负载（Unix/Linux）
double getSystemLoadAverage();

// 获取系统 CPU 使用率（扩展方法，需要转换为具体实现）
double getSystemCpuLoad();

// 获取进程 CPU 使用率（扩展方法）
double getProcessCpuLoad();

// 获取进程 CPU 时间（扩展方法）
long getProcessCpuTime();

// 获取总物理内存（扩展方法）
long getTotalPhysicalMemorySize();

// 获取空闲物理内存（扩展方法）
long getFreePhysicalMemorySize();
```

#### 使用示例

```java
OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

// 基本信息
System.out.println("操作系统: " + osBean.getName());
System.out.println("架构: " + osBean.getArch());
System.out.println("版本: " + osBean.getVersion());
System.out.println("可用处理器: " + osBean.getAvailableProcessors());

// 系统负载（Unix/Linux）
double loadAverage = osBean.getSystemLoadAverage();
System.out.println("系统平均负载: " + loadAverage);

// 扩展方法（需要转换为具体实现）
if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
    com.sun.management.OperatingSystemMXBean sunOsBean = 
        (com.sun.management.OperatingSystemMXBean) osBean;
    
    // 系统 CPU 使用率
    double systemCpuLoad = sunOsBean.getSystemCpuLoad();
    System.out.println("系统 CPU 使用率: " + systemCpuLoad);
    
    // 进程 CPU 使用率
    double processCpuLoad = sunOsBean.getProcessCpuLoad();
    System.out.println("进程 CPU 使用率: " + processCpuLoad);
    
    // 进程 CPU 时间
    long processCpuTime = sunOsBean.getProcessCpuTime();
    System.out.println("进程 CPU 时间: " + processCpuTime + " ns");
    
    // 物理内存
    long totalMemory = sunOsBean.getTotalPhysicalMemorySize();
    long freeMemory = sunOsBean.getFreePhysicalMemorySize();
    System.out.println("总物理内存: " + totalMemory);
    System.out.println("空闲物理内存: " + freeMemory);
}
```

#### 在项目中的使用

```java
// OperatingSystemBeanManager.java
static {
    OPERATING_SYSTEM_BEAN = ManagementFactory.getOperatingSystemMXBean();
    // 使用反射调用扩展方法，兼容不同 JVM
}

// SystemMetricManager.java
public static String getSystemMetric() {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    double systemAvgLoad = osBean.getSystemLoadAverage();
    double systemCpuUsage = OperatingSystemBeanManager.getSystemCpuUsage();
    int cpuCores = osBean.getAvailableProcessors();
    // ...
}
```

### 5. MemoryPoolMXBean（内存池管理）

用于监控各个内存池的使用情况。

#### 获取方式

```java
List<MemoryPoolMXBean> memoryPools = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
```

#### 核心方法

```java
// 获取内存池名称
String getName();

// 获取内存池类型
MemoryType getType();

// 获取内存使用情况
MemoryUsage getUsage();

// 获取峰值内存使用情况
MemoryUsage getPeakUsage();

// 获取内存使用阈值
long getUsageThreshold();

// 设置内存使用阈值
void setUsageThreshold(long threshold);

// 是否支持使用阈值
boolean isUsageThresholdSupported();

// 是否超过使用阈值
boolean isUsageThresholdExceeded();
```

#### 使用示例

```java
List<MemoryPoolMXBean> memoryPools = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);

for (MemoryPoolMXBean pool : memoryPools) {
    System.out.println("内存池名称: " + pool.getName());
    System.out.println("内存池类型: " + pool.getType());
    
    MemoryUsage usage = pool.getUsage();
    if (usage != null) {
        System.out.println("已使用: " + usage.getUsed());
        System.out.println("最大值: " + usage.getMax());
        System.out.println("已提交: " + usage.getCommitted());
    }
    
    // 检查是否超过阈值
    if (pool.isUsageThresholdSupported() && pool.isUsageThresholdExceeded()) {
        System.out.println("警告: 内存池 " + pool.getName() + " 超过使用阈值");
    }
}
```

#### 在项目中的使用

```java
// MemoryMetricsCaptor.java
@Override
public void run() {
    val memoryPoolBeans = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
    for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
        String name = memoryPoolBean.getName();
        boolean isLongLivedPool = isLongLivedPool(name);
        if (isLongLivedPool) {
            MemoryUsage usage = memoryPoolBean.getUsage();
            used = usage.getUsed();
            max = usage.getMax();
            break;
        }
    }
}
```

### 6. GarbageCollectorMXBean（垃圾收集器管理）

用于监控垃圾收集器信息。

#### 获取方式

```java
List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getPlatformMXBeans(GarbageCollectorMXBean.class);
```

#### 核心方法

```java
// 获取垃圾收集器名称
String getName();

// 获取收集次数
long getCollectionCount();

// 获取收集时间
long getCollectionTime();

// 获取管理的内存池名称
String[] getMemoryPoolNames();
```

#### 使用示例

```java
List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getPlatformMXBeans(GarbageCollectorMXBean.class);

for (GarbageCollectorMXBean gcBean : gcBeans) {
    System.out.println("GC 名称: " + gcBean.getName());
    System.out.println("收集次数: " + gcBean.getCollectionCount());
    System.out.println("收集时间: " + gcBean.getCollectionTime() + " ms");
    
    String[] pools = gcBean.getMemoryPoolNames();
    System.out.println("管理的内存池: " + Arrays.toString(pools));
}
```

### 7. CompilationMXBean（编译管理）

用于监控 JIT 编译器信息。

#### 获取方式

```java
CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
```

#### 核心方法

```java
// 获取编译器名称
String getName();

// 获取总编译时间
long getTotalCompilationTime();

// 是否支持编译时间监控
boolean isCompilationTimeMonitoringSupported();
```

#### 使用示例

```java
CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();

if (compilationBean.isCompilationTimeMonitoringSupported()) {
    System.out.println("编译器名称: " + compilationBean.getName());
    System.out.println("总编译时间: " + compilationBean.getTotalCompilationTime() + " ms");
}
```

## 在项目中的使用

### 1. 系统指标监控

```java
// SystemMetricManager.java
public static String getSystemMetric() {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    double systemAvgLoad = osBean.getSystemLoadAverage();
    double systemCpuUsage = OperatingSystemBeanManager.getSystemCpuUsage();
    int cpuCores = osBean.getAvailableProcessors();
    // ...
}
```

### 2. CPU 指标捕获

```java
// CpuMetricsCaptor.java
OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
RuntimeMXBean runtimeBean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
// 计算 CPU 使用率
```

### 3. 内存指标捕获

```java
// MemoryMetricsCaptor.java
List<MemoryPoolMXBean> memoryPools = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
for (MemoryPoolMXBean pool : memoryPools) {
    if (isLongLivedPool(pool.getName())) {
        MemoryUsage usage = pool.getUsage();
        // 获取老年代内存使用情况
    }
}
```

## 使用工具查看

### 1. JConsole

1. 打开 JConsole
2. 连接到应用
3. 在 **MBeans** 标签页查看：
   - `java.lang:type=Memory` - 内存信息
   - `java.lang:type=Threading` - 线程信息
   - `java.lang:type=Runtime` - 运行时信息
   - `java.lang:type=OperatingSystem` - 操作系统信息
   - `java.lang:type=MemoryPool,name=*` - 内存池信息
   - `java.lang:type=GarbageCollector,name=*` - GC 信息

### 2. VisualVM

1. 打开 VisualVM
2. 连接到应用
3. 在 **MBeans** 标签页查看系统 MXBean
4. 在 **Monitor** 标签页查看内存和 CPU 图表

### 3. 程序化访问

```java
import java.lang.management.*;

// 获取 MBeanServer
MBeanServer server = ManagementFactory.getPlatformMBeanServer();

// 查询所有内存相关的 MBean
Set<ObjectName> memoryBeans = server.queryNames(
    new ObjectName("java.lang:type=Memory,*"),
    null
);

// 获取内存使用情况
ObjectName memoryName = new ObjectName("java.lang:type=Memory");
Object heapUsage = server.getAttribute(memoryName, "HeapMemoryUsage");
System.out.println("堆内存使用: " + heapUsage);
```

## 完整示例

### 系统监控工具

```java
import java.lang.management.*;
import java.util.List;

public class SystemMonitor {
    
    public static void printSystemInfo() {
        // 1. 运行时信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        System.out.println("=== JVM 信息 ===");
        System.out.println("JVM 名称: " + runtimeBean.getVmName());
        System.out.println("JVM 版本: " + runtimeBean.getVmVersion());
        System.out.println("运行时间: " + runtimeBean.getUptime() + " ms");
        
        // 2. 内存信息
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.out.println("\n=== 内存信息 ===");
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        System.out.println("堆内存已使用: " + formatBytes(heapUsage.getUsed()));
        System.out.println("堆内存最大值: " + formatBytes(heapUsage.getMax()));
        System.out.println("堆内存使用率: " + 
            (heapUsage.getUsed() * 100.0 / heapUsage.getMax()) + "%");
        
        // 3. 线程信息
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        System.out.println("\n=== 线程信息 ===");
        System.out.println("当前线程数: " + threadBean.getThreadCount());
        System.out.println("峰值线程数: " + threadBean.getPeakThreadCount());
        System.out.println("总启动线程数: " + threadBean.getTotalStartedThreadCount());
        
        // 4. 操作系统信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        System.out.println("\n=== 操作系统信息 ===");
        System.out.println("操作系统: " + osBean.getName());
        System.out.println("架构: " + osBean.getArch());
        System.out.println("可用处理器: " + osBean.getAvailableProcessors());
        System.out.println("系统负载: " + osBean.getSystemLoadAverage());
        
        // 5. 内存池信息
        List<MemoryPoolMXBean> memoryPools = 
            ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
        System.out.println("\n=== 内存池信息 ===");
        for (MemoryPoolMXBean pool : memoryPools) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                System.out.println(pool.getName() + ": " + 
                    formatBytes(usage.getUsed()) + " / " + 
                    formatBytes(usage.getMax()));
            }
        }
        
        // 6. GC 信息
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getPlatformMXBeans(GarbageCollectorMXBean.class);
        System.out.println("\n=== GC 信息 ===");
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println(gcBean.getName() + 
                ": 次数=" + gcBean.getCollectionCount() + 
                ", 时间=" + gcBean.getCollectionTime() + "ms");
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}
```

## 最佳实践

### 1. 使用 PlatformMXBean 方法

```java
// ✅ 推荐：使用 getPlatformMXBean，支持扩展接口
OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

// ⚠️ 可以：使用 getOperatingSystemMXBean，但可能不支持扩展方法
OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
```

### 2. 处理扩展方法

```java
// 检查是否为扩展实现
if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
    com.sun.management.OperatingSystemMXBean sunOsBean = 
        (com.sun.management.OperatingSystemMXBean) osBean;
    double cpuLoad = sunOsBean.getSystemCpuLoad();
}
```

### 3. 异常处理

```java
try {
    MemoryUsage usage = memoryPoolBean.getUsage();
    if (usage != null) {
        // 使用 usage
    }
} catch (InternalError e) {
    // 处理异常（某些情况下 getUsage() 可能抛出 InternalError）
    log.warn("Failed to get memory usage", e);
}
```

### 4. 性能考虑

```java
// 缓存 MXBean 实例，避免重复获取
private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
```

## 常见问题

### 1. 扩展方法不可用

**问题**: 调用扩展方法（如 `getSystemCpuLoad()`）时抛出异常。

**原因**: 标准接口不包含扩展方法，需要转换为具体实现。

**解决**:
```java
if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
    com.sun.management.OperatingSystemMXBean sunOsBean = 
        (com.sun.management.OperatingSystemMXBean) osBean;
    double cpuLoad = sunOsBean.getSystemCpuLoad();
}
```

### 2. 内存使用信息为 null

**问题**: `MemoryPoolMXBean.getUsage()` 返回 null。

**原因**: 某些内存池可能不支持使用情况查询。

**解决**:
```java
MemoryUsage usage = memoryPoolBean.getUsage();
if (usage != null) {
    // 使用 usage
}
```

### 3. 系统负载不可用

**问题**: `getSystemLoadAverage()` 返回 -1。

**原因**: 某些操作系统不支持系统负载查询。

**解决**:
```java
double loadAverage = osBean.getSystemLoadAverage();
if (loadAverage >= 0) {
    // 使用负载值
} else {
    // 使用其他指标（如 CPU 使用率）
}
```

## 总结

系统提供的 MXBean 是 Java 平台强大的监控工具：

1. **MemoryMXBean**: 监控 JVM 内存使用
2. **ThreadMXBean**: 监控线程信息
3. **RuntimeMXBean**: 监控 JVM 运行时信息
4. **OperatingSystemMXBean**: 监控操作系统信息
5. **MemoryPoolMXBean**: 监控各个内存池
6. **GarbageCollectorMXBean**: 监控垃圾收集器
7. **CompilationMXBean**: 监控 JIT 编译器

通过 `ManagementFactory` 可以方便地获取这些 MXBean，实现对 JVM 和系统的全面监控。

