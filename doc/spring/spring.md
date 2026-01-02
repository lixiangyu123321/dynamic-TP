# Spring 模块

## 模块概述

`spring` 模块是 DynamicTp 框架的 Spring 集成模块，提供了与 Spring 框架的深度集成功能，包括 Spring Bean 的自动注册、Spring 生命周期管理、Spring 上下文管理等。

## 主要功能

### 1. 注解支持

- **@EnableDynamicTp**: 启用动态线程池注解
  - 在 Spring Boot 启动类上使用
  - 启用框架的自动配置功能
  - 导入必要的配置类

- **@DynamicTp**: 动态线程池注解
  - 标记需要被框架管理的线程池 Bean
  - 支持 JUC 线程池和 Spring 线程池

### 2. Bean 后处理

- **DtpPostProcessor**: Bean 后处理器
  - 自动识别和注册线程池 Bean
  - 支持 `ThreadPoolExecutor` 和 `ThreadPoolTaskExecutor`
  - 自动增强线程池功能

### 3. 配置类

- **DtpBaseBeanConfiguration**: 基础 Bean 配置类
  - 配置框架的核心 Bean
  - 注册线程池生命周期管理
  - 配置事件监听器

- **DtpConfigurationSelector**: 配置选择器
  - 根据条件选择加载的配置类
  - 支持不同的运行模式

### 4. Bean 定义注册

- **DtpBeanDefinitionRegistrar**: Bean 定义注册器
  - 动态注册 Bean 定义
  - 支持编程式 Bean 注册

- **DtpBaseBeanDefinitionRegistrar**: 基础 Bean 定义注册器
  - 提供 Bean 定义注册的通用逻辑

### 5. 上下文管理

- **SpringContextHolder**: Spring 上下文持有者
  - 管理 Spring ApplicationContext
  - 提供 Bean 获取功能
  - 提供环境变量访问功能

### 6. 生命周期管理

- **DtpLifecycleSpringAdapter**: Spring 生命周期适配器
  - 适配 Spring 的生命周期接口
  - 实现线程池的优雅关闭

### 7. 事件监听

- **DtpApplicationListener**: 应用事件监听器
  - 监听 Spring 应用事件
  - 处理上下文刷新事件

### 8. 配置刷新

- **AbstractSpringRefresher**: Spring 配置刷新器抽象类
  - 提供 Spring 环境下的配置刷新功能
  - 支持从 Spring Environment 读取配置

## 核心类说明

### @EnableDynamicTp

启用动态线程池的注解：

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DtpConfigurationSelector.class)
public @interface EnableDynamicTp {
}
```

使用方式：

```java
@EnableDynamicTp
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### DtpPostProcessor

Bean 后处理器，自动识别和注册线程池：

- `postProcessAfterInitialization()`: 在 Bean 初始化后处理
- 自动识别 `ThreadPoolExecutor` 和 `ThreadPoolTaskExecutor`
- 自动注册到 `DtpRegistry`
- 支持功能增强

### SpringContextHolder

Spring 上下文持有者：

- `getBean()`: 获取 Spring Bean
- `getBeansOfType()`: 获取指定类型的所有 Bean
- `getEnvironment()`: 获取环境配置
- `getEnvironmentProperty()`: 获取环境属性

### DtpLifecycleSpringAdapter

Spring 生命周期适配器：

- 实现 `SmartLifecycle` 接口
- 在 Spring 容器关闭时优雅关闭线程池
- 尽可能处理队列中的任务

## 工作原理

### 1. Bean 自动注册

1. `DtpPostProcessor` 在 Bean 初始化后检查是否为线程池
2. 如果是线程池，自动注册到 `DtpRegistry`
3. 如果是 `DtpExecutor`，进行功能增强
4. 如果是普通线程池，包装为 `ExecutorWrapper`

### 2. 生命周期管理

1. 框架实现 `SmartLifecycle` 接口
2. 在 Spring 容器启动时启动框架
3. 在 Spring 容器关闭时优雅关闭线程池

### 3. 配置刷新

1. 监听配置中心的变化
2. 从 Spring Environment 读取配置
3. 刷新线程池配置

## 依赖关系

- `spring-context`: Spring 上下文
- `dynamic-tp-core`: 依赖核心模块

## 使用场景

### 1. 自动管理 Spring Bean 中的线程池

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    @DynamicTp
    public ThreadPoolExecutor myExecutor() {
        return new ThreadPoolExecutor(
            10, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200)
        );
    }
}
```

### 2. 管理 Spring 线程池

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    @DynamicTp
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        return executor;
    }
}
```

### 3. 获取线程池

```java
@Service
public class MyService {
    
    @Autowired
    private ThreadPoolExecutor myExecutor;
    
    public void doSomething() {
        myExecutor.execute(() -> {
            // 业务逻辑
        });
    }
}
```

或者通过 `DtpRegistry` 获取：

```java
DtpExecutor executor = DtpRegistry.getExecutor("myExecutor");
executor.execute(() -> {
    // 业务逻辑
});
```

## 配置说明

### 启用框架

在启动类上添加 `@EnableDynamicTp` 注解：

```java
@EnableDynamicTp
@SpringBootApplication
public class Application {
    // ...
}
```

### 标记线程池

使用 `@DynamicTp` 注解标记需要管理的线程池：

```java
@Bean
@DynamicTp
public ThreadPoolExecutor myExecutor() {
    // ...
}
```

## 特性

1. **零侵入**: 通过 Bean 后处理器自动识别线程池，无需修改业务代码
2. **自动注册**: 自动将线程池注册到框架进行管理
3. **功能增强**: 自动增强线程池功能（监控、告警等）
4. **生命周期管理**: 与 Spring 生命周期集成，支持优雅关闭
5. **配置刷新**: 支持从 Spring Environment 读取配置并刷新

## 注意事项

1. **Bean 顺序**: 确保线程池 Bean 在框架初始化后创建
2. **循环依赖**: 注意避免循环依赖问题
3. **作用域**: 建议使用单例作用域的线程池 Bean
4. **关闭顺序**: 框架会在 Spring 容器关闭前关闭线程池

