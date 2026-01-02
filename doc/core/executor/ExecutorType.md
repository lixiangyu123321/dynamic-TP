# ExecutorType

## 概述

`ExecutorType` 是执行器类型枚举，定义了框架支持的所有执行器类型及其对应的类。

## 核心作用

1. **类型定义**: 定义执行器类型
2. **类型映射**: 映射类型名称到对应的类
3. **类型查询**: 根据名称获取对应的类

## 枚举定义

```java
@Getter
@AllArgsConstructor
public enum ExecutorType {
    COMMON("common", DtpExecutor.class),
    EAGER("eager", EagerDtpExecutor.class),
    SCHEDULED("scheduled", ScheduledDtpExecutor.class),
    ORDERED("ordered", OrderedDtpExecutor.class),
    PRIORITY("priority", PriorityDtpExecutor.class);
    
    private final String name;
    private final Class<?> clazz;
}
```

## 枚举值

### COMMON

- **名称**: "common"
- **类**: `DtpExecutor`
- **说明**: 普通动态线程池

---

### EAGER

- **名称**: "eager"
- **类**: `EagerDtpExecutor`
- **说明**: IO 密集型线程池

---

### SCHEDULED

- **名称**: "scheduled"
- **类**: `ScheduledDtpExecutor`
- **说明**: 调度线程池

---

### ORDERED

- **名称**: "ordered"
- **类**: `OrderedDtpExecutor`
- **说明**: 有序线程池

---

### PRIORITY

- **名称**: "priority"
- **类**: `PriorityDtpExecutor`
- **说明**: 优先级线程池

## 核心方法

### getClass(String name)

**作用**: 根据名称获取对应的类

**实现**:
```java
public static Class<?> getClass(String name) {
    for (ExecutorType type : ExecutorType.values()) {
        if (type.name.equals(name)) {
            return type.getClazz();
        }
    }
    return COMMON.getClazz();  // 默认返回 COMMON
}
```

**说明**: 如果找不到对应的类型，返回 `COMMON` 类型

## 使用场景

### 1. 配置解析

在配置解析时，根据类型名称创建对应的执行器：

```java
String executorType = props.getExecutorType();
Class<?> executorClass = ExecutorType.getClass(executorType);
// 使用反射创建执行器实例
```

### 2. 类型判断

判断执行器类型：

```java
if (executor instanceof DtpExecutor) {
    // 普通执行器
} else if (executor instanceof EagerDtpExecutor) {
    // IO 密集型执行器
}
```

## 注意事项

1. **默认类型**: 如果类型名称不匹配，默认返回 `COMMON`
2. **类型名称**: 类型名称区分大小写
3. **扩展性**: 可以通过配置扩展新的执行器类型

