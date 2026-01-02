# Timer 模块文档

`core/timer` 目录包含了框架的超时定时任务相关功能，包括队列超时、执行超时等。

## 目录结构

```
timer/
├── AbstractTimeoutTimerTask.java  # 抽象超时定时任务
├── QueueTimeoutTimerTask.java    # 队列超时定时任务
└── RunTimeoutTimerTask.java      # 执行超时定时任务
```

## 模块说明

### 核心类

- **[AbstractTimeoutTimerTask](AbstractTimeoutTimerTask.md)**: 抽象超时定时任务基类
- **[QueueTimeoutTimerTask](QueueTimeoutTimerTask.md)**: 队列超时定时任务
- **[RunTimeoutTimerTask](RunTimeoutTimerTask.md)**: 执行超时定时任务

## 快速导航

- 想了解超时任务基类？查看 [AbstractTimeoutTimerTask](AbstractTimeoutTimerTask.md)
- 想了解队列超时？查看 [QueueTimeoutTimerTask](QueueTimeoutTimerTask.md)
- 想了解执行超时？查看 [RunTimeoutTimerTask](RunTimeoutTimerTask.md)

