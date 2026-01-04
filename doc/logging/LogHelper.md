# LogHelper

## 概述

`LogHelper` 是 DynamicTp 日志模块的辅助类，提供统一的监控日志输出接口。它是一个工具类，采用单例模式管理监控日志 Logger，确保整个应用使用同一个监控日志实例。

## 核心作用

1. **监控 Logger 管理**: 统一管理监控日志 Logger 实例
2. **日志初始化**: 在静态代码块中自动初始化日志配置
3. **统一访问接口**: 提供静态方法获取监控 Logger，方便其他模块使用

## 核心属性

```java
private static Logger monitorLogger;  // 监控日志 Logger 实例
```

## 核心方法

### init(Logger logger)

**作用**: 初始化监控日志 Logger

**参数**:
- `logger`: 要设置的监控日志 Logger 实例

**实现**:
```java
public static void init(Logger logger) {
    monitorLogger = logger;
}
```

**说明**: 
- 由日志实现类（`DtpLogbackLogging` 或 `DtpLog4j2Logging`）调用
- 设置监控日志 Logger 实例，供后续使用

---

### getMonitorLogger()

**作用**: 获取监控日志 Logger 实例

**返回值**: 监控日志 Logger 实例，可能为 null（如果未初始化）

**实现**:
```java
public static Logger getMonitorLogger() {
    return monitorLogger;
}
```

**使用示例**:
```java
Logger monitorLogger = LogHelper.getMonitorLogger();
if (monitorLogger != null) {
    monitorLogger.info("监控数据: {}", jsonData);
}
```

---

### 静态初始化块

**作用**: 自动加载日志配置

**实现**:
```java
static {
    DtpLoggingInitializer.getInstance().loadConfiguration();
}
```

**说明**:
- 在类加载时自动执行
- 调用 `DtpLoggingInitializer` 加载日志配置
- 确保在使用监控 Logger 前已完成初始化

## 设计特点

### 1. 工具类设计

采用 `final` 类和私有构造函数，防止实例化，所有方法都是静态方法。

### 2. 自动初始化

在静态代码块中自动初始化日志配置，无需手动调用。

### 3. 单例管理

使用静态变量管理监控 Logger，确保整个应用只有一个监控日志实例。

### 4. 简单易用

提供简单的静态方法，方便其他模块获取和使用监控 Logger。

## 使用场景

### 1. 监控数据输出

在监控数据采集器中，使用 `LogHelper` 输出监控数据：

```java
public class LogCollector extends AbstractCollector {
    @Override
    public void collect(ThreadPoolStats stats) {
        Logger monitorLogger = LogHelper.getMonitorLogger();
        if (monitorLogger != null) {
            String jsonData = JSON.toJSONString(stats);
            monitorLogger.info(jsonData);
        }
    }
}
```

### 2. 日志配置初始化

日志实现类在初始化完成后，调用 `LogHelper.init()` 设置 Logger：

```java
// DtpLogbackLogging 中
@Override
public void initMonitorLogger() {
    LogHelper.init(getLoggerContext().getLogger(MONITOR_LOG_NAME));
}
```

## 初始化流程

```
1. LogHelper 类加载
   ↓
2. 静态代码块执行
   ↓
3. 调用 DtpLoggingInitializer.getInstance().loadConfiguration()
   ↓
4. DtpLoggingInitializer 检测日志框架并创建实现
   ↓
5. 调用日志实现的 loadConfiguration() 加载配置
   ↓
6. 调用日志实现的 initMonitorLogger() 初始化 Logger
   ↓
7. 日志实现调用 LogHelper.init(logger) 设置 Logger
   ↓
8. 初始化完成，可以使用 LogHelper.getMonitorLogger() 获取 Logger
```

## 注意事项

1. **空值检查**: `getMonitorLogger()` 可能返回 null，使用前需要检查
2. **初始化时机**: Logger 的初始化是异步的，在静态代码块中执行，但实际设置可能在后续步骤
3. **线程安全**: 静态变量的读写需要考虑线程安全，但通常初始化在单线程环境下完成
4. **Logger 类型**: 返回的 Logger 是 SLF4J 的 `Logger` 接口，具体实现取决于使用的日志框架

## 相关类

- **DtpLoggingInitializer**: 日志初始化器，在静态代码块中被调用
- **DtpLogbackLogging**: Logback 实现，调用 `init()` 设置 Logger
- **DtpLog4j2Logging**: Log4j2 实现，调用 `init()` 设置 Logger
- **LogCollector**: 日志采集器，使用 `getMonitorLogger()` 输出监控数据

## 使用示例

### 完整使用流程

```java
// 1. LogHelper 自动初始化（在类加载时）
// 静态代码块会自动调用 DtpLoggingInitializer

// 2. 获取监控 Logger
Logger monitorLogger = LogHelper.getMonitorLogger();

// 3. 检查并输出日志
if (monitorLogger != null) {
    ThreadPoolStats stats = new ThreadPoolStats();
    // ... 填充统计数据
    String json = JSON.toJSONString(stats);
    monitorLogger.info(json);
}
```

### 在采集器中使用

```java
@Component
public class LogCollector extends AbstractCollector {
    
    @Override
    public void collect(ThreadPoolStats stats) {
        Logger logger = LogHelper.getMonitorLogger();
        if (logger != null && logger.isInfoEnabled()) {
            String jsonData = JSON.toJSONString(stats);
            logger.info(jsonData);
        }
    }
}
```

