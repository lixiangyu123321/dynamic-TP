# PlatformEnum 详解

## 文件位置

```
jvmti/jvmti-runtime/src/main/java/org/dromara/dynamictp/jvmti/PlatformEnum.java
```

## 概述

`PlatformEnum` 是一个枚举类型，用于表示支持的操作系统平台。它定义了 JVMTI 模块支持的所有操作系统类型，为平台检测和原生库选择提供类型安全的枚举值。

## 类声明

```java
public enum PlatformEnum {
    WINDOWS,
    LINUX,
    MACOSX,
    UNKNOWN
}
```

**设计特点**：
- **枚举类型**：使用 Java 枚举，类型安全
- **简单明了**：只包含必要的平台类型
- **未知平台支持**：提供 `UNKNOWN` 作为兜底值

## 枚举值

### WINDOWS

```java
WINDOWS
```

**含义**：Microsoft Windows 操作系统

**包含版本**：
- Windows 10
- Windows 11
- Windows Server 2016/2019/2022
- 其他 Windows 版本

**检测条件**：`os.name` 系统属性以 "windows" 开头

### LINUX

```java
LINUX
```

**含义**：Linux 操作系统（各种发行版）

**包含发行版**：
- Ubuntu
- CentOS
- Red Hat Enterprise Linux
- Debian
- Fedora
- Alpine Linux
- 其他 Linux 发行版

**检测条件**：`os.name` 系统属性以 "linux" 开头

### MACOSX

```java
MACOSX
```

**含义**：macOS（原 Mac OS X）操作系统

**包含版本**：
- macOS 10.x (OS X)
- macOS 11.x (Big Sur)
- macOS 12.x (Monterey)
- macOS 13.x (Ventura)
- 其他 macOS 版本

**检测条件**：`os.name` 系统属性以 "mac" 或 "darwin" 开头

**命名说明**：虽然 Apple 已经将操作系统名称改为 "macOS"，但枚举名称仍使用 "MACOSX" 以保持兼容性。

### UNKNOWN

```java
UNKNOWN
```

**含义**：未知或不支持的操作系统

**使用场景**：
- 操作系统名称无法识别
- 不支持的操作系统
- 检测失败的情况

**处理方式**：当平台为 `UNKNOWN` 时，通常无法加载对应的原生库

## 使用方式

### 1. 在 OSUtils 中使用

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
}
```

### 2. 平台判断

```java
if (OSUtils.platform == PlatformEnum.WINDOWS) {
    // Windows 特定逻辑
} else if (OSUtils.platform == PlatformEnum.LINUX) {
    // Linux 特定逻辑
} else if (OSUtils.platform == PlatformEnum.MACOSX) {
    // macOS 特定逻辑
} else {
    // 未知平台处理
}
```

### 3. Switch 语句

```java
switch (OSUtils.platform) {
    case WINDOWS:
        // Windows 逻辑
        break;
    case LINUX:
        // Linux 逻辑
        break;
    case MACOSX:
        // macOS 逻辑
        break;
    case UNKNOWN:
        // 未知平台处理
        break;
}
```

## 设计特点

### 1. 类型安全

使用枚举类型而不是字符串常量，提供编译时类型检查。

### 2. 简单明了

只包含实际支持的平台，不包含所有可能的操作系统。

### 3. 扩展性

如果需要支持新平台，只需：
1. 在枚举中添加新的枚举值
2. 在 `OSUtils` 中添加检测逻辑
3. 在 `JVMTIUtil` 中添加对应的库文件处理

### 4. 未知平台支持

提供 `UNKNOWN` 作为兜底值，确保在所有情况下都有有效的枚举值。

## 相关类

- **OSUtils**：使用 `PlatformEnum` 存储检测到的平台类型
- **JVMTIUtil**：间接使用（通过 `OSUtils.platform`）

## 代码来源

根据类注释，此文件参考自 [Alibaba Arthas](https://github.com/alibaba/arthas) 项目。

## 扩展支持

如果需要添加新的平台支持，例如支持 Solaris：

```java
public enum PlatformEnum {
    WINDOWS,
    LINUX,
    MACOSX,
    SOLARIS,  // 新增
    UNKNOWN
}
```

然后在 `OSUtils` 中添加检测逻辑：

```java
static {
    // ... 现有逻辑 ...
    else if (OPERATING_SYSTEM_NAME.startsWith("solaris") || OPERATING_SYSTEM_NAME.startsWith("sunos")) {
        platform = PlatformEnum.SOLARIS;
    } else {
        platform = PlatformEnum.UNKNOWN;
    }
}
```

## 注意事项

### 1. 平台检测准确性

枚举值依赖于 `OSUtils` 的检测逻辑，确保检测逻辑与枚举值一致。

### 2. 大小写

枚举值使用全大写，符合 Java 枚举命名规范。

### 3. 兼容性

新增枚举值需要确保向后兼容，不影响现有代码。

## 使用示例

### 完整示例

```java
public class PlatformInfo {
    public static void printPlatformInfo() {
        PlatformEnum platform = OSUtils.platform;
        
        System.out.println("当前平台: " + platform);
        
        switch (platform) {
            case WINDOWS:
                System.out.println("运行在 Windows 系统上");
                break;
            case LINUX:
                System.out.println("运行在 Linux 系统上");
                break;
            case MACOSX:
                System.out.println("运行在 macOS 系统上");
                break;
            case UNKNOWN:
                System.out.println("无法识别的操作系统");
                break;
        }
    }
}
```

