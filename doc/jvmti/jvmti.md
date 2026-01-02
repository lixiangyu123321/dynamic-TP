# JVMTI 模块

## 模块概述

`jvmti` 模块是 DynamicTp 框架的 JVMTI 技术模块，使用 JVMTI（JVM Tool Interface）技术实现一些 Java 代码无法实现的功能，如获取 JVM 中类的所有存活实例。

## 主要功能

### 1. JVMTI 接口

- **JVMTI**: JVMTI 工具类
  - 提供获取类实例的功能
  - 支持获取单个实例或多个实例
  - 支持限制获取数量

### 2. 原生库

- **jvmti-runtime**: JVMTI 运行时模块
  - 包含 JVMTI 的原生库实现
  - 支持多平台（Windows、Linux、macOS）
  - 提供 JNI 接口

- **jvmti-build**: JVMTI 构建模块
  - 包含 JVMTI 原生代码
  - 用于构建原生库

### 3. 工具类

- **JVMTIUtil**: JVMTI 工具类
  - 检测系统平台
  - 获取原生库名称
  - 提供平台相关的工具方法

- **NativeUtil**: 原生库工具类
  - 从 JAR 包中加载原生库
  - 处理不同平台的原生库加载

- **OSUtils**: 操作系统工具类
  - 检测操作系统类型
  - 获取系统架构信息

## 核心功能

### 1. 获取类实例

通过 JVMTI 技术可以获取 JVM 中某个类的所有存活实例：

```java
// 获取单个实例
MyClass instance = JVMTI.getInstance(MyClass.class);

// 获取所有实例
List<MyClass> instances = JVMTI.getInstances(MyClass.class);

// 限制获取数量
List<MyClass> instances = JVMTI.getInstances(MyClass.class, 10);
```

### 2. 强制 GC

通过 JVMTI 技术可以强制触发 GC：

```java
JVMTI.forceGc();
```

## 核心类说明

### JVMTI

JVMTI 工具类，提供获取类实例的功能：

- `getInstance()`: 获取类的单个实例（期望只有一个实例）
- `getInstances()`: 获取类的所有实例
- `getInstances(limit)`: 获取类的实例（限制数量）

### JVMTIUtil

JVMTI 工具类：

- `detectLibName()`: 检测原生库名称
- 根据系统平台返回相应的原生库名称

### NativeUtil

原生库工具类：

- `loadLibraryFromJar()`: 从 JAR 包中加载原生库
- 自动处理不同平台的原生库加载

### OSUtils

操作系统工具类：

- `getOSName()`: 获取操作系统名称
- `getOSArch()`: 获取系统架构
- `isWindows()`: 判断是否为 Windows
- `isLinux()`: 判断是否为 Linux
- `isMac()`: 判断是否为 macOS

## 工作原理

### 1. 原生库加载

1. 检测系统平台和架构
2. 从 JAR 包中提取相应的原生库
3. 加载原生库到 JVM

### 2. JVMTI Agent

1. 通过 JVMTI Agent 初始化 JVMTI 环境
2. 获取 JVMTI 能力（如对象标记）
3. 提供 JNI 接口供 Java 代码调用

### 3. 实例获取

1. 通过 JVMTI API 遍历堆中的对象
2. 过滤出指定类的实例
3. 返回实例列表

## 使用场景

### 1. 获取线程池实例

在某些场景下，需要获取中间件内部的线程池实例，但这些实例可能无法直接访问。通过 JVMTI 技术可以获取这些实例：

```java
// 获取 Thrift 线程池实例
ThriftThreadPool pool = JVMTI.getInstance(ThriftThreadPool.class);
```

### 2. 适配器实现

在适配器模块中，使用 JVMTI 技术获取中间件的线程池实例，实现适配功能。

## 技术限制

### 1. 平台支持

- **仅支持 64 位架构**: 目前仅支持 64 位 CPU 架构
- **多平台支持**: 支持 Windows、Linux、macOS

### 2. 性能影响

- **GC 影响**: 获取实例时可能需要触发 GC，可能影响性能
- **内存占用**: 获取大量实例可能占用较多内存

### 3. 使用注意事项

- **谨慎使用**: 获取实例功能需要谨慎使用，可能影响性能
- **限制数量**: 建议限制获取的实例数量
- **单例检查**: `getInstance()` 方法期望只有一个实例，如果存在多个实例会抛出异常

## 依赖关系

- 无 Java 依赖（仅依赖 JVM 本身）
- 需要编译原生库（C++ 代码）

## 原生库

框架提供了预编译的原生库：

- **Windows**: `jvmti-windows-x86_64.dll`
- **Linux**: `libjvmti-linux-x86_64.so`
- **macOS**: `libjvmti-darwin-x86_64.dylib`

## 使用示例

### 获取线程池实例

```java
// 获取单个线程池实例
ThreadPoolExecutor executor = JVMTI.getInstance(ThreadPoolExecutor.class);

// 获取所有线程池实例（限制数量）
List<ThreadPoolExecutor> executors = JVMTI.getInstances(ThreadPoolExecutor.class, 10);
```

### 在适配器中使用

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

## 注意事项

1. **平台限制**: 仅支持 64 位架构
2. **性能影响**: 获取实例可能触发 GC，影响性能
3. **内存占用**: 获取大量实例可能占用较多内存
4. **使用场景**: 主要用于适配器场景，获取无法直接访问的线程池实例
5. **异常处理**: `getInstance()` 如果存在多个实例会抛出异常

## 技术细节

### JVMTI Agent 初始化

```cpp
int init_agent(JavaVM *vm, void *reserved) {
    // 获取 JVMTI 环境
    jint rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_2);
    
    // 添加能力
    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;
    jvmti->AddCapabilities(&capabilities);
    
    return JNI_OK;
}
```

### 实例获取实现

通过 JVMTI API 的 `IterateOverInstancesOfClass` 函数遍历堆中的对象，过滤出指定类的实例。

