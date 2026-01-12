# JVMTI 模块总结

## 模块概述

`jvmti` 模块是 DynamicTp 框架中用于实现 Java 代码无法完成的高级功能的模块。它利用 JVMTI（JVM Tool Interface）技术，通过原生代码（C++）和 JNI（Java Native Interface）接口，提供了获取 JVM 堆中对象实例、强制触发 GC 等底层操作能力。

## 模块结构

```
jvmti/
├── pom.xml                    # 父模块 POM
├── jvmti-build/              # 原生库构建模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/             # Java 接口定义
│       │   └── JVMTI.java
│       └── native/           # 原生代码
│           └── src/
│               └── jni-library.cpp
└── jvmti-runtime/            # 运行时模块
    ├── pom.xml
    └── src/main/
        ├── java/             # Java 实现
        │   └── org/dromara/dynamictp/jvmti/
        │       ├── JVMTI.java          # 核心工具类
        │       ├── JVMTIUtil.java      # 原生库名称检测
        │       ├── NativeUtil.java     # 原生库加载工具
        │       ├── OSUtils.java        # 操作系统检测工具
        │       └── PlatformEnum.java   # 平台枚举
        └── resources/        # 编译好的原生库
            ├── libJniLibrary-x64.dll    # Windows x64
            ├── libJniLibrary-x64.so     # Linux x64
            └── libJniLibrary.dylib      # macOS
```

## 核心文件作用

### 1. JVMTI.java

**作用**：JVMTI 功能的核心 Java 接口类

**主要功能**：
- 自动加载原生库
- 提供获取类实例的方法（`getInstance`、`getInstances`）
- 提供强制 GC 的方法（`forceGc`）
- 管理 JVMTI 可用状态

**关键特性**：
- 静态初始化自动加载原生库
- 线程安全的原生方法调用
- 支持限制实例数量以提高性能

### 2. JVMTIUtil.java

**作用**：检测当前平台对应的原生库文件名

**主要功能**：
- 根据操作系统和 CPU 架构检测库文件名
- 支持 Windows、Linux、macOS 三大平台
- 支持多种 CPU 架构（x86_64、ARM 等）

**关键特性**：
- 自动检测，无需手动配置
- 静态初始化，性能无影响

### 3. NativeUtil.java

**作用**：从 JAR 包中加载原生库的工具类

**主要功能**：
- 从 JAR 包提取原生库到临时目录
- 加载原生库到 JVM
- 管理临时文件和目录

**关键特性**：
- 解决 JAR 包中原生库无法直接加载的问题
- 自动清理临时文件
- 异常安全处理

### 4. OSUtils.java

**作用**：操作系统和架构检测工具类

**主要功能**：
- 检测操作系统类型（Windows、Linux、macOS）
- 检测 CPU 架构（x86_64、ARM、等）
- 规范化架构名称

**关键特性**：
- 支持多种架构变体的识别
- 提供便捷的检测方法
- 自动规范化架构名称

### 5. PlatformEnum.java

**作用**：平台枚举类型

**主要功能**：
- 定义支持的操作系统平台
- 提供类型安全的平台标识

**关键特性**：
- 类型安全
- 简单明了
- 易于扩展

### 6. jni-library.cpp

**作用**：JVMTI 原生库的 C++ 实现

**主要功能**：
- 初始化 JVMTI Agent
- 实现堆对象遍历逻辑
- 实现对象标记和检索
- 实现强制 GC 功能

**关键特性**：
- 使用 JVMTI API 访问 JVM 内部
- 高效的对象遍历算法
- 支持限制遍历数量

## 模块工作流程

### 1. 初始化流程

```
应用启动
    ↓
JVMTI 类加载
    ↓
静态代码块执行
    ↓
OSUtils 检测操作系统和架构
    ↓
JVMTIUtil 检测原生库文件名
    ↓
NativeUtil 从 JAR 加载原生库
    ↓
原生库的 JNI_OnLoad 被调用
    ↓
初始化 JVMTI Agent
    ↓
设置 AVAILABLE = true
    ↓
初始化完成
```

### 2. 获取实例流程

```
调用 JVMTI.getInstances()
    ↓
检查 AVAILABLE
    ↓
调用原生方法 getInstances0()
    ↓
JNI 调用 C++ 代码
    ↓
生成唯一 tag
    ↓
遍历堆对象 (IterateOverInstancesOfClass)
    ↓
为匹配对象设置 tag
    ↓
获取所有标记的对象 (GetObjectsWithTags)
    ↓
创建并返回 Java 对象数组
```

## 模块功能

### 1. 获取类实例

**功能**：获取 JVM 堆中指定类的所有存活实例

**使用场景**：
- 适配器模式中获取中间件的线程池实例
- 统计某个类的实例数量
- 内存分析工具

**API**：
```java
// 获取单个实例
T instance = JVMTI.getInstance(MyClass.class);

// 获取所有实例
List<T> instances = JVMTI.getInstances(MyClass.class);

// 限制数量
List<T> instances = JVMTI.getInstances(MyClass.class, 10);
```

**特点**：
- 只能获取存活的对象
- 支持限制数量以提高性能
- 可能触发 GC，影响性能

### 2. 强制 GC

**功能**：强制触发垃圾回收

**使用场景**：
- 在获取实例前清理已回收的对象
- 内存分析工具
- 性能测试

**API**：
```java
JVMTI.forceGc();
```

**特点**：
- 同步操作，会阻塞直到 GC 完成
- 即使 JVM 参数禁用 GC 也能触发
- 可能影响应用性能

## 模块设计特点

### 1. 平台自适应

- 自动检测操作系统和 CPU 架构
- 加载对应平台的原生库
- 支持 Windows、Linux、macOS
- 支持多种 CPU 架构

### 2. 容错设计

- 初始化失败不影响框架其他功能
- 如果 JVMTI 不可用，方法返回空列表
- 异常处理完善，不会导致崩溃

### 3. 性能优化

- 支持限制实例数量，提前终止遍历
- 使用对象标记机制，避免重复遍历
- 静态初始化，后续调用无额外开销

### 4. 易于使用

- 提供简单的 Java API
- 自动初始化，无需手动配置
- 透明的原生库加载

## 技术限制

### 1. 平台支持

- ⚠️ **主要支持 64 位架构**：x86_64、aarch64
- ⚠️ **平台限制**：Windows、Linux、macOS
- ⚠️ **原生库依赖**：需要编译好的原生库文件

### 2. 性能影响

- ⚠️ **耗时操作**：遍历整个堆可能非常耗时
- ⚠️ **可能触发 GC**：影响应用性能
- ⚠️ **内存占用**：获取大量实例可能占用内存

### 3. JVM 限制

- 需要支持 JVMTI 的 JVM（HotSpot、OpenJ9 等）
- 某些功能可能需要特定的 JVM 启动参数

## 在 DynamicTp 中的应用

### 1. 适配器模块

在适配器模块中，用于获取中间件内部的线程池实例：

```java
// Thrift 适配器示例
ThriftThreadPool pool = JVMTI.getInstance(ThriftThreadPool.class);
if (pool != null) {
    ExecutorWrapper wrapper = ExecutorWrapper.of(pool.getExecutor());
    executors.put("thrift", wrapper);
}
```

**优势**：
- 无需修改中间件代码
- 自动发现线程池实例
- 支持多种中间件框架

### 2. 使用场景

- **无法直接访问的线程池**：中间件内部的线程池
- **第三方库的线程池**：没有公开 API 的线程池
- **动态创建的线程池**：运行时创建的线程池

## 模块优势

### 1. 功能强大

利用 JVMTI 的强大能力，实现 Java 代码无法完成的功能。

### 2. 平台兼容

支持主流操作系统和 CPU 架构，具有良好的跨平台能力。

### 3. 易于集成

提供简单的 Java API，易于在项目中使用。

### 4. 容错性好

初始化失败不影响框架其他功能，保证框架的可用性。

## 模块劣势

### 1. 平台依赖

需要为每个平台编译原生库，增加维护成本。

### 2. 性能开销

堆遍历操作可能很耗时，影响应用性能。

### 3. 技术复杂度

涉及原生代码、JNI、JVMTI 等技术，复杂度较高。

### 4. 调试困难

原生代码的调试比 Java 代码困难。

## 最佳实践

### 1. 谨慎使用

- 避免在高频代码路径中调用
- 使用限制数量减少遍历时间
- 缓存结果，避免重复调用

### 2. 错误处理

```java
List<MyClass> instances = JVMTI.getInstances(MyClass.class);
if (instances.isEmpty()) {
    // 可能是 JVMTI 不可用，或者真的没有实例
    log.warn("No instances found or JVMTI not available");
}
```

### 3. 性能优化

```java
// 推荐：限制数量
List<MyClass> instances = JVMTI.getInstances(MyClass.class, 10);

// 不推荐：获取所有实例
List<MyClass> instances = JVMTI.getInstances(MyClass.class);
```

### 4. 异步调用

在高并发场景下，考虑异步调用：

```java
CompletableFuture.supplyAsync(() -> {
    return JVMTI.getInstances(MyClass.class, 10);
}).thenAccept(instances -> {
    // 处理结果
});
```

## 模块依赖

### 编译时依赖

- JDK（包含 JNI 和 JVMTI 头文件）
- C++ 编译器（GCC、Clang、MSVC）
- Maven（构建工具）

### 运行时依赖

- JVM（支持 JVMTI）
- 原生库文件（`.dll`、`.so`、`.dylib`）

## 总结

JVMTI 模块是 DynamicTp 框架中一个功能强大但技术复杂的模块。它通过原生代码和 JVMTI 技术，实现了 Java 代码无法完成的高级功能，为框架提供了获取 JVM 内部对象的能力。虽然在性能和平台支持方面存在一些限制，但在适配器模式等特定场景下，它提供了不可替代的功能。

**核心价值**：
- 实现 Java 代码无法完成的功能
- 支持适配器模式中的线程池发现
- 提供底层 JVM 操作能力

**适用场景**：
- 需要获取无法直接访问的对象
- 适配器模式中的实例发现
- 内存分析和调试工具

**注意事项**：
- 性能开销较大，谨慎使用
- 主要支持 64 位架构
- 需要对应的原生库文件

