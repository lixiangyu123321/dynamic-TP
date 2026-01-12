# JVMTIUtil 详解

## 文件位置

```
jvmti/jvmti-runtime/src/main/java/org/dromara/dynamictp/jvmti/JVMTIUtil.java
```

## 概述

`JVMTIUtil` 是 JVMTI 模块的工具类，负责根据当前操作系统和 CPU 架构检测并返回对应的原生库文件名。它是 JVMTI 原生库加载的前置步骤，确保能够加载正确的平台特定原生库文件。

## 类声明

```java
public class JVMTIUtil {
    // ...
}
```

**设计特点**：
- **工具类**：使用私有构造函数，防止实例化
- **静态方法**：所有方法都是静态方法
- **自动检测**：在静态代码块中自动检测平台并设置库名称

## 核心属性

```java
private static String libName;
```

**作用**：存储检测到的原生库文件名

**初始化**：在静态代码块中根据操作系统和架构自动设置

## 核心方法

### 静态初始化块

```java
static {
    if (OSUtils.isMac()) {
        libName = "libJniLibrary.dylib";
    }
    if (OSUtils.isLinux()) {
        if (OSUtils.isArm32()) {
            libName = "libJniLibrary-arm.so";
        } else if (OSUtils.isArm64()) {
            libName = "libJniLibrary-aarch64.so";
        } else if (OSUtils.isX8664()) {
            libName = "libJniLibrary-x64.so";
        } else {
            libName = "libJniLibrary-" + OSUtils.arch() + ".so";
        }
    }
    if (OSUtils.isWindows()) {
        libName = "libJniLibrary-x64.dll";
        if (OSUtils.isX86()) {
            libName = "libJniLibrary-x86.dll";
        }
    }
}
```

**检测逻辑**：

1. **macOS 平台**：
   - 文件名：`libJniLibrary.dylib`
   - 不考虑架构（macOS 主要是 x64 和 arm64，但库文件统一）

2. **Linux 平台**：
   - ARM 32 位：`libJniLibrary-arm.so`
   - ARM 64 位（aarch64）：`libJniLibrary-aarch64.so`
   - x86_64：`libJniLibrary-x64.so`
   - 其他架构：`libJniLibrary-{arch}.so`（使用规范化后的架构名）

3. **Windows 平台**：
   - x86（32 位）：`libJniLibrary-x86.dll`
   - x64（默认）：`libJniLibrary-x64.dll`

**检测顺序**：
- 先检查 macOS（`OSUtils.isMac()`）
- 再检查 Linux（`OSUtils.isLinux()`）
- 最后检查 Windows（`OSUtils.isWindows()`）

**注意**：使用 `if` 而不是 `else if`，这意味着如果系统同时匹配多个条件，后面的会覆盖前面的（虽然在实际中这种情况很少见）。

### detectLibName()

```java
public static String detectLibName()
```

**功能**：返回检测到的原生库文件名

**返回值**：
- 原生库文件名（如 `libJniLibrary-x64.so`）
- 可能为 `null`（如果无法检测平台）

**实现**：
```java
public static String detectLibName() {
    return libName;
}
```

**使用示例**：
```java
String libName = JVMTIUtil.detectLibName();
if (libName != null) {
    NativeUtil.loadLibraryFromJar(libName);
}
```

## 支持的平台和库文件映射

| 操作系统 | CPU 架构 | 库文件名 |
|---------|---------|---------|
| macOS | 任意 | `libJniLibrary.dylib` |
| Linux | ARM 32 位 | `libJniLibrary-arm.so` |
| Linux | ARM 64 位 (aarch64) | `libJniLibrary-aarch64.so` |
| Linux | x86_64 | `libJniLibrary-x64.so` |
| Linux | 其他架构 | `libJniLibrary-{arch}.so` |
| Windows | x86 (32 位) | `libJniLibrary-x86.dll` |
| Windows | x64 (64 位) | `libJniLibrary-x64.dll` |

## 工作原理

### 检测流程

```
类加载
    ↓
静态代码块执行
    ↓
检查操作系统类型 (OSUtils)
    ↓
检查 CPU 架构 (OSUtils)
    ↓
根据操作系统和架构组合确定库文件名
    ↓
设置 libName 静态变量
    ↓
后续调用 detectLibName() 直接返回
```

### 平台检测依赖

`JVMTIUtil` 依赖 `OSUtils` 类进行平台检测：
- `OSUtils.isMac()` - 检测 macOS
- `OSUtils.isLinux()` - 检测 Linux
- `OSUtils.isWindows()` - 检测 Windows
- `OSUtils.isArm32()` - 检测 ARM 32 位
- `OSUtils.isArm64()` - 检测 ARM 64 位
- `OSUtils.isX86()` - 检测 x86（32 位）
- `OSUtils.isX8664()` - 检测 x86_64（64 位）
- `OSUtils.arch()` - 获取规范化后的架构名称

## 设计特点

### 1. 自动检测

在类加载时自动检测平台，无需手动调用初始化方法。

### 2. 平台适配

根据不同的操作系统和 CPU 架构返回对应的原生库文件名。

### 3. 简单易用

提供单一方法 `detectLibName()` 获取结果，使用简单。

### 4. 库文件命名规范

遵循各平台的原生库命名规范：
- Linux: `.so` 后缀
- Windows: `.dll` 后缀
- macOS: `.dylib` 后缀

## 使用场景

### 1. JVMTI 初始化

在 `JVMTI` 类的静态初始化块中使用：

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

### 2. 动态加载原生库

如果需要动态加载原生库：

```java
String libName = JVMTIUtil.detectLibName();
if (libName != null) {
    try {
        NativeUtil.loadLibraryFromJar(libName);
        System.out.println("成功加载原生库: " + libName);
    } catch (IOException e) {
        System.err.println("加载原生库失败: " + libName);
    }
} else {
    System.err.println("无法检测平台或平台不支持");
}
```

## 注意事项

### 1. 平台支持

- 主要支持常见的平台组合
- 某些特殊架构可能没有对应的原生库文件
- 如果平台不被支持，`libName` 可能为 `null`

### 2. 库文件存在性

- `detectLibName()` 只返回库文件名，不检查文件是否存在
- 文件是否存在由 `NativeUtil.loadLibraryFromJar()` 检查

### 3. 平台检测顺序

- 使用多个独立的 `if` 语句
- 如果多个条件同时满足，后面的会覆盖前面的
- 在实际使用中，这种情况很少见

## 相关类

- **OSUtils**：操作系统和架构检测工具类
- **PlatformEnum**：平台枚举类型
- **NativeUtil**：原生库加载工具类
- **JVMTI**：JVMTI 主类，使用 `detectLibName()` 的结果

## 代码来源

根据类注释，此文件参考自 [Alibaba Arthas](https://github.com/alibaba/arthas) 项目。

## 扩展支持

如果需要支持新的平台或架构，需要：

1. 在 `jvmti-runtime/src/main/resources/` 目录下添加对应的原生库文件
2. 在静态初始化块中添加检测逻辑
3. 确保库文件命名与检测逻辑一致

## 示例：添加新平台支持

```java
static {
    // 现有检测逻辑...
    
    // 添加新平台支持
    if (OSUtils.isSolaris()) {
        if (OSUtils.isSparc64()) {
            libName = "libJniLibrary-sparc64.so";
        } else {
            libName = "libJniLibrary-sparc.so";
        }
    }
}
```

