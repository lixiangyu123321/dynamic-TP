# JVMTI 详解

## 文件位置

```
jvmti/jvmti-runtime/src/main/java/org/dromara/dynamictp/jvmti/JVMTI.java
```

## 概述

`JVMTI` 是 DynamicTp 框架中用于封装 JVMTI（JVM Tool Interface）功能的核心工具类。它提供了在 Java 代码中无法实现的功能，如获取 JVM 堆中某个类的所有存活实例、强制触发 GC 等。通过 JNI 调用原生代码，利用 JVMTI 的强大能力来实现这些底层操作。

## 类声明

```java
@Slf4j
public class JVMTI {
    // ...
}
```

**设计特点**：
- **工具类**：使用私有构造函数，防止实例化
- **静态方法**：所有方法都是静态方法
- **线程安全**：使用 `synchronized` 保证原生方法的线程安全

## 核心常量

```java
private static final AtomicBoolean AVAILABLE = new AtomicBoolean(false);
```

**作用**：标记 JVMTI 功能是否可用
- `false`：JVMTI 初始化失败或不可用
- `true`：JVMTI 成功初始化，可以使用

## 核心方法

### 静态初始化块

```java
static {
    try {
        NativeUtil.loadLibraryFromJar(JVMTIUtil.detectLibName());
        AVAILABLE.set(true);
    } catch (Throwable t) {
        log.error("JVMTI initialization failed!", t);
    }
}
```

**初始化流程**：
1. 检测系统平台并获取对应的原生库名称
2. 从 JAR 包中加载原生库到临时目录
3. 加载原生库到 JVM
4. 如果成功，设置 `AVAILABLE` 为 `true`
5. 如果失败，记录错误日志，但不会抛出异常（保证框架其他功能可用）

**特点**：
- 自动初始化：在类加载时自动执行
- 容错处理：初始化失败不影响框架其他功能
- 平台自适应：自动检测并加载对应平台的原生库

### getInstance(Class<T> klass)

```java
public static <T> T getInstance(final Class<T> klass)
```

**功能**：获取 JVM 中指定类的单个存活实例

**参数**：
- `klass`：要获取实例的类对象

**返回值**：
- 类的单个实例，如果没有找到返回 `null`
- 如果找到多个实例，抛出 `RuntimeException`

**实现逻辑**：
```java
public static <T> T getInstance(final Class<T> klass) {
    final List<T> instances = getInstances(klass, 1);
    if (CollectionUtils.isEmpty(instances)) {
        return null;
    }
    if (instances.size() > 1) {
        throw new RuntimeException("expect only one instance, actually find many instances !");
    }
    return instances.get(0);
}
```

**使用场景**：
- 获取单例对象（期望只有一个实例）
- 获取特定类型的唯一实例
- 适配器模式中获取中间件的线程池实例

**使用示例**：
```java
// 获取 Thrift 线程池实例（期望只有一个）
ThriftThreadPool pool = JVMTI.getInstance(ThriftThreadPool.class);
if (pool != null) {
    // 使用线程池实例
    ExecutorWrapper wrapper = ExecutorWrapper.of(pool.getExecutor());
}
```

**注意事项**：
- 如果类有多个实例，会抛出异常
- 如果没有实例，返回 `null`
- 只查找存活的对象，已回收的对象不会被找到

### getInstances(Class<T> klass)

```java
public static <T> List<T> getInstances(final Class<T> klass)
```

**功能**：获取 JVM 中指定类的所有存活实例

**参数**：
- `klass`：要获取实例的类对象

**返回值**：
- 所有存活实例的列表（可能为空）

**实现逻辑**：
```java
public static <T> List<T> getInstances(final Class<T> klass) {
    return getInstances(klass, -1);
}
```

**使用场景**：
- 获取某个类型的所有实例
- 统计分析某个类的实例数量
- 批量处理某个类型的所有实例

**使用示例**：
```java
// 获取所有线程池执行器实例
List<ThreadPoolExecutor> executors = JVMTI.getInstances(ThreadPoolExecutor.class);
executors.forEach(executor -> {
    // 处理每个线程池
    System.out.println("线程池: " + executor);
});
```

**注意事项**：
- ⚠️ **性能警告**：此方法会遍历整个堆，可能非常耗时
- ⚠️ **内存影响**：可能触发 GC，影响性能
- ⚠️ **谨慎使用**：不建议在高并发场景下频繁调用

### getInstances(Class<T> klass, int limit)

```java
public static <T> List<T> getInstances(final Class<T> klass, final int limit)
```

**功能**：获取 JVM 中指定类的存活实例，支持限制数量

**参数**：
- `klass`：要获取实例的类对象
- `limit`：实例数量限制
  - `limit < 0`：不限制，获取所有实例
  - `limit = 0`：不获取任何实例
  - `limit > 0`：最多获取 `limit` 个实例

**返回值**：
- 实例列表（可能为空，如果 `AVAILABLE` 为 `false`）

**实现逻辑**：
```java
public static <T> List<T> getInstances(final Class<T> klass, final int limit) {
    if (!AVAILABLE.get()) {
        return Collections.emptyList();
    }
    return Arrays.asList(getInstances0(klass, limit));
}
```

**使用场景**：
- 限制获取数量，提高性能
- 只获取前几个实例用于测试
- 分批处理实例

**使用示例**：
```java
// 只获取前 10 个实例
List<ThreadPoolExecutor> executors = JVMTI.getInstances(ThreadPoolExecutor.class, 10);
executors.forEach(executor -> {
    // 处理线程池
});
```

**注意事项**：
- **推荐使用限制**：建议传入一个较小的正数限制值，避免遍历整个堆
- **性能优化**：限制数量可以提前终止遍历，提高性能
- **线程安全**：方法使用 `synchronized` 保证线程安全

### getInstances0(Class<T> klass, int limit) - 原生方法

```java
private static synchronized native <T> T[] getInstances0(Class<T> klass, int limit);
```

**功能**：原生方法，实际执行 JVMTI 调用

**特点**：
- **原生方法**：通过 JNI 调用 C++ 代码
- **同步方法**：使用 `synchronized` 保证线程安全
- **私有方法**：外部不能直接调用

**底层实现**（在 `jni-library.cpp` 中）：
1. 为每个对象生成唯一的 tag
2. 使用 `IterateOverInstancesOfClass` 遍历堆中的对象
3. 为匹配的对象标记 tag
4. 使用 `GetObjectsWithTags` 获取所有标记的对象
5. 根据 limit 限制返回数量

### forceGc()

```java
public static synchronized native void forceGc();
```

**功能**：强制触发垃圾回收

**特点**：
- **原生方法**：通过 JNI 调用 JVMTI 的 `ForceGarbageCollection`
- **同步方法**：使用 `synchronized` 保证线程安全
- **强制执行**：即使 JVM 参数禁用了 GC，也能强制触发

**使用场景**：
- 在获取实例前清理已回收的对象
- 测试 GC 行为
- 内存分析工具

**使用示例**：
```java
// 强制触发 GC
JVMTI.forceGc();

// 然后获取实例
List<MyClass> instances = JVMTI.getInstances(MyClass.class);
```

**注意事项**：
- ⚠️ **性能影响**：强制 GC 会暂停应用，影响性能
- ⚠️ **谨慎使用**：不建议在生产环境频繁调用
- ⚠️ **阻塞操作**：会阻塞当前线程直到 GC 完成

## 工作原理

### 1. 初始化流程

```
JVMTI 类加载
    ↓
静态代码块执行
    ↓
检测系统平台 (OSUtils)
    ↓
获取原生库名称 (JVMTIUtil.detectLibName())
    ↓
从 JAR 加载原生库 (NativeUtil.loadLibraryFromJar())
    ↓
调用 System.load() 加载原生库
    ↓
原生库的 JNI_OnLoad() 被调用
    ↓
初始化 JVMTI Agent (init_agent())
    ↓
设置 AVAILABLE = true
```

### 2. 获取实例流程

```
调用 getInstances()
    ↓
检查 AVAILABLE
    ↓
调用原生方法 getInstances0()
    ↓
JNI 调用 C++ 代码
    ↓
生成唯一 tag
    ↓
初始化 limitCounter
    ↓
IterateOverInstancesOfClass() 遍历堆
    ↓
为匹配的对象标记 tag
    ↓
GetObjectsWithTags() 获取所有标记的对象
    ↓
根据 limit 限制数量
    ↓
返回对象数组
    ↓
转换为 Java List
```

### 3. 原生库加载流程

```
检测操作系统和架构
    ↓
确定原生库文件名
    ↓
从 JAR 包的 resources 目录读取
    ↓
写入临时目录
    ↓
调用 System.load() 加载
    ↓
JNI_OnLoad 初始化 JVMTI
```

## 设计特点

### 1. 平台自适应

自动检测操作系统和 CPU 架构，加载对应的原生库：
- Windows x64: `libJniLibrary-x64.dll`
- Linux x64: `libJniLibrary-x64.so`
- macOS: `libJniLibrary.dylib`
- 其他架构也有相应支持

### 2. 容错设计

- 初始化失败不影响框架其他功能
- 如果 JVMTI 不可用，方法返回空列表而不是抛出异常
- 原生方法失败时会记录错误但不崩溃

### 3. 线程安全

- 使用 `synchronized` 保证原生方法的线程安全
- 使用 `AtomicBoolean` 保证状态标志的原子性

### 4. 性能优化

- 支持限制实例数量，提前终止遍历
- 使用对象标记机制，避免重复遍历
- 只在需要时才调用原生方法

## 使用场景

### 1. 适配器模式

在适配器模块中，用于获取中间件内部的线程池实例：

```java
public class ThriftDtpAdapter extends AbstractDtpAdapter {
    
    @Override
    protected void initialize() {
        // 使用 JVMTI 获取 Thrift 线程池实例
        ThriftThreadPool pool = JVMTI.getInstance(ThriftThreadPool.class);
        if (pool != null) {
            ExecutorWrapper wrapper = ExecutorWrapper.of(pool.getExecutor());
            executors.put("thrift", wrapper);
        }
    }
}
```

### 2. 实例统计

统计某个类的实例数量：

```java
List<ThreadPoolExecutor> executors = JVMTI.getInstances(ThreadPoolExecutor.class);
System.out.println("当前 JVM 中有 " + executors.size() + " 个线程池实例");
```

### 3. 内存分析

在内存分析工具中使用：

```java
// 强制 GC
JVMTI.forceGc();

// 获取所有大对象
List<LargeObject> objects = JVMTI.getInstances(LargeObject.class);
```

## 注意事项

### 1. 平台限制

- ⚠️ **仅支持 64 位架构**：目前主要支持 64 位 CPU 架构
- ⚠️ **平台支持**：Windows、Linux、macOS
- ⚠️ **原生库依赖**：需要对应的原生库文件存在

### 2. 性能影响

- ⚠️ **耗时操作**：遍历整个堆可能非常耗时（几秒到几十秒）
- ⚠️ **可能触发 GC**：获取实例时可能触发 GC，影响性能
- ⚠️ **内存占用**：获取大量实例可能占用较多内存

### 3. 使用建议

- ✅ **使用限制**：建议使用 `getInstances(klass, limit)` 限制数量
- ✅ **缓存结果**：避免频繁调用，可以缓存结果
- ✅ **异步调用**：在高并发场景下，考虑异步调用
- ❌ **避免频繁调用**：不要在循环或高频代码中调用
- ❌ **避免在生产环境强制 GC**：`forceGc()` 会影响性能

### 4. 错误处理

```java
// 检查 JVMTI 是否可用
List<MyClass> instances = JVMTI.getInstances(MyClass.class);
if (instances.isEmpty()) {
    // 可能是 JVMTI 不可用，或者真的没有实例
    log.warn("No instances found or JVMTI not available");
}
```

## 相关类

- **JVMTIUtil**：检测原生库名称
- **NativeUtil**：从 JAR 加载原生库
- **OSUtils**：检测操作系统和架构
- **jni-library.cpp**：原生库实现

## 依赖关系

- **JVMTI API**：依赖 JVM 提供的 JVMTI 接口
- **JNI**：通过 JNI 调用原生代码
- **原生库**：需要编译好的原生库文件（`.dll`、`.so`、`.dylib`）

## 技术限制

### 1. 架构限制

目前主要支持 64 位架构：
- x86_64 (Intel/AMD 64 位)
- aarch_64 (ARM 64 位)

### 2. JVM 限制

- 需要支持 JVMTI 的 JVM（HotSpot、OpenJ9 等）
- 某些 JVMTI 功能可能需要特定的 JVM 启动参数

### 3. 功能限制

- 只能获取存活的对象（已回收的对象无法获取）
- 只能获取堆中的对象（栈上的对象无法获取）
- 获取实例的准确性依赖于 GC 的状态

## 参考资源

- [JVMTI 规范](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html)
- [JNI 规范](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/)
- [Alibaba Arthas](https://github.com/alibaba/arthas) - 参考了其实现

