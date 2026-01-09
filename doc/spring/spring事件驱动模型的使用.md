你想了解Spring中`ApplicationListener<ApplicationEvent>`的具体使用方法，它是Spring事件驱动模型的核心接口，用于监听并处理Spring容器发布的各类事件（如容器启动、Bean初始化、自定义事件等）。下面我会从**核心概念、基础用法、自定义事件、进阶技巧**四个维度，用可直接运行的代码示例帮你彻底掌握。

### 一、核心概念先理清
- **ApplicationListener**：事件监听器接口，泛型指定要监听的事件类型，实现`onApplicationEvent()`方法处理事件；
- **ApplicationEvent**：所有Spring事件的父类，包含事件源（`getSource()`）、事件发布时间等基础信息；
- **事件发布者**：通过`ApplicationContext.publishEvent()`发布事件，Spring容器自身也会发布内置事件（如容器刷新、关闭）。

核心流程：`发布事件（publishEvent）` → `容器广播事件` → `对应监听器的onApplicationEvent()执行`。

### 二、基础用法：监听Spring内置事件
Spring内置了多种事件（如容器刷新、关闭、Bean初始化完成等），直接实现`ApplicationListener`即可监听。

#### 步骤1：实现ApplicationListener接口（监听容器刷新事件）
```java
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

// 1. 标记为Spring组件（让容器扫描到）
@Component
// 2. 泛型指定要监听的事件：ContextRefreshedEvent（容器刷新完成事件）
public class ContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

    // 3. 实现事件处理方法
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 获取事件源（这里是ApplicationContext）
        Object source = event.getSource();
        System.out.println("监听到容器刷新完成事件！");
        System.out.println("事件源：" + source.getClass().getSimpleName());
        System.out.println("事件发布时间：" + event.getTimestamp());
        
        // 可在这里执行容器启动后的初始化逻辑（如加载配置、初始化资源）
        System.out.println("执行容器启动后的初始化操作...");
    }
}
```

#### 步骤2：启动Spring容器，触发事件
```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

// 配置类：扫描监听器所在包
@ComponentScan("com.example.listener")
public class ListenerConfig {
    public static void main(String[] args) {
        // 启动Spring容器，会自动发布ContextRefreshedEvent
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ListenerConfig.class);
        
        // 关闭容器（会发布ContextClosedEvent，可自行监听）
        context.close();
    }
}
```

#### 执行结果：
```
监听到容器刷新完成事件！
事件源：AnnotationConfigApplicationContext
事件发布时间：17364xxxxxxx
执行容器启动后的初始化操作...
```

#### 常用内置事件列表（直接复用）
| 事件类型 | 触发时机 | 典型用途 |
|----------|----------|----------|
| `ContextRefreshedEvent` | 容器刷新/启动完成 | 执行全局初始化逻辑 |
| `ContextClosedEvent` | 容器关闭时 | 释放资源（如线程池、连接池） |
| `ContextStartedEvent` | 容器启动（调用`start()`） | 启动定时任务、MQ消费者 |
| `BeanCreatedEvent` | Bean创建完成 | 监听特定Bean的初始化 |
| `ApplicationReadyEvent` | 应用完全启动（可接收请求） | 应用启动完成后的通知 |

### 三、进阶用法：自定义事件+监听
实际开发中更多是自定义业务事件（如订单创建、用户注册），步骤如下：

#### 步骤1：定义自定义事件（继承ApplicationEvent）
```java
import org.springframework.context.ApplicationEvent;

// 自定义事件：订单创建事件
public class OrderCreatedEvent extends ApplicationEvent {
    // 事件携带的业务数据
    private Long orderId;
    private String orderName;

    // 构造器：必须传入事件源（如当前类、Service等）
    public OrderCreatedEvent(Object source, Long orderId, String orderName) {
        super(source);
        this.orderId = orderId;
        this.orderName = orderName;
    }

    // getter方法（供监听器获取业务数据）
    public Long getOrderId() { return orderId; }
    public String getOrderName() { return orderName; }
}
```

#### 步骤2：实现监听器监听自定义事件
```java
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

// 订单创建事件监听器
@Component
public class OrderCreatedListener implements ApplicationListener<OrderCreatedEvent> {

    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        // 获取事件中的业务数据
        Long orderId = event.getOrderId();
        String orderName = event.getOrderName();
        
        // 处理业务逻辑（如发送短信、更新库存、记录日志）
        System.out.println("监听到订单创建事件：");
        System.out.println("订单ID：" + orderId + "，订单名称：" + orderName);
        System.out.println("执行订单创建后的逻辑：发送短信通知...");
    }
}
```

#### 步骤3：发布自定义事件
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

// 订单服务：发布事件
@Service
public class OrderService {

    // 注入ApplicationContext（事件发布者）
    @Autowired
    private ApplicationContext applicationContext;

    // 创建订单的业务方法
    public void createOrder(Long orderId, String orderName) {
        // 1. 执行业务逻辑（创建订单）
        System.out.println("创建订单：" + orderId + "-" + orderName);
        
        // 2. 发布自定义事件（解耦后续逻辑）
        applicationContext.publishEvent(new OrderCreatedEvent(this, orderId, orderName));
    }
}
```

#### 步骤4：测试自定义事件
```java
public class CustomEventTest {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ListenerConfig.class);
        
        // 获取订单服务，调用创建订单方法
        OrderService orderService = context.getBean(OrderService.class);
        orderService.createOrder(1001L, "小米手机");
        
        context.close();
    }
}
```

#### 执行结果：
```
创建订单：1001-小米手机
监听到订单创建事件：
订单ID：1001，订单名称：小米手机
执行订单创建后的逻辑：发送短信通知...
```

### 四、更简洁的用法：@EventListener注解（替代实现接口）
Spring 4.2+提供了`@EventListener`注解，无需实现`ApplicationListener`接口，直接标记方法即可监听事件，更简洁：

```java
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    // 注解指定监听的事件类型
    @EventListener(OrderCreatedEvent.class)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        System.out.println("注解式监听器处理订单事件：" + event.getOrderId());
    }

    // 监听多个事件（用数组）
    @EventListener({ContextRefreshedEvent.class, ContextClosedEvent.class})
    public void handleMultiEvent(ApplicationEvent event) {
        System.out.println("监听多事件：" + event.getClass().getSimpleName());
    }
}
```

### 五、关键注意事项
1. **事件是同步的**：默认情况下，发布事件和处理事件是同步的（发布者会等待监听器执行完成），若需异步，可结合`@Async`注解（需开启异步支持`@EnableAsync`）；
2. **监听器需被Spring管理**：监听器类必须加`@Component`/`@Service`等注解，让Spring扫描到，否则无法监听事件；
3. **泛型类型匹配**：监听器泛型指定的事件类型会自动匹配（如`ApplicationListener<ApplicationEvent>`会监听所有事件）；
4. **事件源可以是任意对象**：自定义事件的构造器中，`source`参数可传入当前业务类（如`this`），便于定位事件来源。

### 总结
1. **核心用法**：
    - 监听内置事件：实现`ApplicationListener<内置事件类型>`，加`@Component`；
    - 自定义事件：继承`ApplicationEvent`定义事件 → 实现监听器 → 通过`ApplicationContext.publishEvent()`发布；
2. **简化写法**：用`@EventListener`注解替代实现接口，更灵活；
3. **核心价值**：实现业务逻辑解耦（如订单创建后，短信、库存、日志逻辑通过监听器处理，无需耦合在订单方法中）。