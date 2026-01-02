# ConfigHandler

## 概述

`ConfigHandler` 是配置处理器，负责解析配置文件。它支持多种配置格式（Properties、YAML、JSON），并通过 SPI 机制支持扩展。

## 核心作用

1. **配置解析**: 解析配置文件内容
2. **格式支持**: 支持多种配置格式
3. **SPI 扩展**: 支持通过 SPI 扩展配置解析器

## 核心属性

```java
private static final List<ConfigParser> PARSERS = Lists.newArrayList();  // 配置解析器列表
```

## 初始化

在构造方法中初始化解析器：

```java
private ConfigHandler() {
    List<ConfigParser> loadedParses = ExtensionServiceLoader.get(ConfigParser.class);
    if (CollectionUtils.isNotEmpty(loadedParses)) {
        PARSERS.addAll(loadedParses);
    }
    PARSERS.add(new PropertiesConfigParser());  // Properties 解析器
    PARSERS.add(new YamlConfigParser());        // YAML 解析器
    PARSERS.add(new JsonConfigParser());        // JSON 解析器
}
```

**解析器顺序**:
1. SPI 加载的解析器
2. Properties 解析器
3. YAML 解析器
4. JSON 解析器

## 核心方法

### parseConfig(String content, ConfigFileTypeEnum type)

**作用**: 解析配置内容

**实现**:
```java
public Map<Object, Object> parseConfig(String content, ConfigFileTypeEnum type) throws IOException {
    for (ConfigParser parser : PARSERS) {
        if (parser.supports(type)) {
            return parser.doParse(content);
        }
    }
    return Collections.emptyMap();
}
```

**流程**:
1. 遍历解析器列表
2. 找到支持指定类型的解析器
3. 调用解析器的 `doParse()` 方法
4. 返回解析结果

---

### getInstance()

**作用**: 获取单例实例

**实现**:
```java
public static ConfigHandler getInstance() {
    return ConfigHandlerHolder.INSTANCE;
}

private static class ConfigHandlerHolder {
    private static final ConfigHandler INSTANCE = new ConfigHandler();
}
```

**说明**: 使用静态内部类实现单例模式

## 使用场景

### 1. 解析配置文件

```java
ConfigHandler handler = ConfigHandler.getInstance();
Map<Object, Object> properties = handler.parseConfig(configContent, ConfigFileTypeEnum.YAML);
```

### 2. 扩展配置解析器

通过 SPI 扩展配置解析器：

```java
// 实现 ConfigParser 接口
public class CustomConfigParser implements ConfigParser {
    @Override
    public boolean supports(ConfigFileTypeEnum type) {
        return type == ConfigFileTypeEnum.CUSTOM;
    }
    
    @Override
    public Map<Object, Object> doParse(String content) throws IOException {
        // 解析逻辑
    }
}

// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.common.parser.config.ConfigParser
// com.example.CustomConfigParser
```

## 设计特点

### 1. 单例模式

使用静态内部类实现线程安全的单例。

### 2. SPI 扩展

支持通过 SPI 机制扩展配置解析器。

### 3. 多格式支持

内置支持 Properties、YAML、JSON 格式。

## 注意事项

1. **解析器顺序**: 解析器按添加顺序查找
2. **异常处理**: 解析异常需要调用方处理
3. **空结果**: 如果找不到支持的解析器，返回空 Map

