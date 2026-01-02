# Extension 模块

## 模块概述

`extension` 模块是 DynamicTp 框架的扩展模块，提供了各种扩展功能的实现，包括限流、通知、链路追踪等功能。这些扩展功能通过 SPI 机制集成到框架中，用户可以根据需要选择使用。

## 主要功能

### 1. 限流扩展

- **extension-limiter-redis**: Redis 限流扩展
  - 基于 Redis 实现的分布式限流
  - 支持多种限流算法（令牌桶、漏桶等）
  - 提供限流器接口和实现

### 2. 通知扩展

- **extension-notify-email**: 邮件通知扩展
  - 支持通过邮件发送告警和通知
  - 提供邮件模板
  - 支持 HTML 格式邮件

- **extension-notify-yunzhijia**: 云之家通知扩展
  - 支持通过云之家发送告警和通知
  - 集成云之家 API

### 3. 链路追踪扩展

- **extension-skywalking**: SkyWalking 链路追踪扩展
  - 集成 SkyWalking 链路追踪
  - 支持线程池任务的链路追踪
  - 提供 TaskWrapper 实现

- **extension-opentelemetry**: OpenTelemetry 链路追踪扩展
  - 集成 OpenTelemetry 链路追踪
  - 支持线程池任务的链路追踪
  - 提供 TaskWrapper 实现

### 4. Agent 扩展

- **extension-agent**: Agent 扩展
  - 提供 Agent 相关的功能
  - 支持通过 Agent 方式集成框架

## 子模块说明

### extension-limiter-redis

Redis 限流扩展：

- **RedisRateLimiter**: Redis 限流器实现
  - 基于 Redis 的分布式限流
  - 支持多种限流算法
  - 提供 Lua 脚本实现原子操作

- **RateLimitEnum**: 限流算法枚举
  - 定义支持的限流算法类型

功能特点：

1. **分布式限流**: 基于 Redis 实现分布式限流
2. **多种算法**: 支持令牌桶、漏桶等限流算法
3. **原子操作**: 使用 Lua 脚本保证原子性
4. **高性能**: 基于 Redis 的高性能限流

### extension-notify-email

邮件通知扩展：

- **EmailNotifier**: 邮件通知器实现
  - 发送邮件告警和通知
  - 支持 HTML 格式邮件
  - 提供邮件模板

功能特点：

1. **邮件发送**: 支持通过 SMTP 发送邮件
2. **HTML 模板**: 提供 HTML 格式的邮件模板
3. **告警通知**: 线程池异常时发送邮件告警
4. **配置变更通知**: 配置变更时发送通知邮件

### extension-notify-yunzhijia

云之家通知扩展：

- **YunzhijiaNotifier**: 云之家通知器实现
  - 集成云之家 API
  - 发送告警和通知消息

功能特点：

1. **云之家集成**: 集成云之家企业通讯平台
2. **消息推送**: 支持推送告警和通知消息
3. **企业通讯**: 适用于企业内部通讯场景

### extension-skywalking

SkyWalking 链路追踪扩展：

- **SwTraceTaskWrapper**: SkyWalking 任务包装器
  - 包装线程池任务，支持链路追踪
  - 传递 SkyWalking 上下文

功能特点：

1. **链路追踪**: 支持线程池任务的链路追踪
2. **上下文传递**: 自动传递 SkyWalking 上下文
3. **任务包装**: 通过 TaskWrapper 机制实现

### extension-opentelemetry

OpenTelemetry 链路追踪扩展：

- **OpenTelemetryTaskWrapper**: OpenTelemetry 任务包装器
  - 包装线程池任务，支持链路追踪
  - 传递 OpenTelemetry 上下文

功能特点：

1. **链路追踪**: 支持线程池任务的链路追踪
2. **上下文传递**: 自动传递 OpenTelemetry 上下文
3. **标准协议**: 基于 OpenTelemetry 标准协议

### extension-agent

Agent 扩展：

- **AgentAware**: Agent 感知器
  - 提供 Agent 相关的感知功能
  - 支持通过 Agent 方式集成

功能特点：

1. **Agent 集成**: 支持通过 Java Agent 方式集成
2. **无侵入**: 无需修改代码即可集成
3. **自动发现**: 自动发现和管理线程池

## 核心类说明

### RedisRateLimiter

Redis 限流器：

- `tryAcquire()`: 尝试获取限流许可
- `acquire()`: 获取限流许可（阻塞）
- 支持多种限流算法

### EmailNotifier

邮件通知器：

- `sendAlarm()`: 发送告警邮件
- `sendNotice()`: 发送通知邮件
- 支持 HTML 格式邮件

### SwTraceTaskWrapper

SkyWalking 任务包装器：

- `wrap()`: 包装任务，传递链路追踪上下文
- 自动处理 SkyWalking 上下文传递

### OpenTelemetryTaskWrapper

OpenTelemetry 任务包装器：

- `wrap()`: 包装任务，传递链路追踪上下文
- 自动处理 OpenTelemetry 上下文传递

## 使用方式

### 1. 引入依赖

```xml
<!-- Redis 限流 -->
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-extension-limiter-redis</artifactId>
    <version>${version}</version>
</dependency>

<!-- 邮件通知 -->
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-extension-notify-email</artifactId>
    <version>${version}</version>
</dependency>

<!-- SkyWalking 链路追踪 -->
<dependency>
    <groupId>org.dromara.dynamictp</groupId>
    <artifactId>dynamic-tp-extension-skywalking</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置扩展功能

```yaml
spring:
  dynamic:
    tp:
      # 启用限流
      limiter:
        enabled: true
        type: redis
      
      # 启用邮件通知
      notify:
        email:
          enabled: true
          smtp-host: smtp.example.com
          smtp-port: 587
          username: user@example.com
          password: password
      
      # 启用链路追踪
      task-wrappers:
        - skywalking
```

## 扩展机制

框架通过 SPI 机制支持扩展：

1. **接口定义**: 框架定义扩展接口
2. **SPI 加载**: 通过 SPI 机制加载扩展实现
3. **自动集成**: 扩展实现自动集成到框架中

### 自定义扩展

可以通过实现相应的接口来扩展功能：

1. **限流扩展**: 实现 `RateLimiter` 接口
2. **通知扩展**: 实现 `Notifier` 接口
3. **链路追踪扩展**: 实现 `TaskWrapper` 接口
4. **Agent 扩展**: 实现 `ExecutorAware` 接口

## 依赖关系

- `dynamic-tp-core`: 依赖核心模块
- 各扩展功能的具体依赖（如 Redis、邮件客户端、链路追踪 SDK 等）

## 使用场景

1. **分布式限流**: 在分布式环境下实现限流功能
2. **多渠道通知**: 通过多种渠道发送告警和通知
3. **链路追踪**: 在微服务场景下追踪线程池任务的调用链
4. **Agent 集成**: 通过 Agent 方式无侵入集成框架

## 注意事项

1. **依赖管理**: 注意管理扩展功能的依赖，避免冲突
2. **性能影响**: 某些扩展功能可能对性能有影响，需要评估
3. **配置正确性**: 确保扩展功能的配置正确
4. **版本兼容**: 注意扩展功能与框架版本的兼容性

