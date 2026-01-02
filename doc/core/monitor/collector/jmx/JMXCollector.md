# JMXCollector

## 概述

`JMXCollector` 是 JMX 收集器，继承自 `AbstractCollector`。它将线程池指标注册到 JMX，可以通过 JConsole、VisualVM 等工具查看。

## 核心作用

1. **JMX 注册**: 将线程池指标注册到 JMX
2. **MBean 管理**: 通过 MBean 管理线程池指标
3. **工具支持**: 支持 JConsole、VisualVM 等工具查看

## 核心属性

```java
public static final String DTP_METRIC_NAME_PREFIX = "dtp.thread.pool";  // MBean 名称前缀
private static final Map<String, ThreadPoolStats> GAUGE_CACHE = new ConcurrentHashMap<>();  // 缓存
```

## 核心方法

### collect(ThreadPoolStats threadPoolStats)

**作用**: 收集指标并注册到 JMX

**实现**:
```java
@Override
public void collect(ThreadPoolStats threadPoolStats) {
    if (GAUGE_CACHE.containsKey(threadPoolStats.getPoolName())) {
        // 如果已注册，更新缓存对象
        ThreadPoolStats poolStats = GAUGE_CACHE.get(threadPoolStats.getPoolName());
        BeanUtil.copyProperties(threadPoolStats, poolStats);
    } else {
        // 如果未注册，注册 MBean
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(DTP_METRIC_NAME_PREFIX + ":name=" + threadPoolStats.getPoolName());
            ThreadPoolStatsJMX stats = new ThreadPoolStatsJMX(threadPoolStats);
            server.registerMBean(stats, name);
        } catch (JMException e) {
            log.error("collect thread pool stats error", e);
        }
        GAUGE_CACHE.put(threadPoolStats.getPoolName(), threadPoolStats);
    }
}
```

**流程**:
1. 检查是否已注册
2. 如果已注册，更新缓存对象的属性
3. 如果未注册，创建 MBean 并注册到 JMX

---

### type()

**作用**: 获取收集器类型

**返回**: "jmx"

## 使用场景

### 1. 配置收集器

```yaml
spring:
  dynamic:
    tp:
      collector-types: [jmx]
```

### 2. 使用 JConsole 查看

1. 启动应用
2. 打开 JConsole
3. 连接到应用
4. 在 MBeans 标签页查看 `dtp.thread.pool` 下的指标

### 3. 使用 VisualVM 查看

1. 启动应用
2. 打开 VisualVM
3. 连接到应用
4. 在 MBeans 标签页查看指标

## 设计特点

### 1. MBean 注册

使用标准 JMX MBean 机制注册指标。

### 2. 缓存机制

使用缓存机制，避免重复注册 MBean。

### 3. 属性更新

通过更新缓存对象的属性来更新指标。

## 注意事项

1. **MBean 名称**: MBean 名称格式为 `dtp.thread.pool:name={poolName}`
2. **重复注册**: 已注册的 MBean 不会重复注册
3. **属性更新**: 通过更新对象属性来更新指标

