# NotifyHelper

## 概述

`NotifyHelper` 是通知助手类，提供通知相关的辅助方法。它用于获取通知项、平台、告警键等。

## 核心作用

1. **通知项获取**: 获取执行器的通知项
2. **平台获取**: 获取通知平台
3. **告警键管理**: 管理告警键的映射关系
4. **通知初始化**: 初始化执行器的通知配置

## 核心属性

```java
private static final List<String> COMMON_ALARM_KEYS = Lists.newArrayList("alarmType", "alarmValue");
private static final Set<String> LIVENESS_ALARM_KEYS = Sets.newHashSet("corePoolSize", "maximumPoolSize", "poolSize", "activeCount");
private static final Set<String> CAPACITY_ALARM_KEYS = Sets.newHashSet("queueType", "queueCapacity", "queueSize", "queueRemaining");
private static final Set<String> REJECT_ALARM_KEYS = Sets.newHashSet("rejectType", "rejectCount");
private static final Set<String> RUN_TIMEOUT_ALARM_KEYS = Sets.newHashSet("runTimeoutCount");
private static final Set<String> QUEUE_TIMEOUT_ALARM_KEYS = Sets.newHashSet("queueTimeoutCount");
private static final Map<String, Set<String>> ALARM_KEYS;  // 告警键映射
```

## 核心方法

### getAllAlarmKeys()

**作用**: 获取所有告警键

**返回**: 所有告警键的集合

---

### getAlarmKeys(NotifyItemEnum notifyItemEnum)

**作用**: 获取指定告警类型的告警键

**返回**: 告警键集合

---

### getNotifyItem(ExecutorWrapper executor, NotifyItemEnum notifyType)

**作用**: 获取通知项

**返回**: `Optional<NotifyItem>`

**说明**: 从执行器包装器中获取指定类型的通知项

---

### getPlatform(String platformId)

**作用**: 获取通知平台

**返回**: `Optional<NotifyPlatform>`

**说明**: 根据平台 ID 获取通知平台配置

---

### updateNotifyInfo(ExecutorWrapper executorWrapper, TpExecutorProps props, List<NotifyPlatform> platforms)

**作用**: 更新通知信息

**说明**: 更新执行器的通知配置

---

### initNotify(DtpExecutor executor)

**作用**: 初始化通知

**说明**: 初始化执行器的通知配置

## 使用场景

### 1. 获取通知项

```java
Optional<NotifyItem> notifyItem = NotifyHelper.getNotifyItem(executorWrapper, CHANGE);
```

### 2. 获取平台

```java
Optional<NotifyPlatform> platform = NotifyHelper.getPlatform(platformId);
```

### 3. 获取告警键

```java
Set<String> alarmKeys = NotifyHelper.getAlarmKeys(NotifyItemEnum.REJECT);
```

## 设计特点

### 1. 静态方法

所有方法都是静态方法，便于使用。

### 2. 键映射管理

统一管理告警键的映射关系。

### 3. Optional 使用

使用 `Optional` 处理可能为空的值。

## 注意事项

1. **键映射**: 告警键映射在静态代码块中初始化
2. **空值处理**: 使用 `Optional` 处理可能为空的值
3. **线程安全**: 静态方法需要保证线程安全

