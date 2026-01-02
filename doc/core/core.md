# Core 模块

## 模块概述

`core` 模块是 DynamicTp 框架的核心模块，提供了动态线程池的核心功能实现，包括线程池管理、监控、告警、配置刷新等核心能力。

## 主要功能

### 1. 线程池管理

- **DtpRegistry**: 线程池注册中心，维护所有注册的动态线程池执行器
  - 提供线程池的注册、获取、刷新等功能
  - 支持自动注册和手动注册两种方式

- **DtpExecutor**: 动态线程池执行器，继承自 `ThreadPoolExecutor`
  - 支持运行时动态调整核心参数（核心线程数、最大线程数、存活时间等）
  - 提供任务包装功能，支持上下文传递
  - 支持多种拒绝策略
  - 支持任务超时监控

- **EagerDtpExecutor**: IO 密集型场景使用的线程池
  - 适用于 IO 密集型任务
  - 采用任务队列满时立即创建线程的策略

- **ScheduledDtpExecutor**: 调度线程池
  - 支持定时任务和周期性任务

- **OrderedDtpExecutor**: 有序线程池
  - 保证任务按提交顺序执行

### 2. 配置管理

- **配置刷新机制**: 支持从配置中心动态刷新线程池配置
  - `AbstractRefresher`: 抽象刷新器，定义了配置刷新的基本流程
  - 支持多种配置中心（Nacos、Apollo、Zookeeper 等）

- **配置转换**: 将配置中心的配置转换为线程池参数
  - `ExecutorConverter`: 执行器转换器，负责配置与执行器之间的转换

### 3. 监控模块

- **DtpMonitor**: 线程池监控管理器
  - 定时采集线程池指标数据
  - 支持多种监控指标（线程池维度、队列维度、任务维度、TPS、TPXX 等）
  - 提供监控数据收集接口

- **SystemMetricManager**: 系统指标管理器
  - 采集系统级别的指标（CPU、内存等）

### 4. 告警通知

- **AlarmManager**: 告警管理器
  - 监控线程池运行状态
  - 触发告警阈值时推送告警信息
  - 支持多种告警维度（活性、容量、拒绝、任务等待超时、任务执行超时）

- **NoticeManager**: 通知管理器
  - 配置变更时推送通知消息
  - 支持多种通知平台（企微、钉钉、飞书、邮件等）

### 5. 生命周期管理

- **DtpLifecycle**: 线程池生命周期管理
  - 管理线程池的启动、停止
  - 优雅关闭线程池
  - 在 Spring 容器关闭前尽可能处理队列中的任务

### 6. 任务增强

- **TaskWrapper**: 任务包装器接口
  - 支持任务包装功能，实现上下文传递
  - 支持 MDC、TTL、链路追踪等场景

- **TaskWrappers**: 任务包装器管理器
  - 管理多个任务包装器
  - 支持链式包装

### 7. 拒绝策略

- **RejectHandlerGetter**: 拒绝策略获取器
  - 提供多种内置拒绝策略
  - 支持自定义拒绝策略

### 8. 感知机制

- **AwareManager**: 感知管理器
  - 管理各种感知器（ExecutorAware）
  - 支持线程池状态变化感知
  - 按顺序执行所有感知器的增强逻辑

- **ExecutorAware**: 线程池感知器接口
  - 提供线程池生命周期各个阶段的增强点
  - 支持自定义扩展，通过 SPI 机制加载
  - 内置三种感知器：性能监控、任务超时、任务拒绝

## 核心类说明

### DtpRegistry

线程池注册中心，是框架的核心组件之一：

- `registerExecutor()`: 注册线程池
- `getExecutor()`: 获取指定名称的线程池
- `getAllExecutors()`: 获取所有注册的线程池
- `refresh()`: 刷新线程池配置

### DtpExecutor

动态线程池执行器，扩展了 `ThreadPoolExecutor`：

- 支持动态调整核心参数
- 支持任务包装
- 支持超时监控
- 支持多种拒绝策略

### DtpMonitor

监控管理器：

- 定时采集线程池指标
- 支持多种监控数据采集方式（Micrometer、Logging、JMX 等）
- 提供监控数据收集接口

## 依赖关系

- `dynamic-tp-common`: 依赖通用模块
- `dynamic-tp-logging`: 依赖日志模块
- `micrometer-core`: 监控指标采集（可选）
- `transmittable-thread-local`: 线程本地变量传递
- `equator`: 对象比较工具

## 使用场景

1. **动态线程池管理**: 通过配置中心动态调整线程池参数
2. **线程池监控**: 实时监控线程池运行状态
3. **告警通知**: 线程池异常时及时告警
4. **任务增强**: 支持任务包装，实现上下文传递
5. **优雅关闭**: 支持线程池优雅关闭

## ExecutorAware 详解

### 作用

`ExecutorAware` 是框架提供的线程池感知器接口，用于在线程池生命周期的各个关键节点进行增强处理。它提供了一种可扩展的机制，允许开发者在线程池执行任务的不同阶段插入自定义逻辑。

### 核心方法

`ExecutorAware` 接口提供了以下增强点：

1. **register(ExecutorWrapper wrapper)**: 线程池注册时调用
   - 当线程池注册到框架时触发
   - 可用于初始化感知器相关的资源

2. **refresh(ExecutorWrapper wrapper, TpExecutorProps props)**: 配置刷新时调用
   - 当线程池配置发生变化时触发
   - 可用于更新感知器的配置

3. **remove(ExecutorWrapper wrapper)**: 线程池移除时调用
   - 当线程池从框架中移除时触发
   - 可用于清理感知器相关的资源

4. **execute(Executor executor, Runnable r)**: 任务提交时调用
   - 在任务提交到线程池时触发
   - 可用于记录任务提交信息、启动监控等

5. **beforeExecute(Executor executor, Thread t, Runnable r)**: 任务执行前调用
   - 在线程开始执行任务前触发
   - 可用于设置线程上下文、启动任务计时等

6. **afterExecute(Executor executor, Runnable r, Throwable t)**: 任务执行后调用
   - 在线程执行完任务后触发
   - 可用于清理线程上下文、记录任务完成信息等

7. **shutdown(Executor executor)**: 线程池关闭时调用
   - 在线程池调用 shutdown() 时触发

8. **shutdownNow(Executor executor, List<Runnable> tasks)**: 线程池立即关闭时调用
   - 在线程池调用 shutdownNow() 时触发
   - 可获取被取消的任务列表

9. **terminated(Executor executor)**: 线程池终止时调用
   - 在线程池完全终止后触发

10. **beforeReject(Runnable r, Executor executor)**: 任务被拒绝前调用
    - 在任务被拒绝策略拒绝前触发
    - 可用于记录拒绝信息、触发告警等

11. **afterReject(Runnable r, Executor executor)**: 任务被拒绝后调用
    - 在任务被拒绝策略拒绝后触发

### 内置感知器

框架内置了三种感知器实现：

#### 1. PerformanceMonitorAware（性能监控感知器）

- **名称**: `monitor`
- **顺序**: 1
- **功能**: 
  - 在任务提交时启动任务统计
  - 在任务完成时记录任务完成信息
  - 在任务被拒绝时也记录完成信息
  - 用于性能监控和指标采集

#### 2. TaskTimeoutAware（任务超时感知器）

- **名称**: `timeout`
- **顺序**: 2
- **功能**:
  - 监控任务在队列中的等待时间（queueTimeout）
  - 监控任务执行时间（runTimeout）
  - 任务超时时可尝试中断任务（tryInterrupt）
  - 在任务提交时启动队列超时监控
  - 在任务执行前取消队列超时监控，启动执行超时监控
  - 在任务执行后取消执行超时监控

#### 3. TaskRejectAware（任务拒绝感知器）

- **名称**: `reject`
- **顺序**: 3
- **功能**:
  - 在任务被拒绝前触发
  - 记录拒绝次数
  - 触发拒绝告警
  - 记录详细的拒绝日志信息

### 使用方式

#### 1. 配置感知器

在线程池配置中指定需要启用的感知器：

```yaml
spring:
  dynamic:
    tp:
      executors:
        - threadPoolName: dtpExecutor1
          aware-names: [monitor, timeout, reject]  # 指定启用的感知器
```

如果不指定 `aware-names`，则默认启用所有感知器。

#### 2. 自定义感知器

通过实现 `ExecutorAware` 接口创建自定义感知器：

```java
public class CustomAware implements ExecutorAware {
    
    @Override
    public int getOrder() {
        return 10;  // 执行顺序
    }
    
    @Override
    public String getName() {
        return "custom";  // 感知器名称
    }
    
    @Override
    public void beforeExecute(Executor executor, Thread t, Runnable r) {
        // 自定义逻辑
        System.out.println("任务执行前: " + r);
    }
    
    @Override
    public void afterExecute(Executor executor, Runnable r, Throwable t) {
        // 自定义逻辑
        System.out.println("任务执行后: " + r);
    }
}
```

#### 3. SPI 注册

通过 SPI 机制注册自定义感知器：

1. 创建 `META-INF/services/org.dromara.dynamictp.core.aware.ExecutorAware` 文件
2. 在文件中写入实现类的全限定名：

```
com.example.CustomAware
```

#### 4. 编程式注册

也可以通过 `AwareManager.add()` 方法编程式注册：

```java
AwareManager.add(new CustomAware());
```

### 执行顺序

感知器按照 `getOrder()` 方法返回的顺序执行，顺序值越小越先执行。框架内置感知器的执行顺序：

1. PerformanceMonitorAware (order=1)
2. TaskTimeoutAware (order=2)
3. TaskRejectAware (order=3)

### 注意事项

1. **异常处理**: 每个感知器的执行都有异常保护，单个感知器异常不会影响其他感知器的执行
2. **性能影响**: 感知器会在每个任务执行的关键节点被调用，需要注意性能影响
3. **线程安全**: 感知器的实现需要考虑线程安全性
4. **顺序依赖**: 如果感知器之间有依赖关系，需要合理设置执行顺序

## 扩展点

- **配置中心**: 通过实现 `ConfigHandler` 接口支持自定义配置中心
- **监控采集**: 通过实现 `MetricsCollector` 接口支持自定义监控采集方式
- **通知告警**: 通过实现 `Notifier` 接口支持自定义通知平台
- **任务包装**: 通过实现 `TaskWrapper` 接口支持自定义任务包装逻辑
- **拒绝策略**: 通过实现 `RejectedExecutionHandler` 接口支持自定义拒绝策略
- **感知器**: 通过实现 `ExecutorAware` 接口支持自定义线程池感知逻辑

