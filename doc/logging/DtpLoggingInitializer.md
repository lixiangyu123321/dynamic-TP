# DtpLoggingInitializer

## 概述

`DtpLoggingInitializer` 是 DynamicTp 框架的日志初始化器，负责自动检测和初始化日志框架。它采用单例模式，在静态代码块中自动检测可用的日志框架（Logback 或 Log4j2），并创建相应的日志实现实例。

## 核心作用

1. **日志框架自动检测**: 自动检测 Classpath 中可用的日志框架
2. **日志实现创建**: 根据检测结果创建相应的日志实现（`DtpLogbackLogging` 或 `DtpLog4j2Logging`）
3. **日志配置加载**: 加载日志配置文件和初始化监控日志 Logger
4. **单例模式**: 确保整个应用只有一个日志初始化器实例

## 核心属性

```java
private static AbstractDtpLogging dtpLogging;  // 日志实现实例
```

## 核心方法

### getInstance()

**作用**: 获取日志初始化器的单例实例

**实现**:
```java
public static DtpLoggingInitializer getInstance() {
    return LoggingInstance.INSTANCE;
}
```

**说明**: 
- 使用静态内部类实现线程安全的单例模式
- 通过 `LoggingInstance` 内部类持有单例实例

---

### loadConfiguration()

**作用**: 加载日志配置并初始化监控日志 Logger

**实现**:
```java
public void loadConfiguration() {
    if (Objects.isNull(dtpLogging)) {
        return;
    }
    dtpLogging.loadConfiguration();
    dtpLogging.initMonitorLogger();
}
```

**流程**:
1. 检查日志实现是否已创建
2. 如果为空，直接返回（说明没有可用的日志框架）
3. 调用日志实现的 `loadConfiguration()` 加载配置
4. 调用日志实现的 `initMonitorLogger()` 初始化监控 Logger

---

### 静态初始化块

**作用**: 自动检测并创建日志框架实现

**实现**:
```java
static {
    try {
        Class.forName("ch.qos.logback.classic.Logger");
        dtpLogging = new DtpLogbackLogging();
    } catch (ClassNotFoundException e) {
        try {
            Class.forName("org.apache.logging.log4j.LogManager");
            dtpLogging = new DtpLog4j2Logging();
        } catch (ClassNotFoundException classNotFoundException) {
            log.error("DynamicTp initialize logging failed, please check whether logback or log4j related dependencies exist.");
        }
    }
}
```

**检测流程**:
1. 首先尝试检测 Logback：通过 `Class.forName("ch.qos.logback.classic.Logger")` 检测
2. 如果 Logback 存在，创建 `DtpLogbackLogging` 实例
3. 如果 Logback 不存在，尝试检测 Log4j2：通过 `Class.forName("org.apache.logging.log4j.LogManager")` 检测
4. 如果 Log4j2 存在，创建 `DtpLog4j2Logging` 实例
5. 如果两者都不存在，记录错误日志

## 设计特点

### 1. 自动检测机制

通过 `Class.forName()` 动态检测日志框架，无需在编译时依赖具体的日志框架实现。

### 2. 优先级策略

检测顺序：Logback > Log4j2
- 优先使用 Logback（更常用）
- 如果 Logback 不可用，则使用 Log4j2

### 3. 单例模式

使用静态内部类实现线程安全的单例模式，确保整个应用只有一个初始化器实例。

### 4. 延迟初始化

日志配置的加载是延迟的，只有在调用 `loadConfiguration()` 时才真正加载配置。

## 使用场景

### 1. 框架启动时自动初始化

在框架启动时，`LogHelper` 的静态代码块会调用初始化器：

```java
// LogHelper 中的静态初始化
static {
    DtpLoggingInitializer.getInstance().loadConfiguration();
}
```

### 2. 手动初始化

如果需要手动初始化日志配置：

```java
DtpLoggingInitializer initializer = DtpLoggingInitializer.getInstance();
initializer.loadConfiguration();
```

## 类关系

```
DtpLoggingInitializer
    ├── AbstractDtpLogging (抽象日志类)
    │   ├── DtpLogbackLogging (Logback 实现)
    │   └── DtpLog4j2Logging (Log4j2 实现)
    └── LogHelper (日志辅助类，使用初始化器)
```

## 注意事项

1. **日志框架依赖**: 确保项目中至少包含 Logback 或 Log4j2 的依赖
2. **初始化时机**: 日志初始化在静态代码块中自动执行，确保在使用前已完成检测
3. **错误处理**: 如果两个日志框架都不可用，会记录错误日志，但不影响框架其他功能
4. **线程安全**: 单例模式确保多线程环境下的安全性
5. **配置加载**: `loadConfiguration()` 方法可以被多次调用，但建议只调用一次

## 相关类

- **AbstractDtpLogging**: 抽象日志基类
- **DtpLogbackLogging**: Logback 日志实现
- **DtpLog4j2Logging**: Log4j2 日志实现
- **LogHelper**: 日志辅助类，使用初始化器加载配置

