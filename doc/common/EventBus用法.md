# EventBus 核心用法详解
EventBus 是 Guava 提供的事件总线框架（上述代码为核心实现），核心作用是实现**发布-订阅模式**，解耦事件生产者和消费者。以下从「基础概念、核心用法、完整示例、高级特性」四个维度讲解其使用方式。

## 一、核心概念先了解
| 组件/概念         | 作用                                                                 |
|--------------------|----------------------------------------------------------------------|
| EventBus 实例      | 事件总线核心，负责注册订阅者、发布事件、分发事件                     |
| 订阅者（Subscriber） | 注册到 EventBus 的对象，包含标注了订阅方法的类（如 `@Subscribe` 方法） |
| 事件（Event）      | 任意 POJO 对象，是生产者和消费者之间传递的消息载体                   |
| 订阅方法           | 订阅者中处理事件的方法（需标注 `@Subscribe`，参数为事件类型）         |
| 分发器（Dispatcher）| 事件分发策略（如按线程分发、直接分发）                               |
| 异常处理器         | 处理订阅方法执行时抛出的异常（默认 LoggingHandler 打印日志）         |

## 二、基础使用步骤（核心流程）
### 步骤1：引入依赖（Maven）
Guava 是 EventBus 的核心依赖，需先引入：
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version> <!-- 推荐使用最新稳定版 -->
</dependency>
```

### 步骤2：定义事件（任意POJO）
事件是普通 Java 对象，无需继承/实现任何接口，示例：
```java
// 1. 简单事件：用户注册事件
public class UserRegisterEvent {
    private String userId;
    private String username;

    // 构造器、getter/setter
    public UserRegisterEvent(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // 省略getter/setter
}

// 2. 空事件（无数据）：系统启动事件
public class SystemStartEvent {}
```

### 步骤3：定义订阅者（含订阅方法）
订阅者是普通对象，其中**订阅方法需满足以下规则**：
- 方法标注 `@com.google.common.eventbus.Subscribe`；
- 方法只有一个参数（对应要处理的事件类型）；
- 方法访问修饰符无限制（public/private/protected 均可）。

示例订阅者：
```java
import com.google.common.eventbus.Subscribe;

// 订阅者1：用户事件处理器
public class UserEventHandler {
    // 处理用户注册事件
    @Subscribe
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("处理用户注册事件：");
        System.out.println("用户ID：" + event.getUserId());
        System.out.println("用户名：" + event.getUsername());
        // 业务逻辑：如发送注册邮件、初始化用户数据等
    }
}

// 订阅者2：系统事件处理器
public class SystemMonitor {
    // 处理系统启动事件
    @Subscribe
    public void onSystemStart(SystemStartEvent event) {
        System.out.println("监听到系统启动事件，执行初始化操作...");
        // 业务逻辑：如加载配置、连接数据库等
    }

    // 处理DeadEvent（无订阅者的事件）
    @Subscribe
    public void handleDeadEvent(DeadEvent event) {
        System.out.println("未找到订阅者的事件：" + event.getEvent());
    }
}
```

### 步骤4：创建 EventBus 并使用
核心操作：创建总线 → 注册订阅者 → 发布事件 → （可选）注销订阅者。

#### 4.1 基础用法（默认总线）
```java
import com.google.common.eventbus.EventBus;

public class EventBusDemo {
    public static void main(String[] args) {
        // 1. 创建默认 EventBus 实例（标识符为 "default"，直接执行器，默认异常处理器）
        EventBus eventBus = new EventBus();

        // 2. 注册订阅者（可注册多个）
        eventBus.register(new UserEventHandler());
        eventBus.register(new SystemMonitor());

        // 3. 发布事件（事件会自动分发给所有匹配的订阅方法）
        // 发布用户注册事件
        eventBus.post(new UserRegisterEvent("1001", "张三"));
        // 发布系统启动事件
        eventBus.post(new SystemStartEvent());
        // 发布一个无订阅者的事件（会被封装为 DeadEvent）
        eventBus.post("这是一个无订阅者的字符串事件");

        // 4. （可选）注销订阅者（不再接收事件）
        eventBus.unregister(new SystemMonitor());
    }
}
```

#### 4.2 输出结果
```
处理用户注册事件：
用户ID：1001
用户名：张三
监听到系统启动事件，执行初始化操作...
未找到订阅者的事件：这是一个无订阅者的字符串事件
```

## 三、高级用法
### 3.1 自定义 EventBus 标识符
标识符用于区分不同的总线（如业务总线、系统总线），便于日志排查：
```java
// 创建自定义标识符的 EventBus
EventBus businessBus = new EventBus("business-event-bus");
System.out.println("总线标识符：" + businessBus.identifier()); // 输出：business-event-bus
```

### 3.2 自定义异常处理器
默认异常处理器（LoggingHandler）仅打印日志，可自定义异常处理逻辑：
```java
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

// 自定义异常处理器
public class CustomExceptionHandler implements SubscriberExceptionHandler {
    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        // 自定义逻辑：如记录告警、发送邮件、打印详细日志
        System.err.println("订阅方法执行异常：");
        System.err.println("事件类型：" + context.getEvent().getClass().getName());
        System.err.println("订阅方法：" + context.getSubscriberMethod().getName());
        System.err.println("异常信息：" + exception.getMessage());
    }
}

// 使用自定义异常处理器创建 EventBus
EventBus eventBus = new EventBus(new CustomExceptionHandler());
```

### 3.3 异步事件分发（指定线程池）
默认 EventBus 是同步分发（发布线程执行订阅方法），可通过指定线程池实现异步：
```java
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;

// 创建固定线程池的执行器
Executor executor = Executors.newFixedThreadPool(5);
// 创建异步 EventBus（使用自定义执行器）
EventBus asyncEventBus = new EventBus(
    "async-bus", 
    MoreExecutors.listeningDecorator(executor), // 包装为监听执行器
    com.google.common.eventbus.Dispatcher.perThreadDispatchQueue(), // 按线程分发队列
    new CustomExceptionHandler()
);

// 发布事件（订阅方法会在线程池线程中执行）
asyncEventBus.post(new UserRegisterEvent("1002", "李四"));
```

### 3.4 处理 DeadEvent（无订阅者的事件）
如果发布的事件没有任何订阅者，EventBus 会自动将其封装为 `DeadEvent` 发布，可通过订阅 `DeadEvent` 统一处理：
```java
@Subscribe
public void handleDeadEvent(DeadEvent deadEvent) {
    Object originalEvent = deadEvent.getEvent();
    System.out.println("未找到订阅者的事件类型：" + originalEvent.getClass().getName());
    System.out.println("事件内容：" + originalEvent);
}
```

## 四、关键注意事项
1. **订阅方法规则**：
    - 必须只有一个参数（事件类型），多参数会导致无法注册；
    - 方法需标注 `@Subscribe`（Guava 包下的注解，非自定义）；
    - 事件匹配规则：订阅方法参数类型 == 发布事件类型（或父类），如 `Object` 类型的订阅方法可接收所有事件。

2. **线程模型**：
    - 默认 `MoreExecutors.directExecutor()`：同步分发，发布线程执行订阅方法；
    - 自定义线程池：异步分发，订阅方法在线程池线程中执行，需注意线程安全。

3. **注册/注销**：
    - 同一个订阅者对象多次注册，订阅方法会被执行多次；
    - 注销时需传入**同一个对象实例**（new 新对象注销无效）；
    - 建议在对象生命周期结束时注销（如 Spring Bean 的 destroy 阶段）。

4. **异常处理**：
    - 单个订阅方法抛出异常，不会影响其他订阅方法执行；
    - 异常会被 `SubscriberExceptionHandler` 捕获，默认仅打印日志，建议自定义异常处理器。

## 五、总结
| 核心操作         | 代码示例                                  |
|------------------|-------------------------------------------|
| 创建总线         | `EventBus bus = new EventBus("custom");`  |
| 注册订阅者       | `bus.register(subscriber);`               |
| 发布事件         | `bus.post(event);`                        |
| 注销订阅者       | `bus.unregister(subscriber);`             |
| 自定义异常处理器 | `new EventBus(customExceptionHandler);`   |
| 异步分发         | `new EventBus(id, executor, dispatcher, handler);` |

EventBus 是轻量级事件总线，无需定义复杂的接口/继承关系，仅通过注解即可实现解耦，适合中小型项目的事件驱动开发（如业务解耦、系统监控、消息通知等场景）。