# AwareTypeEnum

## 概述

`AwareTypeEnum` 是感知器类型枚举，定义了框架内置的感知器类型及其顺序和名称。

## 核心作用

1. **类型定义**: 定义内置感知器的类型
2. **顺序管理**: 定义感知器的执行顺序
3. **名称管理**: 定义感知器的名称

## 枚举定义

```java
@Getter
@AllArgsConstructor
public enum AwareTypeEnum {
    PERFORMANCE_MONITOR_AWARE(1, "monitor"),
    TASK_TIMEOUT_AWARE(2, "timeout"),
    TASK_REJECT_AWARE(3, "reject");
    
    private final int order;
    private final String name;
}
```

## 枚举值

### PERFORMANCE_MONITOR_AWARE

- **顺序**: 1
- **名称**: "monitor"
- **对应类**: `PerformanceMonitorAware`
- **作用**: 性能监控感知器

---

### TASK_TIMEOUT_AWARE

- **顺序**: 2
- **名称**: "timeout"
- **对应类**: `TaskTimeoutAware`
- **作用**: 任务超时感知器

---

### TASK_REJECT_AWARE

- **顺序**: 3
- **名称**: "reject"
- **对应类**: `TaskRejectAware`
- **作用**: 任务拒绝感知器

## 使用场景

### 1. 在感知器实现中使用

```java
public class PerformanceMonitorAware extends TaskStatAware {
    @Override
    public int getOrder() {
        return AwareTypeEnum.PERFORMANCE_MONITOR_AWARE.getOrder();
    }
    
    @Override
    public String getName() {
        return AwareTypeEnum.PERFORMANCE_MONITOR_AWARE.getName();
    }
}
```

### 2. 配置感知器

在配置中通过名称指定感知器：

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          aware-names: [monitor, timeout, reject]
```

## 设计特点

### 1. 顺序定义

通过 `order` 字段定义执行顺序，数值越小越先执行。

### 2. 名称定义

通过 `name` 字段定义感知器名称，用于配置和识别。

### 3. 类型安全

使用枚举保证类型安全，避免硬编码字符串。

## 注意事项

1. **顺序值**: 顺序值越小，执行越早
2. **名称唯一性**: 感知器名称应该唯一
3. **扩展感知器**: 扩展感知器应该使用不同的顺序值和名称

