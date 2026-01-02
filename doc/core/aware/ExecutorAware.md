# ExecutorAware

## 概述

`ExecutorAware` 是执行器感知器接口，继承自 `DtpAware`。它定义了线程池生命周期中的各种感知点，允许在特定事件发生时注入自定义逻辑。

## 核心作用

1. **生命周期感知**: 感知线程池的注册、刷新、移除等生命周期事件
2. **任务执行感知**: 感知任务的提交、执行前、执行后等事件
3. **关闭感知**: 感知线程池的关闭、终止等事件
4. **拒绝感知**: 感知任务被拒绝的事件

## 接口定义

```java
public interface ExecutorAware extends DtpAware {
    int getOrder();                    // 获取执行顺序
    String getName();                  // 获取感知器名称
    void register(ExecutorWrapper wrapper);  // 注册执行器
    void refresh(ExecutorWrapper wrapper, TpExecutorProps props);  // 刷新配置
    void remove(ExecutorWrapper wrapper);    // 移除执行器
    void execute(Executor executor, Runnable r);  // 任务提交
    void beforeExecute(Executor executor, Thread t, Runnable r);  // 任务执行前
    void afterExecute(Executor executor, Runnable r, Throwable t);  // 任务执行后
    void shutdown(Executor executor);  // 关闭执行器
    void shutdownNow(Executor executor, List<Runnable> tasks);  // 立即关闭
    void terminated(Executor executor);  // 执行器终止
    void beforeReject(Runnable r, Executor executor);  // 拒绝前
    void afterReject(Runnable r, Executor executor);   // 拒绝后
}
```

## 核心方法

### getOrder() / getName()

**作用**: 获取感知器的执行顺序和名称

**说明**: 
- `getOrder()`: 返回执行顺序，数值越小越先执行
- `getName()`: 返回感知器名称，用于配置和识别

---

### register(ExecutorWrapper wrapper)

**作用**: 执行器注册时调用

**说明**: 当执行器注册到框架时，会调用此方法

---

### refresh(ExecutorWrapper wrapper, TpExecutorProps props)

**作用**: 配置刷新时调用

**说明**: 当执行器配置刷新时，会调用此方法

---

### remove(ExecutorWrapper wrapper)

**作用**: 执行器移除时调用

**说明**: 当执行器从框架移除时，会调用此方法

---

### execute(Executor executor, Runnable r)

**作用**: 任务提交时调用

**说明**: 当任务提交到执行器时，会调用此方法

---

### beforeExecute(Executor executor, Thread t, Runnable r)

**作用**: 任务执行前调用

**说明**: 当任务开始执行前，会调用此方法

---

### beforeExecuteWrap(Executor executor, Thread t, Runnable r)

**作用**: 任务执行前包装

**实现**:
```java
default Runnable beforeExecuteWrap(Executor executor, Thread t, Runnable r) {
    beforeExecute(executor, t, r);
    return r;
}
```

**说明**: 支持任务包装，返回包装后的任务

---

### afterExecute(Executor executor, Runnable r, Throwable t)

**作用**: 任务执行后调用

**说明**: 当任务执行完成后，会调用此方法

---

### afterExecuteWrap(Executor executor, Runnable r, Throwable t)

**作用**: 任务执行后包装

**实现**:
```java
default Runnable afterExecuteWrap(Executor executor, Runnable r, Throwable t) {
    afterExecute(executor, r, t);
    return r;
}
```

---

### shutdown(Executor executor) / shutdownNow(Executor executor, List<Runnable> tasks)

**作用**: 执行器关闭时调用

**说明**: 当执行器关闭时，会调用相应的方法

---

### terminated(Executor executor)

**作用**: 执行器终止时调用

**说明**: 当执行器完全终止时，会调用此方法

---

### beforeReject(Runnable r, Executor executor) / afterReject(Runnable r, Executor executor)

**作用**: 任务拒绝前后调用

**说明**: 当任务被拒绝时，会调用相应的方法

---

### beforeRejectWrap(Runnable r, Executor executor) / afterRejectWrap(Runnable r, Executor executor)

**作用**: 任务拒绝前后包装

**说明**: 支持任务包装，返回包装后的任务

## 内置实现

框架提供了三个内置实现：

1. **PerformanceMonitorAware**: 性能监控感知器
2. **TaskTimeoutAware**: 任务超时感知器
3. **TaskRejectAware**: 任务拒绝感知器

## 使用场景

### 1. 实现自定义感知器

```java
public class CustomAware implements ExecutorAware {
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public void execute(Executor executor, Runnable r) {
        // 任务提交时的逻辑
    }
    
    @Override
    public void beforeExecute(Executor executor, Thread t, Runnable r) {
        // 任务执行前的逻辑
    }
    
    @Override
    public void afterExecute(Executor executor, Runnable r, Throwable t) {
        // 任务执行后的逻辑
    }
}
```

### 2. 通过 SPI 注册

```java
// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.aware.ExecutorAware
// com.example.CustomAware
```

## 设计特点

### 1. 观察者模式

采用观察者模式，感知器作为观察者，执行器作为被观察者。

### 2. 默认实现

所有方法都提供默认实现，子类只需实现需要的方法。

### 3. 有序执行

通过 `getOrder()` 控制执行顺序。

## 注意事项

1. **执行顺序**: 通过 `getOrder()` 控制执行顺序
2. **异常处理**: 感知器中的异常会被捕获，不会影响其他感知器
3. **性能影响**: 感知器会在关键路径上执行，需要注意性能

