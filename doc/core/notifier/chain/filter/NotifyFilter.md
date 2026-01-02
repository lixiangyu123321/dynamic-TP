# NotifyFilter

## 概述

`NotifyFilter` 是通知过滤器接口，继承自 `Filter<BaseNotifyCtx>`。它用于在责任链中过滤和处理通知/告警。

## 核心作用

1. **过滤器定义**: 定义通知过滤器的标准接口
2. **类型支持**: 支持不同类型的通知（告警、通知）
3. **责任链处理**: 在责任链中处理通知

## 接口定义

```java
public interface NotifyFilter extends Filter<BaseNotifyCtx> {
    default boolean supports(NotifyTypeEnum notifyType) {
        return true;
    }
}
```

## 核心方法

### supports(NotifyTypeEnum notifyType)

**作用**: 检查是否支持指定的通知类型

**参数**: `notifyType` - 通知类型（ALARM 或 COMMON）

**返回**: `true` 表示支持，`false` 表示不支持

**默认实现**: 返回 `true`，表示支持所有类型

---

### doFilter(BaseNotifyCtx context, InvokerChain<BaseNotifyCtx> chain)

**作用**: 执行过滤（继承自 `Filter`）

**说明**: 在责任链中处理通知，可以继续传递或中断

## 内置实现

框架提供了以下内置实现：

1. **BaseAlarmFilter**: 基础告警过滤器
2. **BaseNoticeFilter**: 基础通知过滤器
3. **SilentCheckFilter**: 静默检查过滤器

## 使用场景

### 1. 实现自定义过滤器

```java
@Component
public class CustomFilter implements NotifyFilter {
    @Override
    public boolean supports(NotifyTypeEnum notifyType) {
        return notifyType == NotifyTypeEnum.ALARM;
    }
    
    @Override
    public void doFilter(BaseNotifyCtx context, InvokerChain<BaseNotifyCtx> chain) {
        // 过滤逻辑
        if (shouldFilter(context)) {
            return;  // 中断责任链
        }
        chain.proceed(context);  // 继续传递
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
}
```

## 设计特点

### 1. 责任链模式

使用责任链模式处理通知，支持过滤器链。

### 2. 类型支持

通过 `supports()` 方法支持不同类型的通知。

### 3. 顺序控制

通过 `getOrder()` 控制过滤器执行顺序。

## 注意事项

1. **类型支持**: 需要实现 `supports()` 方法
2. **顺序控制**: 通过 `getOrder()` 控制执行顺序
3. **责任链**: 需要调用 `chain.proceed()` 继续传递

