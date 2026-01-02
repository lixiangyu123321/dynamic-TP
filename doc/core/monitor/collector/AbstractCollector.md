# AbstractCollector

## 概述

`AbstractCollector` 是抽象收集器基类，实现了 `MetricsCollector` 接口。它提供了 `support()` 方法的默认实现。

## 核心作用

1. **基类实现**: 为收集器提供基础实现
2. **类型支持**: 提供类型支持检查的默认实现

## 核心方法

### support(String type)

**作用**: 检查是否支持指定的收集器类型

**实现**:
```java
@Override
public boolean support(String type) {
    return this.type().equalsIgnoreCase(type);
}
```

**说明**: 通过比较 `type()` 方法返回的类型名称来判断是否支持

## 使用场景

### 1. 作为基类

子类继承 `AbstractCollector` 来实现收集器：

```java
public class MyCollector extends AbstractCollector {
    @Override
    public String type() {
        return "my";
    }
    
    @Override
    public void collect(ThreadPoolStats poolStats) {
        // 收集指标
    }
}
```

## 设计特点

### 1. 模板方法模式

提供 `support()` 方法的默认实现，子类只需实现 `type()` 和 `collect()` 方法。

### 2. 类型检查

通过类型名称进行支持检查，不区分大小写。

## 注意事项

1. **类型名称**: `type()` 方法返回的类型名称用于支持检查
2. **大小写**: 类型比较不区分大小写

