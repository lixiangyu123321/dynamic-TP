# Executor 模块文档

`core/executor` 目录包含了框架的各种线程池执行器实现，包括基础执行器、IO 密集型执行器、有序执行器、调度执行器、优先级执行器等。

## 目录结构

```
executor/
├── DtpExecutor.java                    # 动态线程池执行器（基础）
├── NamedThreadFactory.java             # 命名线程工厂
├── ExecutorType.java                   # 执行器类型枚举
├── OrderedDtpExecutor.java             # 有序线程池执行器
├── ScheduledDtpExecutor.java           # 调度线程池执行器
├── eager/
│   ├── EagerDtpExecutor.java          # IO 密集型线程池执行器
│   └── TaskQueue.java                  # 任务队列
└── priority/
    ├── Priority.java                   # 优先级接口
    ├── PriorityDtpExecutor.java        # 优先级线程池执行器
    ├── PriorityRunnable.java           # 优先级 Runnable
    ├── PriorityCallable.java            # 优先级 Callable
    └── PriorityFutureTask.java          # 优先级 FutureTask
```

## 模块说明

### 核心执行器

- **[DtpExecutor](DtpExecutor.md)**: 动态线程池执行器，框架的基础执行器
- **[NamedThreadFactory](NamedThreadFactory.md)**: 命名线程工厂，为线程提供有意义的名称

### 特殊执行器

- **[OrderedDtpExecutor](OrderedDtpExecutor.md)**: 有序线程池，保证相同 key 的任务按顺序执行
- **[ScheduledDtpExecutor](ScheduledDtpExecutor.md)**: 调度线程池，支持定时和周期性任务
- **[EagerDtpExecutor](eager/EagerDtpExecutor.md)**: IO 密集型线程池，队列满时立即创建线程
- **[PriorityDtpExecutor](priority/PriorityDtpExecutor.md)**: 优先级线程池，支持按优先级执行任务

### 辅助类

- **[ExecutorType](ExecutorType.md)**: 执行器类型枚举

## 快速导航

- 想了解基础执行器？查看 [DtpExecutor](DtpExecutor.md)
- 想了解有序执行？查看 [OrderedDtpExecutor](OrderedDtpExecutor.md)
- 想了解调度执行？查看 [ScheduledDtpExecutor](ScheduledDtpExecutor.md)
- 想了解 IO 密集型执行？查看 [EagerDtpExecutor](eager/EagerDtpExecutor.md)
- 想了解优先级执行？查看 [PriorityDtpExecutor](priority/PriorityDtpExecutor.md)

