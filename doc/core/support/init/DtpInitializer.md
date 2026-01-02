# DtpInitializer

## 概述

`DtpInitializer` 是动态线程池初始化器接口，用于在框架启动时执行初始化逻辑。它支持通过 SPI 机制扩展，允许在框架启动时执行自定义初始化操作。

## 核心作用

1. **初始化扩展**: 提供框架初始化的扩展点
2. **有序执行**: 支持按顺序执行初始化器
3. **参数传递**: 支持传递初始化参数

## 接口定义

```java
public interface DtpInitializer {
    default int getOrder() {
        return 0;
    }
    
    String getName();
    
    void init(Object... args);
}
```

## 核心方法

### getOrder()

**作用**: 返回初始化器的执行顺序

**默认值**: `0`

**说明**: 顺序值越小，执行越早

---

### getName()

**作用**: 返回初始化器的名称

**说明**: 用于标识初始化器

---

### init(Object... args)

**作用**: 执行初始化逻辑

**参数**: `args` - 初始化参数（可变参数）

**说明**: 框架启动时会调用此方法执行初始化

## 使用场景

### 1. 自定义初始化

在框架启动时执行自定义初始化逻辑：

```java
public class CustomInitializer implements DtpInitializer {
    
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public void init(Object... args) {
        // 自定义初始化逻辑
        System.out.println("Custom initializer init");
    }
}
```

### 2. SPI 注册

创建 `META-INF/services/org.dromara.dynamictp.core.support.init.DtpInitializer` 文件：

```
com.example.CustomInitializer
```

## 执行流程

1. **SPI 加载**: 通过 SPI 机制加载所有初始化器
2. **排序**: 按 `getOrder()` 排序
3. **执行**: 依次调用每个初始化器的 `init()` 方法

## 注意事项

1. **执行顺序**: 通过 `getOrder()` 控制执行顺序
2. **异常处理**: 初始化器中的异常需要自行处理
3. **参数传递**: 框架可能传递初始化参数，需要根据实际情况处理

