# ThreadPoolStatsMXBean

## 概述

`ThreadPoolStatsMXBean` 是线程池统计信息的 MBean 接口，用于通过 JMX 暴露线程池指标。

## 核心作用

1. **JMX 接口**: 定义 JMX MBean 接口
2. **指标暴露**: 暴露线程池统计信息
3. **工具支持**: 支持 JConsole、VisualVM 等工具查看

## 接口定义

```java
@MXBean
public interface ThreadPoolStatsMXBean {
    ThreadPoolStats getThreadPoolStats();
    void setThreadPoolStats(ThreadPoolStats threadPoolStats);
}
```

## 核心方法

### getThreadPoolStats()

**作用**: 获取线程池统计信息

**返回**: `ThreadPoolStats` 对象，包含线程池的各种指标

---

### setThreadPoolStats(ThreadPoolStats threadPoolStats)

**作用**: 设置线程池统计信息

**参数**: `ThreadPoolStats` 对象

**说明**: 用于更新线程池指标

## 实现类

### ThreadPoolStatsJMX

`ThreadPoolStatsJMX` 实现了 `ThreadPoolStatsMXBean` 接口：

```java
public class ThreadPoolStatsJMX implements ThreadPoolStatsMXBean {
    private ThreadPoolStats threadPoolStats;
    
    @Override
    public ThreadPoolStats getThreadPoolStats() {
        return this.threadPoolStats;
    }
    
    @Override
    public void setThreadPoolStats(ThreadPoolStats threadPoolStats) {
        this.threadPoolStats = threadPoolStats;
    }
}
```

## 使用场景

### 1. JMX 查看

通过 JConsole 或 VisualVM 查看线程池指标：

1. 连接到应用
2. 在 MBeans 标签页找到 `dtp.thread.pool:name={poolName}`
3. 查看 `ThreadPoolStats` 属性

### 2. 程序访问

通过程序访问 JMX：

```java
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
ObjectName name = new ObjectName("dtp.thread.pool:name=dtpExecutor1");
ThreadPoolStatsMXBean mbean = JMX.newMBeanProxy(server, name, ThreadPoolStatsMXBean.class);
ThreadPoolStats stats = mbean.getThreadPoolStats();
```

## 设计特点

### 1. MXBean 注解

使用 `@MXBean` 注解，自动处理复杂类型。

### 2. 标准接口

遵循 JMX 标准，兼容各种 JMX 工具。

### 3. 属性暴露

通过属性暴露线程池指标。

## 注意事项

1. **MXBean**: 使用 `@MXBean` 而不是 `@MBean`
2. **对象引用**: MBean 必须持有对象的引用
3. **属性更新**: 通过 `setThreadPoolStats()` 更新指标

