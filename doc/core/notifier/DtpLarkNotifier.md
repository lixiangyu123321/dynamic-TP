# DtpLarkNotifier

## 概述

`DtpLarkNotifier` 是飞书通知器，继承自 `AbstractDtpNotifier`。它用于向飞书发送告警和通知消息。

## 核心作用

1. **飞书通知**: 向飞书发送配置变更通知
2. **飞书告警**: 向飞书发送告警消息
3. **消息格式化**: 使用飞书特定的消息模板和颜色

## 核心方法

### platform()

**作用**: 获取平台名称

**返回**: "lark"

---

### getNoticeTemplate()

**作用**: 获取通知模板

**返回**: 飞书通知模板（`LarkNotifyConst.LARK_CHANGE_NOTICE_TEMPLATE`）

---

### getAlarmTemplate()

**作用**: 获取告警模板

**返回**: 飞书告警模板（`LarkNotifyConst.LARK_ALARM_TEMPLATE`）

---

### getColors()

**作用**: 获取颜色配置

**返回**: 飞书消息的颜色配置（高亮颜色、普通颜色）

---

### formatReceivers(String receives)

**作用**: 格式化接收人

**实现**:
```java
@Override
protected String formatReceivers(String receives) {
    String[] receivers = StringUtils.split(receives, ',');
    return Arrays.stream(receivers)
            .map(receiver -> {
                if (receiver.startsWith(LARK_OPENID_PREFIX)) {
                    return String.format(LARK_AT_FORMAT_OPENID, receiver);
                } else {
                    return String.format(LARK_AT_FORMAT_USERNAME, receiver);
                }
            })
            .collect(Collectors.joining(" "));
}
```

**说明**: 
- 如果接收人以 `ou_` 开头，使用 OpenID 格式
- 否则使用用户名格式

## 使用场景

### 1. 配置飞书通知

```yaml
spring:
  dynamic:
    tp:
      platforms:
        - platform: lark
          url: https://open.feishu.cn/open-apis/bot/v2/hook/xxx
      executors:
        - threadPoolName: dtpExecutor1
          notify-items:
            - type: reject
              enabled: true
              platform-ids: [lark]
```

## 设计特点

### 1. 继承抽象类

继承 `AbstractDtpNotifier`，复用消息构建逻辑。

### 2. 模板配置

使用飞书特定的消息模板。

### 3. 接收人格式

支持 OpenID 和用户名两种格式。

## 注意事项

1. **模板格式**: 使用飞书支持的消息格式
2. **接收人格式**: 支持 OpenID 和用户名格式
3. **颜色格式**: 使用飞书支持的颜色格式

