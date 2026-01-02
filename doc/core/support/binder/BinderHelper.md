# BinderHelper

## 概述

`BinderHelper` 是配置绑定辅助类，提供静态方法帮助获取和使用 `PropertiesBinder`。它使用单例模式和 SPI 机制来管理 `PropertiesBinder` 实例。

## 核心作用

1. **Binder 管理**: 管理 `PropertiesBinder` 实例
2. **SPI 加载**: 通过 SPI 机制加载 `PropertiesBinder` 实现
3. **便捷方法**: 提供便捷的绑定方法

## 核心方法

### getBinder()

**作用**: 获取 `PropertiesBinder` 实例

**实现流程**:

1. **从单例获取**:
   ```java
   PropertiesBinder binder = Singleton.INST.get(PropertiesBinder.class);
   if (Objects.nonNull(binder)) {
       return binder;
   }
   ```

2. **SPI 加载**:
   ```java
   final PropertiesBinder loadedFirstBinder = ExtensionServiceLoader.getFirst(PropertiesBinder.class);
   if (Objects.isNull(loadedFirstBinder)) {
       log.error("DynamicTp refresh, no SPI for PropertiesBinder.");
       return null;
   }
   ```

3. **缓存到单例**:
   ```java
   Singleton.INST.single(PropertiesBinder.class, loadedFirstBinder);
   return loadedFirstBinder;
   ```

**说明**:
- 首先尝试从单例获取
- 如果不存在，通过 SPI 加载第一个实现
- 加载后缓存到单例，避免重复加载

---

### bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties)

**作用**: 从 Map 绑定配置属性

**实现**:
```java
public static void bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties) {
    final PropertiesBinder binder = getBinder();
    if (Objects.isNull(binder)) {
        return;  // 没有找到 binder，直接返回
    }
    binder.bindDtpProperties(properties, dtpProperties);
}
```

---

### bindDtpProperties(Object environment, DtpProperties dtpProperties)

**作用**: 从 Environment 对象绑定配置属性

**实现**:
```java
public static void bindDtpProperties(Object environment, DtpProperties dtpProperties) {
    final PropertiesBinder binder = getBinder();
    if (Objects.isNull(binder)) {
        return;
    }
    binder.bindDtpProperties(environment, dtpProperties);
}
```

## 使用场景

### 1. 配置刷新

在配置刷新时使用：

```java
Map<String, Object> newConfig = configCenter.getConfig();
BinderHelper.bindDtpProperties(newConfig, dtpProperties);
```

### 2. 初始化配置

在框架初始化时使用：

```java
Environment environment = applicationContext.getEnvironment();
BinderHelper.bindDtpProperties(environment, dtpProperties);
```

## 设计特点

### 1. 单例模式

使用 `Singleton` 管理 `PropertiesBinder` 实例，确保全局唯一。

### 2. SPI 机制

通过 SPI 机制加载实现，支持扩展。

### 3. 懒加载

首次使用时才加载，避免不必要的初始化。

### 4. 空值处理

如果没有找到 binder，直接返回，不抛出异常。

## 注意事项

1. **SPI 要求**: 需要配置中心模块提供 `PropertiesBinder` 实现
2. **单例缓存**: Binder 实例会被缓存，后续调用直接使用缓存的实例
3. **错误处理**: 如果没有找到 binder，会记录错误日志但不抛出异常

