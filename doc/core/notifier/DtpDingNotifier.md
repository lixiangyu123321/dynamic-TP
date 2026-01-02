# DtpDingNotifier

## 概述

`DtpDingNotifier` 是钉钉通知器，继承自 `AbstractDtpNotifier`。它用于向钉钉发送告警和通知消息。

## 核心作用

1. **钉钉通知**: 向钉钉发送配置变更通知
2. **钉钉告警**: 向钉钉发送告警消息
3. **消息格式化**: 使用钉钉特定的消息模板和颜色

## 核心方法

### platform()

**作用**: 获取平台名称

**返回**: "ding"

---

### getNoticeTemplate()

**作用**: 获取通知模板

**返回**: 钉钉通知模板（`DingNotifyConst.DING_CHANGE_NOTICE_TEMPLATE`）

---

### getAlarmTemplate()

**作用**: 获取告警模板

**返回**: 钉钉告警模板（`DingNotifyConst.DING_ALARM_TEMPLATE`）

---

### getColors()

**作用**: 获取颜色配置

**返回**: 钉钉消息的颜色配置（高亮颜色、普通颜色）

## 使用场景

### 1. 配置钉钉通知

```yaml
spring:
  dynamic:
    tp:
      platforms:
        - platform: ding
          url: https://oapi.dingtalk.com/robot/send?access_token=xxx
          secret: xxx
      executors:
        - threadPoolName: dtpExecutor1
          notify-items:
            - type: reject
              enabled: true
              platform-ids: [ding]
```

## 设计特点

### 1. 继承抽象类

继承 `AbstractDtpNotifier`，复用消息构建逻辑。

### 2. 模板配置

使用钉钉特定的消息模板。

### 3. 颜色支持

支持消息内容高亮。

## 注意事项

1. **模板格式**: 使用钉钉支持的消息格式
2. **颜色格式**: 使用钉钉支持的颜色格式
3. **接收人**: 支持 @ 接收人

