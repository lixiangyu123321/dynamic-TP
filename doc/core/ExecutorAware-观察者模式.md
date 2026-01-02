# ExecutorAware 观察者模式设计

## 概述

`ExecutorAware` 是 DynamicTp 框架中基于观察者模式设计的线程池感知器接口。它采用"感知-响应"的设计理念，允许开发者在线程池生命周期的各个关键节点感知状态变化并做出响应，而无需修改线程池的核心执行逻辑。

## 观察者模式语义

### 设计理念

`ExecutorAware` 的设计遵循观察者模式（Observer Pattern）的核心思想：

1. **观察者（Observer）**: `ExecutorAware` 实现类作为观察者
2. **被观察者（Subject）**: 线程池（Executor）作为被观察者
3. **事件（Event）**: 线程池生命周期中的各种事件（注册、刷新、任务提交、任务执行等）
4. **通知机制**: 当事件发生时，框架自动通知所有注册的感知器

### 与传统 AOP 的区别

虽然 `ExecutorAware` 在功能上类似于 AOP（面向切面编程），但在设计语义上有本质区别：

| 特性 | AOP（拦截器模式） | ExecutorAware（观察者模式） |
|------|------------------|---------------------------|
| **设计理念** | 拦截并修改执行流程 | 感知事件并响应 |
| **主动性** | 主动拦截、修改 | 被动感知、响应 |
| **职责** | 增强/修改业务逻辑 | 观察/监控/记录 |
| **侵入性** | 可能修改执行流程 | 不修改核心逻辑 |
| **语义** | "拦截-增强" | "感知-响应" |

### 观察者模式实现

#### 1. 主题（Subject）- 线程池

线程池作为被观察的主题，在关键生命周期节点触发事件：

```java
// 伪代码示例
public class DtpExecutor extends ThreadPoolExecutor {
    
    public void execute(Runnable command) {
        // 1. 触发任务提交事件
        AwareManager.execute(this, command);
        
        // 2. 执行核心逻辑
        super.execute(command);
    }
    
    protected void beforeExecute(Thread t, Runnable r) {
        // 1. 触发任务执行前事件
        AwareManager.beforeExecute(this, t, r);
        
        // 2. 执行核心逻辑
        super.beforeExecute(t, r);
    }
    
    protected void afterExecute(Runnable r, Throwable t) {
        // 1. 执行核心逻辑
        super.afterExecute(r, t);
        
        // 2. 触发任务执行后事件
        AwareManager.afterExecute(this, r, t);
    }
}
```

#### 2. 观察者（Observer）- ExecutorAware

`ExecutorAware` 实现类作为观察者，感知并响应事件：

```java
public interface ExecutorAware {
    // 观察者接口，定义可感知的事件
    void execute(Executor executor, Runnable r);
    void beforeExecute(Executor executor, Thread t, Runnable r);
    void afterExecute(Executor executor, Runnable r, Throwable t);
    // ... 其他事件
}
```

#### 3. 通知机制 - AwareManager

`AwareManager` 作为通知中心，管理所有观察者并分发事件：

```java
public class AwareManager {
    // 观察者列表
    private static final List<ExecutorAware> EXECUTOR_AWARE_LIST = new ArrayList<>();
    
    // 通知所有观察者
    public static void execute(Executor executor, Runnable r) {
        for (ExecutorAware aware : EXECUTOR_AWARE_LIST) {
            aware.execute(executor, r);  // 通知观察者
        }
    }
}
```

## 事件类型

`ExecutorAware` 定义了线程池生命周期中的各种事件类型：

### 1. 生命周期事件

| 事件 | 方法 | 触发时机 | 语义 |
|------|------|---------|------|
| **注册事件** | `register()` | 线程池注册到框架时 | 感知到新线程池注册 |
| **刷新事件** | `refresh()` | 线程池配置刷新时 | 感知到配置变化 |
| **移除事件** | `remove()` | 线程池从框架移除时 | 感知到线程池移除 |
| **关闭事件** | `shutdown()` | 线程池关闭时 | 感知到关闭请求 |
| **立即关闭事件** | `shutdownNow()` | 线程池立即关闭时 | 感知到立即关闭请求 |
| **终止事件** | `terminated()` | 线程池完全终止后 | 感知到线程池终止 |

### 2. 任务执行事件

| 事件 | 方法 | 触发时机 | 语义 |
|------|------|---------|------|
| **任务提交事件** | `execute()` | 任务提交到线程池时 | 感知到新任务提交 |
| **执行前事件** | `beforeExecute()` | 线程开始执行任务前 | 感知到任务即将执行 |
| **执行后事件** | `afterExecute()` | 线程执行完任务后 | 感知到任务执行完成 |
| **拒绝前事件** | `beforeReject()` | 任务被拒绝前 | 感知到任务将被拒绝 |
| **拒绝后事件** | `afterReject()` | 任务被拒绝后 | 感知到任务已被拒绝 |

## 观察者模式的优势

### 1. 解耦

- **线程池与感知逻辑解耦**: 线程池不需要知道有哪些感知器，只需触发事件
- **感知器之间解耦**: 各个感知器相互独立，互不影响

### 2. 可扩展性

- **易于添加新感知器**: 只需实现 `ExecutorAware` 接口并注册
- **支持动态注册**: 可以在运行时动态添加或移除感知器

### 3. 职责分离

- **线程池职责**: 专注于任务执行的核心逻辑
- **感知器职责**: 专注于监控、统计、告警等横切关注点

### 4. 灵活性

- **按需启用**: 可以为不同线程池配置不同的感知器
- **执行顺序**: 通过 `getOrder()` 控制感知器的执行顺序

## 实际应用场景

### 场景 1: 性能监控

```java
public class PerformanceMonitorAware implements ExecutorAware {
    
    @Override
    public void execute(Executor executor, Runnable r) {
        // 感知到任务提交，开始统计
        startTask(r);
    }
    
    @Override
    public void afterExecute(Executor executor, Runnable r, Throwable t) {
        // 感知到任务完成，记录统计信息
        completeTask(r);
    }
}
```

**语义**: "我感知到任务提交和完成，记录性能指标"

### 场景 2: 超时监控

```java
public class TaskTimeoutAware implements ExecutorAware {
    
    @Override
    public void execute(Executor executor, Runnable r) {
        // 感知到任务提交，启动队列超时监控
        startQueueTimeoutTask(r);
    }
    
    @Override
    public void beforeExecute(Executor executor, Thread t, Runnable r) {
        // 感知到任务即将执行，启动执行超时监控
        startRunTimeoutTask(t, r);
    }
}
```

**语义**: "我感知到任务状态变化，监控超时情况"

### 场景 3: 拒绝处理

```java
public class TaskRejectAware implements ExecutorAware {
    
    @Override
    public void beforeReject(Runnable r, Executor executor) {
        // 感知到任务将被拒绝，记录并告警
        recordReject();
        triggerAlarm();
    }
}
```

**语义**: "我感知到任务拒绝事件，记录并告警"

## 设计原则

### 1. 单一职责原则

每个感知器只关注一个特定的横切关注点：
- `PerformanceMonitorAware`: 只关注性能统计
- `TaskTimeoutAware`: 只关注超时监控
- `TaskRejectAware`: 只关注拒绝处理

### 2. 开闭原则

- **对扩展开放**: 可以轻松添加新的感知器
- **对修改封闭**: 不需要修改线程池的核心代码

### 3. 依赖倒置原则

- 线程池依赖 `ExecutorAware` 接口，而非具体实现
- 感知器实现 `ExecutorAware` 接口，依赖抽象而非具体

### 4. 接口隔离原则

- `ExecutorAware` 接口提供了多个方法，但都是默认实现
- 感知器只需实现需要的方法，无需实现所有方法

## 与 Spring Aware 的对比

DynamicTp 的 `ExecutorAware` 设计灵感来源于 Spring 的 `Aware` 接口系列：

| Spring Aware | DynamicTp ExecutorAware | 语义 |
|-------------|------------------------|------|
| `ApplicationContextAware` | `ExecutorAware` | 感知应用上下文/线程池 |
| `BeanNameAware` | - | 感知 Bean 名称 |
| `EnvironmentAware` | - | 感知环境配置 |

**共同点**:
- 都采用"感知-响应"的设计理念
- 都遵循观察者模式
- 都强调"感知"而非"拦截"

## 总结

`ExecutorAware` 采用观察者模式设计，具有以下特点：

1. **语义清晰**: "感知器"比"拦截器"更准确地描述了其职责
2. **设计优雅**: 符合观察者模式的设计原则
3. **易于扩展**: 支持灵活添加新的感知逻辑
4. **职责分离**: 线程池核心逻辑与横切关注点分离
5. **符合约定**: 遵循 Spring 框架的命名习惯

这种设计使得框架既保持了线程池核心逻辑的简洁性，又提供了强大的扩展能力，是观察者模式在实际框架设计中的优秀应用。

