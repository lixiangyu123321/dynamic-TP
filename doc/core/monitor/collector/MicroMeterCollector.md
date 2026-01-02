# MicroMeterCollector

## 概述

`MicroMeterCollector` 是 Micrometer 收集器，继承自 `AbstractCollector`。它将线程池指标注册到 Micrometer，支持 Prometheus、InfluxDB 等监控系统。

## 核心作用

1. **指标注册**: 将线程池指标注册到 Micrometer
2. **监控集成**: 支持 Prometheus、InfluxDB 等监控系统
3. **Gauge 指标**: 使用 Gauge 类型指标

## 核心属性

```java
public static final String DTP_METRIC_NAME_PREFIX = "thread.pool";  // 指标名称前缀
public static final String POOL_NAME_TAG = "thread.pool.name";      // 线程池名称标签
public static final String POOL_ALIAS_TAG = "thread.pool.alias";    // 线程池别名标签
public static final String APP_NAME_TAG = "app.name";               // 应用名称标签
private static final Map<String, ThreadPoolStats> GAUGE_CACHE = new ConcurrentHashMap<>();  // Gauge 缓存
```

## 核心方法

### collect(ThreadPoolStats threadPoolStats)

**作用**: 收集指标并注册到 Micrometer

**实现**:
```java
@Override
public void collect(ThreadPoolStats threadPoolStats) {
    ThreadPoolStats oldStats = GAUGE_CACHE.get(threadPoolStats.getPoolName());
    if (Objects.isNull(oldStats)) {
        GAUGE_CACHE.put(threadPoolStats.getPoolName(), threadPoolStats);
    } else {
        BeanUtil.copyProperties(threadPoolStats, oldStats);
    }
    gauge(GAUGE_CACHE.get(threadPoolStats.getPoolName()));
}
```

**说明**: 
- 使用缓存机制，保证 Gauge 引用同一个对象
- 更新缓存对象的属性
- 调用 `gauge()` 方法注册指标

---

### gauge(ThreadPoolStats poolStats)

**作用**: 注册 Gauge 指标

**实现**:
```java
public void gauge(ThreadPoolStats poolStats) {
    Iterable<Tag> tags = getTags(poolStats);
    
    // 注册各种指标
    Metrics.gauge(metricName("core.size"), tags, poolStats, ThreadPoolStats::getCorePoolSize);
    Metrics.gauge(metricName("maximum.size"), tags, poolStats, ThreadPoolStats::getMaximumPoolSize);
    // ... 其他指标
}
```

**注册的指标**:
- `thread.pool.core.size`: 核心线程数
- `thread.pool.maximum.size`: 最大线程数
- `thread.pool.current.size`: 当前线程数
- `thread.pool.largest.size`: 历史最大线程数
- `thread.pool.active.count`: 活跃线程数
- `thread.pool.task.count`: 任务总数
- `thread.pool.completed.task.count`: 已完成任务数
- `thread.pool.wait.task.count`: 等待任务数
- `thread.pool.queue.size`: 队列大小
- `thread.pool.queue.capacity`: 队列容量
- `thread.pool.queue.remaining.capacity`: 队列剩余容量
- `thread.pool.reject.count`: 拒绝次数
- `thread.pool.run.timeout.count`: 执行超时次数
- `thread.pool.queue.timeout.count`: 队列超时次数
- `thread.pool.tps`: 每秒事务数
- `thread.pool.completed.task.time.*`: 任务执行时间指标（平均、最大、最小、TP 分位数）

---

### getTags(ThreadPoolStats poolStats)

**作用**: 获取指标标签

**实现**:
```java
private Iterable<Tag> getTags(ThreadPoolStats poolStats) {
    List<Tag> tags = new ArrayList<>(3);
    tags.add(Tag.of(POOL_NAME_TAG, poolStats.getPoolName()));
    tags.add(Tag.of(APP_NAME_TAG, CommonUtil.getInstance().getServiceName()));
    tags.add(Tag.of(POOL_ALIAS_TAG, Optional.ofNullable(poolStats.getPoolAliasName())
            .orElse(poolStats.getPoolName())));
    return tags;
}
```

**标签**:
- `thread.pool.name`: 线程池名称
- `app.name`: 应用名称
- `thread.pool.alias`: 线程池别名（如果为空，使用名称）

---

### type()

**作用**: 获取收集器类型

**返回**: "micrometer"

## 使用场景

### 1. 配置收集器

```yaml
spring:
  dynamic:
    tp:
      collector-types: [micrometer]
```

### 2. Prometheus 集成

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```

### 3. 指标查询

通过 Prometheus 查询线程池指标：

```promql
thread_pool_current_size{thread_pool_name="dtpExecutor1"}
```

## 设计特点

### 1. Gauge 缓存

使用缓存机制，保证 Gauge 引用同一个对象，避免重复注册。

### 2. 标签支持

支持标签，便于指标分类和查询。

### 3. 标准格式

使用 Micrometer 标准格式，兼容各种监控系统。

## 注意事项

1. **Micrometer 依赖**: 需要引入 Micrometer 依赖
2. **Gauge 引用**: Gauge 必须持有对象的强引用
3. **指标名称**: 指标名称遵循 Micrometer 命名规范

