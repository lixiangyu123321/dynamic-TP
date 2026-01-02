# DynamicTp 注解

## 概述

`@DynamicTp` 是一个注解，用于标记需要被 DynamicTp 框架管理的线程池 Bean。它主要用于管理 JUC 的 `ThreadPoolExecutor` 和 Spring 的 `ThreadPoolTaskExecutor`。

## 核心作用

1. **标记线程池**: 标记需要被框架管理的线程池 Bean
2. **自动注册**: 框架会自动识别并注册标记的线程池
3. **功能增强**: 自动为线程池添加监控、告警等功能

## 注解定义

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicTp {
    String value() default "";
}
```

## 使用方式

### 1. 标记 ThreadPoolExecutor Bean

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    @DynamicTp("myThreadPool")  // 指定线程池名称
    public ThreadPoolExecutor myExecutor() {
        return new ThreadPoolExecutor(
            10, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200)
        );
    }
}
```

### 2. 标记 ThreadPoolTaskExecutor Bean

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    @DynamicTp("myTaskExecutor")  // 指定线程池名称
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        return executor;
    }
}
```

### 3. 使用 Bean 名称

如果不指定 `value`，框架会使用 Bean 的方法名作为线程池名称：

```java
@Bean
@DynamicTp  // 线程池名称为 "myExecutor"
public ThreadPoolExecutor myExecutor() {
    return new ThreadPoolExecutor(...);
}
```

## 工作原理

### 1. Bean 后处理

框架通过 `DtpPostProcessor`（Bean 后处理器）识别标记的线程池：

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // 检查是否为线程池
    if (!(bean instanceof ThreadPoolExecutor) && !(bean instanceof ThreadPoolTaskExecutor)) {
        return bean;
    }
    
    // 检查是否有 @DynamicTp 注解
    // 如果有，注册到框架
    return registerAndReturnCommon(bean, beanName);
}
```

### 2. 自动注册

识别到标记的线程池后，框架会：

1. 创建 `ExecutorWrapper` 包装线程池
2. 注册到 `DtpRegistry`
3. 启用感知器功能（监控、告警等）
4. 支持配置刷新

### 3. 功能增强

注册后的线程池会自动获得：

- **监控功能**: 自动采集线程池指标
- **告警功能**: 异常时自动告警
- **配置刷新**: 支持动态调整参数
- **统计功能**: 自动统计任务执行情况

## 注解属性

### value()

**类型**: `String`

**默认值**: `""`

**作用**: 指定线程池名称

**说明**:
- 如果指定了值，使用该值作为线程池名称
- 如果未指定（空字符串），使用 Bean 的方法名作为线程池名称
- 线程池名称优先级：`@DynamicTp` 的 `value` > Bean 方法名

## 使用场景

### 1. 管理现有线程池

如果项目中已有线程池 Bean，只需添加 `@DynamicTp` 注解即可被框架管理：

```java
@Bean
@DynamicTp("existingPool")
public ThreadPoolExecutor existingExecutor() {
    // 现有的线程池配置
    return new ThreadPoolExecutor(...);
}
```

### 2. 兼容性管理

对于不想修改代码的线程池，可以通过注解方式接入框架：

```java
@Bean
@DynamicTp
public ThreadPoolTaskExecutor springExecutor() {
    // Spring 线程池
    return new ThreadPoolTaskExecutor();
}
```

### 3. 统一管理

通过注解统一管理所有线程池，无需手动注册：

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    @DynamicTp("pool1")
    public ThreadPoolExecutor pool1() { ... }
    
    @Bean
    @DynamicTp("pool2")
    public ThreadPoolExecutor pool2() { ... }
    
    @Bean
    @DynamicTp("pool3")
    public ThreadPoolTaskExecutor pool3() { ... }
}
```

## 注意事项

### 1. 必须配合 Spring 使用

`@DynamicTp` 注解需要 Spring 的 Bean 后处理器支持，只能在 Spring 环境中使用。

### 2. 线程池类型支持

目前支持以下类型的线程池：

- `ThreadPoolExecutor`（JUC）
- `ThreadPoolTaskExecutor`（Spring）

### 3. 与 DtpExecutor 的区别

| 特性 | @DynamicTp + ThreadPoolExecutor | DtpExecutor |
|------|-------------------------------|-------------|
| **创建方式** | 手动创建 | 框架创建 |
| **功能** | 基础功能 | 完整功能 |
| **配置刷新** | 支持 | 支持 |
| **任务包装** | 支持 | 支持 |
| **超时监控** | 不支持 | 支持 |

### 4. Bean 名称优先级

线程池名称的优先级：

1. `@DynamicTp` 的 `value` 属性（如果指定）
2. Bean 的方法名（如果未指定 `value`）

## 示例

### 完整示例

```java
@Configuration
@EnableDynamicTp
public class ThreadPoolConfig {
    
    /**
     * 使用注解指定线程池名称
     */
    @Bean
    @DynamicTp("httpClientPool")
    public ThreadPoolExecutor httpClientExecutor() {
        return new ThreadPoolExecutor(
            10, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new NamedThreadFactory("http-client"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 使用 Bean 方法名作为线程池名称
     */
    @Bean
    @DynamicTp
    public ThreadPoolExecutor asyncTaskExecutor() {
        return new ThreadPoolExecutor(
            5, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100)
        );
    }
    
    /**
     * Spring 线程池
     */
    @Bean
    @DynamicTp("springPool")
    public ThreadPoolTaskExecutor springExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(150);
        return executor;
    }
}
```

## 总结

`@DynamicTp` 注解提供了一种简单的方式来管理现有线程池，无需修改业务代码，只需添加注解即可享受框架的监控、告警、配置刷新等功能。

