# AbstractDtpLogging 详解

## 文件位置

```
logging/src/main/java/org/dromara/dynamictp/logging/AbstractDtpLogging.java
```

## 文件位置

```
logging/src/main/java/org/dromara/dynamictp/logging/AbstractDtpLogging.java
```

## 概述

`AbstractDtpLogging` 是 DynamicTp 日志模块的抽象基类，定义了日志初始化的通用逻辑和资源加载方法。它为不同的日志框架实现（Logback 和 Log4j2）提供了统一的抽象接口和公共功能。采用模板方法模式，定义日志初始化的骨架流程，由子类实现具体的日志框架配置加载逻辑。

## 类声明

```java
@Slf4j
public abstract class AbstractDtpLogging {
    // ...
}
```

**设计模式**：
- **模板方法模式**：定义算法骨架，子类实现具体步骤
- **抽象类**：不能被实例化，只能被继承

## 核心作用

1. **抽象接口定义**: 定义日志初始化的抽象方法，由子类实现
2. **日志路径初始化**: 初始化日志文件存储路径
3. **资源加载**: 提供统一的资源 URL 获取方法
4. **常量定义**: 定义监控日志相关的常量

## 核心常量

```java
protected static final String MONITOR_LOG_NAME = "DTP.MONITOR.LOG";  // 监控日志名称
private static final String CLASSPATH_PREFIX = "classpath:";         // Classpath 前缀
private static final String LOGGING_PATH = "LOG.PATH";                // 日志路径系统属性名
```

## 核心方法

### getResourceUrl(String resource)

**作用**: 获取资源的 URL，支持 classpath 和文件系统路径

**参数**:
- `resource`: 资源路径，支持 `classpath:` 前缀或文件路径

**返回值**: 资源的 URL 对象

**实现**:
```java
public URL getResourceUrl(String resource) throws IOException {
    if (resource.startsWith(CLASSPATH_PREFIX)) {
        String path = resource.substring(CLASSPATH_PREFIX.length());
        ClassLoader classLoader = DtpLoggingInitializer.class.getClassLoader();
        URL url = (classLoader != null ? classLoader.getResource(path) : ClassLoader.getSystemResource(path));
        if (url == null) {
            throw new FileNotFoundException("Cannot find file: +" + resource);
        }
        return url;
    }

    try {
        return new URL(resource);
    } catch (MalformedURLException ex) {
        return new File(resource).toURI().toURL();
    }
}
```

**处理逻辑**:
1. 如果资源路径以 `classpath:` 开头，从 Classpath 中加载资源
2. 使用 `DtpLoggingInitializer` 的 ClassLoader 或系统 ClassLoader 查找资源
3. 如果找不到资源，抛出 `FileNotFoundException`
4. 如果不是 classpath 路径，尝试作为 URL 解析
5. 如果 URL 解析失败，尝试作为文件路径处理

---

### loadConfiguration()

**作用**: 加载日志配置（抽象方法）

**说明**: 由子类实现具体的日志配置加载逻辑

---

### initMonitorLogger()

**作用**: 初始化监控日志 Logger（抽象方法）

**说明**: 由子类实现具体的监控 Logger 初始化逻辑

---

### 静态初始化块

**作用**: 初始化日志文件存储路径

**实现**:
```java
static {
    try {
        DtpProperties dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
        String logPath = dtpProperties.getLogPath();
        if (StringUtils.isBlank(logPath)) {
            String userHome = System.getProperty("user.home");
            System.setProperty(LOGGING_PATH, userHome + File.separator + "logs");
        } else {
            System.setProperty(LOGGING_PATH, logPath);
        }
    } catch (Exception e) {
        log.error("DynamicTp logging env init failed, if collectType is not logging, this error can be ignored.", e);
    }
}
```

**初始化流程**:
1. 从 `DtpProperties` 获取配置的日志路径
2. 如果配置的路径为空，使用默认路径：`${user.home}/logs`
3. 如果配置了路径，使用配置的路径
4. 将路径设置到系统属性 `LOG.PATH` 中，供日志配置文件使用
5. 如果初始化失败，记录错误日志（如果采集类型不是 logging，可以忽略此错误）

## 设计特点

### 1. 模板方法模式

定义日志初始化的骨架，子类实现具体的配置加载和 Logger 初始化逻辑。

### 2. 资源加载抽象

提供统一的资源加载方法，支持多种资源路径格式：
- Classpath 资源：`classpath:dtp-logback.xml`
- URL 资源：`http://example.com/config.xml`
- 文件系统路径：`/path/to/config.xml`

### 3. 路径配置灵活

支持通过配置文件和系统属性两种方式配置日志路径，提供默认值保证可用性。

### 4. 异常容错

静态初始化块中的异常不会影响框架其他功能，如果采集类型不是 logging，可以忽略初始化错误。

## 使用场景

### 1. 作为基类

子类继承 `AbstractDtpLogging` 实现具体的日志框架支持：

```java
public class DtpLogbackLogging extends AbstractDtpLogging {
    @Override
    public void loadConfiguration() {
        // Logback 配置加载逻辑
    }
    
    @Override
    public void initMonitorLogger() {
        // Logback Logger 初始化逻辑
    }
}
```

### 2. 资源加载

子类使用 `getResourceUrl()` 方法加载配置文件：

```java
URL configUrl = getResourceUrl("classpath:dtp-logback.xml");
```

### 3. 路径配置

日志配置文件可以使用系统属性 `LOG.PATH`：

```xml
<file>${LOG.PATH}/dynamictp/${APP.NAME}.monitor.log</file>
```

## 子类实现

### DtpLogbackLogging

Logback 日志实现，使用 `getResourceUrl()` 加载 Logback 配置文件。

### DtpLog4j2Logging

Log4j2 日志实现，使用 `getResourceUrl()` 加载 Log4j2 配置文件。

## 注意事项

1. **抽象方法**: 子类必须实现 `loadConfiguration()` 和 `initMonitorLogger()` 方法
2. **资源路径**: 使用 `classpath:` 前缀时，路径应该是相对于 Classpath 的路径
3. **路径配置**: 日志路径会在静态初始化块中设置，确保在使用前已初始化
4. **异常处理**: 静态初始化中的异常会被捕获并记录，不会中断类加载
5. **系统属性**: `LOG.PATH` 系统属性会被日志配置文件引用，确保路径一致性

## 相关类

- **DtpLoggingInitializer**: 日志初始化器，使用抽象类的子类
- **DtpLogbackLogging**: Logback 实现类
- **DtpLog4j2Logging**: Log4j2 实现类
- **DtpProperties**: 配置属性类，提供日志路径配置
- **ContextManagerHelper**: 上下文管理器，用于获取配置 Bean

