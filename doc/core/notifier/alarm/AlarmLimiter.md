# AlarmLimiter

## 概述

`AlarmLimiter` 是告警限流器，用于控制告警频率。它使用 Guava Cache 实现限流，在静默期内不发送告警。

## 核心作用

1. **告警限流**: 控制告警发送频率
2. **静默期**: 在静默期内不发送告警
3. **自动过期**: 使用 Cache 的过期机制自动清理

## 核心属性

```java
private static final Map<String, Cache<String, String>> ALARM_LIMITER;  // 告警限流器缓存
```

## 核心方法

### initAlarmLimiter(String threadPoolName, NotifyItem notifyItem)

**作用**: 初始化告警限流器

**实现**:
```java
public static void initAlarmLimiter(String threadPoolName, NotifyItem notifyItem) {
    if (NotifyItemEnum.CHANGE.getValue().equalsIgnoreCase(notifyItem.getType())) {
        return;  // CHANGE 类型不需要限流
    }
    
    String key = genKey(threadPoolName, notifyItem.getType());
    Cache<String, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(notifyItem.getSilencePeriod(), TimeUnit.SECONDS)
            .build();
    ALARM_LIMITER.put(key, cache);
}
```

**说明**: 
- CHANGE 类型不需要限流
- 使用 `notifyItem.getSilencePeriod()` 作为过期时间（静默期）

---

### putVal(String threadPoolName, String type)

**作用**: 放入限流值

**实现**:
```java
public static void putVal(String threadPoolName, String type) {
    String key = genKey(threadPoolName, type);
    ALARM_LIMITER.get(key).put(type, type);
}
```

**说明**: 发送告警后，将值放入 Cache，开始静默期

---

### isAllowed(String threadPoolName, String type)

**作用**: 检查是否允许发送告警

**实现**:
```java
public static boolean isAllowed(String threadPoolName, String type) {
    String key = genKey(threadPoolName, type);
    return StringUtils.isBlank(getAlarmLimitInfo(key, type));
}
```

**返回**: `true` 表示允许发送，`false` 表示在静默期内

**说明**: 如果 Cache 中存在值，说明在静默期内，不允许发送

---

### getAlarmLimitInfo(String key, String type)

**作用**: 获取限流信息

**实现**:
```java
public static String getAlarmLimitInfo(String key, String type) {
    val cache = ALARM_LIMITER.get(key);
    if (Objects.isNull(cache)) {
        return null;
    }
    return cache.getIfPresent(type);
}
```

---

### genKey(String threadPoolName, String type)

**作用**: 生成缓存键

**实现**:
```java
public static String genKey(String threadPoolName, String type) {
    return threadPoolName + "#" + type;
}
```

**键格式**: `{threadPoolName}#{type}`

## 使用场景

### 1. 告警限流

在发送告警前检查是否允许：

```java
if (AlarmLimiter.isAllowed(threadPoolName, notifyType)) {
    // 发送告警
    AlarmLimiter.putVal(threadPoolName, notifyType);  // 开始静默期
}
```

## 设计特点

### 1. Guava Cache

使用 Guava Cache 实现限流，支持过期自动清理。

### 2. 静默期

通过 Cache 的过期时间实现静默期。

### 3. 简单实现

实现简单，只存储类型字符串。

## 注意事项

1. **初始化**: 需要先初始化告警限流器
2. **静默期**: 使用 `notifyItem.getSilencePeriod()` 作为静默期
3. **内存占用**: Cache 会占用一定内存，但会自动过期清理

