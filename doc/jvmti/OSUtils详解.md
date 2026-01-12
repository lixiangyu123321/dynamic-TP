# OSUtils 详解

## 文件位置

```
jvmti/jvmti-runtime/src/main/java/org/dromara/dynamictp/jvmti/OSUtils.java
```

## 概述

`OSUtils` 是操作系统工具类，用于检测当前运行环境的操作系统类型和 CPU 架构信息。它为 JVMTI 模块提供平台检测能力，确保能够加载正确的平台特定原生库。该类参考自 Alibaba Arthas 项目的实现。

## 类声明

```java
public class OSUtils {
    // ...
}
```

**设计特点**：
- **工具类**：使用私有构造函数，防止实例化
- **静态方法**：所有方法都是静态方法
- **自动检测**：在静态代码块中自动检测操作系统和架构

## 核心常量

```java
private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
private static final String OPERATING_SYSTEM_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
```

**作用**：
- `OPERATING_SYSTEM_NAME`：操作系统名称（转换为小写）
- `OPERATING_SYSTEM_ARCH`：CPU 架构名称（转换为小写）

**获取方式**：通过 `System.getProperty()` 获取系统属性

## 核心属性

```java
static PlatformEnum platform;
static String arch;
```

**作用**：
- `platform`：平台枚举类型（WINDOWS、LINUX、MACOSX、UNKNOWN）
- `arch`：规范化后的架构名称（如 `x86_64`、`aarch_64`）

**初始化**：在静态代码块中根据系统属性自动设置

## 静态初始化块

```java
static {
    if (OPERATING_SYSTEM_NAME.startsWith("linux")) {
        platform = PlatformEnum.LINUX;
    } else if (OPERATING_SYSTEM_NAME.startsWith("mac") || OPERATING_SYSTEM_NAME.startsWith("darwin")) {
        platform = PlatformEnum.MACOSX;
    } else if (OPERATING_SYSTEM_NAME.startsWith("windows")) {
        platform = PlatformEnum.WINDOWS;
    } else {
        platform = PlatformEnum.UNKNOWN;
    }
    arch = normalizeArch(OPERATING_SYSTEM_ARCH);
}
```

**检测逻辑**：
1. **Linux**：操作系统名以 "linux" 开头
2. **macOS**：操作系统名以 "mac" 或 "darwin" 开头
3. **Windows**：操作系统名以 "windows" 开头
4. **其他**：标记为 `UNKNOWN`

**架构规范化**：调用 `normalizeArch()` 方法规范化架构名称

## 核心方法

### isWindows()

```java
public static boolean isWindows()
```

**功能**：判断是否为 Windows 系统

**返回值**：`true` 表示是 Windows，`false` 表示不是

**实现**：
```java
public static boolean isWindows() {
    return platform == PlatformEnum.WINDOWS;
}
```

### isLinux()

```java
public static boolean isLinux()
```

**功能**：判断是否为 Linux 系统

**返回值**：`true` 表示是 Linux，`false` 表示不是

**实现**：
```java
public static boolean isLinux() {
    return platform == PlatformEnum.LINUX;
}
```

### isMac()

```java
public static boolean isMac()
```

**功能**：判断是否为 macOS 系统

**返回值**：`true` 表示是 macOS，`false` 表示不是

**实现**：
```java
public static boolean isMac() {
    return platform == PlatformEnum.MACOSX;
}
```

### isCygwinOrMinGW()

```java
public static boolean isCygwinOrMinGW()
```

**功能**：判断是否为 Cygwin 或 MinGW 环境（Windows 下的类 Unix 环境）

**返回值**：`true` 表示是 Cygwin 或 MinGW，`false` 表示不是

**实现**：
```java
public static boolean isCygwinOrMinGW() {
    if (isWindows()) {
        if ((System.getenv("MSYSTEM") != null && System.getenv("MSYSTEM").startsWith("MINGW"))
                || "/bin/bash".equals(System.getenv("SHELL"))) {
            return true;
        }
    }
    return false;
}
```

**检测逻辑**：
- 检查环境变量 `MSYSTEM` 是否以 "MINGW" 开头
- 或检查环境变量 `SHELL` 是否为 "/bin/bash"

### arch()

```java
public static String arch()
```

**功能**：获取规范化后的 CPU 架构名称

**返回值**：架构名称（如 `x86_64`、`aarch_64`、`x86_32`）

**实现**：
```java
public static String arch() {
    return arch;
}
```

### isArm32()

```java
public static boolean isArm32()
```

**功能**：判断是否为 ARM 32 位架构

**返回值**：`true` 表示是 ARM 32 位，`false` 表示不是

**实现**：
```java
public static boolean isArm32() {
    return "arm_32".equals(arch);
}
```

### isArm64()

```java
public static boolean isArm64()
```

**功能**：判断是否为 ARM 64 位架构（aarch64）

**返回值**：`true` 表示是 ARM 64 位，`false` 表示不是

**实现**：
```java
public static boolean isArm64() {
    return "aarch_64".equals(arch);
}
```

### isX86()

```java
public static boolean isX86()
```

**功能**：判断是否为 x86 32 位架构

**返回值**：`true` 表示是 x86 32 位，`false` 表示不是

**实现**：
```java
public static boolean isX86() {
    return "x86_32".equals(arch);
}
```

### isX8664()

```java
public static boolean isX8664()
```

**功能**：判断是否为 x86_64（AMD64）架构

**返回值**：`true` 表示是 x86_64，`false` 表示不是

**实现**：
```java
public static boolean isX8664() {
    return "x86_64".equals(arch);
}
```

### normalizeArch(String value)

```java
private static String normalizeArch(String value)
```

**功能**：规范化 CPU 架构名称，将不同的架构名称统一为标准格式

**参数**：
- `value`：原始架构名称

**返回值**：规范化后的架构名称

**支持的架构映射**：

| 原始名称 | 规范化名称 | 说明 |
|---------|-----------|------|
| x8664, amd64, ia32e, em64t, x64 | x86_64 | Intel/AMD 64 位 |
| x8632, x86, i[3-6]86, ia32, x32 | x86_32 | Intel/AMD 32 位 |
| ia64w, itanium64 | itanium_64 | Itanium 64 位 |
| ia64n | itanium_32 | Itanium 32 位 |
| sparc, sparc32 | sparc_32 | SPARC 32 位 |
| sparcv9, sparc64 | sparc_64 | SPARC 64 位 |
| arm, arm32 | arm_32 | ARM 32 位 |
| aarch64 | aarch_64 | ARM 64 位 |
| mips, mips32 | mips_32 | MIPS 32 位 |
| mipsel, mips32el | mipsel_32 | MIPS 32 位（小端） |
| mips64 | mips_64 | MIPS 64 位 |
| mips64el | mipsel_64 | MIPS 64 位（小端） |
| ppc, ppc32 | ppc_32 | PowerPC 32 位 |
| ppcle, ppc32le | ppcle_32 | PowerPC 32 位（小端） |
| ppc64 | ppc_64 | PowerPC 64 位 |
| ppc64le | ppcle_64 | PowerPC 64 位（小端） |
| s390 | s390_32 | IBM z/Architecture 32 位 |
| s390x | s390_64 | IBM z/Architecture 64 位 |
| 其他 | 原值 | 无法识别时返回原值 |

**实现逻辑**：
1. 调用 `normalize()` 方法规范化输入字符串
2. 使用正则表达式匹配各种架构名称变体
3. 返回标准化的架构名称

### isMuslLibc()

```java
public static boolean isMuslLibc()
```

**功能**：判断是否为使用 musl libc 的 Linux 系统（如 Alpine Linux）

**返回值**：`true` 表示是 musl libc，`false` 表示不是

**实现**：
```java
public static boolean isMuslLibc() {
    File ldMuslX8664File = new File("/lib/ld-musl-x86_64.so.1");
    File ldMuslAarch64File = new File("/lib/ld-musl-aarch64.so.1");
    return ldMuslX8664File.exists() || ldMuslAarch64File.exists();
}
```

**检测逻辑**：检查是否存在 musl libc 的链接器文件

**用途**：某些原生库可能需要区分 glibc 和 musl libc

### normalize(String value)

```java
private static String normalize(String value)
```

**功能**：规范化字符串，移除特殊字符并转换为小写

**参数**：
- `value`：原始字符串

**返回值**：规范化后的字符串

**实现**：
```java
private static String normalize(String value) {
    if (value == null) {
        return "";
    }
    return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
}
```

**处理逻辑**：
1. 如果为 `null`，返回空字符串
2. 转换为小写（使用 US locale）
3. 移除非字母数字字符

## 工作原理

### 1. 系统属性获取

```java
System.getProperty("os.name")  // 操作系统名称
System.getProperty("os.arch")  // CPU 架构
```

**常见值**：

| 操作系统 | os.name 值 |
|---------|-----------|
| Windows | Windows 10, Windows 11, etc. |
| Linux | Linux |
| macOS | Mac OS X, macOS |

| 架构 | os.arch 值 |
|-----|-----------|
| x86_64 | amd64, x86_64 |
| x86 | x86, i386 |
| ARM 64 | aarch64 |
| ARM 32 | arm |

### 2. 检测流程

```
类加载
    ↓
静态代码块执行
    ↓
获取系统属性 (os.name, os.arch)
    ↓
检测操作系统类型
    ↓
规范化架构名称
    ↓
设置 platform 和 arch
    ↓
后续调用直接返回已检测的值
```

## 支持的平台

### 操作系统

- ✅ **Windows**：所有 Windows 版本
- ✅ **Linux**：所有 Linux 发行版
- ✅ **macOS**：所有 macOS 版本（包括 OS X）
- ⚠️ **其他**：标记为 `UNKNOWN`

### CPU 架构

- ✅ **x86_64**：Intel/AMD 64 位
- ✅ **x86_32**：Intel/AMD 32 位
- ✅ **ARM 64 位**：aarch64
- ✅ **ARM 32 位**：arm
- ✅ **其他架构**：SPARC、MIPS、PowerPC、s390 等

## 使用场景

### 1. JVMTIUtil 中的使用

在 `JVMTIUtil` 的静态初始化块中：

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

### 2. 平台特定代码

```java
if (OSUtils.isLinux()) {
    // Linux 特定逻辑
    if (OSUtils.isMuslLibc()) {
        // Alpine Linux 特定逻辑
    }
} else if (OSUtils.isWindows()) {
    // Windows 特定逻辑
} else if (OSUtils.isMac()) {
    // macOS 特定逻辑
}
```

### 3. 架构检测

```java
String arch = OSUtils.arch();
if (OSUtils.isX8664()) {
    System.out.println("运行在 x86_64 架构上");
} else if (OSUtils.isArm64()) {
    System.out.println("运行在 ARM 64 位架构上");
}
```

## 注意事项

### 1. 检测准确性

- 检测基于系统属性，通常是准确的
- 某些特殊环境（如容器、虚拟化环境）可能返回意外的值
- `UNKNOWN` 平台可能表示不受支持的系统

### 2. 架构规范化

- 架构名称被规范化为标准格式
- 不同的 JVM 实现可能返回不同的原始值
- 规范化确保了一致性

### 3. 线程安全

- 所有变量都是静态的，在类加载时初始化
- 方法都是只读的，不存在并发问题
- 但初始化阶段需要保证线程安全（由 JVM 保证）

### 4. 性能考虑

- 检测在类加载时完成，后续调用无性能开销
- 方法都是简单的比较操作，性能很好

## 相关类

- **PlatformEnum**：平台枚举类型
- **JVMTIUtil**：使用 `OSUtils` 检测平台
- **JVMTI**：间接使用（通过 `JVMTIUtil`）

## 代码来源

根据类注释，此文件参考自 [Alibaba Arthas](https://github.com/alibaba/arthas) 项目。

## 扩展支持

如果需要支持新的平台或架构：

1. 在 `PlatformEnum` 中添加新的平台枚举
2. 在静态初始化块中添加检测逻辑
3. 在 `normalizeArch()` 中添加架构规范化逻辑
4. 添加对应的检测方法（如 `isNewPlatform()`）

## 测试示例

```java
// 检测平台
System.out.println("操作系统: " + OSUtils.platform);
System.out.println("CPU 架构: " + OSUtils.arch());
System.out.println("是 Windows: " + OSUtils.isWindows());
System.out.println("是 Linux: " + OSUtils.isLinux());
System.out.println("是 macOS: " + OSUtils.isMac());
System.out.println("是 x86_64: " + OSUtils.isX8664());
System.out.println("是 ARM 64: " + OSUtils.isArm64());
```

