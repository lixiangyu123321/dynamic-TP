# Notifier 模块文档

`core/notifier` 目录包含了框架的通知器相关功能，包括告警、通知、过滤器、上下文等。

## 目录结构

```
notifier/
├── DtpNotifier.java                    # 通知器接口
├── AbstractDtpNotifier.java           # 抽象通知器
├── DtpDingNotifier.java               # 钉钉通知器
├── DtpWechatNotifier.java             # 企微通知器
├── DtpLarkNotifier.java               # 飞书通知器
├── manager/                           # 管理器
│   ├── AlarmManager.java              # 告警管理器
│   ├── NoticeManager.java             # 通知管理器
│   ├── NotifyHelper.java              # 通知助手
│   └── NotifyFilterBuilder.java       # 通知过滤器构建器
├── context/                           # 上下文
│   ├── BaseNotifyCtx.java             # 基础通知上下文
│   ├── AlarmCtx.java                  # 告警上下文
│   ├── NoticeCtx.java                 # 通知上下文
│   └── DtpNotifyCtxHolder.java        # 通知上下文持有者
├── alarm/                              # 告警
│   ├── AlarmCounter.java              # 告警计数器
│   └── AlarmLimiter.java              # 告警限流器
├── chain/                              # 责任链
│   ├── filter/                         # 过滤器
│   │   ├── NotifyFilter.java          # 通知过滤器接口
│   │   ├── BaseAlarmFilter.java        # 基础告警过滤器
│   │   ├── BaseNoticeFilter.java      # 基础通知过滤器
│   │   └── SilentCheckFilter.java     # 静默检查过滤器
│   └── invoker/                        # 调用器
│       ├── AlarmInvoker.java          # 告警调用器
│       └── NoticeInvoker.java         # 通知调用器
└── capture/                            # 捕获
    ├── CapturedExecutor.java          # 捕获的执行器
    └── CapturedBlockingQueue.java     # 捕获的阻塞队列
```

## 模块说明

### 核心接口

- **[DtpNotifier](DtpNotifier.md)**: 通知器接口
- **[AbstractDtpNotifier](AbstractDtpNotifier.md)**: 抽象通知器基类

### 实现类

- **[DtpDingNotifier](DtpDingNotifier.md)**: 钉钉通知器
- **[DtpWechatNotifier](DtpWechatNotifier.md)**: 企微通知器
- **[DtpLarkNotifier](DtpLarkNotifier.md)**: 飞书通知器

### 管理器

- **[AlarmManager](manager/AlarmManager.md)**: 告警管理器
- **[NoticeManager](manager/NoticeManager.md)**: 通知管理器
- **[NotifyHelper](manager/NotifyHelper.md)**: 通知助手
- **[NotifyFilterBuilder](manager/NotifyFilterBuilder.md)**: 通知过滤器构建器

### 上下文

- **[BaseNotifyCtx](context/BaseNotifyCtx.md)**: 基础通知上下文
- **[AlarmCtx](context/AlarmCtx.md)**: 告警上下文
- **[NoticeCtx](context/NoticeCtx.md)**: 通知上下文
- **[DtpNotifyCtxHolder](context/DtpNotifyCtxHolder.md)**: 通知上下文持有者

### 告警

- **[AlarmCounter](alarm/AlarmCounter.md)**: 告警计数器
- **[AlarmLimiter](alarm/AlarmLimiter.md)**: 告警限流器

### 责任链

- **[NotifyFilter](chain/filter/NotifyFilter.md)**: 通知过滤器接口
- **[AlarmInvoker](chain/invoker/AlarmInvoker.md)**: 告警调用器
- **[NoticeInvoker](chain/invoker/NoticeInvoker.md)**: 通知调用器

## 快速导航

- 想了解通知器？查看 [DtpNotifier](DtpNotifier.md)
- 想了解告警管理？查看 [AlarmManager](manager/AlarmManager.md)
- 想了解通知管理？查看 [NoticeManager](manager/NoticeManager.md)

