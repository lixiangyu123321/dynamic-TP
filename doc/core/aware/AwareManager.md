# AwareManager

## 概述

`AwareManager` 是感知器管理器，负责统一管理所有 `ExecutorAware` 实现。它维护感知器列表，并在线程池生命周期事件发生时，将事件分发给所有注册的感知器。

## 核心作用

1. **感知器管理**: 统一管理所有感知器（内置和扩展）
2. **事件分发**: 将线程池生命周期事件分发给所有感知器
3. **异常处理**: 捕获感知器执行中的异常，避免影响其他感知器
4. **有序执行**: 按顺序执行感知器

## 核心属性

```java
// 感知器列表
private static final List<ExecutorAware> EXECUTOR_AWARE_LIST = new ArrayList<>();
```

## 初始化

在静态代码块中初始化：

```java
static {
    // 1. 添加内置感知器
    EXECUTOR_AWARE_LIST.add(new PerformanceMonitorAware());
    EXECUTOR_AWARE_LIST.add(new TaskTimeoutAware());
    EXECUTOR_AWARE_LIST.add(new TaskRejectAware());
    
    // 2. 通过 SPI 加载扩展感知器
    List<ExecutorAware> serviceLoader = ExtensionServiceLoader.get(ExecutorAware.class);
    EXECUTOR_AWARE_LIST.addAll(serviceLoader);
    
    // 3. 按顺序排序
    EXECUTOR_AWARE_LIST.sort(Comparator.comparingInt(ExecutorAware::getOrder));
}
```

**初始化顺序**:
1. 添加内置感知器
2. 通过 SPI 加载扩展感知器
3. 按 `getOrder()` 排序

## 核心方法

### 1. add(ExecutorAware aware)

**作用**: 动态添加感知器

**实现**:
```java
public static void add(ExecutorAware aware) {
    for (ExecutorAware executorAware : EXECUTOR_AWARE_LIST) {
        if (executorAware.getName().equalsIgnoreCase(aware.getName())) {
            return;  // 已存在同名感知器，不重复添加
        }
    }
    EXECUTOR_AWARE_LIST.add(aware);
    EXECUTOR_AWARE_LIST.sort(Comparator.comparingInt(ExecutorAware::getOrder));
}
```

**说明**:
- 检查名称是否已存在，避免重复添加
- 添加后重新排序

---

### 2. register(ExecutorWrapper executorWrapper)

**作用**: 注册执行器到所有感知器

**实现**:
```java
public static void register(ExecutorWrapper executorWrapper) {
    for (ExecutorAware executorAware : EXECUTOR_AWARE_LIST) {
        val awareNames = executorWrapper.getAwareNames();
        // 如果 awareNames 为空，注册所有感知器
        if (CollectionUtils.isEmpty(awareNames) || awareNames.contains(executorAware.getName())) {
            executorAware.register(executorWrapper);
        }
    }
}
```

**说明**:
- 如果 `awareNames` 为空，注册所有感知器
- 否则只注册 `awareNames` 中包含的感知器

---

### 3. refresh(ExecutorWrapper executorWrapper, TpExecutorProps props)

**作用**: 刷新执行器配置时通知所有感知器

**实现**:
```java
public static void refresh(ExecutorWrapper executorWrapper, TpExecutorProps props) {
    for (ExecutorAware executorAware : EXECUTOR_AWARE_LIST) {
        val awareNames = props.getAwareNames();
        if (CollectionUtils.isEmpty(awareNames) || awareNames.contains(executorAware.getName())) {
            executorAware.refresh(executorWrapper, props);
        } else {
            executorAware.remove(executorWrapper);  // 如果不在配置中，移除
        }
    }
}
```

**说明**:
- 如果感知器在配置中，调用 `refresh()`
- 如果感知器不在配置中，调用 `remove()`

---

### 4. execute(Executor executor, Runnable r)

**作用**: 任务提交时通知所有感知器

**实现**:
```java
public static void execute(Executor executor, Runnable r) {
    for (ExecutorAware aware : EXECUTOR_AWARE_LIST) {
        try {
            aware.execute(executor, r);
        } catch (Exception e) {
            log.error("DynamicTp aware [{}], enhance execute error.", aware.getName(), e);
        }
    }
}
```

**异常处理**: 捕获异常，避免影响其他感知器

---

### 5. beforeExecute(Executor executor, Thread t, Runnable r)

**作用**: 任务执行前通知所有感知器

**实现**:
```java
public static void beforeExecute(Executor executor, Thread t, Runnable r) {
    for (ExecutorAware aware : EXECUTOR_AWARE_LIST) {
        try {
            r = aware.beforeExecuteWrap(executor, t, r);
        } catch (Exception e) {
            log.error("DynamicTp aware [{}], enhance beforeExecute error.", aware.getName(), e);
        }
    }
}
```

**说明**: 支持任务包装，返回包装后的任务

---

### 6. afterExecute(Executor executor, Runnable r, Throwable t)

**作用**: 任务执行后通知所有感知器

**实现**:
```java
public static void afterExecute(Executor executor, Runnable r, Throwable t) {
    for (ExecutorAware aware : EXECUTOR_AWARE_LIST) {
        try {
            r = aware.afterExecuteWrap(executor, r, t);
        } catch (Exception e) {
            log.error("DynamicTp aware [{}], enhance afterExecute error.", aware.getName(), e);
        }
    }
}
```

---

### 7. shutdown(Executor executor)

**作用**: 执行器关闭时通知所有感知器

---

### 8. shutdownNow(Executor executor, List<Runnable> tasks)

**作用**: 执行器立即关闭时通知所有感知器

---

### 9. terminated(Executor executor)

**作用**: 执行器终止时通知所有感知器

---

### 10. beforeReject(Runnable r, Executor executor)

**作用**: 任务拒绝前通知所有感知器

---

### 11. afterReject(Runnable r, Executor executor)

**作用**: 任务拒绝后通知所有感知器

## 设计特点

### 1. 统一管理

所有感知器由 `AwareManager` 统一管理，提供统一的入口。

### 2. 异常隔离

每个感知器的异常都被捕获，不会影响其他感知器：

```java
try {
    aware.execute(executor, r);
} catch (Exception e) {
    log.error("DynamicTp aware [{}], enhance execute error.", aware.getName(), e);
}
```

### 3. 有序执行

感知器按 `getOrder()` 排序，保证执行顺序。

### 4. 选择性注册

支持通过 `awareNames` 选择性地注册感知器。

## 使用场景

### 1. 框架内部使用

框架在以下场景自动调用 `AwareManager`：

- 执行器注册时：`AwareManager.register()`
- 配置刷新时：`AwareManager.refresh()`
- 任务执行时：`AwareManager.execute()`, `beforeExecute()`, `afterExecute()`
- 执行器关闭时：`AwareManager.shutdown()`

### 2. 扩展感知器

通过 SPI 机制扩展感知器：

```java
// 实现 ExecutorAware 接口
public class CustomAware implements ExecutorAware {
    // 实现接口方法
}

// 创建 SPI 配置文件
// META-INF/services/org.dromara.dynamictp.core.aware.ExecutorAware
// com.example.CustomAware
```

### 3. 动态添加感知器

```java
AwareManager.add(new CustomAware());
```

## 注意事项

1. **执行顺序**: 感知器按 `getOrder()` 排序执行
2. **异常处理**: 感知器中的异常不会影响其他感知器
3. **线程安全**: 感知器列表在静态代码块中初始化，后续只读
4. **选择性注册**: 可以通过 `awareNames` 控制哪些感知器生效

