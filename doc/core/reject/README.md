# Reject 模块文档

`core/reject` 目录包含了框架的拒绝策略相关功能，包括拒绝策略获取、拒绝策略代理等。

## 目录结构

```
reject/
├── RejectHandlerGetter.java           # 拒绝策略获取器
└── RejectedInvocationHandler.java      # 拒绝策略调用处理器
```

## 模块说明

### 核心类

- **[RejectHandlerGetter](RejectHandlerGetter.md)**: 拒绝策略获取器，负责创建和代理拒绝策略
- **[RejectedInvocationHandler](RejectedInvocationHandler.md)**: 拒绝策略调用处理器，用于代理拒绝策略

## 快速导航

- 想了解拒绝策略获取？查看 [RejectHandlerGetter](RejectHandlerGetter.md)
- 想了解拒绝策略代理？查看 [RejectedInvocationHandler](RejectedInvocationHandler.md)

