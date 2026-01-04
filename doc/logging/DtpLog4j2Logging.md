# DtpLog4j2Logging

## 概述

`DtpLog4j2Logging` 是 DynamicTp 日志模块的 Log4j2 实现类，继承自 `AbstractDtpLogging`。它负责加载 Log4j2 配置文件并初始化监控日志 Logger，支持将线程池监控数据输出到独立的日志文件中。

## 核心作用

1. **Log4j2 配置加载**: 加载 Log4j2 配置文件（`dtp-log4j2.xml`）
2. **配置合并**: 将 DynamicTp 的日志配置合并到应用的主 Log4j2 配置中
3. **监控 Logger 初始化**: 初始化监控日志 Logger 并注册到 `LogHelper`

## 核心常量

```java
private static final String LOG4J2_LOCATION = "classpath:dtp-log4j2.xml";  // Log4j2 配置文件路径
private static final String LOGGER_NAME_PREFIX = "DTP";                     // Logger 名称前缀
```

## 核心方法

### loadConfiguration()

**作用**: 加载 Log4j2 配置文件并合并到主配置

**实现**:
```java
@Override
public void loadConfiguration() {
    LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    Configuration configuration = loadConfiguration(loggerContext, LOG4J2_LOCATION);
    if (configuration == null) {
        return;
    }

    configuration.start();
    Map<String, Appender> appenderMap = configuration.getAppenders();
    Configuration contextConfiguration = loggerContext.getConfiguration();
    for (Appender appender : appenderMap.values()) {
        contextConfiguration.addAppender(appender);
    }
    Map<String, LoggerConfig> loggers = configuration.getLoggers();
    loggers.forEach((k, v) -> {
        if (k.startsWith(LOGGER_NAME_PREFIX)) {
            contextConfiguration.addLogger(k, v);
        }
    });

    loggerContext.updateLoggers();
}
```

**处理流程**:
1. 获取当前应用的 Log4j2 `LoggerContext`
2. 调用 `loadConfiguration()` 加载 DynamicTp 的配置文件
3. 如果加载失败，直接返回
4. 启动加载的配置
5. 获取配置中的所有 Appender
6. 将 Appender 添加到主配置中
7. 获取配置中的所有 Logger（名称以 `DTP` 开头）
8. 将符合条件的 Logger 添加到主配置中
9. 更新 LoggerContext，使配置生效

**说明**:
- 使用应用现有的 `LoggerContext`，而不是创建新的
- 只合并名称以 `DTP` 开头的 Logger，避免影响其他 Logger
- 所有 Appender 都会被合并，供 Logger 使用

---

### loadConfiguration(LoggerContext loggerContext, String location)

**作用**: 加载指定路径的 Log4j2 配置文件

**参数**:
- `loggerContext`: Log4j2 的 LoggerContext
- `location`: 配置文件路径

**返回值**: 加载的 Configuration 对象，如果加载失败返回 null

**实现**:
```java
private Configuration loadConfiguration(LoggerContext loggerContext, String location) {
    try {
        URL url = getResourceUrl(location);
        ConfigurationSource source = new ConfigurationSource(url.openStream(), url);
        return ConfigurationFactory.getInstance().getConfiguration(loggerContext, source);
    } catch (Exception e) {
        log.error("Cannot initialize dtp log4j2 logging.");
        return null;
    }
}
```

**处理流程**:
1. 使用父类的 `getResourceUrl()` 方法获取配置文件 URL
2. 创建 `ConfigurationSource` 对象
3. 使用 `ConfigurationFactory` 加载配置
4. 如果加载失败，记录错误日志并返回 null

---

### initMonitorLogger()

**作用**: 初始化监控日志 Logger

**实现**:
```java
@Override
public void initMonitorLogger() {
    LogHelper.init(getLogger(MONITOR_LOG_NAME));
}
```

**处理流程**:
1. 使用 `getLogger()` 方法获取名为 `DTP.MONITOR.LOG` 的 Logger
2. 调用 `LogHelper.init()` 注册 Logger，供其他模块使用

**说明**:
- `MONITOR_LOG_NAME` 常量定义在父类中，值为 `"DTP.MONITOR.LOG"`
- `getLogger()` 方法来自 SLF4J 的 `LoggerFactory`

## 配置文件

### dtp-log4j2.xml

Log4j2 配置文件，通常包含以下内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Properties>
        <Property name="LOG_PATH">${sys:LOG.PATH}</Property>
        <Property name="APP_NAME">${sys:APP.NAME}</Property>
    </Properties>
    
    <Appenders>
        <!-- 监控日志文件 Appender -->
        <RollingFile name="MONITOR_LOG_FILE"
                     fileName="${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log"
                     filePattern="${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log.%d{yyyy-MM-dd}.%i">
            <PatternLayout>
                <Pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${APP_NAME}", "thread_pool_metrics": %m}%n</Pattern>
            </PatternLayout>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="200MB"/>
            </Policies>
            <DefaultRolloverStrategy max="7"/>
        </RollingFile>
    </Appenders>
    
    <Loggers>
        <!-- 监控日志 Logger -->
        <Logger name="DTP.MONITOR.LOG" level="INFO" additivity="false">
            <AppenderRef ref="MONITOR_LOG_FILE"/>
        </Logger>
    </Loggers>
</Configuration>
```

**配置说明**:
- **系统属性**: 使用 `${sys:LOG.PATH}` 和 `${sys:APP.NAME}` 引用系统属性
- **滚动策略**: 按时间和大小滚动，支持压缩归档
- **日志格式**: JSON 格式，包含时间戳、应用名称和监控数据
- **Logger 名称**: 必须为 `DTP.MONITOR.LOG`，且名称以 `DTP` 开头

## 设计特点

### 1. 配置合并策略

将 DynamicTp 的配置合并到应用的主 Log4j2 配置中，而不是创建独立的配置上下文。

### 2. 选择性合并

只合并名称以 `DTP` 开头的 Logger，避免影响应用的其他日志配置。

### 3. Appender 共享

所有 Appender 都会被合并，可以被多个 Logger 共享使用。

### 4. 资源加载复用

使用父类的 `getResourceUrl()` 方法加载配置文件，支持多种资源路径格式。

## 使用场景

### 1. 自动初始化

在 `DtpLoggingInitializer` 检测到 Log4j2 时自动创建：

```java
// DtpLoggingInitializer 中
try {
    Class.forName("org.apache.logging.log4j.LogManager");
    dtpLogging = new DtpLog4j2Logging();
} catch (ClassNotFoundException e) {
    // ...
}
```

### 2. 配置加载流程

```java
DtpLog4j2Logging logging = new DtpLog4j2Logging();
logging.loadConfiguration();      // 加载并合并配置
logging.initMonitorLogger();      // 初始化监控 Logger
```

## 初始化流程

```
1. DtpLoggingInitializer 检测到 Log4j2
   ↓
2. 创建 DtpLog4j2Logging 实例
   ↓
3. 调用 loadConfiguration()
   ↓
4. 获取应用的主 LoggerContext
   ↓
5. 加载 dtp-log4j2.xml 配置文件
   ↓
6. 合并 Appender 到主配置
   ↓
7. 合并 DTP 开头的 Logger 到主配置
   ↓
8. 更新 LoggerContext 使配置生效
   ↓
9. 调用 initMonitorLogger()
   ↓
10. 获取 DTP.MONITOR.LOG Logger
    ↓
11. 调用 LogHelper.init() 注册 Logger
    ↓
12. 初始化完成，可以使用 LogHelper.getMonitorLogger() 获取 Logger
```

## 与 Logback 实现的区别

| 特性 | DtpLogbackLogging | DtpLog4j2Logging |
|------|-------------------|------------------|
| 配置上下文 | 创建独立的 LoggerContext | 使用应用的主 LoggerContext |
| 配置方式 | 独立配置，不影响主配置 | 合并到主配置中 |
| Logger 过滤 | 无，使用配置中的所有 Logger | 只合并 DTP 开头的 Logger |
| Appender 处理 | 独立使用 | 合并到主配置，可共享 |

## 注意事项

1. **配置文件位置**: 配置文件 `dtp-log4j2.xml` 必须在 Classpath 中
2. **Logger 名称**: 配置文件中必须定义名为 `DTP.MONITOR.LOG` 的 Logger，且名称必须以 `DTP` 开头
3. **系统属性**: 配置文件依赖 `LOG.PATH` 和 `APP.NAME` 系统属性
4. **依赖要求**: 项目必须包含 Log4j2 相关依赖（`log4j-core`、`log4j-api`）
5. **配置合并**: 配置会合并到应用的主 Log4j2 配置中，注意避免冲突
6. **异常处理**: 配置加载失败会记录错误，但不会抛出异常

## 相关类

- **AbstractDtpLogging**: 抽象基类，提供资源加载和路径初始化
- **DtpLoggingInitializer**: 日志初始化器，负责创建和调用本类
- **LogHelper**: 日志辅助类，接收初始化后的 Logger
- **LoggerContext**: Log4j2 的日志上下文类
- **ConfigurationFactory**: Log4j2 的配置工厂类

## 依赖要求

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j-impl</artifactId>
    <optional>true</optional>
</dependency>
```

## 配置示例

### 完整配置文件示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Properties>
        <Property name="LOG_PATH">${sys:LOG.PATH:-${sys:user.home}/logs}</Property>
        <Property name="APP_NAME">${sys:APP.NAME:-application}</Property>
    </Properties>
    
    <Appenders>
        <!-- 监控日志文件 Appender -->
        <RollingFile name="MONITOR_LOG_FILE"
                     fileName="${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log"
                     filePattern="${LOG_PATH}/dynamictp/${APP_NAME}.monitor.log.%d{yyyy-MM-dd}.%i.gz">
            <PatternLayout>
                <Pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${APP_NAME}", "thread_pool_metrics": %m}%n</Pattern>
            </PatternLayout>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="200MB"/>
            </Policies>
            <DefaultRolloverStrategy max="7" fileIndex="min"/>
        </RollingFile>
    </Appenders>
    
    <Loggers>
        <!-- 监控日志 Logger -->
        <Logger name="DTP.MONITOR.LOG" level="INFO" additivity="false">
            <AppenderRef ref="MONITOR_LOG_FILE"/>
        </Logger>
    </Loggers>
</Configuration>
```

