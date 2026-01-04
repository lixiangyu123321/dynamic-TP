# DtpLogbackLogging

## 概述

`DtpLogbackLogging` 是 DynamicTp 日志模块的 Logback 实现类，继承自 `AbstractDtpLogging`。它负责加载 Logback 配置文件并初始化监控日志 Logger，支持将线程池监控数据输出到独立的日志文件中。

## 核心作用

1. **Logback 配置加载**: 加载 Logback 配置文件（`dtp-logback.xml`）
2. **LoggerContext 管理**: 创建和管理独立的 LoggerContext
3. **监控 Logger 初始化**: 初始化监控日志 Logger 并注册到 `LogHelper`

## 核心属性

```java
private static final String LOGBACK_LOCATION = "classpath:dtp-logback.xml";  // Logback 配置文件路径
private LoggerContext loggerContext;  // Logback LoggerContext 实例
```

## 核心方法

### loadConfiguration()

**作用**: 加载 Logback 配置文件

**实现**:
```java
@Override
public void loadConfiguration() {
    try {
        loggerContext = new LoggerContext();
        new ContextInitializer(loggerContext).configureByResource(getResourceUrl(LOGBACK_LOCATION));
    } catch (Exception e) {
        log.error("Cannot initialize dtp logback logging.");
    }
}
```

**处理流程**:
1. 创建新的 `LoggerContext` 实例
2. 使用 `ContextInitializer` 初始化上下文
3. 通过 `getResourceUrl()` 获取配置文件 URL（继承自父类）
4. 调用 `configureByResource()` 加载配置文件
5. 如果加载失败，记录错误日志

**说明**:
- 创建独立的 `LoggerContext`，不会影响应用的主日志配置
- 配置文件路径为 `classpath:dtp-logback.xml`
- 使用父类的 `getResourceUrl()` 方法加载资源

---

### initMonitorLogger()

**作用**: 初始化监控日志 Logger

**实现**:
```java
@Override
public void initMonitorLogger() {
    LogHelper.init(getLoggerContext().getLogger(MONITOR_LOG_NAME));
}
```

**处理流程**:
1. 从 `LoggerContext` 获取名为 `DTP.MONITOR.LOG` 的 Logger
2. 调用 `LogHelper.init()` 注册 Logger，供其他模块使用

**说明**:
- `MONITOR_LOG_NAME` 常量定义在父类中，值为 `"DTP.MONITOR.LOG"`
- Logger 名称需要在配置文件中定义对应的 Logger 配置

---

### getLoggerContext()

**作用**: 获取 LoggerContext 实例

**返回值**: Logback 的 LoggerContext 实例

**实现**:
```java
public LoggerContext getLoggerContext() {
    return loggerContext;
}
```

**说明**: 提供访问 LoggerContext 的方法，用于获取 Logger 或其他配置信息

## 配置文件

### dtp-logback.xml

Logback 配置文件，通常包含以下内容：

```xml
<configuration>
    <!-- 定义日志文件路径变量 -->
    <property name="LOG_PATH" value="${LOG.PATH}"/>
    <property name="APP_NAME" value="${APP.NAME}"/>
    
    <!-- 监控日志 Appender -->
    <appender name="MONITOR_LOG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/dynamictp/${APP_NAME}.monitor.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>200MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${APP_NAME}", "thread_pool_metrics": %m}%n</pattern>
        </encoder>
    </appender>
    
    <!-- 监控日志 Logger -->
    <logger name="DTP.MONITOR.LOG" level="INFO" additivity="false">
        <appender-ref ref="MONITOR_LOG_FILE"/>
    </logger>
</configuration>
```

**配置说明**:
- **日志文件路径**: 使用系统属性 `LOG.PATH` 和 `APP.NAME`
- **滚动策略**: 按时间和大小滚动，支持压缩归档
- **日志格式**: JSON 格式，包含时间戳、应用名称和监控数据
- **Logger 名称**: 必须为 `DTP.MONITOR.LOG`，与代码中的常量一致

## 设计特点

### 1. 独立 LoggerContext

创建独立的 `LoggerContext`，不会影响应用的主日志配置，实现隔离。

### 2. 资源加载复用

使用父类的 `getResourceUrl()` 方法加载配置文件，支持多种资源路径格式。

### 3. 配置与代码分离

日志配置在 XML 文件中，代码只负责加载和初始化，便于维护和定制。

### 4. 错误容错

配置加载失败时记录错误日志，但不影响框架其他功能。

## 使用场景

### 1. 自动初始化

在 `DtpLoggingInitializer` 检测到 Logback 时自动创建：

```java
// DtpLoggingInitializer 中
try {
    Class.forName("ch.qos.logback.classic.Logger");
    dtpLogging = new DtpLogbackLogging();
} catch (ClassNotFoundException e) {
    // ...
}
```

### 2. 配置加载流程

```java
DtpLogbackLogging logging = new DtpLogbackLogging();
logging.loadConfiguration();      // 加载配置文件
logging.initMonitorLogger();      // 初始化监控 Logger
```

## 初始化流程

```
1. DtpLoggingInitializer 检测到 Logback
   ↓
2. 创建 DtpLogbackLogging 实例
   ↓
3. 调用 loadConfiguration()
   ↓
4. 创建 LoggerContext
   ↓
5. 加载 dtp-logback.xml 配置文件
   ↓
6. 调用 initMonitorLogger()
   ↓
7. 从 LoggerContext 获取 DTP.MONITOR.LOG Logger
   ↓
8. 调用 LogHelper.init() 注册 Logger
   ↓
9. 初始化完成，可以使用 LogHelper.getMonitorLogger() 获取 Logger
```

## 注意事项

1. **配置文件位置**: 配置文件 `dtp-logback.xml` 必须在 Classpath 中
2. **Logger 名称**: 配置文件中必须定义名为 `DTP.MONITOR.LOG` 的 Logger
3. **系统属性**: 配置文件依赖 `LOG.PATH` 和 `APP.NAME` 系统属性
4. **依赖要求**: 项目必须包含 Logback 相关依赖（`logback-classic`、`logback-core`）
5. **独立上下文**: 使用独立的 LoggerContext，不会影响应用主日志配置
6. **异常处理**: 配置加载失败会记录错误，但不会抛出异常

## 相关类

- **AbstractDtpLogging**: 抽象基类，提供资源加载和路径初始化
- **DtpLoggingInitializer**: 日志初始化器，负责创建和调用本类
- **LogHelper**: 日志辅助类，接收初始化后的 Logger
- **LoggerContext**: Logback 的日志上下文类
- **ContextInitializer**: Logback 的上下文初始化器

## 依赖要求

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <optional>true</optional>
</dependency>
```

## 配置示例

### 完整配置文件示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 系统属性 -->
    <property name="LOG_PATH" value="${LOG.PATH:-${user.home}/logs}"/>
    <property name="APP_NAME" value="${APP.NAME:-application}"/>
    
    <!-- 监控日志文件 Appender -->
    <appender name="MONITOR_LOG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/dynamictp/${APP_NAME}.monitor.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>200MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${APP_NAME}", "thread_pool_metrics": %m}%n</pattern>
        </encoder>
    </appender>
    
    <!-- 监控日志 Logger -->
    <logger name="DTP.MONITOR.LOG" level="INFO" additivity="false">
        <appender-ref ref="MONITOR_LOG_FILE"/>
    </logger>
</configuration>
```

