# DtpWechatNotifier

## 概述

`DtpWechatNotifier` 是企业微信通知器，继承自 `AbstractDtpNotifier`。它用于向企业微信发送告警和通知消息。

## 核心作用

1. **企微通知**: 向企业微信发送配置变更通知
2. **企微告警**: 向企业微信发送告警消息
3. **消息格式化**: 使用企业微信特定的消息模板和颜色

## 核心方法

### platform()

**作用**: 获取平台名称

**返回**: "wechat"

---

### getNoticeTemplate()

**作用**: 获取通知模板

**返回**: 企业微信通知模板（`WechatNotifyConst.WECHAT_CHANGE_NOTICE_TEMPLATE`）

---

### getAlarmTemplate()

**作用**: 获取告警模板

**返回**: 企业微信告警模板（`WechatNotifyConst.WECHAT_ALARM_TEMPLATE`）

---

### getColors()

**作用**: 获取颜色配置

**返回**: 企业微信消息的颜色配置（高亮颜色、普通颜色）

## 使用场景

### 1. 配置企业微信通知

```yaml
spring:
  dynamic:
    tp:
      platforms:
        - platform: wechat
          url: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
      executors:
        - threadPoolName: dtpExecutor1
          notify-items:
            - type: reject
              enabled: true
              platform-ids: [wechat]
```

## 设计特点

### 1. 继承抽象类

继承 `AbstractDtpNotifier`，复用消息构建逻辑。

### 2. 模板配置

使用企业微信特定的消息模板。

### 3. 颜色支持

支持消息内容高亮。

## 注意事项

1. **模板格式**: 使用企业微信支持的消息格式
2. **颜色格式**: 使用企业微信支持的颜色格式
3. **接收人**: 支持 @ 接收人

