# AbstractRefresher

## 概述

`AbstractRefresher` 是配置刷新器的抽象实现，提供了配置解析、绑定、刷新的通用逻辑。子类只需要实现特定的配置获取逻辑。

## 核心作用

1. **配置解析**: 解析配置内容
2. **配置绑定**: 将配置绑定到 `DtpProperties`
3. **配置刷新**: 刷新线程池配置
4. **事件发布**: 发布刷新事件

## 核心属性

```java
protected final DtpProperties dtpProperties;  // 配置属性
```

## 核心方法

### refresh(String content, ConfigFileTypeEnum fileType)

**作用**: 刷新配置（从字符串内容）

**实现**:
```java
@Override
public void refresh(String content, ConfigFileTypeEnum fileType) {
    if (StringUtils.isBlank(content) || Objects.isNull(fileType)) {
        log.warn("DynamicTp refresh, empty content or null fileType.");
        return;
    }
    
    try {
        val configHandler = ConfigHandler.getInstance();
        val properties = configHandler.parseConfig(content, fileType);
        refresh(properties);
    } catch (IOException e) {
        log.error("DynamicTp refresh error, content: {}, fileType: {}", content, fileType, e);
    }
}
```

**流程**:
1. 验证参数
2. 解析配置内容
3. 调用 `refresh(Map)` 方法

---

### refresh(Map<Object, Object> properties)

**作用**: 刷新配置（从 Map）

**实现**:
```java
protected void refresh(Map<Object, Object> properties) {
    if (MapUtils.isEmpty(properties)) {
        log.warn("DynamicTp refresh, empty properties.");
        return;
    }
    BinderHelper.bindDtpProperties(properties, dtpProperties);
    doRefresh(dtpProperties);
}
```

**流程**:
1. 验证配置
2. 绑定配置到 `DtpProperties`
3. 执行刷新

---

### refresh(Object environment)

**作用**: 刷新配置（从 Environment 对象）

**实现**:
```java
protected void refresh(Object environment) {
    BinderHelper.bindDtpProperties(environment, dtpProperties);
    doRefresh(dtpProperties);
}
```

---

### doRefresh(DtpProperties properties)

**作用**: 执行刷新

**实现**:
```java
protected void doRefresh(DtpProperties properties) {
    DtpRegistry.refresh(properties);  // 刷新执行器
    publishEvent(properties);         // 发布事件
}
```

---

### needRefresh(Set<String> changedKeys)

**作用**: 判断是否需要刷新

**实现**:
```java
protected boolean needRefresh(Set<String> changedKeys) {
    if (CollectionUtils.isEmpty(changedKeys)) {
        return false;
    }
    changedKeys = changedKeys.stream()
            .filter(str -> str.startsWith(MAIN_PROPERTIES_PREFIX))
            .collect(Collectors.toSet());
    return CollectionUtils.isNotEmpty(changedKeys);
}
```

**说明**: 只刷新以 `spring.dynamic.tp` 开头的配置

---

### publishEvent(DtpProperties dtpProperties)

**作用**: 发布刷新事件

**实现**:
```java
private void publishEvent(DtpProperties dtpProperties) {
    RefreshEvent event = new RefreshEvent(this, dtpProperties);
    EventBusManager.post(event);
}
```

## 使用场景

### 1. 实现配置中心刷新器

```java
public class NacosRefresher extends AbstractRefresher {
    public NacosRefresher(DtpProperties dtpProperties) {
        super(dtpProperties);
    }
    
    @NacosConfigListener(dataId = "dynamic-tp", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String config) {
        refresh(config, ConfigFileTypeEnum.YAML);
    }
}
```

### 2. 实现文件监听刷新器

```java
public class FileRefresher extends AbstractRefresher {
    @Override
    public void refresh(String content, ConfigFileTypeEnum fileType) {
        super.refresh(content, fileType);
    }
}
```

## 设计特点

### 1. 模板方法模式

定义刷新流程的骨架，子类实现特定逻辑。

### 2. 配置绑定

使用 `BinderHelper` 统一绑定配置。

### 3. 事件发布

刷新后发布事件，其他组件可以监听。

## 注意事项

1. **配置格式**: 需要指定正确的配置格式
2. **配置前缀**: 只刷新以 `spring.dynamic.tp` 开头的配置
3. **异常处理**: 需要处理配置解析异常

