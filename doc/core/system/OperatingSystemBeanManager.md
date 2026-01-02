# OperatingSystemBeanManager

## 概述

`OperatingSystemBeanManager` 是操作系统 Bean 管理器，提供系统信息的访问。它封装了不同 JVM 实现（HotSpot、J9）的差异，提供统一的系统信息访问接口。

## 核心作用

1. **系统信息访问**: 提供系统信息的访问接口
2. **JVM 兼容**: 兼容不同 JVM 实现（HotSpot、J9）
3. **方法反射**: 使用反射调用 JVM 特定方法

## 核心属性

```java
private static final OperatingSystemMXBean OPERATING_SYSTEM_BEAN;  // 操作系统 Bean
private static final Class<?> OPERATING_SYSTEM_BEAN_CLASS;        // Bean 类
private static final Method SYSTEM_CPU_USAGE_METHOD;               // 系统 CPU 使用率方法
private static final Method PROCESS_CPU_TIME_METHOD;              // 进程 CPU 时间方法
private static final Method FREE_PHYSICAL_MEM_METHOD;             // 空闲物理内存方法
private static final Method TOTAL_PHYSICAL_MEM_METHOD;            // 总物理内存方法
```

## 初始化

在静态代码块中初始化：

```java
static {
    OPERATING_SYSTEM_BEAN = ManagementFactory.getOperatingSystemMXBean();
    OPERATING_SYSTEM_BEAN_CLASS = loadOne(OPERATING_SYSTEM_BEAN_CLASS_NAMES);
    SYSTEM_CPU_USAGE_METHOD = deduceMethod("getSystemCpuLoad");
    PROCESS_CPU_TIME_METHOD = deduceMethod("getProcessCpuTime");
    
    Method totalPhysicalMem = deduceMethod("getTotalPhysicalMemorySize");
    // getTotalPhysicalMemory for ibm jdk 7.
    TOTAL_PHYSICAL_MEM_METHOD = totalPhysicalMem != null ? totalPhysicalMem :
            deduceMethod("getTotalPhysicalMemory");
    
    FREE_PHYSICAL_MEM_METHOD = deduceMethod("getFreePhysicalMemorySize");
}
```

**支持的 JVM**:
- HotSpot: `com.sun.management.OperatingSystemMXBean`
- J9: `com.ibm.lang.management.OperatingSystemMXBean`

## 核心方法

### getOperatingSystemBean()

**作用**: 获取操作系统 Bean

**返回**: `OperatingSystemMXBean` 实例

---

### getSystemCpuUsage()

**作用**: 获取系统 CPU 使用率

**实现**:
```java
public static double getSystemCpuUsage() {
    return MethodUtil.invokeAndReturnDouble(SYSTEM_CPU_USAGE_METHOD, OPERATING_SYSTEM_BEAN);
}
```

**返回**: 系统 CPU 使用率（0.0 - 1.0）

---

### getProcessCpuTime()

**作用**: 获取进程 CPU 时间

**实现**:
```java
public static long getProcessCpuTime() {
    return MethodUtil.invokeAndReturnLong(PROCESS_CPU_TIME_METHOD, OPERATING_SYSTEM_BEAN);
}
```

**返回**: 进程 CPU 时间（纳秒）

---

### getTotalPhysicalMem() / getFreePhysicalMem()

**作用**: 获取总/空闲物理内存

**实现**:
```java
public static long getTotalPhysicalMem() {
    return MethodUtil.invokeAndReturnLong(TOTAL_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
}

public static long getFreePhysicalMem() {
    return MethodUtil.invokeAndReturnLong(FREE_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
}
```

**返回**: 内存大小（字节）

---

### loadOne(List<String> classNames)

**作用**: 加载类（尝试多个类名）

**实现**:
```java
private static Class<?> loadOne(List<String> classNames) {
    for (String className : classNames) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn("Failed to load operating system bean class.", e);
        }
    }
    return null;
}
```

**说明**: 按顺序尝试加载类，直到成功或全部失败

---

### deduceMethod(String name)

**作用**: 推导方法（通过反射）

**实现**:
```java
private static Method deduceMethod(String name) {
    if (Objects.isNull(OPERATING_SYSTEM_BEAN_CLASS)) {
        return null;
    }
    try {
        OPERATING_SYSTEM_BEAN_CLASS.cast(OPERATING_SYSTEM_BEAN);
        return OPERATING_SYSTEM_BEAN_CLASS.getDeclaredMethod(name);
    } catch (Exception e) {
        return null;
    }
}
```

**说明**: 通过反射获取方法，如果失败返回 `null`

## 使用场景

### 1. 获取系统信息

```java
double systemCpuUsage = OperatingSystemBeanManager.getSystemCpuUsage();
long processCpuTime = OperatingSystemBeanManager.getProcessCpuTime();
long totalMem = OperatingSystemBeanManager.getTotalPhysicalMem();
long freeMem = OperatingSystemBeanManager.getFreePhysicalMem();
```

### 2. 系统指标监控

由 `SystemMetricManager` 使用：

```java
double systemCpuUsage = OperatingSystemBeanManager.getSystemCpuUsage();
```

## 设计特点

### 1. JVM 兼容

兼容不同 JVM 实现，自动选择合适的方法。

### 2. 反射调用

使用反射调用 JVM 特定方法，避免编译时依赖。

### 3. 优雅降级

如果方法不存在，返回 `null` 或默认值，不影响系统运行。

## 注意事项

1. **JVM 差异**: 不同 JVM 实现的方法名可能不同
2. **方法可用性**: 某些方法可能在某些 JVM 上不可用
3. **性能影响**: 反射调用会有一定的性能开销

