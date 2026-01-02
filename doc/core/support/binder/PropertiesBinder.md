# PropertiesBinder

## 概述

`PropertiesBinder` 是配置属性绑定器接口，用于将配置源的属性绑定到 `DtpProperties` 对象。它支持从 Map 或 Environment 对象绑定配置。

## 核心作用

1. **配置绑定**: 将配置源的属性绑定到 `DtpProperties`
2. **多源支持**: 支持从 Map 或 Environment 绑定
3. **生命周期钩子**: 提供绑定前后的钩子方法

## 接口定义

```java
public interface PropertiesBinder {
    void bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties);
    void bindDtpProperties(Object environment, DtpProperties dtpProperties);
    default void beforeBind(Object source, DtpProperties dtpProperties) { }
    default void afterBind(Object source, DtpProperties dtpProperties) { }
}
```

## 核心方法

### bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties)

**作用**: 从 Map 绑定配置属性

**参数**:
- `properties`: 配置属性 Map
- `dtpProperties`: 目标配置对象

**说明**: 将 Map 中的属性值绑定到 `DtpProperties` 对象

---

### bindDtpProperties(Object environment, DtpProperties dtpProperties)

**作用**: 从 Environment 对象绑定配置属性

**参数**:
- `environment`: 环境对象（如 Spring Environment）
- `dtpProperties`: 目标配置对象

**说明**: 从环境对象中读取配置并绑定到 `DtpProperties`

---

### beforeBind(Object source, DtpProperties dtpProperties)

**作用**: 绑定前的钩子方法

**默认实现**: 空操作

**使用场景**: 绑定前进行预处理

---

### afterBind(Object source, DtpProperties dtpProperties)

**作用**: 绑定后的钩子方法

**默认实现**: 空操作

**使用场景**: 绑定后进行后处理

## 使用场景

### 1. 配置中心绑定

从配置中心获取配置后，绑定到 `DtpProperties`：

```java
Map<String, Object> config = configCenter.getConfig();
PropertiesBinder binder = ...;
binder.bindDtpProperties(config, dtpProperties);
```

### 2. Spring Environment 绑定

从 Spring Environment 绑定配置：

```java
Environment environment = applicationContext.getEnvironment();
binder.bindDtpProperties(environment, dtpProperties);
```

## 实现方式

框架通过 SPI 机制加载 `PropertiesBinder` 实现，通常由配置中心模块提供实现。

## 注意事项

1. **SPI 机制**: 通过 SPI 机制加载实现
2. **单例**: 通常使用单例模式
3. **绑定顺序**: 先调用 `beforeBind()`，然后绑定，最后调用 `afterBind()`

