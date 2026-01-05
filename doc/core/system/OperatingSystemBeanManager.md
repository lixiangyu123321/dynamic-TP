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

# 详解 `OperatingSystemBeanManager` 代码
这是一个**兼容不同 JVM 实现（HotSpot/J9）的操作系统信息获取工具类**，核心通过反射技术突破 JDK 标准 `OperatingSystemMXBean` 的功能限制，获取更详细的系统CPU、物理内存等信息，下面逐部分拆解详解。

## 一、整体核心定位
1.  **设计目标**：兼容 Sun/Oracle HotSpot JVM 和 IBM J9 JVM，获取标准 `java.lang.management.OperatingSystemMXBean` 不提供的高级系统监控指标（系统CPU使用率、进程CPU耗时、物理内存总量/空闲量）。
2.  **核心技术**：反射（解决不同 JVM 实现的 `OperatingSystemMXBean` 非标准 API 兼容问题）、静态初始化（保证资源仅加载一次，提升性能）、Slf4j 日志记录。
3.  **访问方式**：提供静态工具方法，无需实例化（私有构造方法），符合工具类设计规范。

## 二、逐部分代码详解
### 1. 类注解与成员变量定义
```java
@Slf4j
public class OperatingSystemBeanManager {
    /**
     * com.ibm for J9
     * com.sun for HotSpot
     * 操作系统管理类
     */
    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
            "com.sun.management.OperatingSystemMXBean", "com.ibm.lang.management.OperatingSystemMXBean");

    private static final OperatingSystemMXBean OPERATING_SYSTEM_BEAN;

    private static final Class<?> OPERATING_SYSTEM_BEAN_CLASS;

    private static final Method SYSTEM_CPU_USAGE_METHOD;

    private static final Method PROCESS_CPU_TIME_METHOD;

    private static final Method FREE_PHYSICAL_MEM_METHOD;

    private static final Method TOTAL_PHYSICAL_MEM_METHOD;
```
- **`@Slf4j`**：Lombok 注解，自动生成 `org.slf4j.Logger` 实例 `log`，用于记录日志（无需手动声明 `private static final Logger log = LoggerFactory.getLogger(...)`）。
- **`OPERATING_SYSTEM_BEAN_CLASS_NAMES`**：
    - 存储两个非标准 JVM 实现的 `OperatingSystemMXBean` 全类名，用于兼容不同 JVM：
        - `com.sun.management.OperatingSystemMXBean`：Sun/Oracle HotSpot JVM（主流 JVM，如 JDK8/11/17 官方版本）的扩展实现。
        - `com.ibm.lang.management.OperatingSystemMXBean`：IBM J9 JVM（主要用于 IBM WebSphere 等中间件）的扩展实现。
    - 为什么不用标准 API？：JDK 标准 `java.lang.management.OperatingSystemMXBean` 仅提供少量基础信息（如操作系统名称、架构），无 CPU 使用率、物理内存详情等高级指标，而不同 JVM 厂商提供了各自的扩展实现，需通过反射兼容。
- **静态常量成员（后续静态代码块初始化）**：
    - `OPERATING_SYSTEM_BEAN`：JVM 提供的操作系统 MXBean 实例（核心操作对象）。
    - `OPERATING_SYSTEM_BEAN_CLASS`：成功加载的 JVM 专属 `OperatingSystemMXBean` 字节码对象（反射核心）。
    - 四个 `Method` 常量：反射获取的目标方法对象，对应高级系统指标的获取方法，避免多次反射获取方法，提升性能。

### 2. 静态代码块（核心初始化逻辑）
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
静态代码块的特点是**类加载时执行，且仅执行一次**，这里用于完成所有核心资源的初始化，避免每次调用工具方法都重复加载/反射，是工具类的常用优化手段。

逐行解析：
1.  `OPERATING_SYSTEM_BEAN = ManagementFactory.getOperatingSystemMXBean();`
    - 通过 JDK 标准 `ManagementFactory` 获取操作系统 MXBean 实例，该实例实际是当前 JVM 厂商的扩展实现（HotSpot/J9），向上转型为标准 `OperatingSystemMXBean`。
2.  `OPERATING_SYSTEM_BEAN_CLASS = loadOne(OPERATING_SYSTEM_BEAN_CLASS_NAMES);`
    - 调用自定义 `loadOne` 方法，加载列表中第一个能成功找到的 JVM 专属 `OperatingSystemMXBean` 类，返回其 `Class<?>` 对象。
    - 若两个类都加载失败（如非 HotSpot/J9 JVM），返回 `null`，后续相关方法也会返回 `null`，不会抛出异常，保证兼容性。
3.  `SYSTEM_CPU_USAGE_METHOD = deduceMethod("getSystemCpuLoad");`：反射获取「系统CPU使用率」方法对象。
4.  `PROCESS_CPU_TIME_METHOD = deduceMethod("getProcessCpuTime");`：反射获取「当前进程CPU耗时」方法对象。
5.  物理内存总量方法兼容：
    - 先尝试获取 `getTotalPhysicalMemorySize`（HotSpot JVM 及高版本 J9 JVM 的方法名）。
    - 若获取失败（如 IBM JDK 7），则尝试获取 `getTotalPhysicalMemory`（低版本 J9 JVM 的方法名），最终赋值给 `TOTAL_PHYSICAL_MEM_METHOD`，解决不同 JVM 版本的方法名差异问题。
6.  `FREE_PHYSICAL_MEM_METHOD = deduceMethod("getFreePhysicalMemorySize");`：反射获取「空闲物理内存大小」方法对象。

### 3. 私有构造方法
```java
private OperatingSystemBeanManager() { }
```
- 工具类的典型设计：**私有化构造方法，禁止外部通过 `new` 关键字实例化**。
- 因为该类的所有功能都通过静态方法提供，无需创建实例，私有化构造方法可以避免不必要的实例化，同时防止类被继承（子类无法调用父类私有构造方法）。

### 4. 公开静态工具方法（对外提供功能）
这部分是类的「对外接口」，提供了简洁的方法供外部调用，无需关心底层反射细节。
```java
// 获取操作系统 MXBean 实例（供外部扩展使用）
public static OperatingSystemMXBean getOperatingSystemBean() {
    return OPERATING_SYSTEM_BEAN;
}

// 获取系统CPU使用率（返回 double 类型，通常是 0.0~1.0 的比例值）
public static double getSystemCpuUsage() {
    return MethodUtil.invokeAndReturnDouble(SYSTEM_CPU_USAGE_METHOD, OPERATING_SYSTEM_BEAN);
}

// 获取当前进程CPU耗时（返回 long 类型，单位通常是纳秒）
public static long getProcessCpuTime() {
    return MethodUtil.invokeAndReturnLong(PROCESS_CPU_TIME_METHOD, OPERATING_SYSTEM_BEAN);
}

// 获取物理内存总量（返回 long 类型，单位：字节）
public static long getTotalPhysicalMem() {
    return MethodUtil.invokeAndReturnLong(TOTAL_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
}

// 获取空闲物理内存大小（返回 long 类型，单位：字节）
public static long getFreePhysicalMem() {
    return MethodUtil.invokeAndReturnLong(FREE_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
}
```
- 特点1：所有方法都是 `public static`，外部可直接通过 `OperatingSystemBeanManager.getSystemCpuUsage()` 调用。
- 特点2：委托给 `MethodUtil` 工具类执行反射方法调用，封装了反射调用的异常处理、参数传递等细节，简化当前类逻辑。
- 返回值适配：根据方法功能返回对应类型（`double` 对应 CPU 使用率，`long` 对应内存/耗时），符合业务使用习惯。

### 5. 私有辅助方法（内部支撑逻辑）
这部分方法仅在类内部使用，封装了重复的反射逻辑，提升代码可维护性。

#### （1）`loadOne` 方法：加载兼容的 JVM 扩展类
```java
private static Class<?> loadOne(List<String> classNames) {
    for (String className : classNames) {
        try {
            // 加载指定全类名的类，返回 Class<?> 对象
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // 加载失败时记录警告日志，继续尝试下一个类
            log.warn("Failed to load operating system bean class.", e);
        }
    }
    // 所有类都加载失败，返回 null
    return null;
}
```
- 核心逻辑：遍历类名列表，尝试加载每个类，**一旦加载成功立即返回**（保证优先加载 HotSpot 的实现，符合主流场景）。
- 异常处理：捕获 `ClassNotFoundException`（类不存在时抛出），记录警告日志但不中断程序，保证对非目标 JVM 的兼容性。
- 返回值：成功加载返回 `Class<?>`，全部失败返回 `null`，为后续方法提供容错基础。

#### （2）`deduceMethod` 方法：反射获取指定名称的无参方法
```java
private static Method deduceMethod(String name) {
    // 前置判断：若兼容类未加载成功，直接返回 null
    if (Objects.isNull(OPERATING_SYSTEM_BEAN_CLASS)) {
        return null;
    }
    try {
        // 类型校验：验证当前 OPERATING_SYSTEM_BEAN 是 OPERATING_SYSTEM_BEAN_CLASS 的实例
        OPERATING_SYSTEM_BEAN_CLASS.cast(OPERATING_SYSTEM_BEAN);
        // 反射获取无参的声明方法（getDeclaredMethod 可获取所有访问权限的方法，包括 private）
        return OPERATING_SYSTEM_BEAN_CLASS.getDeclaredMethod(name);
    } catch (Exception e) {
        // 捕获所有异常（类型转换异常、方法不存在异常等），返回 null
        return null;
    }
}
```
- 前置容错：先判断兼容类是否加载成功，避免空指针异常。
- 关键步骤：
    1.  `OPERATING_SYSTEM_BEAN_CLASS.cast(OPERATING_SYSTEM_BEAN)`：类型转换校验，确保当前获取的 MXBean 实例是已加载的扩展类的实例，避免后续反射调用出现类型不匹配问题。
    2.  `OPERATING_SYSTEM_BEAN_CLASS.getDeclaredMethod(name)`：反射获取指定名称的**无参方法**（因为目标监控方法均无参数），`getDeclaredMethod` 相比 `getMethod`，能获取类的所有声明方法（包括 `private`/`protected`），兼容性更强。
- 异常处理：捕获所有可能的异常（`ClassCastException`、`NoSuchMethodException` 等），直接返回 `null`，不抛出异常，保证工具类的健壮性（外部调用时仅需处理返回值为 0 或 null 的情况，无需额外捕获异常）。

## 三、核心设计亮点与注意事项
### 1. 设计亮点
（1）**兼容性优先**：兼容 HotSpot/J9 两种主流 JVM，甚至低版本 IBM JDK 7，避免因 JVM 差异导致项目部署失败。
（2）**性能优化**：
- 静态初始化：类加载时仅执行一次反射和类加载，后续调用直接复用已初始化的 `Method` 对象。
- 避免重复反射：将 `Method` 对象缓存为静态常量，无需每次调用都重新获取方法。
  （3）**健壮性保障**：
- 所有异常均内部捕获，返回 `null` 而非抛出运行时异常，防止工具类影响主业务流程。
- 私有构造方法，符合工具类设计规范，避免不必要的实例化。
  （4）**封装性良好**：外部无需关心底层反射逻辑，仅需调用简洁的静态方法，降低使用成本。

### 2. 注意事项与潜在风险
（1）**依赖非标准 API**：该类依赖的 `com.sun.management.OperatingSystemMXBean` 和 `com.ibm.lang.management.OperatingSystemMXBean` 是非 JDK 标准 API，可能在未来 JDK 版本中发生变更或被移除。
（2）**`MethodUtil` 依赖**：该类的功能依赖外部 `MethodUtil` 工具类（`invokeAndReturnDouble`、`invokeAndReturnLong`），若该工具类不存在或实现有问题，会导致当前类功能失效。
（3）**返回值容错**：当兼容类加载失败或方法获取失败时，`MethodUtil` 可能返回默认值（如 0.0、0），外部调用时需要做好判空或默认值处理。
（4）**日志仅记录警告**：类加载失败时仅记录 `warn` 日志，若需要更严格的监控，可调整为 `error` 日志或增加告警机制。

## 四、总结
1.  该类是**兼容多 JVM 的系统监控工具类**，核心通过反射获取非标准 JVM 扩展 API 的高级系统指标。
2.  核心流程：静态代码块初始化（加载兼容类 + 反射获取方法）→ 公开静态方法对外提供功能 → 内部辅助方法封装反射逻辑。
3.  设计核心：**兼容性、性能、健壮性**，符合 Java 工具类的最佳实践。
4.  典型使用场景：项目监控系统（如获取服务器 CPU 使用率、物理内存使用情况）、运维数据采集等。

