# DynamicTp 模块文档

本文档目录包含了 DynamicTp 框架各个模块的详细说明文档。

## 模块列表

### 核心模块

- **[core](core/core.md)**: 核心模块，提供动态线程池的核心功能实现
- **[common](common/common.md)**: 通用模块，提供框架共享的基础工具类和实体类

### 集成模块

- **[spring](spring/spring.md)**: Spring 集成模块，提供与 Spring 框架的深度集成
- **[starter](starter/starter.md)**: Spring Boot 启动器模块，提供自动配置功能

### 适配器模块

- **[adapter](adapter/adapter.md)**: 适配器模块，集成第三方中间件的线程池管理
  - 支持 Dubbo、RocketMQ、Hystrix、gRPC、Motan、OkHttp3、Brpc、Tars、SofaRpc、RabbitMQ、Liteflow、Thrift 等中间件
  - 支持 Tomcat、Jetty、Undertow 等 Web 服务器

### 功能模块

- **[logging](logging/logging.md)**: 日志模块，提供线程池监控数据的日志采集
- **[extension](extension/extension.md)**: 扩展模块，提供限流、通知、链路追踪等扩展功能
  - Redis 限流扩展
  - 邮件通知扩展
  - 云之家通知扩展
  - SkyWalking 链路追踪扩展
  - OpenTelemetry 链路追踪扩展
  - Agent 扩展

### 技术模块

- **[jvmti](jvmti/jvmti.md)**: JVMTI 技术模块，使用 JVMTI 技术实现高级功能

### 测试模块

- **[benchmark](benchmark/benchmark.md)**: 性能基准测试模块，使用 JMH 进行性能测试

## 模块关系

```
┌─────────────┐
│   starter   │  Spring Boot 启动器
└──────┬──────┘
       │
       ├───┐
       │   │
┌──────▼───▼──────┐
│      core       │  核心模块
└──────┬──────────┘
       │
       ├───┐
       │   │
┌──────▼───▼──────┐
│     common      │  通用模块
└─────────────────┘

┌─────────────┐
│   adapter   │  适配器模块（依赖 core）
└─────────────┘

┌─────────────┐
│   spring    │  Spring 集成（依赖 core）
└─────────────┘

┌─────────────┐
│   logging   │  日志模块（依赖 core）
└─────────────┘

┌─────────────┐
│  extension  │  扩展模块（依赖 core）
└─────────────┘

┌─────────────┐
│    jvmti    │  JVMTI 模块（独立）
└─────────────┘
```

## 快速导航

- 想了解框架的核心功能？查看 [core](core/core.md)
- 想了解如何集成框架？查看 [starter](starter/starter.md) 和 [spring](spring/spring.md)
- 想了解如何集成第三方中间件？查看 [adapter](adapter/adapter.md)
- 想了解扩展功能？查看 [extension](extension/extension.md)
- 想了解日志采集？查看 [logging](logging/logging.md)
- 想了解性能测试？查看 [benchmark](benchmark/benchmark.md)

## 文档说明

每个模块的文档包含以下内容：

1. **模块概述**: 模块的作用和定位
2. **主要功能**: 模块提供的核心功能
3. **核心类说明**: 重要类的说明
4. **依赖关系**: 模块的依赖情况
5. **使用场景**: 模块的使用场景
6. **配置说明**: 配置相关的说明（如适用）

## 贡献

如有文档问题或建议，欢迎提交 Issue 或 Pull Request。

