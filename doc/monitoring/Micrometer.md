# Micrometer 应用监控度量工具

## 概述

**Micrometer** 是一款开源的应用监控度量工具，定位为 Java 应用的**度量门面（Metrics Facade）**。其核心目标是统一不同监控系统的度量接口，让开发者可以用一套 API 采集应用指标，无需关心底层监控系统的实现差异。

### 核心定位

- **度量门面**: 类似于 SLF4J 在日志领域的作用，Micrometer 在监控领域提供统一的抽象接口
- **多系统支持**: 支持 Prometheus、InfluxDB、CloudWatch、Datadog、New Relic 等多种监控系统
- **标准化**: 提供标准化的指标采集 API，简化监控集成

### 主要特性

1. **统一 API**: 一套 API 适配多种监控系统
2. **丰富指标类型**: 支持 Counter、Gauge、Timer、Summary 等多种指标类型
3. **标签支持**: 强大的标签（Tag）系统，支持多维度的指标分类
4. **自动集成**: 与 Spring Boot Actuator 深度集成
5. **高性能**: 低开销的指标采集，适合生产环境

## 核心概念

### 1. MeterRegistry（度量注册表）

`MeterRegistry` 是 Micrometer 的核心接口，用于注册和管理指标。不同的监控系统有对应的 Registry 实现：

- `PrometheusMeterRegistry`: Prometheus 注册表
- `InfluxMeterRegistry`: InfluxDB 注册表
- `CloudWatchMeterRegistry`: AWS CloudWatch 注册表
- `CompositeMeterRegistry`: 组合注册表，可同时向多个系统发送指标

### 2. Meter（度量器）

Meter 是具体的指标实例，Micrometer 支持以下类型：

#### Counter（计数器）

用于记录单调递增的数值，如请求总数、错误总数等。

```java
Counter counter = Counter.builder("http.requests")
    .tag("status", "200")
    .register(registry);
counter.increment();
```

#### Gauge（仪表）

用于记录当前时刻的瞬时值，如当前线程数、队列大小等。

```java
Gauge.builder("thread.pool.size", threadPool, ThreadPoolExecutor::getPoolSize)
    .tag("pool", "dtpExecutor1")
    .register(registry);
```

#### Timer（计时器）

用于记录耗时操作，自动计算速率、平均值、分位数等。

```java
Timer timer = Timer.builder("http.request.duration")
    .tag("method", "GET")
    .register(registry);
timer.record(Duration.ofMillis(100));
```

#### Summary（摘要）

用于记录分布式的数值，如响应大小、消息大小等。

```java
DistributionSummary summary = DistributionSummary.builder("response.size")
    .tag("endpoint", "/api/users")
    .register(registry);
summary.record(1024);
```

### 3. Tag（标签）

标签用于对指标进行多维度的分类和过滤，是 Micrometer 强大的特性之一。

```java
List<Tag> tags = Arrays.asList(
    Tag.of("app.name", "my-app"),
    Tag.of("thread.pool.name", "dtpExecutor1"),
    Tag.of("thread.pool.alias", "异步线程池")
);
```

## 在 DynamicTp 中的使用

### MicroMeterCollector

DynamicTp 通过 `MicroMeterCollector` 将线程池指标注册到 Micrometer，支持将指标导出到各种监控系统。

### 核心实现

```java
public class MicroMeterCollector extends AbstractCollector {
    
    // 指标名称前缀
    public static final String DTP_METRIC_NAME_PREFIX = "thread.pool";
    
    // Gauge 缓存，保证引用同一个对象
    private static final Map<String, ThreadPoolStats> GAUGE_CACHE = new ConcurrentHashMap<>();
    
    @Override
    public void collect(ThreadPoolStats threadPoolStats) {
        // 使用缓存机制，保证 Gauge 引用同一个对象
        ThreadPoolStats oldStats = GAUGE_CACHE.get(threadPoolStats.getPoolName());
        if (Objects.isNull(oldStats)) {
            GAUGE_CACHE.put(threadPoolStats.getPoolName(), threadPoolStats);
        } else {
            BeanUtil.copyProperties(threadPoolStats, oldStats);
        }
        gauge(GAUGE_CACHE.get(threadPoolStats.getPoolName()));
    }
}
```

### 注册的指标

`MicroMeterCollector` 注册了以下线程池指标（全部为 Gauge 类型）：

#### 线程池维度指标

- `thread.pool.core.size`: 核心线程数
- `thread.pool.maximum.size`: 最大线程数
- `thread.pool.current.size`: 当前线程数
- `thread.pool.largest.size`: 历史最大线程数
- `thread.pool.active.count`: 活跃线程数

#### 任务维度指标

- `thread.pool.task.count`: 任务总数
- `thread.pool.completed.task.count`: 已完成任务数
- `thread.pool.wait.task.count`: 等待任务数

#### 队列维度指标

- `thread.pool.queue.size`: 队列大小
- `thread.pool.queue.capacity`: 队列容量
- `thread.pool.queue.remaining.capacity`: 队列剩余容量

#### 异常维度指标

- `thread.pool.reject.count`: 拒绝次数
- `thread.pool.run.timeout.count`: 执行超时次数
- `thread.pool.queue.timeout.count`: 队列超时次数

#### 性能维度指标

- `thread.pool.tps`: 每秒事务数（TPS）
- `thread.pool.completed.task.time.avg`: 任务执行平均时间
- `thread.pool.completed.task.time.max`: 任务执行最大时间
- `thread.pool.completed.task.time.min`: 任务执行最小时间
- `thread.pool.completed.task.time.tp50`: TP50 分位数
- `thread.pool.completed.task.time.tp75`: TP75 分位数
- `thread.pool.completed.task.time.tp90`: TP90 分位数
- `thread.pool.completed.task.time.tp95`: TP95 分位数
- `thread.pool.completed.task.time.tp99`: TP99 分位数
- `thread.pool.completed.task.time.tp999`: TP999 分位数

### 指标标签

每个指标都包含以下标签：

- `thread.pool.name`: 线程池名称
- `thread.pool.alias`: 线程池别名（如果为空，使用名称）
- `app.name`: 应用名称

## 配置使用

### 1. 添加依赖

#### Maven

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

#### 如果需要导出到特定监控系统，添加对应的 Registry 依赖

**Prometheus**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**InfluxDB**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-influx</artifactId>
</dependency>
```

**CloudWatch**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch2</artifactId>
</dependency>
```

### 2. 启用 Micrometer 收集器

在 DynamicTp 配置中启用 Micrometer 收集器：

```yaml
spring:
  dynamic:
    tp:
      enabled: true
      enabledCollect: true                    # 开启监控指标采集
      collectorTypes: micrometer              # 使用 Micrometer 收集器
      monitorInterval: 5                      # 监控采集间隔（秒）
```

### 3. 配置 Spring Boot Actuator（推荐）

如果使用 Spring Boot，可以集成 Actuator 来暴露指标：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,metrics,health    # 暴露 Prometheus 端点
  metrics:
    export:
      prometheus:
        enabled: true                          # 启用 Prometheus 导出
```

### 4. 访问指标

#### Prometheus 格式

访问 `/actuator/prometheus` 端点获取 Prometheus 格式的指标：

```bash
curl http://localhost:8080/actuator/prometheus
```

输出示例：
```
# HELP thread_pool_current_size Current thread pool size
# TYPE thread_pool_current_size gauge
thread_pool_current_size{app_name="my-app",thread_pool_alias="异步线程池",thread_pool_name="dtpExecutor1"} 10.0
```

#### JSON 格式

访问 `/actuator/metrics` 端点获取 JSON 格式的指标：

```bash
curl http://localhost:8080/actuator/metrics/thread.pool.current.size
```

## 支持的监控系统

### 1. Prometheus

**特点**:
- 开源的时间序列数据库
- 强大的查询语言 PromQL
- 广泛用于 Kubernetes 环境

**配置**:
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus
```

**查询示例**:
```promql
# 查询所有线程池的当前线程数
thread_pool_current_size

# 查询特定线程池的活跃线程数
thread_pool_active_count{thread_pool_name="dtpExecutor1"}

# 查询线程池拒绝次数
thread_pool_reject_count{app_name="my-app"}
```

### 2. InfluxDB

**特点**:
- 高性能的时间序列数据库
- 适合实时监控场景

**配置**:
```yaml
management:
  metrics:
    export:
      influx:
        enabled: true
        uri: http://localhost:8086
        db: metrics
        userName: admin
        password: admin
```

### 3. CloudWatch

**特点**:
- AWS 的监控服务
- 适合 AWS 环境

**配置**:
```yaml
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: my-app
```

### 4. Datadog

**特点**:
- 商业监控平台
- 提供丰富的可视化

**配置**:
```yaml
management:
  metrics:
    export:
      datadog:
        enabled: true
        apiKey: your-api-key
        applicationKey: your-app-key
```

## 使用示例

### 示例 1: 基础配置

```yaml
spring:
  application:
    name: my-application
  dynamic:
    tp:
      enabled: true
      enabledCollect: true
      collectorTypes: micrometer
      monitorInterval: 5
      executors:
        - threadPoolName: dtpExecutor1
          threadPoolAliasName: 异步线程池
          corePoolSize: 10
          maximumPoolSize: 20

management:
  endpoints:
    web:
      exposure:
        include: prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### 示例 2: 多收集器配置

可以同时使用多个收集器：

```yaml
spring:
  dynamic:
    tp:
      enabled: true
      enabledCollect: true
      collectorTypes: micrometer,logging,jmx    # 同时使用多个收集器
      monitorInterval: 5
```

### 示例 3: 自定义指标查询

使用 PromQL 查询线程池指标：

```promql
# 查询线程池使用率
thread_pool_current_size / thread_pool_maximum_size

# 查询队列使用率
thread_pool_queue_size / thread_pool_queue_capacity

# 查询平均任务执行时间（按线程池分组）
avg(thread_pool_completed_task_time_avg) by (thread_pool_name)

# 查询 TP99 分位数
thread_pool_completed_task_time_tp99{thread_pool_name="dtpExecutor1"}
```

### 示例 4: Grafana 仪表板配置

在 Grafana 中创建线程池监控仪表板：

```json
{
  "targets": [
    {
      "expr": "thread_pool_current_size{thread_pool_name=\"dtpExecutor1\"}",
      "legendFormat": "当前线程数"
    },
    {
      "expr": "thread_pool_active_count{thread_pool_name=\"dtpExecutor1\"}",
      "legendFormat": "活跃线程数"
    },
    {
      "expr": "thread_pool_queue_size{thread_pool_name=\"dtpExecutor1\"}",
      "legendFormat": "队列大小"
    }
  ]
}
```

## 设计特点

### 1. Gauge 缓存机制

`MicroMeterCollector` 使用缓存机制保证 Gauge 引用同一个对象：

```java
// 使用缓存，避免重复创建对象
ThreadPoolStats oldStats = GAUGE_CACHE.get(threadPoolStats.getPoolName());
if (Objects.isNull(oldStats)) {
    GAUGE_CACHE.put(threadPoolStats.getPoolName(), threadPoolStats);
} else {
    BeanUtil.copyProperties(threadPoolStats, oldStats);
}
```

**原因**: Micrometer 的 Gauge 必须持有对象的强引用，如果每次创建新对象，会导致旧对象被 GC，指标失效。

### 2. 标签系统

使用标签对指标进行多维度分类：

```java
List<Tag> tags = Arrays.asList(
    Tag.of("thread.pool.name", poolStats.getPoolName()),
    Tag.of("app.name", CommonUtil.getInstance().getServiceName()),
    Tag.of("thread.pool.alias", poolStats.getPoolAliasName())
);
```

**优势**: 可以通过标签灵活过滤和聚合指标。

### 3. 标准命名规范

指标名称遵循 Micrometer 命名规范：

- 使用小写字母和点号分隔
- 语义清晰，易于理解
- 统一前缀 `thread.pool`

## 最佳实践

### 1. 选择合适的监控系统

- **Kubernetes 环境**: 推荐使用 Prometheus
- **AWS 环境**: 推荐使用 CloudWatch
- **需要实时监控**: 推荐使用 InfluxDB
- **商业环境**: 可以考虑 Datadog、New Relic

### 2. 指标采集频率

- **生产环境**: 建议 5-10 秒采集一次
- **开发环境**: 可以设置更短的间隔（1-3 秒）
- **高负载环境**: 可以适当增加间隔（10-30 秒）

### 3. 标签使用建议

- **不要过度使用标签**: 每个标签组合都会创建新的时间序列
- **使用有意义的标签**: 如应用名称、环境、线程池名称等
- **避免高基数标签**: 如用户 ID、请求 ID 等

### 4. 监控告警配置

基于 Micrometer 指标配置告警规则：

**Prometheus 告警规则示例**:
```yaml
groups:
  - name: thread_pool_alerts
    rules:
      - alert: ThreadPoolRejectHigh
        expr: rate(thread_pool_reject_count[5m]) > 10
        annotations:
          summary: "线程池拒绝率过高"
      
      - alert: ThreadPoolQueueFull
        expr: thread_pool_queue_size / thread_pool_queue_capacity > 0.8
        annotations:
          summary: "线程池队列使用率过高"
      
      - alert: ThreadPoolTp99High
        expr: thread_pool_completed_task_time_tp99 > 1000
        annotations:
          summary: "线程池 TP99 响应时间过高"
```

### 5. 性能优化

- **使用 CompositeMeterRegistry**: 如果需要同时导出到多个系统
- **合理设置采集间隔**: 避免过于频繁的采集影响性能
- **监控指标数量**: 避免创建过多的指标和标签组合

## 注意事项

1. **依赖要求**: 必须引入 `micrometer-core` 依赖，如需导出到特定系统，还需引入对应的 Registry 依赖

2. **Gauge 引用**: Gauge 必须持有对象的强引用，`MicroMeterCollector` 使用缓存机制保证引用

3. **指标命名**: 遵循 Micrometer 命名规范，使用小写字母和点号分隔

4. **标签基数**: 避免使用高基数标签（如用户 ID），会导致指标爆炸

5. **内存占用**: 每个指标和标签组合都会占用内存，注意控制指标数量

6. **线程安全**: Micrometer 的 API 是线程安全的，可以在多线程环境中使用

## 相关资源

- **Micrometer 官方文档**: https://micrometer.io/docs
- **Prometheus 文档**: https://prometheus.io/docs
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **DynamicTp MicroMeterCollector**: [MicroMeterCollector.md](../core/monitor/collector/MicroMeterCollector.md)

## 总结

Micrometer 作为 Java 应用的度量门面，为 DynamicTp 提供了强大的监控能力。通过统一的 API，可以轻松将线程池指标导出到各种监控系统，实现全方位的监控和告警。结合 Spring Boot Actuator，可以快速搭建完整的监控体系。

