# AlarmCounter

## 概述

`AlarmCounter` 是告警计数器，用于统计告警次数和记录最后告警时间。它使用 Guava Cache 存储告警信息，支持过期自动清理。

## 核心作用

1. **告警计数**: 统计告警次数
2. **时间记录**: 记录最后告警时间
3. **过期清理**: 使用 Cache 的过期机制自动清理

## 核心属性

```java
private static final Map<String, Cache<String, AlarmInfo>> ALARM_INFO_CACHE;  // 告警信息缓存
private static final Map<String, String> LAST_ALARM_TIME_MAP;                 // 最后告警时间映射
```

## 核心方法

### initAlarmCounter(String threadPoolName, NotifyItem notifyItem)

**作用**: 初始化告警计数器

**实现**:
```java
public static void initAlarmCounter(String threadPoolName, NotifyItem notifyItem) {
    if (NotifyItemEnum.CHANGE.getValue().equalsIgnoreCase(notifyItem.getType())) {
        return;  // CHANGE 类型不需要计数
    }
    
    String key = buildKey(threadPoolName, notifyItem.getType());
    Cache<String, AlarmInfo> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(notifyItem.getPeriod(), TimeUnit.SECONDS)
            .build();
    ALARM_INFO_CACHE.put(key, cache);
}
```

**说明**: 
- CHANGE 类型不需要计数
- 使用 `notifyItem.getPeriod()` 作为过期时间

---

### getAlarmInfo(String threadPoolName, String notifyType)

**作用**: 获取告警信息

**实现**:
```java
public static AlarmInfo getAlarmInfo(String threadPoolName, String notifyType) {
    String key = buildKey(threadPoolName, notifyType);
    val cache = ALARM_INFO_CACHE.get(key);
    if (Objects.isNull(cache)) {
        throw new DtpException("Alarm info cache has not been initialized for " + key);
    }
    return cache.getIfPresent(notifyType);
}
```

**说明**: 如果缓存未初始化，抛出异常

---

### incAlarmCount(String threadPoolName, String notifyType)

**作用**: 增加告警计数

**实现**:
```java
public static void incAlarmCount(String threadPoolName, String notifyType) {
    AlarmInfo alarmInfo = getAlarmInfo(threadPoolName, notifyType);
    if (Objects.isNull(alarmInfo)) {
        String key = buildKey(threadPoolName, notifyType);
        alarmInfo = new AlarmInfo().setNotifyItem(NotifyItemEnum.of(notifyType));
        ALARM_INFO_CACHE.get(key).put(notifyType, alarmInfo);
    }
    alarmInfo.incCounter();
}
```

**流程**:
1. 获取告警信息
2. 如果不存在，创建新的告警信息
3. 增加计数

---

### reset(String threadPoolName, String notifyType)

**作用**: 重置告警计数

**实现**:
```java
public static void reset(String threadPoolName, String notifyType) {
    val alarmInfo = getAlarmInfo(threadPoolName, notifyType);
    if (Objects.nonNull(alarmInfo)) {
        alarmInfo.reset();
    }
    LAST_ALARM_TIME_MAP.put(buildKey(threadPoolName, notifyType), DateUtil.now());
}
```

**说明**: 重置计数并记录最后告警时间

---

### getLastAlarmTime(String threadPoolName, String notifyType)

**作用**: 获取最后告警时间

**返回**: 最后告警时间字符串

---

### buildKey(String threadPoolName, String notifyItemType)

**作用**: 构建缓存键

**实现**:
```java
private static String buildKey(String threadPoolName, String notifyItemType) {
    return threadPoolName + "#" + notifyItemType;
}
```

**键格式**: `{threadPoolName}#{notifyItemType}`

## 使用场景

### 1. 告警计数

在告警发送时增加计数：

```java
AlarmCounter.incAlarmCount(threadPoolName, notifyType);
```

### 2. 告警重置

在告警发送后重置计数：

```java
AlarmCounter.reset(threadPoolName, notifyType);
```

### 3. 获取告警信息

```java
AlarmInfo alarmInfo = AlarmCounter.getAlarmInfo(threadPoolName, notifyType);
int count = alarmInfo.getCount();
```

## 设计特点

### 1. Guava Cache

使用 Guava Cache 存储告警信息，支持过期自动清理。

### 2. 键映射

使用 `{threadPoolName}#{notifyItemType}` 作为键。

### 3. 线程安全

使用 `ConcurrentHashMap` 和 Guava Cache 保证线程安全。

## 注意事项

1. **初始化**: 需要先初始化告警计数器
2. **过期时间**: 使用 `notifyItem.getPeriod()` 作为过期时间
3. **内存占用**: Cache 会占用一定内存，但会自动过期清理

