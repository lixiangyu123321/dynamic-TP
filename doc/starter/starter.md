# Starter 模块

## 模块概述

`starter` 模块是 DynamicTp 框架的 Spring Boot 启动器模块，提供了 Spring Boot 自动配置功能，简化框架的集成和使用。

## 主要功能

### 1. 自动配置

- **DtpBootBeanConfiguration**: Spring Boot 自动配置类
  - 自动配置框架的核心 Bean
  - 初始化线程池注册中心
  - 配置监控和告警组件

- **DtpApplicationContextInitializer**: 应用上下文初始化器
  - 在 Spring 容器初始化时进行框架初始化
  - 设置上下文管理器

### 2. 配置中心集成

提供多种配置中心的 Spring Boot Starter：

#### Nacos
- **starter-nacos**: Nacos 配置中心集成
- **cloud-starter-nacos**: Spring Cloud Nacos 集成

#### Apollo
- **starter-apollo**: Apollo 配置中心集成

#### Zookeeper
- **starter-zookeeper**: Zookeeper 配置中心集成
- **cloud-starter-zookeeper**: Spring Cloud Zookeeper 集成

#### Etcd
- **starter-etcd**: Etcd 配置中心集成

#### Consul
- **cloud-starter-consul**: Spring Cloud Consul 集成

#### Polaris
- **cloud-starter-polaris**: Spring Cloud Polaris 集成

#### Huawei Cloud
- **cloud-starter-huawei**: 华为云配置中心集成

### 3. 适配器集成

提供各种中间件适配器的 Spring Boot Starter：

- **starter-adapter-common**: 适配器通用配置
- **starter-adapter-dubbo**: Dubbo 适配器
- **starter-adapter-rocketmq**: RocketMQ 适配器
- **starter-adapter-hystrix**: Hystrix 适配器
- **starter-adapter-grpc**: gRPC 适配器
- **starter-adapter-motan**: Motan 适配器
- **starter-adapter-okhttp3**: OkHttp3 适配器
- **starter-adapter-brpc**: Brpc 适配器
- **starter-adapter-tars**: Tars 适配器
- **starter-adapter-sofa**: SofaRpc 适配器
- **starter-adapter-rabbitmq**: RabbitMQ 适配器
- **starter-adapter-liteflow**: Liteflow 适配器
- **starter-adapter-thrift**: Thrift 适配器
- **starter-adapter-webserver**: Web 服务器适配器（Tomcat、Jetty、Undertow）

### 4. 扩展功能集成

- **starter-extension**: 扩展功能集成
  - 集成各种扩展功能（限流、通知、链路追踪等）

## 子模块说明

### starter-common

Spring Boot 通用启动器：

- 提供框架的核心自动配置
- 集成 Spring Boot Actuator
- 提供配置属性绑定

### starter-configcenter

配置中心启动器集合：

- 提供各种配置中心的自动配置
- 简化配置中心的集成

### starter-adapter

适配器启动器集合：

- 提供各种中间件适配器的自动配置
- 简化适配器的集成

### starter-extension

扩展功能启动器：

- 集成各种扩展功能
- 提供扩展功能的自动配置

## 核心类说明

### DtpBootBeanConfiguration

Spring Boot 自动配置类：

- 配置框架的核心 Bean
- 初始化线程池注册中心
- 配置监控和告警组件

### DtpApplicationContextInitializer

应用上下文初始化器：

- 在 Spring 容器初始化时进行框架初始化
- 设置 Spring 上下文管理器

## 工作原理

1. **自动配置**: 通过 Spring Boot 的自动配置机制，自动配置框架组件
2. **条件装配**: 根据依赖和配置条件，自动装配相应的组件
3. **SPI 机制**: 使用 SPI 机制加载扩展实现
4. **事件驱动**: 通过 Spring 事件机制触发框架初始化

## 依赖关系

- `dynamic-tp-core`: 依赖核心模块
- `dynamic-tp-logging`: 依赖日志模块
- `dynamic-tp-spring`: 依赖 Spring 集成模块
- `spring-boot-starter`: Spring Boot 启动器
- `spring-boot-starter-actuator`: Spring Boot Actuator

## 使用场景

1. **快速集成**: 通过引入相应的 Starter，快速集成框架
2. **自动配置**: 无需手动配置，框架自动完成初始化
3. **按需加载**: 根据引入的依赖，自动加载相应的功能模块
4. **配置简化**: 通过 Spring Boot 配置属性，简化配置

## 使用方式

### 1. 引入依赖

```xml
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-spring-boot-starter-common</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 引入配置中心 Starter

```xml
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-spring-boot-starter-nacos</artifactId>
    <version>${version}</version>
</dependency>
```

### 3. 引入适配器 Starter

```xml
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-spring-boot-starter-adapter-dubbo</artifactId>
    <version>${version}</version>
</dependency>
```

### 4. 启用框架

在启动类上添加 `@EnableDynamicTp` 注解：

```java
@EnableDynamicTp
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 配置说明

框架支持通过 `application.yml` 或 `application.properties` 进行配置：

```yaml
spring:
  dynamic:
    tp:
      enabled: true
      config-center-type: nacos
      executors:
        - threadPoolName: dtpExecutor1
          corePoolSize: 10
          maximumPoolSize: 20
          queueCapacity: 200
```

