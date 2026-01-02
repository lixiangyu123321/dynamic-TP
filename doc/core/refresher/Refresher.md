# Refresher

## 概述

`Refresher` 是配置刷新器接口，定义了配置刷新的方法。它用于从配置中心或其他来源刷新线程池配置。

## 核心作用

1. **配置刷新**: 刷新线程池配置
2. **格式支持**: 支持多种配置格式（YAML、Properties 等）

## 接口定义

```java
public interface Refresher {
    void refresh(String content, ConfigFileTypeEnum fileType);
}
```

## 核心方法

### refresh(String content, ConfigFileTypeEnum fileType)

**作用**: 刷新配置

**参数**:
- `content`: 配置内容（字符串）
- `fileType`: 配置文件类型（YAML、Properties 等）

**说明**: 解析配置内容并刷新线程池配置

## 实现类

### AbstractRefresher

`AbstractRefresher` 是 `Refresher` 的抽象实现：

```java
public abstract class AbstractRefresher implements Refresher {
    @Override
    public void refresh(String content, ConfigFileTypeEnum fileType) {
        // 解析配置
        val configHandler = ConfigHandler.getInstance();
        val properties = configHandler.parseConfig(content, fileType);
        refresh(properties);
    }
    
    protected void refresh(Map<Object, Object> properties) {
        // 绑定配置
        BinderHelper.bindDtpProperties(properties, dtpProperties);
        doRefresh(dtpProperties);
    }
    
    protected void doRefresh(DtpProperties properties) {
        // 刷新执行器
        DtpRegistry.refresh(properties);
        // 发布事件
        publishEvent(properties);
    }
}
```

## 使用场景

### 1. 配置中心集成

在配置中心监听器中实现 `Refresher`：

```java
public class NacosRefresher extends AbstractRefresher {
    @Override
    public void refresh(String content, ConfigFileTypeEnum fileType) {
        super.refresh(content, fileType);
    }
}
```

### 2. 手动刷新

```java
Refresher refresher = new MyRefresher(dtpProperties);
refresher.refresh(configContent, ConfigFileTypeEnum.YAML);
```

## 设计特点

### 1. 格式支持

支持多种配置格式（YAML、Properties 等）。

### 2. 事件发布

刷新后发布 `RefreshEvent` 事件。

### 3. 抽象实现

提供 `AbstractRefresher` 抽象类，简化实现。

## 注意事项

1. **配置格式**: 需要指定正确的配置格式
2. **配置内容**: 配置内容需要符合框架的配置结构
3. **事件发布**: 刷新后会发布事件，其他组件可以监听

