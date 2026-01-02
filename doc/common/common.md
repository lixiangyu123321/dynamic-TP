# Common 模块

## 模块概述

`common` 模块是 DynamicTp 框架的通用模块，提供了框架各模块共享的基础工具类、实体类、常量定义等通用功能。

## 主要功能

### 1. 工具类

- **CommonUtil**: 通用工具类
  - 获取应用名称、环境、端口等信息
  - 网络相关工具方法

- **JsonUtil**: JSON 工具类
  - 提供 JSON 序列化和反序列化功能
  - 支持多种 JSON 库（Jackson、Gson、FastJson）
  - 使用 SPI 机制自动检测可用的 JSON 库

- **StringUtil**: 字符串工具类
  - 字符串处理相关工具方法

- **ReflectionUtil**: 反射工具类
  - 反射操作相关工具方法

- **StreamUtil**: 流工具类
  - Stream 操作相关工具方法

- **UUIDUtil**: UUID 工具类
  - 生成 UUID 的工具方法

- **VersionUtil**: 版本工具类
  - 版本号比较和处理相关工具方法

### 2. 实体类

- **DtpProperties**: 动态线程池配置属性
  - 包含所有线程池配置信息
  - 支持从配置文件或配置中心加载

- **DtpExecutorProps**: 线程池执行器属性
  - 单个线程池的配置信息
  - 包含核心线程数、最大线程数、队列类型等参数

- **ThreadPoolStats**: 线程池统计信息
  - 线程池运行时的各项指标数据
  - 包含线程池维度、队列维度、任务维度等指标

- **ServiceInstance**: 服务实例信息
  - 服务实例的基本信息（IP、端口、服务名等）

- **TpMainFields**: 线程池主要字段
  - 线程池的核心参数封装

### 3. 管理器

- **ContextManager**: 上下文管理器接口
  - 提供获取 Bean、环境变量等功能
  - 支持多种实现（Spring、非 Spring 环境）

- **ContextManagerHelper**: 上下文管理器辅助类
  - 提供便捷的上下文访问方法

- **EventBusManager**: 事件总线管理器
  - 基于 Google Guava EventBus 实现
  - 支持事件发布和订阅
  - 用于模块间通信

### 4. 队列实现

- **VariableLinkedBlockingQueue**: 可变容量阻塞队列
  - 支持动态调整队列容量
  - 用于动态线程池的队列管理

- **MemorySafeLinkedBlockingQueue**: 内存安全阻塞队列
  - 防止队列过大导致内存溢出
  - 提供内存保护机制

### 5. JSON 解析器

- **JsonParser**: JSON 解析器接口
  - 定义 JSON 序列化和反序列化的标准接口
  - 支持多种实现（Jackson、Gson、FastJson）

### 6. 扩展加载

- **[ExtensionServiceLoader](util/ExtensionServiceLoader.md)**: 扩展服务加载器
  - 统一的 SPI 服务加载工具
  - 支持扩展点的动态加载
  - 提供缓存机制，避免重复加载

### 7. 异常定义

- **DtpException**: 框架自定义异常
  - 框架内部使用的异常类

### 8. 事件定义

- **RefreshEvent**: 配置刷新事件
- **CollectEvent**: 监控采集事件
- **AlarmCheckEvent**: 告警检查事件
- **CustomContextRefreshedEvent**: 上下文刷新事件

### 9. 常量定义

- **DynamicTpConst**: 框架常量定义
  - 包含各种常量值（配置键、默认值等）

## 核心类说明

### JsonUtil

JSON 工具类，支持多种 JSON 库：

- 自动检测 Classpath 中可用的 JSON 库
- 支持 Jackson、Gson、FastJson
- 提供统一的序列化和反序列化接口

### ContextManagerHelper

上下文管理器辅助类：

- `getBean()`: 获取 Spring Bean
- `getBeansOfType()`: 获取指定类型的所有 Bean
- `getEnvironment()`: 获取环境配置
- `getEnvironmentProperty()`: 获取环境属性

### EventBusManager

事件总线管理器：

- `register()`: 注册事件监听器
- `post()`: 发布事件
- `destroy()`: 销毁事件总线

## 依赖关系

- `slf4j-api`: 日志接口
- `jackson-core`, `jackson-databind`: Jackson JSON 库（可选）
- `gson`: Gson JSON 库（可选）
- `fastjson`: FastJson JSON 库（可选）
- `hutool-http`: HTTP 工具库
- `byte-buddy`: 字节码操作库
- `snakeyaml`: YAML 解析库

## 使用场景

1. **JSON 处理**: 框架内部 JSON 序列化和反序列化
2. **上下文管理**: 在非 Spring 环境下提供上下文访问能力
3. **事件通信**: 模块间通过事件总线进行通信
4. **工具方法**: 提供各种通用工具方法供其他模块使用

## 设计特点

1. **SPI 机制**: 使用 SPI 机制实现扩展点，支持多种实现
2. **可选依赖**: JSON 库等依赖为可选，运行时自动检测
3. **环境适配**: 支持 Spring 和非 Spring 环境
4. **事件驱动**: 使用事件总线实现模块间解耦

