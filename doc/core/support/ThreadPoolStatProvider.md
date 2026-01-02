# ThreadPoolStatProvider

## 概述

`ThreadPoolStatProvider` 是线程池统计信息提供者，为每个线程池提供统一的统计功能。它是框架中任务统计、性能监控、超时监控等功能的底层支撑组件。

> 详细说明请参考：[ThreadPoolStatProvider-作用说明](../ThreadPoolStatProvider-作用说明.md)

## 核心作用

1. **任务性能统计**: 记录任务的执行时间，计算 TPS、响应时间分布等性能指标
2. **超时监控**: 监控任务在队列中的等待时间和执行时间，支持超时告警和中断
3. **拒绝统计**: 统计被拒绝的任务数量
4. **超时统计**: 统计执行超时和队列等待超时的任务数量

## 快速导航

- [详细说明文档](../ThreadPoolStatProvider-作用说明.md)

