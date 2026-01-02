# AbstractDtpNotifier

## 概述

`AbstractDtpNotifier` 是抽象通知器基类，实现了 `DtpNotifier` 接口。它提供了通知和告警消息构建的通用逻辑，子类只需实现模板和颜色配置。

## 核心作用

1. **消息构建**: 构建通知和告警消息内容
2. **内容高亮**: 高亮显示关键信息
3. **模板抽象**: 抽象消息模板，由子类实现

## 核心属性

```java
protected Notifier notifier;  // 底层通知器
```

## 核心方法

### sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs)

**作用**: 发送配置变更通知

**实现**:
```java
@Override
public void sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs) {
    String content = buildNoticeContent(notifyPlatform, oldFields, diffs);
    if (StringUtils.isBlank(content)) {
        log.debug("Notice content is empty, ignore send notice message.");
        return;
    }
    notifier.send(newTargetPlatform(notifyPlatform), content);
}
```

**流程**:
1. 构建通知内容
2. 检查内容是否为空
3. 发送通知

---

### sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum)

**作用**: 发送告警消息

**实现**:
```java
@Override
public void sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum) {
    String content = buildAlarmContent(notifyPlatform, notifyItemEnum);
    if (StringUtils.isBlank(content)) {
        log.debug("Alarm content is empty, ignore send alarm message.");
        return;
    }
    notifier.send(newTargetPlatform(notifyPlatform), content);
}
```

---

### buildNoticeContent(NotifyPlatform platform, TpMainFields oldFields, List<String> diffs)

**作用**: 构建通知内容

**实现**:
```java
protected String buildNoticeContent(NotifyPlatform platform, TpMainFields oldFields, List<String> diffs) {
    BaseNotifyCtx context = DtpNotifyCtxHolder.get();
    ExecutorWrapper executorWrapper = context.getExecutorWrapper();
    val executor = executorWrapper.getExecutor();
    
    String content = String.format(
            getNoticeTemplate(),
            // 服务信息
            CommonUtil.getInstance().getServiceName(),
            CommonUtil.getInstance().getIp() + ":" + CommonUtil.getInstance().getPort(),
            CommonUtil.getInstance().getEnv(),
            // 线程池信息
            populatePoolName(executorWrapper),
            // 配置变更信息
            oldFields.getCorePoolSize(), executor.getCorePoolSize(),
            oldFields.getMaxPoolSize(), executor.getMaximumPoolSize(),
            // ... 其他信息
    );
    return highlightNotifyContent(content, diffs);
}
```

**内容包含**:
- 服务信息（名称、IP、端口、环境）
- 线程池信息（名称、别名）
- 配置变更信息（旧值、新值）
- 接收人信息
- 时间信息

---

### buildAlarmContent(NotifyPlatform platform, NotifyItemEnum notifyItemEnum)

**作用**: 构建告警内容

**实现**:
```java
protected String buildAlarmContent(NotifyPlatform platform, NotifyItemEnum notifyItemEnum) {
    AlarmCtx context = (AlarmCtx) DtpNotifyCtxHolder.get();
    ExecutorWrapper executorWrapper = context.getExecutorWrapper();
    NotifyItem notifyItem = context.getNotifyItem();
    
    String content = String.format(
            getAlarmTemplate(),
            // 服务信息
            CommonUtil.getInstance().getServiceName(),
            // ... 线程池详细信息
            // ... 告警信息
            // ... 系统指标
    );
    return highlightAlarmContent(content, notifyItemEnum);
}
```

**内容包含**:
- 服务信息
- 线程池详细信息
- 告警类型和值
- 系统指标
- 接收人信息
- 时间信息

---

### getNoticeTemplate() / getAlarmTemplate() / getColors()

**作用**: 获取模板和颜色配置（抽象方法）

**说明**: 由子类实现，提供平台特定的模板和颜色配置

---

### highlightNotifyContent(String content, List<String> diffs)

**作用**: 高亮通知内容

**说明**: 高亮显示变化的配置项

---

### highlightAlarmContent(String content, NotifyItemEnum notifyItemEnum)

**作用**: 高亮告警内容

**说明**: 高亮显示告警相关的关键信息

## 使用场景

### 1. 作为基类

子类继承 `AbstractDtpNotifier` 实现通知器：

```java
public class CustomNotifier extends AbstractDtpNotifier {
    public CustomNotifier(Notifier notifier) {
        super(notifier);
    }
    
    @Override
    protected String getNoticeTemplate() {
        return "配置变更通知模板";
    }
    
    @Override
    protected String getAlarmTemplate() {
        return "告警消息模板";
    }
    
    @Override
    protected Pair<String, String> getColors() {
        return Pair.of("高亮颜色", "普通颜色");
    }
}
```

## 设计特点

### 1. 模板方法模式

定义消息构建的骨架，子类实现模板。

### 2. 内容高亮

支持内容高亮，突出关键信息。

### 3. 上下文使用

使用 `DtpNotifyCtxHolder` 获取上下文信息。

## 注意事项

1. **模板格式**: 使用 `String.format()` 格式化模板
2. **颜色配置**: 不同平台的颜色格式可能不同
3. **上下文**: 需要确保上下文已设置

