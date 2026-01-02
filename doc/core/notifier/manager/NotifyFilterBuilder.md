# NotifyFilterBuilder

## 概述

`NotifyFilterBuilder` 是通知过滤器构建器，负责构建告警和通知的责任链。它收集所有过滤器，按顺序构建责任链。

## 核心作用

1. **责任链构建**: 构建告警和通知的责任链
2. **过滤器收集**: 收集所有注册的过滤器
3. **顺序排序**: 按顺序对过滤器进行排序

## 核心方法

### getAlarmInvokerChain()

**作用**: 获取告警责任链

**实现**:
```java
public static InvokerChain<BaseNotifyCtx> getAlarmInvokerChain() {
    val filters = ContextManagerHelper.getBeansOfType(NotifyFilter.class);
    Collection<NotifyFilter> alarmFilters = Lists.newArrayList(filters.values());
    alarmFilters.add(new BaseAlarmFilter());
    alarmFilters.add(new SilentCheckFilter());
    alarmFilters = alarmFilters.stream()
            .filter(x -> x.supports(NotifyTypeEnum.ALARM))
            .sorted(Comparator.comparing(Filter::getOrder))
            .collect(Collectors.toList());
    return InvokerChainFactory.buildInvokerChain(new AlarmInvoker(), alarmFilters.toArray(new NotifyFilter[0]));
}
```

**流程**:
1. 从 Spring 容器获取所有 `NotifyFilter` Bean
2. 添加内置过滤器（`BaseAlarmFilter`、`SilentCheckFilter`）
3. 过滤出支持告警类型的过滤器
4. 按顺序排序
5. 构建责任链

---

### getCommonInvokerChain()

**作用**: 获取通知责任链

**实现**:
```java
public static InvokerChain<BaseNotifyCtx> getCommonInvokerChain() {
    val filters = ContextManagerHelper.getBeansOfType(NotifyFilter.class);
    Collection<NotifyFilter> noticeFilters = Lists.newArrayList(filters.values());
    noticeFilters.add(new BaseNoticeFilter());
    noticeFilters = noticeFilters.stream()
            .filter(x -> x.supports(NotifyTypeEnum.COMMON))
            .sorted(Comparator.comparing(Filter::getOrder))
            .collect(Collectors.toList());
    return InvokerChainFactory.buildInvokerChain(new NoticeInvoker(), noticeFilters.toArray(new NotifyFilter[0]));
}
```

**流程**:
1. 从 Spring 容器获取所有 `NotifyFilter` Bean
2. 添加内置过滤器（`BaseNoticeFilter`）
3. 过滤出支持通知类型的过滤器
4. 按顺序排序
5. 构建责任链

## 内置过滤器

### 告警过滤器

- **BaseAlarmFilter**: 基础告警过滤器
- **SilentCheckFilter**: 静默检查过滤器

### 通知过滤器

- **BaseNoticeFilter**: 基础通知过滤器

## 使用场景

### 1. 扩展过滤器

通过 Spring Bean 注册自定义过滤器：

```java
@Component
public class CustomFilter implements NotifyFilter {
    @Override
    public boolean supports(NotifyTypeEnum type) {
        return type == NotifyTypeEnum.ALARM;
    }
    
    @Override
    public void doFilter(BaseNotifyCtx context, InvokerChain<BaseNotifyCtx> chain) {
        // 过滤逻辑
        chain.proceed(context);
    }
}
```

## 设计特点

### 1. 责任链模式

使用责任链模式处理告警和通知。

### 2. 过滤器收集

从 Spring 容器收集过滤器，支持扩展。

### 3. 顺序控制

通过 `getOrder()` 控制过滤器执行顺序。

## 注意事项

1. **过滤器顺序**: 通过 `getOrder()` 控制执行顺序
2. **类型支持**: 过滤器需要实现 `supports()` 方法
3. **Spring 集成**: 需要 Spring 容器支持

