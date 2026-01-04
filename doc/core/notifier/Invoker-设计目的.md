# Invoker 设计目的

## 概述

`Invoker` 是 DynamicTp 框架中责任链模式的核心组件，用于实现通知和告警的过滤链处理机制。它作为责任链的终点，负责实际执行通知或告警的发送操作。

## 设计目的

### 1. 解耦过滤逻辑和执行逻辑

**问题**: 如果不使用 Invoker，过滤逻辑和执行逻辑会耦合在一起，难以维护和扩展。

**解决方案**: 通过 Invoker 将执行逻辑从过滤链中分离出来：

```
Filter1 → Filter2 → Filter3 → Invoker (实际执行)
  ↓         ↓         ↓
过滤逻辑  过滤逻辑  过滤逻辑   执行逻辑
```

**优势**:
- 过滤器只负责过滤，不关心如何执行
- Invoker 只负责执行，不关心过滤逻辑
- 职责清晰，易于理解和维护

### 2. 实现责任链模式的终点

在责任链模式中，需要一个明确的终点来执行实际操作：

```java
// 责任链结构
Filter1 → Filter2 → Filter3 → Invoker
  ↓         ↓         ↓         ↓
过滤     过滤     过滤     执行
```

**Invoker 的作用**:
- 作为责任链的最后一个节点
- 所有过滤器通过后，由 Invoker 执行实际操作
- 提供统一的执行入口

### 3. 支持灵活的过滤器组合

通过 Invoker 和 Filter 的组合，可以灵活构建不同的责任链：

```java
// 告警责任链
AlarmFilter1 → AlarmFilter2 → AlarmInvoker

// 通知责任链
NoticeFilter1 → NoticeFilter2 → NoticeInvoker
```

**优势**:
- 可以为不同类型的通知构建不同的责任链
- 可以动态添加或移除过滤器
- 可以控制过滤器的执行顺序

### 4. 统一上下文管理

Invoker 负责统一管理通知上下文：

```java
public void invoke(BaseNotifyCtx context) {
    try {
        DtpNotifyCtxHolder.set(context);  // 设置上下文
        // 执行实际操作
    } finally {
        DtpNotifyCtxHolder.remove();  // 清理上下文
    }
}
```

**优势**:
- 确保上下文正确设置和清理
- 避免上下文泄漏
- 异常安全（使用 try-finally）

## 核心组件

### 1. Invoker 接口

```java
public interface Invoker<T> {
    void invoke(T context);
}
```

**作用**: 定义统一的调用接口，所有执行器都实现此接口。

### 2. InvokerChain

```java
public class InvokerChain<T> {
    private Invoker<T> head;
    
    public void proceed(T context) {
        head.invoke(context);
    }
}
```

**作用**: 责任链容器，持有责任链的头部 Invoker。

### 3. InvokerChainFactory

```java
public static<T> InvokerChain<T> buildInvokerChain(
    Invoker<T> target, 
    Filter<T>... filters
) {
    InvokerChain<T> invokerChain = new InvokerChain<>();
    Invoker<T> last = target;  // 从终点开始
    // 从后往前构建责任链
    for (int i = filters.length - 1; i >= 0; i--) {
        Invoker<T> next = last;
        Filter<T> filter = filters[i];
        last = context -> filter.doFilter(context, next);
    }
    invokerChain.setHead(last);
    return invokerChain;
}
```

**作用**: 工厂类，负责将 Filter 和 Invoker 组合成责任链。

**构建过程**:
1. 从 Invoker（终点）开始
2. 从后往前遍历 Filter 数组
3. 每个 Filter 包装下一个 Invoker
4. 最终形成：Filter1 → Filter2 → ... → Invoker

### 4. 具体实现

#### AlarmInvoker

```java
public class AlarmInvoker implements Invoker<BaseNotifyCtx> {
    @Override
    public void invoke(BaseNotifyCtx context) {
        val executorWrapper = context.getExecutorWrapper();
        val notifyItem = context.getNotifyItem();
        try {
            DtpNotifyCtxHolder.set(context);
            NotifierHandler.getInstance().sendAlarm(NotifyItemEnum.of(notifyItem.getType()));
            AlarmCounter.reset(executorWrapper.getThreadPoolName(), notifyItem.getType());
        } finally {
            DtpNotifyCtxHolder.remove();
        }
    }
}
```

**作用**: 告警责任链的终点，负责发送告警消息。

#### NoticeInvoker

```java
public class NoticeInvoker implements Invoker<BaseNotifyCtx> {
    @Override
    public void invoke(BaseNotifyCtx context) {
        try {
            DtpNotifyCtxHolder.set(context);
            val noticeCtx = (NoticeCtx) context;
            NotifierHandler.getInstance().sendNotice(
                noticeCtx.getOldFields(), 
                noticeCtx.getDiffs()
            );
        } finally {
            DtpNotifyCtxHolder.remove();
        }
    }
}
```

**作用**: 通知责任链的终点，负责发送配置变更通知。

## 工作流程

### 责任链构建流程

```
1. NotifyFilterBuilder.getAlarmInvokerChain()
   ↓
2. 收集所有过滤器（从 Spring 容器 + 内置过滤器）
   ↓
3. 过滤和排序（按 supports() 和 getOrder()）
   ↓
4. InvokerChainFactory.buildInvokerChain(AlarmInvoker, filters)
   ↓
5. 构建责任链：
   Filter1 → Filter2 → Filter3 → AlarmInvoker
   ↓
6. 返回 InvokerChain
```

### 责任链执行流程

```
1. chain.proceed(context)
   ↓
2. head.invoke(context)  // head 是第一个 Filter 包装的 Invoker
   ↓
3. Filter1.doFilter(context, nextInvoker)
   ↓
4. 如果通过过滤，调用 nextInvoker.invoke(context)
   ↓
5. Filter2.doFilter(context, nextInvoker)
   ↓
6. 如果通过过滤，调用 nextInvoker.invoke(context)
   ↓
7. ...（继续传递）
   ↓
8. AlarmInvoker.invoke(context)  // 最终执行
   ↓
9. 发送告警消息
```

## 设计模式

### 责任链模式（Chain of Responsibility）

Invoker 设计基于责任链模式：

```
请求 → Filter1 → Filter2 → Filter3 → Invoker → 响应
```

**特点**:
- 每个节点都可以处理请求或传递给下一个节点
- 可以动态组合节点
- 解耦请求发送者和接收者

### 模板方法模式

Invoker 的实现遵循模板方法模式：

```java
public void invoke(BaseNotifyCtx context) {
    try {
        // 前置处理：设置上下文
        DtpNotifyCtxHolder.set(context);
        
        // 模板方法：子类实现具体逻辑
        doInvoke(context);
        
    } finally {
        // 后置处理：清理上下文
        DtpNotifyCtxHolder.remove();
    }
}
```

## 使用示例

### 1. 构建告警责任链

```java
// 在 NotifyFilterBuilder 中
public static InvokerChain<BaseNotifyCtx> getAlarmInvokerChain() {
    // 收集过滤器
    Collection<NotifyFilter> alarmFilters = ...;
    
    // 构建责任链
    return InvokerChainFactory.buildInvokerChain(
        new AlarmInvoker(),  // 终点
        alarmFilters.toArray(new NotifyFilter[0])  // 过滤器
    );
}
```

### 2. 执行责任链

```java
// 在 AlarmManager 中
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getAlarmInvokerChain();
AlarmCtx context = new AlarmCtx(executorWrapper, notifyItem);
chain.proceed(context);  // 执行责任链
```

### 3. 过滤器实现

```java
public class CustomFilter implements NotifyFilter {
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
        return 10;  // 执行顺序
    }
}
```

## 设计优势

### 1. 可扩展性

- **添加新过滤器**: 只需实现 `NotifyFilter` 接口，注册为 Spring Bean
- **修改执行逻辑**: 只需修改 Invoker 实现，不影响过滤器
- **支持多种类型**: 可以为不同类型构建不同的责任链

### 2. 可维护性

- **职责单一**: 每个组件职责明确
- **易于测试**: 可以单独测试每个组件
- **代码清晰**: 责任链结构清晰，易于理解

### 3. 灵活性

- **动态组合**: 可以根据配置动态组合过滤器
- **顺序控制**: 通过 `getOrder()` 控制执行顺序
- **条件过滤**: 通过 `supports()` 控制是否参与责任链

### 4. 解耦性

- **过滤器与执行器解耦**: 过滤器不关心如何执行
- **执行器与过滤器解耦**: 执行器不关心有哪些过滤器
- **业务逻辑解耦**: 不同业务逻辑通过不同责任链处理

## 与其他设计模式的对比

### 与拦截器模式

| 特性 | Invoker 责任链 | 拦截器模式 |
|------|---------------|-----------|
| **结构** | 链式结构 | 拦截器列表 |
| **传递方式** | 显式传递 nextInvoker | 通过拦截器链传递 |
| **终点处理** | 明确的 Invoker | 目标方法 |
| **适用场景** | 过滤链处理 | AOP 拦截 |

### 与策略模式

| 特性 | Invoker 责任链 | 策略模式 |
|------|---------------|---------|
| **选择方式** | 顺序执行所有节点 | 选择一个策略 |
| **组合方式** | 链式组合 | 单一策略 |
| **适用场景** | 需要多个处理步骤 | 需要选择一种处理方式 |

## 总结

Invoker 设计的核心目的是：

1. **解耦**: 将过滤逻辑和执行逻辑分离
2. **终点**: 作为责任链的明确终点，执行实际操作
3. **统一**: 统一管理上下文和异常处理
4. **扩展**: 支持灵活的过滤器组合和扩展

通过 Invoker 设计，DynamicTp 实现了一个灵活、可扩展的通知和告警处理机制，既保证了代码的清晰性，又提供了强大的扩展能力。

