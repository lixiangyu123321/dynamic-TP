# Spring 事件驱动模型详解

## 概述

Spring 事件驱动模型是 Spring 框架提供的一种发布-订阅（Publish-Subscribe）模式实现，用于实现组件之间的解耦通信。通过事件机制，组件可以发布事件，其他组件可以监听并响应这些事件，实现松耦合的架构设计。

### 核心概念

- **事件（Event）**：继承自 `ApplicationEvent` 的事件对象，包含事件源和相关信息
- **发布者（Publisher）**：通过 `ApplicationEventPublisher` 发布事件的组件
- **监听者（Listener）**：实现 `ApplicationListener` 或使用 `@EventListener` 注解的组件
- **事件广播器（Event Multicaster）**：负责将事件分发给所有注册的监听者

### 优势

1. **解耦**：发布者和监听者之间没有直接依赖关系
2. **异步支持**：可以异步处理事件，提高系统响应性
3. **扩展性**：可以轻松添加新的监听者而不修改现有代码
4. **灵活性**：支持同步和异步两种处理方式

## 核心接口和类

### 1. ApplicationEvent

所有 Spring 事件的基类：

```java
public abstract class ApplicationEvent extends EventObject {
    private static final long serialVersionUID = 7099057708183571937L;
    private final long timestamp;

    public ApplicationEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    public final long getTimestamp() {
        return this.timestamp;
    }
}
```

**特点**：
- 继承自 `java.util.EventObject`
- 包含事件源（source）
- 包含事件时间戳（timestamp）

### 2. ApplicationEventPublisher

用于发布事件的接口：

```java
public interface ApplicationEventPublisher {
    default void publishEvent(ApplicationEvent event) {
        publishEvent((Object) event);
    }
    
    void publishEvent(Object event);
}
```

**实现类**：
- `ApplicationContext`：实现了 `ApplicationEventPublisher` 接口
- 可以直接调用 `applicationContext.publishEvent(event)` 发布事件

### 3. ApplicationListener

事件监听器接口：

```java
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
    void onApplicationEvent(E event);
}
```

**特点**：
- 泛型接口，可以指定监听的事件类型
- 功能接口，可以使用 Lambda 表达式
- 监听器在接收到事件时调用 `onApplicationEvent()` 方法

### 4. SmartApplicationListener

更智能的监听器接口，支持事件类型过滤和顺序控制：

```java
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType);
    
    default boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return true;
    }
    
    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

**特点**：
- 可以根据事件类型决定是否处理
- 可以根据事件源类型决定是否处理
- 支持设置监听器执行顺序

## 内置事件类型

### 1. ContextRefreshedEvent

Spring 容器刷新完成后发布的事件：

```java
public class ContextRefreshedEvent extends ApplicationContextEvent {
    public ContextRefreshedEvent(ApplicationContext source) {
        super(source);
    }
}
```

**发布时机**：
- `ApplicationContext` 初始化或刷新完成后
- 所有 Bean 都已创建并初始化完成

**使用场景**：
- 执行初始化逻辑
- 启动后台任务
- 初始化缓存等

### 2. ContextStartedEvent

Spring 容器启动后发布的事件：

```java
public class ContextStartedEvent extends ApplicationContextEvent {
    public ContextStartedEvent(ApplicationContext source) {
        super(source);
    }
}
```

**发布时机**：
- 调用 `ApplicationContext.start()` 方法后
- 在 `ContextRefreshedEvent` 之后

### 3. ContextStoppedEvent

Spring 容器停止后发布的事件：

```java
public class ContextStoppedEvent extends ApplicationContextEvent {
    public ContextStoppedEvent(ApplicationContext source) {
        super(source);
    }
}
```

**发布时机**：
- 调用 `ApplicationContext.stop()` 方法后

### 4. ContextClosedEvent

Spring 容器关闭后发布的事件：

```java
public class ContextClosedEvent extends ApplicationContextEvent {
    public ContextClosedEvent(ApplicationContext source) {
        super(source);
    }
}
```

**发布时机**：
- 调用 `ApplicationContext.close()` 方法后
- 应用关闭时

### 5. RequestHandledEvent（已废弃）

处理 HTTP 请求完成后发布的事件（在 Spring 5.3+ 中已废弃）。

## 自定义事件

### 1. 创建自定义事件类

```java
import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件
 */
public class UserRegisteredEvent extends ApplicationEvent {
    private final String username;
    private final String email;

    public UserRegisteredEvent(Object source, String username, String email) {
        super(source);
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
```

### 2. 发布自定义事件

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void registerUser(String username, String email) {
        // 注册用户的业务逻辑
        System.out.println("注册用户: " + username);
        
        // 发布用户注册事件
        UserRegisteredEvent event = new UserRegisteredEvent(this, username, email);
        eventPublisher.publishEvent(event);
    }
}
```

### 3. 监听自定义事件

```java
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredListener implements ApplicationListener<UserRegisteredEvent> {
    
    @Override
    public void onApplicationEvent(UserRegisteredEvent event) {
        System.out.println("收到用户注册事件: " + event.getUsername());
        // 发送欢迎邮件
        sendWelcomeEmail(event.getEmail());
    }
    
    private void sendWelcomeEmail(String email) {
        System.out.println("发送欢迎邮件到: " + email);
    }
}
```

## 事件监听方式

### 1. 实现 ApplicationListener 接口

```java
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("Spring 容器已刷新: " + event.getApplicationContext());
    }
}
```

**优点**：
- 明确指定监听的事件类型
- 编译时类型安全

**缺点**：
- 需要实现接口
- 一个类只能监听一种事件类型（除非使用泛型通配符）

### 2. 使用 @EventListener 注解（推荐）

Spring 4.2+ 引入了 `@EventListener` 注解，更加灵活：

```java
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("处理用户注册事件: " + event.getUsername());
    }
    
    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        System.out.println("容器已刷新");
    }
}
```

**优点**：
- 代码更简洁
- 一个类可以监听多种事件
- 方法名可以自由命名
- 支持条件表达式

**特性**：

#### 条件监听

```java
@Component
public class ConditionalEventListener {
    
    @EventListener(condition = "#event.username != null && #event.username.length() > 5")
    public void handleLongUsername(UserRegisteredEvent event) {
        System.out.println("用户名长度大于5: " + event.getUsername());
    }
}
```

#### 监听多种事件类型

```java
@Component
public class MultipleEventListener {
    
    @EventListener({UserRegisteredEvent.class, ContextRefreshedEvent.class})
    public void handleMultipleEvents(ApplicationEvent event) {
        if (event instanceof UserRegisteredEvent) {
            UserRegisteredEvent userEvent = (UserRegisteredEvent) event;
            System.out.println("用户注册: " + userEvent.getUsername());
        } else if (event instanceof ContextRefreshedEvent) {
            System.out.println("容器已刷新");
        }
    }
}
```

#### 获取事件源

```java
@Component
public class SourceEventListener {
    
    @EventListener
    public void handleEvent(UserRegisteredEvent event) {
        Object source = event.getSource();
        System.out.println("事件源: " + source);
    }
}
```

### 3. 使用 SmartApplicationListener

适用于需要根据事件类型或事件源进行条件判断的场景：

```java
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class SmartUserListener implements SmartApplicationListener {
    
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return UserRegisteredEvent.class.isAssignableFrom(eventType);
    }
    
    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return UserService.class.isAssignableFrom(sourceType);
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof UserRegisteredEvent) {
            UserRegisteredEvent userEvent = (UserRegisteredEvent) event;
            System.out.println("智能监听器处理: " + userEvent.getUsername());
        }
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 设置高优先级
    }
}
```

**特点**：
- 可以根据事件类型过滤
- 可以根据事件源类型过滤
- 可以设置执行顺序

## 异步事件处理

### 1. 使用 @Async 注解

```java
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncEventListener {
    
    @Async
    @EventListener
    public void handleAsyncEvent(UserRegisteredEvent event) {
        System.out.println("异步处理事件: " + event.getUsername() + 
                         " 线程: " + Thread.currentThread().getName());
        // 模拟耗时操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**配置异步支持**：

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 启用异步支持
}
```

### 2. 配置异步执行器

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-event-");
        executor.initialize();
        return executor;
    }
}
```

### 3. 指定异步执行器

```java
@Component
public class AsyncEventListener {
    
    @Async("taskExecutor")
    @EventListener
    public void handleAsyncEvent(UserRegisteredEvent event) {
        // 使用指定的执行器
    }
}
```

## 事件监听顺序

### 1. 使用 @Order 注解

```java
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderedListeners {
    
    @Order(1)
    @EventListener
    public void firstListener(UserRegisteredEvent event) {
        System.out.println("第一个监听器");
    }
    
    @Order(2)
    @EventListener
    public void secondListener(UserRegisteredEvent event) {
        System.out.println("第二个监听器");
    }
    
    @Order(3)
    @EventListener
    public void thirdListener(UserRegisteredEvent event) {
        System.out.println("第三个监听器");
    }
}
```

**执行顺序**：
- `@Order` 值越小，优先级越高
- 默认值为 `Ordered.LOWEST_PRECEDENCE`

### 2. 实现 Ordered 接口

```java
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class OrderedListener implements ApplicationListener<UserRegisteredEvent>, Ordered {
    
    @Override
    public void onApplicationEvent(UserRegisteredEvent event) {
        System.out.println("有序监听器处理事件");
    }
    
    @Override
    public int getOrder() {
        return 100; // 设置顺序
    }
}
```

## 项目中的实际应用

### 1. OnceApplicationContextEventListener

项目中的抽象基类，确保事件只执行一次：

```java
@Slf4j
public abstract class OnceApplicationContextEventListener 
    implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 只处理来自原始 ApplicationContext 的事件
        if (isOriginalEventSource(event) && event instanceof ApplicationContextEvent) {
            if (event instanceof ContextRefreshedEvent) {
                onContextRefreshedEvent((ContextRefreshedEvent) event);
            } else if (event instanceof ContextStartedEvent) {
                onContextStartedEvent((ContextStartedEvent) event);
            } else if (event instanceof ContextStoppedEvent) {
                onContextStoppedEvent((ContextStoppedEvent) event);
            } else if (event instanceof ContextClosedEvent) {
                onContextClosedEvent((ContextClosedEvent) event);
            }
        }
    }

    protected void onContextRefreshedEvent(ContextRefreshedEvent event) {
    }

    protected void onContextStartedEvent(ContextStartedEvent event) {
    }

    protected void onContextStoppedEvent(ContextStoppedEvent event) {
    }

    protected void onContextClosedEvent(ContextClosedEvent event) {
    }

    /**
     * 判断是否是原始 ApplicationContext 发布的事件
     * 防止在父子容器环境中重复处理事件
     */
    private boolean isOriginalEventSource(ApplicationEvent event) {
        return nullSafeEquals(this.applicationContext, event.getSource());
    }

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
```

**设计要点**：
- 防止在父子容器环境中重复处理事件
- 提供模板方法，子类只需重写特定方法
- 确保每个事件只处理一次

### 2. DtpApplicationListener

监听 Spring 容器刷新事件，发布自定义事件：

```java
@Slf4j
public class DtpApplicationListener extends OnceApplicationContextEventListener {

    @Override
    protected void onContextRefreshedEvent(ContextRefreshedEvent event) {
        CustomContextRefreshedEvent refreshedEvent = new CustomContextRefreshedEvent(this);
        // 通过 EventBusManager 发布事件
        EventBusManager.post(refreshedEvent);
    }
}
```

**作用**：
- 监听 Spring 容器刷新事件
- 转换为框架内部的自定义事件
- 通过 EventBus 发布，实现框架内部的事件通信

### 3. NacosRefresher

使用 `SmartApplicationListener` 监听 Nacos 配置变更事件：

```java
@Slf4j
public class NacosRefresher extends AbstractSpringRefresher implements SmartApplicationListener {

    public NacosRefresher(DtpProperties dtpProperties) {
        super(dtpProperties);
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        // 只处理 NacosConfigEvent 及其子类
        return NacosConfigEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof NacosConfigEvent) {
            // 当 Nacos 配置变更时，刷新线程池配置
            refresh(environment);
        }
    }
}
```

**特点**：
- 使用 `SmartApplicationListener` 过滤事件类型
- 只处理 Nacos 配置变更事件
- 配置变更时自动刷新线程池

### 4. AbstractWebServerDtpAdapter

监听 Web 服务器初始化事件：

```java
@Slf4j
public abstract class AbstractWebServerDtpAdapter<A extends Executor> 
    extends AbstractDtpAdapter implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof WebServerInitializedEvent) {
            try {
                DtpProperties dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
                // Web 服务器初始化时，初始化适配器
                initialize();
                afterInitialize();
                refresh(dtpProperties);
            } catch (Exception e) {
                log.error("DynamicTp adapter, {} init failed.", getTpName(), e);
            }
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        ApplicationContext applicationContext = SpringContextHolder.getInstance();
        WebServer webServer = ((WebServerApplicationContext) applicationContext).getWebServer();
        doEnhance(webServer);
    }

    protected abstract void doEnhance(WebServer webServer);
}
```

**作用**：
- 监听 Spring Boot Web 服务器初始化事件
- 在 Web 服务器启动后初始化适配器
- 支持 Tomcat、Undertow 等 Web 服务器

## 事件传播机制

### 1. 父子容器事件传播

在 Spring 中，如果存在父子容器，事件会在父子容器之间传播：

```java
// 父容器
ApplicationContext parentContext = new ClassPathXmlApplicationContext("parent.xml");

// 子容器
GenericApplicationContext childContext = new GenericApplicationContext();
childContext.setParent(parentContext);
childContext.refresh();

// 在子容器中发布事件，会传播到父容器
childContext.publishEvent(new MyEvent(childContext));
```

**处理方式**：
- 使用 `OnceApplicationContextEventListener` 确保只处理一次
- 检查事件源是否为原始容器

### 2. 阻止事件传播

```java
@Component
public class PropagationControlListener implements ApplicationListener<MyEvent> {
    
    @Override
    public void onApplicationEvent(MyEvent event) {
        // 检查事件源，只处理来自特定容器的事件
        if (event.getSource() instanceof ApplicationContext) {
            ApplicationContext source = (ApplicationContext) event.getSource();
            if (source == getMyApplicationContext()) {
                // 只处理来自自己容器的事件
                handleEvent(event);
            }
        }
    }
}
```

## 最佳实践

### 1. 事件类设计

```java
// 好的设计：事件包含必要的信息
public class UserRegisteredEvent extends ApplicationEvent {
    private final String username;
    private final String email;
    private final LocalDateTime registeredTime;
    
    public UserRegisteredEvent(Object source, String username, String email) {
        super(source);
        this.username = username;
        this.email = email;
        this.registeredTime = LocalDateTime.now();
    }
}

// 不好的设计：事件只包含 ID，监听器需要查询数据库
public class UserRegisteredEvent extends ApplicationEvent {
    private final Long userId; // 监听器需要查询数据库获取详细信息
}
```

### 2. 监听器职责单一

```java
// 好的设计：每个监听器只负责一个职责
@Component
public class EmailListener {
    @EventListener
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        // 只发送邮件
    }
}

@Component
public class LogListener {
    @EventListener
    public void logUserRegistration(UserRegisteredEvent event) {
        // 只记录日志
    }
}

// 不好的设计：一个监听器做多件事
@Component
public class UserRegistrationHandler {
    @EventListener
    public void handleUserRegistration(UserRegisteredEvent event) {
        sendEmail(event);
        logEvent(event);
        updateCache(event);
        notifyOtherSystems(event);
        // 太多职责
    }
}
```

### 3. 异常处理

```java
@Component
public class RobustEventListener {
    
    @EventListener
    public void handleEvent(UserRegisteredEvent event) {
        try {
            // 处理事件
            processEvent(event);
        } catch (Exception e) {
            // 记录异常，但不影响其他监听器
            log.error("处理事件失败", e);
            // 可以选择重试或通知管理员
        }
    }
}
```

### 4. 异步处理耗时操作

```java
@Component
public class AsyncEventProcessor {
    
    // 同步处理：快速响应
    @EventListener
    public void handleQuickOperation(UserRegisteredEvent event) {
        // 快速操作，如更新内存缓存
        cache.put(event.getUsername(), event);
    }
    
    // 异步处理：耗时操作
    @Async
    @EventListener
    public void handleSlowOperation(UserRegisteredEvent event) {
        // 耗时操作，如发送邮件、调用外部 API
        sendWelcomeEmail(event.getEmail());
    }
}
```

### 5. 避免循环依赖

```java
// 不好的设计：事件发布者和监听者在同一个 Bean 中，可能产生循环依赖
@Service
public class UserService {
    
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @Autowired
    private UserService self; // 可能导致循环依赖
    
    public void registerUser(String username) {
        publisher.publishEvent(new UserRegisteredEvent(this, username));
    }
}

// 好的设计：分离关注点
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    public void registerUser(String username) {
        publisher.publishEvent(new UserRegisteredEvent(this, username));
    }
}

@Component
public class UserEventListener {
    @EventListener
    public void handleEvent(UserRegisteredEvent event) {
        // 处理事件
    }
}
```

## 性能考虑

### 1. 事件处理时间

- 同步事件处理会阻塞发布者，应保持快速
- 耗时操作应使用异步处理
- 避免在事件处理中执行数据库事务等长时间操作

### 2. 监听器数量

- 监听器越多，事件分发时间越长
- 考虑使用条件表达式减少不必要的处理
- 对于高频事件，优化监听器逻辑

### 3. 事件对象大小

- 事件对象应该尽量小，避免传递大量数据
- 可以使用事件 ID，监听器按需查询数据

## 常见问题

### 1. 监听器未执行

**原因**：
- 监听器未注册为 Spring Bean
- 事件类型不匹配
- 条件表达式过滤掉了事件

**解决**：
```java
// 确保监听器是 Spring Bean
@Component
public class MyListener {
    @EventListener
    public void handleEvent(MyEvent event) {
        // ...
    }
}
```

### 2. 事件重复处理

**原因**：
- 父子容器都监听了事件
- 监听器被多次注册

**解决**：
```java
// 使用 OnceApplicationContextEventListener
public class MyListener extends OnceApplicationContextEventListener {
    @Override
    protected void onContextRefreshedEvent(ContextRefreshedEvent event) {
        // 只执行一次
    }
}
```

### 3. 异步事件顺序问题

**问题**：异步事件处理顺序不确定

**解决**：
```java
// 如果顺序重要，使用同步处理或使用队列
@Component
public class OrderedAsyncListener {
    
    @Async
    @Order(1)
    @EventListener
    public void firstStep(MyEvent event) {
        // 第一步
    }
    
    @Async
    @Order(2)
    @EventListener
    public void secondStep(MyEvent event) {
        // 注意：异步情况下 Order 可能不生效
    }
}
```

## 总结

Spring 事件驱动模型提供了强大的解耦机制：

1. **核心组件**：
   - `ApplicationEvent`：事件基类
   - `ApplicationEventPublisher`：事件发布者
   - `ApplicationListener`：事件监听者
   - `SmartApplicationListener`：智能监听者

2. **监听方式**：
   - 实现 `ApplicationListener` 接口
   - 使用 `@EventListener` 注解（推荐）
   - 实现 `SmartApplicationListener` 接口

3. **异步支持**：
   - 使用 `@Async` 注解实现异步处理
   - 配置异步执行器控制线程池

4. **最佳实践**：
   - 事件包含足够的信息
   - 监听器职责单一
   - 耗时操作使用异步处理
   - 避免循环依赖

5. **在 DynamicTp 项目中的应用**：
   - 使用 `OnceApplicationContextEventListener` 防止重复处理
   - 监听 Spring 容器生命周期事件
   - 监听配置中心配置变更事件
   - 监听 Web 服务器初始化事件

**关键要点**：
- 事件驱动模型实现了发布者和订阅者的解耦
- 支持同步和异步两种处理方式
- 可以轻松扩展新的监听者
- 注意父子容器的事件传播问题
- 合理使用异步处理提高性能

## 参考资源

- [Spring Framework Documentation - Events](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-functionality-events)
- [Spring Framework API - ApplicationEvent](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/ApplicationEvent.html)
- [Spring Framework API - ApplicationListener](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/ApplicationListener.html)

