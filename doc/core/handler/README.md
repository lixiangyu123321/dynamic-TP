# Handler 模块文档

`core/handler` 目录包含了框架的处理器相关功能，包括配置处理、指标收集处理、通知处理等。

## 目录结构

```
handler/
├── ConfigHandler.java         # 配置处理器
├── CollectorHandler.java      # 收集器处理器
└── NotifierHandler.java       # 通知器处理器
```

## 模块说明

### 核心处理器

- **[ConfigHandler](ConfigHandler.md)**: 配置处理器，负责解析配置文件
- **[CollectorHandler](CollectorHandler.md)**: 收集器处理器，负责管理指标收集器
- **[NotifierHandler](NotifierHandler.md)**: 通知器处理器，负责管理通知器

## 快速导航

- 想了解配置解析？查看 [ConfigHandler](ConfigHandler.md)
- 想了解指标收集？查看 [CollectorHandler](CollectorHandler.md)
- 想了解通知处理？查看 [NotifierHandler](NotifierHandler.md)

