# ThreadPoolStatsJMX

## 概述

`ThreadPoolStatsJMX` 是线程池统计信息的 MBean 实现类，实现了 `ThreadPoolStatsMXBean` 接口。它用于通过 JMX 暴露线程池指标。

## 核心作用

1. **MBean 实现**: 实现 `ThreadPoolStatsMXBean` 接口
2. **指标存储**: 存储线程池统计信息
3. **JMX 暴露**: 通过 JMX 暴露指标

## 核心属性

```java
private ThreadPoolStats threadPoolStats;  // 线程池统计信息
```

## 核心方法

### ThreadPoolStatsJMX(ThreadPoolStats threadPoolStats)

**作用**: 构造方法

**说明**: 传入线程池统计信息对象

---

### getThreadPoolStats()

**作用**: 获取线程池统计信息

**实现**:
```java
@Override
public ThreadPoolStats getThreadPoolStats() {
    return this.threadPoolStats;
}
```

---

### setThreadPoolStats(ThreadPoolStats threadPoolStats)

**作用**: 设置线程池统计信息

**实现**:
```java
@Override
public void setThreadPoolStats(ThreadPoolStats threadPoolStats) {
    this.threadPoolStats = threadPoolStats;
}
```

**说明**: 用于更新线程池指标

## 使用场景

### 1. JMX 注册

由 `JMXCollector` 注册到 JMX：

```java
ThreadPoolStatsJMX stats = new ThreadPoolStatsJMX(threadPoolStats);
server.registerMBean(stats, name);
```

### 2. 指标更新

通过 `setThreadPoolStats()` 更新指标：

```java
ThreadPoolStatsJMX mbean = ...;
mbean.setThreadPoolStats(newStats);
```

## 设计特点

### 1. 简单实现

实现简单，只是存储和返回统计信息。

### 2. 属性更新

通过 `setThreadPoolStats()` 更新指标，而不是创建新对象。

### 3. JMX 标准

遵循 JMX 标准，兼容各种 JMX 工具。

## 注意事项

1. **对象引用**: MBean 必须持有对象的引用
2. **属性更新**: 通过 `setThreadPoolStats()` 更新指标
3. **线程安全**: 需要注意线程安全问题

