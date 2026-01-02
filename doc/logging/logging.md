# Logging 模块

## 模块概述

`logging` 模块是 DynamicTp 框架的日志模块，提供了线程池监控数据的日志采集功能。支持将线程池指标数据以 JSON 格式输出到日志文件中，便于后续的日志分析和监控。

## 主要功能

### 1. 日志初始化

- **DtpLoggingInitializer**: 日志初始化器
  - 自动检测可用的日志框架（Logback 或 Log4j2）
  - 初始化相应的日志配置
  - 创建监控日志 Logger

### 2. 日志框架支持

框架支持两种主流的日志框架：

#### Logback
- **DtpLogbackLogging**: Logback 日志实现
  - 加载 Logback 配置文件
  - 配置监控日志 Appender
  - 初始化监控日志 Logger

#### Log4j2
- **DtpLog4j2Logging**: Log4j2 日志实现
  - 加载 Log4j2 配置文件
  - 配置监控日志 Appender
  - 初始化监控日志 Logger

### 3. 日志配置

框架提供了预定义的日志配置文件：

- **dtp-logback.xml**: Logback 日志配置
- **dtp-log4j2.xml**: Log4j2 日志配置

配置文件特点：

1. **独立日志文件**: 监控日志输出到独立的日志文件
2. **JSON 格式**: 日志以 JSON 格式输出，便于解析
3. **滚动策略**: 支持按时间和大小滚动日志文件
4. **压缩归档**: 支持日志文件压缩归档

### 4. 日志辅助类

- **LogHelper**: 日志辅助类
  - 提供统一的日志输出接口
  - 封装监控日志的输出逻辑

- **AbstractDtpLogging**: 抽象日志类
  - 定义日志初始化的通用逻辑
  - 提供资源加载的通用方法

## 核心类说明

### DtpLoggingInitializer

日志初始化器，负责日志框架的初始化和配置：

- 自动检测 Classpath 中的日志框架
- 根据检测结果创建相应的日志实现
- 加载日志配置文件
- 初始化监控日志 Logger

### DtpLogbackLogging

Logback 日志实现：

- `loadConfiguration()`: 加载 Logback 配置
- `initMonitorLogger()`: 初始化监控日志 Logger

### DtpLog4j2Logging

Log4j2 日志实现：

- `loadConfiguration()`: 加载 Log4j2 配置
- `initMonitorLogger()`: 初始化监控日志 Logger

### LogHelper

日志辅助类：

- `init()`: 初始化日志 Logger
- `info()`: 输出 INFO 级别日志
- `error()`: 输出 ERROR 级别日志

## 日志格式

监控日志以 JSON 格式输出，包含以下信息：

```json
{
  "datetime": "2024-01-01 12:00:00.000",
  "app_name": "my-app",
  "thread_pool_metrics": {
    "threadPoolName": "dtpExecutor1",
    "corePoolSize": 10,
    "maximumPoolSize": 20,
    "activeCount": 5,
    "queueSize": 100,
    "completedTaskCount": 1000,
    "rejectCount": 0
  }
}
```

## 日志文件配置

### 日志文件路径

默认日志文件路径：`${user.home}/logs/dynamictp/${app.name}.monitor.log`

可通过配置项 `spring.dynamic.tp.logPath` 自定义日志路径。

### 日志滚动策略

- **按时间滚动**: 每天生成一个新的日志文件
- **按大小滚动**: 单个日志文件达到指定大小时滚动
- **压缩归档**: 滚动后的日志文件自动压缩
- **保留策略**: 可配置保留的日志文件数量和总大小

### 配置示例

#### Logback 配置

```xml
<appender name="MONITOR_LOG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG.PATH}/dynamictp/${APP.NAME}.monitor.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG.PATH}/dynamictp/${APP.NAME}.monitor.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>200MB</maxFileSize>
        <maxHistory>7</maxHistory>
        <totalSizeCap>2GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${APP.NAME}", "thread_pool_metrics": %m}%n</pattern>
    </encoder>
</appender>
```

#### Log4j2 配置

```xml
<RollingFile name="MONITOR_LOG_FILE"
             fileName="${sys:LOG.PATH}/dynamictp/${sys:APP.NAME}.monitor.log"
             filePattern="${sys:LOG.PATH}/dynamictp/${sys:APP.NAME}.monitor.log.%d{yyyy-MM-dd}.%i">
    <PatternLayout>
        <Pattern>{"datetime": "%d{yyyy-MM-dd HH:mm:ss.SSS}", "app_name": "${sys:APP.NAME}", "thread_pool_metrics": %m}%n</Pattern>
    </PatternLayout>
    <Policies>
        <TimeBasedTriggeringPolicy/>
        <SizeBasedTriggeringPolicy size="200MB"/>
    </Policies>
    <DefaultRolloverStrategy max="7"/>
</RollingFile>
```

## 依赖关系

- `slf4j-api`: 日志接口
- `logback-classic`: Logback 日志实现（可选）
- `log4j-core`: Log4j2 日志实现（可选）

## 使用场景

1. **监控数据采集**: 将线程池监控数据输出到日志文件
2. **日志分析**: 通过日志分析工具分析线程池运行情况
3. **问题排查**: 通过日志排查线程池相关问题
4. **性能监控**: 通过日志监控线程池性能指标

## 配置说明

### 启用日志采集

在配置中设置监控采集类型为 `logging`：

```yaml
spring:
  dynamic:
    tp:
      collector-types: logging
```

### 自定义日志路径

```yaml
spring:
  dynamic:
    tp:
      log-path: /var/log/myapp
```

## 注意事项

1. **日志框架检测**: 框架会自动检测可用的日志框架，如果都不可用会记录错误日志
2. **日志文件权限**: 确保应用有权限在指定路径创建日志文件
3. **日志文件大小**: 注意配置日志文件大小和保留策略，避免占用过多磁盘空间
4. **JSON 格式**: 监控日志以 JSON 格式输出，便于后续解析和分析

