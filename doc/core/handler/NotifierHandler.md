# NotifierHandler

## 概述

`NotifierHandler` 是通知器处理器，负责管理通知器。它统一管理所有通知器，并提供发送通知和告警的功能。

## 核心作用

1. **通知器管理**: 统一管理所有通知器
2. **通知发送**: 发送配置变更通知
3. **告警发送**: 发送告警通知
4. **SPI 扩展**: 支持通过 SPI 扩展通知器

## 核心属性

```java
private static final Map<String, DtpNotifier> NOTIFIERS = new HashMap<>();  // 通知器映射
```

## 初始化

在构造方法中初始化通知器：

```java
private NotifierHandler() {
    // 1. 通过 SPI 加载通知器
    List<DtpNotifier> loadedNotifiers = ExtensionServiceLoader.get(DtpNotifier.class);
    loadedNotifiers.forEach(notifier -> NOTIFIERS.put(notifier.platform().toLowerCase(), notifier));
    
    // 2. 添加内置通知器
    DtpNotifier dingNotifier = new DtpDingNotifier(new DingNotifier());
    DtpNotifier wechatNotifier = new DtpWechatNotifier(new WechatNotifier());
    DtpNotifier larkNotifier = new DtpLarkNotifier(new LarkNotifier());
    NOTIFIERS.put(dingNotifier.platform(), dingNotifier);
    NOTIFIERS.put(wechatNotifier.platform(), wechatNotifier);
    NOTIFIERS.put(larkNotifier.platform(), larkNotifier);
}
```

**内置通知器**:
- `DtpDingNotifier`: 钉钉通知器
- `DtpWechatNotifier`: 企微通知器
- `DtpLarkNotifier`: 飞书通知器

## 核心方法

### sendNotice(TpMainFields oldFields, List<String> diffs)

**作用**: 发送配置变更通知

**实现**:
```java
public void sendNotice(TpMainFields oldFields, List<String> diffs) {
    NotifyItem notifyItem = DtpNotifyCtxHolder.get().getNotifyItem();
    for (String platformId : notifyItem.getPlatformIds()) {
        NotifyHelper.getPlatform(platformId).ifPresent(p -> {
            DtpNotifier notifier = NOTIFIERS.get(p.getPlatform().toLowerCase());
            if (notifier != null) {
                notifier.sendChangeMsg(p, oldFields, diffs);
            }
        });
    }
}
```

**流程**:
1. 获取通知项
2. 遍历平台 ID
3. 查找对应的通知器
4. 发送配置变更消息

---

### sendAlarm(NotifyItemEnum notifyItemEnum)

**作用**: 发送告警通知

**实现**:
```java
public void sendAlarm(NotifyItemEnum notifyItemEnum) {
    NotifyItem notifyItem = DtpNotifyCtxHolder.get().getNotifyItem();
    for (String platformId : notifyItem.getPlatformIds()) {
        NotifyHelper.getPlatform(platformId).ifPresent(p -> {
            DtpNotifier notifier = NOTIFIERS.get(p.getPlatform().toLowerCase());
            if (notifier != null) {
                notifier.sendAlarmMsg(p, notifyItemEnum);
            }
        });
    }
}
```

**流程**:
1. 获取通知项
2. 遍历平台 ID
3. 查找对应的通知器
4. 发送告警消息

---

### getInstance()

**作用**: 获取单例实例

**实现**:
```java
public static NotifierHandler getInstance() {
    return NotifierHandlerHolder.INSTANCE;
}

private static class NotifierHandlerHolder {
    private static final NotifierHandler INSTANCE = new NotifierHandler();
}
```

## 使用场景

### 1. 发送配置变更通知

```java
NotifierHandler handler = NotifierHandler.getInstance();
TpMainFields oldFields = ...;
List<String> diffs = Arrays.asList("corePoolSize", "maxPoolSize");
handler.sendNotice(oldFields, diffs);
```

### 2. 发送告警通知

```java
NotifierHandler handler = NotifierHandler.getInstance();
handler.sendAlarm(NotifyItemEnum.REJECT);
```

### 3. 扩展通知器

通过 SPI 扩展自定义通知器：

```java
// 实现 DtpNotifier 接口
public class CustomNotifier extends AbstractDtpNotifier {
    @Override
    public String platform() {
        return "custom";
    }
    
    @Override
    public void sendChangeMsg(Platform platform, TpMainFields oldFields, List<String> diffs) {
        // 发送配置变更消息
    }
    
    @Override
    public void sendAlarmMsg(Platform platform, NotifyItemEnum notifyItemEnum) {
        // 发送告警消息
    }
}

// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.notifier.DtpNotifier
// com.example.CustomNotifier
```

## 设计特点

### 1. 单例模式

使用静态内部类实现线程安全的单例。

### 2. SPI 扩展

支持通过 SPI 机制扩展通知器。

### 3. 多平台支持

支持同时向多个平台发送通知。

## 注意事项

1. **平台名称**: 平台名称不区分大小写
2. **上下文**: 通知项从 `DtpNotifyCtxHolder` 获取
3. **空值处理**: 如果找不到通知器，会跳过

