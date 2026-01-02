# Adapter 模块

## 模块概述

`adapter` 模块是 DynamicTp 框架的适配器模块，用于集成和管理第三方中间件内部的线程池。通过适配器模式，框架可以统一管理各种中间件的线程池，实现动态调参、监控、告警等功能。

## 主要功能

### 1. 适配器抽象

- **DtpAdapter**: 适配器接口
  - 定义适配器的基本功能
  - 提供线程池获取、刷新、监控等方法

- **AbstractDtpAdapter**: 适配器抽象基类
  - 实现适配器的通用逻辑
  - 处理配置刷新、监控采集等通用功能
  - 支持事件监听机制

- **DtpAdapterListener**: 适配器事件监听器
  - 监听配置刷新、监控采集、告警检查等事件
  - 统一处理所有适配器的事件

### 2. 支持的中间件

框架已集成以下中间件的线程池管理：

#### Web 服务器
- **Tomcat**: Tomcat 线程池适配器
- **Jetty**: Jetty 线程池适配器
- **Undertow**: Undertow 线程池适配器

#### RPC 框架
- **Dubbo**: Dubbo 线程池适配器
- **Grpc**: gRPC 线程池适配器
- **Motan**: Motan 线程池适配器
- **Brpc**: Brpc 线程池适配器
- **Tars**: Tars 线程池适配器
- **Sofa**: SofaRpc 线程池适配器
- **Thrift**: Thrift 线程池适配器

#### 消息队列
- **RocketMQ**: RocketMQ 线程池适配器
- **RabbitMQ**: RabbitMQ 线程池适配器

#### 其他
- **Hystrix**: Hystrix 线程池适配器
- **OkHttp3**: OkHttp3 线程池适配器
- **Liteflow**: Liteflow 线程池适配器

### 3. 适配器功能

每个适配器提供以下功能：

1. **线程池发现**: 自动发现中间件内部的线程池实例
2. **动态调参**: 支持运行时动态调整线程池参数
3. **监控采集**: 采集线程池运行指标
4. **告警通知**: 线程池异常时触发告警
5. **配置刷新**: 响应配置中心的配置变更

## 子模块说明

### adapter-common

适配器通用模块，提供适配器的公共功能：

- `AbstractDtpAdapter`: 适配器抽象基类
- `DtpAdapterListener`: 适配器事件监听器
- 适配器相关的工具类和接口

### adapter-dubbo

Dubbo 线程池适配器：

- 适配 Dubbo 的线程池
- 支持动态调整 Dubbo 线程池参数

### adapter-rocketmq

RocketMQ 线程池适配器：

- 适配 RocketMQ 的线程池
- 支持动态调整 RocketMQ 线程池参数

### adapter-hystrix

Hystrix 线程池适配器：

- 适配 Hystrix 的线程池
- 支持动态调整 Hystrix 线程池参数

### adapter-grpc

gRPC 线程池适配器：

- 适配 gRPC 的线程池
- 支持动态调整 gRPC 线程池参数

### adapter-motan

Motan 线程池适配器：

- 适配 Motan 的线程池
- 支持动态调整 Motan 线程池参数

### adapter-okhttp3

OkHttp3 线程池适配器：

- 适配 OkHttp3 的连接池
- 支持动态调整 OkHttp3 连接池参数

### adapter-brpc

Brpc 线程池适配器：

- 适配 Brpc 的线程池
- 支持动态调整 Brpc 线程池参数

### adapter-tars

Tars 线程池适配器：

- 适配 Tars 的线程池
- 支持动态调整 Tars 线程池参数

### adapter-sofa

SofaRpc 线程池适配器：

- 适配 SofaRpc 的线程池
- 支持动态调整 SofaRpc 线程池参数

### adapter-rabbitmq

RabbitMQ 线程池适配器：

- 适配 RabbitMQ 的线程池
- 支持动态调整 RabbitMQ 线程池参数

### adapter-liteflow

Liteflow 线程池适配器：

- 适配 Liteflow 的线程池
- 支持动态调整 Liteflow 线程池参数

### adapter-thrift

Thrift 线程池适配器：

- 适配 Thrift 的线程池
- 支持动态调整 Thrift 线程池参数
- 使用 JVMTI 技术获取线程池实例

## 核心类说明

### AbstractDtpAdapter

适配器抽象基类，提供通用功能：

- `initialize()`: 初始化适配器
- `refresh()`: 刷新线程池配置
- `getExecutorWrappers()`: 获取所有线程池包装器
- `getMultiPoolStats()`: 获取所有线程池的统计信息

### DtpAdapterListener

适配器事件监听器：

- `handleDtpEvent()`: 处理动态线程池事件
- `doRefresh()`: 执行配置刷新
- `doCollect()`: 执行监控采集
- `doAlarmCheck()`: 执行告警检查

## 工作原理

1. **自动发现**: 适配器在 Spring 容器初始化时自动发现中间件的线程池实例
2. **注册管理**: 将发现的线程池注册到 `DtpRegistry` 进行统一管理
3. **配置刷新**: 监听配置中心的变化，动态刷新线程池参数
4. **监控采集**: 定时采集线程池指标，支持多种监控方式
5. **告警通知**: 监控线程池状态，异常时触发告警

## 依赖关系

- `dynamic-tp-core`: 依赖核心模块
- 各中间件的客户端依赖（如 Dubbo、RocketMQ 等）

## 使用场景

1. **统一管理**: 统一管理应用中所有中间件的线程池
2. **动态调参**: 运行时动态调整中间件线程池参数，无需重启
3. **监控告警**: 实时监控中间件线程池状态，异常及时告警
4. **性能优化**: 根据实际负载动态调整线程池参数，优化性能

## 扩展方式

可以通过实现 `DtpAdapter` 接口来扩展支持新的中间件：

1. 实现 `DtpAdapter` 接口
2. 实现线程池发现逻辑
3. 实现配置刷新逻辑
4. 注册为 Spring Bean

