# DtpNotifier

## 概述

`DtpNotifier` 是通知器接口，定义了通知器的标准方法。它用于向不同的平台（钉钉、企微、飞书等）发送告警和通知消息。

## 核心作用

1. **通知发送**: 发送配置变更通知
2. **告警发送**: 发送告警消息
3. **平台抽象**: 抽象不同通知平台的差异

## 接口定义

```java
public interface DtpNotifier {
    String platform();  // 获取平台名称
    void sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs);  // 发送配置变更消息
    void sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum);  // 发送告警消息
}
```

## 核心方法

### platform()

**作用**: 获取平台名称

**返回**: 平台名称（如 "ding"、"wechat"、"lark" 等）

**说明**: 用于识别和选择通知器

---

### sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs)

**作用**: 发送配置变更通知

**参数**:
- `notifyPlatform`: 通知平台配置
- `oldFields`: 旧的配置字段
- `diffs`: 变化的配置键列表

**说明**: 当线程池配置发生变化时，发送通知消息

---

### sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum)

**作用**: 发送告警消息

**参数**:
- `notifyPlatform`: 通知平台配置
- `notifyItemEnum`: 告警项枚举（如 REJECT、CAPACITY、LIVENESS 等）

**说明**: 当线程池出现告警条件时，发送告警消息

## 内置实现

框架提供了以下内置实现：

1. **DtpDingNotifier**: 钉钉通知器
2. **DtpWechatNotifier**: 企微通知器
3. **DtpLarkNotifier**: 飞书通知器

## 使用场景

### 1. 实现自定义通知器

```java
public class CustomNotifier extends AbstractDtpNotifier {
    @Override
    public String platform() {
        return "custom";
    }
    
    @Override
    public void sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs) {
        // 发送配置变更消息
    }
    
    @Override
    public void sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum) {
        // 发送告警消息
    }
}
```

### 2. 通过 SPI 注册

```java
// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.notifier.DtpNotifier
// com.example.CustomNotifier
```

## 设计特点

### 1. 平台抽象

通过接口抽象不同通知平台的差异。

### 2. SPI 扩展

支持通过 SPI 机制扩展通知器。

### 3. 统一接口

提供统一的接口，便于管理和使用。

## 注意事项

1. **平台名称**: 平台名称应该唯一
2. **消息格式**: 不同平台的消息格式可能不同
3. **异常处理**: 通知发送中的异常需要自行处理

