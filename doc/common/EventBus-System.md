# EventBus 体系功能用法

## 概述

DynamicTp 框架基于 Google Guava 的 `EventBus` 实现了一套事件驱动机制，用于实现模块间的解耦通信。通过 `EventBusManager` 统一管理事件的注册、发布和订阅，支持配置刷新、监控采集、告警检查等多种事件类型。

### 核心定位

- **解耦通信**: 实现发布-订阅模式，解耦事件生产者和消费者
- **统一管理**: 通过 `EventBusManager` 统一管理所有事件
- **模块通信**: 支持配置刷新、监控采集、告警检查等模块间通信
- **扩展性强**: 支持自定义事件类型和订阅者

## 核心组件

### 1. EventBusManager

`EventBusManager` 是事件总线的统一管理器，封装了 Guava EventBus 的核心功能。

#### 类结构

```java
@Slf4j
public class EventBusManager {
    
    /** 单例 EventBus 实例 */
    private static final EventBus EVENT_BUS = new EventBus();
    
    /** 已注册的订阅者集合（用于注销） */
    private static final Set<Object> REGISTERED_OBJECTS = ConcurrentHashMap.newKeySet();
    
    private EventBusManager() { }
}
```

#### 核心方法

##### register(Object object) - 注册订阅者

```java
/**
 * 注册订阅者到事件总线
 * @param object 订阅者对象（包含 @Subscribe 方法的对象）
 */
public static void register(Object object) {
    if (REGISTERED_OBJECTS.add(object)) {
        EVENT_BUS.register(object);
    }
}
```

**说明**:
- 使用 `ConcurrentHashMap.newKeySet()` 保证线程安全
- 使用 `add()` 方法避免重复注册
- 注册后，对象中所有标注了 `@Subscribe` 的方法都会被识别为订阅方法

##### unregister(Object object) - 注销订阅者

```java
/**
 * 从事件总线注销订阅者
 * @param object 要注销的订阅者对象
 */
public static void unregister(Object object) {
    if (REGISTERED_OBJECTS.remove(object)) {
        try {
            EVENT_BUS.unregister(object);
        } catch (IllegalArgumentException e) {
            log.warn("Attempted to unregister an object that was not registered: {}", object, e);
        }
    }
}
```

**说明**:
- 从注册集合中移除对象
- 如果对象未注册，会捕获异常并记录警告日志

##### post(Object event) - 发布事件

```java
/**
 * 发布事件到事件总线
 * @param event 事件对象
 */
public static void post(Object event) {
    EVENT_BUS.post(event);
}
```

**说明**:
- 同步发布事件，所有订阅方法在当前线程执行
- 事件会自动分发给所有匹配的订阅方法

##### getInstance() - 获取 EventBus 实例

```java
/**
 * 获取 EventBus 实例（用于高级用法）
 * @return EventBus 实例
 */
public static EventBus getInstance() {
    return EVENT_BUS;
}
```

##### destroy() - 销毁事件总线

```java
/**
 * 销毁事件总线，注销所有订阅者
 */
public static void destroy() {
    for (Object object : REGISTERED_OBJECTS) {
        try {
            EVENT_BUS.unregister(object);
        } catch (Exception e) {
            log.warn("Attempted to unregister an object that was not registered: {}", object, e);
        }
    }
    REGISTERED_OBJECTS.clear();
}
```

**说明**:
- 在应用关闭时调用，清理所有订阅者
- 防止内存泄漏

---

## 事件类型体系

### 事件类层次结构

```
EventObject (Java 标准库)
    ├── CustomContextRefreshedEvent (上下文刷新事件)
    └── DtpEvent (动态线程池事件基类)
        ├── RefreshEvent (配置刷新事件)
        ├── CollectEvent (监控采集事件)
        └── AlarmCheckEvent (告警检查事件)
```

### 1. DtpEvent（事件基类）

所有动态线程池相关事件的基类，继承自 `EventObject`。

```java
@Getter
public abstract class DtpEvent extends EventObject {
    
    /** 动态线程池配置属性 */
    private final transient DtpProperties dtpProperties;
    
    protected DtpEvent(Object source, DtpProperties dtpProperties) {
        super(source);
        this.dtpProperties = dtpProperties;
    }
}
```

**特点**:
- 包含 `DtpProperties` 配置信息
- 所有子类事件都携带配置信息

### 2. RefreshEvent（配置刷新事件）

当配置中心配置发生变化时发布的事件。

```java
public class RefreshEvent extends DtpEvent {
    
    public RefreshEvent(Object source, DtpProperties dtpProperties) {
        super(source, dtpProperties);
    }
}
```

**发布场景**:
- 配置中心配置变更
- 手动刷新配置

**订阅者**:
- `DtpAdapterListener` - 刷新适配器配置
- `DtpRegistry` - 刷新线程池配置

### 3. CollectEvent（监控采集事件）

当需要采集线程池指标时发布的事件。

```java
public class CollectEvent extends DtpEvent {
    
    public CollectEvent(Object source, DtpProperties dtpProperties) {
        super(source, dtpProperties);
    }
}
```

**发布场景**:
- 定时监控任务执行
- 手动触发指标采集

**订阅者**:
- `DtpAdapterListener` - 采集适配器线程池指标

### 4. AlarmCheckEvent（告警检查事件）

当需要检查告警时发布的事件。

```java
public class AlarmCheckEvent extends DtpEvent {
    
    public AlarmCheckEvent(Object source, DtpProperties dtpProperties) {
        super(source, dtpProperties);
    }
}
```

**发布场景**:
- 定时监控任务执行
- 手动触发告警检查

**订阅者**:
- `DtpAdapterListener` - 检查适配器线程池告警

### 5. CustomContextRefreshedEvent（上下文刷新事件）

Spring 上下文刷新后发布的事件，用于初始化组件。

```java
public class CustomContextRefreshedEvent extends EventObject {
    
    public CustomContextRefreshedEvent(Object source) {
        super(source);
    }
}
```

**发布场景**:
- Spring 应用上下文刷新完成
- 应用启动完成

**订阅者**:
- `DtpRegistry` - 初始化线程池注册表
- `AbstractDtpAdapter` - 初始化适配器
- `DtpMonitor` - 启动监控任务

---

## 使用示例

### 1. 注册订阅者

#### 方式一：在构造函数中注册

```java
public class DtpAdapterListener {
    
    public DtpAdapterListener() {
        EventBusManager.register(this);
    }
    
    @Subscribe
    public void handleDtpEvent(EventObject event) {
        // 处理事件
    }
}
```

#### 方式二：在初始化方法中注册

```java
public class DtpMonitor {
    
    public DtpMonitor(DtpProperties dtpProperties) {
        this.dtpProperties = dtpProperties;
        EventBusManager.register(this);
    }
    
    @Subscribe
    public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        // 处理上下文刷新事件
    }
}
```

### 2. 定义订阅方法

订阅方法需要满足以下规则：

1. **标注 `@Subscribe` 注解**: 使用 `com.google.common.eventbus.Subscribe`
2. **只有一个参数**: 参数类型为要处理的事件类型
3. **访问修饰符无限制**: public/private/protected 均可

#### 示例1：订阅特定事件类型

```java
@Subscribe
public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
    // 只处理 CustomContextRefreshedEvent 事件
    log.info("Context refreshed, initializing...");
    // 初始化逻辑
}
```

#### 示例2：订阅基类事件（处理多种事件）

```java
@Subscribe
public void handleDtpEvent(EventObject event) {
    // 可以处理所有 EventObject 及其子类事件
    if (event instanceof RefreshEvent) {
        RefreshEvent refreshEvent = (RefreshEvent) event;
        doRefresh(refreshEvent.getDtpProperties());
    } else if (event instanceof CollectEvent) {
        CollectEvent collectEvent = (CollectEvent) event;
        doCollect(collectEvent.getDtpProperties());
    } else if (event instanceof AlarmCheckEvent) {
        AlarmCheckEvent alarmCheckEvent = (AlarmCheckEvent) event;
        doAlarmCheck(alarmCheckEvent.getDtpProperties());
    }
}
```

### 3. 发布事件

#### 发布配置刷新事件

```java
// AbstractRefresher.java
private void publishEvent(DtpProperties dtpProperties) {
    RefreshEvent event = new RefreshEvent(this, dtpProperties);
    EventBusManager.post(event);
}
```

#### 发布监控采集事件

```java
// DtpMonitor.java
private void publishCollectEvent() {
    CollectEvent event = new CollectEvent(this, dtpProperties);
    EventBusManager.post(event);
}
```

#### 发布告警检查事件

```java
// DtpMonitor.java
private void publishAlarmCheckEvent() {
    AlarmCheckEvent event = new AlarmCheckEvent(this, dtpProperties);
    EventBusManager.post(event);
}
```

---

## 项目中的实际应用

### 1. DtpRegistry - 线程池注册中心

```java
@Slf4j
public class DtpRegistry {
    
    public DtpRegistry(DtpProperties dtpProperties) {
        DtpRegistry.dtpProperties = dtpProperties;
        EventBusManager.register(this);  // 注册为订阅者
    }
    
    /**
     * 监听上下文刷新事件
     */
    @Subscribe
    public void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        val executors = Optional.ofNullable(dtpProperties.getExecutors())
            .orElse(Collections.emptyList());
        val registeredExecutors = Sets.newHashSet(EXECUTOR_REGISTRY.keySet());
        
        // 处理远程执行器和本地执行器
        Collection<String> remoteExecutors = Collections.emptySet();
        if (CollectionUtils.isNotEmpty(executors)) {
            remoteExecutors = CollectionUtils.intersection(
                executors.stream()
                    .map(DtpExecutorProps::getThreadPoolName)
                    .collect(Collectors.toSet()),
                registeredExecutors
            );
        }
        val localExecutors = CollectionUtils.subtract(registeredExecutors, remoteExecutors);
        
        // 刷新非 Dtp 执行器
        val nonDtpExecutors = executors.stream()
            .filter(e -> !e.isAutoCreate())
            .collect(toList());
        if (CollectionUtils.isNotEmpty(nonDtpExecutors)) {
            nonDtpExecutors.forEach(DtpRegistry::refresh);
        }
        
        log.info("DtpRegistry has been initialized, remote executors: {}, local executors: {}",
            remoteExecutors, localExecutors);
    }
}
```

**作用**:
- 监听上下文刷新事件，初始化线程池注册表
- 区分远程执行器和本地执行器
- 刷新非 Dtp 执行器配置

### 2. AbstractDtpAdapter - 适配器基类

```java
@Slf4j
public abstract class AbstractDtpAdapter implements DtpAdapter {
    
    protected AbstractDtpAdapter() {
        EventBusManager.register(this);  // 注册为订阅者
    }
    
    /**
     * 监听上下文刷新事件
     */
    @Subscribe
    public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        try {
            DtpProperties dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
            initialize();
            afterInitialize();
            refresh(dtpProperties);
            log.info("DynamicTp adapter, {} init end, executors {}", 
                getTpPrefix(), executors.keySet());
        } catch (Throwable e) {
            log.error("DynamicTp adapter, {} init failed.", getTpPrefix(), e);
        }
    }
}
```

**作用**:
- 监听上下文刷新事件，初始化适配器
- 初始化执行器
- 刷新配置

### 3. DtpMonitor - 监控管理器

```java
@Slf4j
public class DtpMonitor {
    
    public DtpMonitor(DtpProperties dtpProperties) {
        this.dtpProperties = dtpProperties;
        EventBusManager.register(this);  // 注册为订阅者
    }
    
    /**
     * 监听上下文刷新事件，启动监控任务
     */
    @Subscribe
    public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        // 如果监控间隔相同，不做刷新处理
        if (monitorInterval == dtpProperties.getMonitorInterval()) {
            return;
        }
        
        // 取消旧的监控任务
        if (monitorFuture != null) {
            monitorFuture.cancel(true);
        }
        
        // 兼容 Spring Boot DevTools 重启场景
        if (monitorExecutor == null || monitorExecutor.isShutdown() 
            || monitorExecutor.isTerminated()) {
            monitorExecutor = ThreadPoolCreator.newScheduledThreadPool("dtp-monitor", 1);
        }
        
        monitorInterval = dtpProperties.getMonitorInterval();
        monitorFuture = monitorExecutor.scheduleWithFixedDelay(
            this::run, 0, monitorInterval, TimeUnit.SECONDS);
    }
    
    private void run() {
        Set<String> executorNames = DtpRegistry.getAllExecutorNames();
        try {
            checkAlarm(executorNames);
            collectMetrics(executorNames);
        } catch (Exception e) {
            log.error("DynamicTp monitor, run error", e);
        }
    }
    
    private void publishCollectEvent() {
        CollectEvent event = new CollectEvent(this, dtpProperties);
        EventBusManager.post(event);  // 发布采集事件
    }
    
    private void publishAlarmCheckEvent() {
        AlarmCheckEvent event = new AlarmCheckEvent(this, dtpProperties);
        EventBusManager.post(event);  // 发布告警检查事件
    }
}
```

**作用**:
- 监听上下文刷新事件，启动定时监控任务
- 定时发布采集事件和告警检查事件
- 收集线程池指标和检查告警

### 4. DtpAdapterListener - 适配器事件监听器

```java
@Slf4j
public class DtpAdapterListener {
    
    public DtpAdapterListener() {
        EventBusManager.register(this);  // 注册为订阅者
    }
    
    /**
     * 处理所有 DtpEvent 事件
     */
    @Subscribe
    public void handleDtpEvent(EventObject event) {
        try {
            if (event instanceof RefreshEvent) {
                RefreshEvent refreshEvent = (RefreshEvent) event;
                doRefresh(refreshEvent.getDtpProperties());
            } else if (event instanceof CollectEvent) {
                CollectEvent collectEvent = (CollectEvent) event;
                doCollect(collectEvent.getDtpProperties());
            } else if (event instanceof AlarmCheckEvent) {
                AlarmCheckEvent alarmCheckEvent = (AlarmCheckEvent) event;
                doAlarmCheck(alarmCheckEvent.getDtpProperties());
            }
        } catch (Exception e) {
            log.error("DynamicTp adapter, event handle failed.", e);
        }
    }
    
    /**
     * 刷新适配器配置
     */
    protected void doRefresh(DtpProperties dtpProperties) {
        val handlerMap = ContextManagerHelper.getBeansOfType(DtpAdapter.class);
        if (MapUtils.isEmpty(handlerMap)) {
            return;
        }
        handlerMap.forEach((k, v) -> v.refresh(dtpProperties));
    }
    
    /**
     * 采集适配器线程池指标
     */
    protected void doCollect(DtpProperties dtpProperties) {
        val handlerMap = ContextManagerHelper.getBeansOfType(DtpAdapter.class);
        if (MapUtils.isEmpty(handlerMap)) {
            return;
        }
        handlerMap.forEach((k, v) -> 
            v.getMultiPoolStats().forEach(ps ->
                CollectorHandler.getInstance().collect(ps, dtpProperties.getCollectorTypes())
            )
        );
    }
    
    /**
     * 检查适配器线程池告警
     */
    protected void doAlarmCheck(DtpProperties dtpProperties) {
        val handlerMap = ContextManagerHelper.getBeansOfType(DtpAdapter.class);
        if (MapUtils.isEmpty(handlerMap)) {
            return;
        }
        handlerMap.forEach((k, v) -> {
            val executorWrapper = v.getExecutorWrappers();
            executorWrapper.forEach((kk, vv) -> 
                AlarmManager.checkAndTryAlarmAsync(vv, SCHEDULE_NOTIFY_ITEMS)
            );
        });
    }
}
```

**作用**:
- 统一处理适配器相关事件
- 刷新、采集、告警检查适配器线程池

### 5. AbstractRefresher - 配置刷新器

```java
public abstract class AbstractRefresher {
    
    /**
     * 发布配置刷新事件
     */
    private void publishEvent(DtpProperties dtpProperties) {
        RefreshEvent event = new RefreshEvent(this, dtpProperties);
        EventBusManager.post(event);  // 发布刷新事件
    }
}
```

**作用**:
- 当配置发生变化时，发布刷新事件
- 通知所有订阅者刷新配置

---

## 事件流转流程

### 1. 应用启动流程

```
Spring 应用启动
    ↓
发布 CustomContextRefreshedEvent
    ↓
DtpRegistry.onContextRefreshedEvent() → 初始化线程池注册表
    ↓
AbstractDtpAdapter.onContextRefreshedEvent() → 初始化适配器
    ↓
DtpMonitor.onContextRefreshedEvent() → 启动监控任务
```

### 2. 配置刷新流程

```
配置中心配置变更
    ↓
AbstractRefresher.refresh() → 检测配置变化
    ↓
发布 RefreshEvent
    ↓
DtpAdapterListener.handleDtpEvent() → 刷新适配器配置
    ↓
DtpRegistry.refresh() → 刷新线程池配置
```

### 3. 监控采集流程

```
定时监控任务执行
    ↓
DtpMonitor.run() → 执行监控逻辑
    ↓
发布 CollectEvent
    ↓
DtpAdapterListener.handleDtpEvent() → 采集适配器指标
    ↓
发布 AlarmCheckEvent
    ↓
DtpAdapterListener.handleDtpEvent() → 检查适配器告警
```

---

## 自定义事件和订阅者

### 1. 定义自定义事件

```java
/**
 * 自定义事件：线程池创建事件
 */
public class ThreadPoolCreatedEvent extends DtpEvent {
    
    private final String threadPoolName;
    private final ExecutorWrapper executorWrapper;
    
    public ThreadPoolCreatedEvent(Object source, DtpProperties dtpProperties,
                                   String threadPoolName, ExecutorWrapper executorWrapper) {
        super(source, dtpProperties);
        this.threadPoolName = threadPoolName;
        this.executorWrapper = executorWrapper;
    }
    
    public String getThreadPoolName() {
        return threadPoolName;
    }
    
    public ExecutorWrapper getExecutorWrapper() {
        return executorWrapper;
    }
}
```

### 2. 定义自定义订阅者

```java
/**
 * 自定义订阅者：线程池创建监听器
 */
@Slf4j
public class ThreadPoolCreatedListener {
    
    public ThreadPoolCreatedListener() {
        EventBusManager.register(this);  // 注册为订阅者
    }
    
    /**
     * 订阅线程池创建事件
     */
    @Subscribe
    public void onThreadPoolCreated(ThreadPoolCreatedEvent event) {
        log.info("Thread pool created: {}, executor: {}", 
            event.getThreadPoolName(), event.getExecutorWrapper());
        // 自定义处理逻辑
    }
}
```

### 3. 发布自定义事件

```java
// 在创建线程池时发布事件
ThreadPoolCreatedEvent event = new ThreadPoolCreatedEvent(
    this, dtpProperties, threadPoolName, executorWrapper);
EventBusManager.post(event);
```

---

## 最佳实践

### 1. 订阅者注册时机

- **构造函数中注册**: 适用于需要立即监听事件的场景
- **初始化方法中注册**: 适用于需要依赖注入完成的场景
- **延迟注册**: 适用于需要条件判断的场景

```java
// ✅ 推荐：在构造函数中注册
public class MyListener {
    public MyListener() {
        EventBusManager.register(this);
    }
}

// ✅ 推荐：在初始化方法中注册
@PostConstruct
public void init() {
    EventBusManager.register(this);
}
```

### 2. 订阅方法设计

- **单一职责**: 每个订阅方法只处理一种事件类型
- **异常处理**: 在订阅方法中捕获异常，避免影响其他订阅者
- **线程安全**: 如果订阅方法可能被并发调用，需要保证线程安全

```java
// ✅ 推荐：单一职责 + 异常处理
@Subscribe
public void handleRefreshEvent(RefreshEvent event) {
    try {
        // 处理刷新事件
    } catch (Exception e) {
        log.error("Handle refresh event failed", e);
    }
}

// ✅ 推荐：线程安全
@Subscribe
public synchronized void handleEvent(Event event) {
    // 线程安全的处理逻辑
}
```

### 3. 事件发布时机

- **配置变更时**: 发布刷新事件
- **定时任务执行时**: 发布采集和告警检查事件
- **应用启动时**: 发布上下文刷新事件

```java
// ✅ 推荐：在配置变更时发布
private void onConfigChanged(DtpProperties newProperties) {
    RefreshEvent event = new RefreshEvent(this, newProperties);
    EventBusManager.post(event);
}
```

### 4. 资源清理

- **应用关闭时**: 调用 `EventBusManager.destroy()` 清理所有订阅者
- **Bean 销毁时**: 调用 `EventBusManager.unregister()` 注销订阅者

```java
// ✅ 推荐：在应用关闭时清理
@PreDestroy
public void destroy() {
    EventBusManager.unregister(this);
}
```

---

## 注意事项

### 1. 订阅方法规则

- **必须标注 `@Subscribe`**: 使用 `com.google.common.eventbus.Subscribe`
- **只能有一个参数**: 参数类型为要处理的事件类型
- **访问修饰符无限制**: public/private/protected 均可

```java
// ✅ 正确
@Subscribe
public void handleEvent(RefreshEvent event) { }

// ❌ 错误：没有 @Subscribe 注解
public void handleEvent(RefreshEvent event) { }

// ❌ 错误：多个参数
@Subscribe
public void handleEvent(RefreshEvent event, String extra) { }
```

### 2. 事件匹配规则

- **精确匹配**: 订阅方法参数类型 == 发布事件类型
- **父类匹配**: 订阅方法参数类型可以是事件类型的父类

```java
// 可以接收 RefreshEvent
@Subscribe
public void handleRefreshEvent(RefreshEvent event) { }

// 可以接收所有 DtpEvent 及其子类
@Subscribe
public void handleDtpEvent(DtpEvent event) { }

// 可以接收所有事件
@Subscribe
public void handleAllEvent(EventObject event) { }
```

### 3. 线程模型

- **同步执行**: 默认情况下，订阅方法在发布线程中同步执行
- **异常隔离**: 单个订阅方法抛出异常不会影响其他订阅方法

```java
// 同步执行，在发布线程中执行
EventBusManager.post(event);  // 阻塞直到所有订阅方法执行完成
```

### 4. 重复注册

- **避免重复注册**: `EventBusManager.register()` 使用 `Set` 避免重复注册
- **注销后重新注册**: 注销后可以重新注册

```java
// ✅ 安全：不会重复注册
EventBusManager.register(listener);
EventBusManager.register(listener);  // 第二次注册无效
```

---

## 总结

EventBus 体系是 DynamicTp 框架的核心通信机制，通过发布-订阅模式实现了模块间的解耦。主要特点：

1. **统一管理**: 通过 `EventBusManager` 统一管理所有事件
2. **类型丰富**: 支持配置刷新、监控采集、告警检查等多种事件类型
3. **易于扩展**: 支持自定义事件类型和订阅者
4. **线程安全**: 使用 `ConcurrentHashMap` 保证线程安全
5. **异常隔离**: 单个订阅方法异常不影响其他订阅者

通过 EventBus 体系，DynamicTp 实现了配置刷新、监控采集、告警检查等功能的解耦，提高了框架的可维护性和扩展性。

