# PerformanceMonitorAware 详细注释

## 类概述

`PerformanceMonitorAware` 是性能监控感知器，用于感知线程池任务的执行情况，并统计任务的性能指标（如响应时间、TPS、TPXX 等）。

## 继承关系

```java
PerformanceMonitorAware extends TaskStatAware implements ExecutorAware
```

- **TaskStatAware**: 提供任务统计的基础功能，管理 `ThreadPoolStatProvider`
- **ExecutorAware**: 线程池感知器接口，定义感知点

## 方法详细注释

### 1. getOrder()

```java
@Override
public int getOrder() {
    return AwareTypeEnum.PERFORMANCE_MONITOR_AWARE.getOrder();
}
```

**作用**: 返回感知器的执行顺序

**返回值**: `1`（在所有感知器中第一个执行）

**含义**: 
- 性能监控需要在其他感知器之前执行，确保能够准确记录任务的完整生命周期
- 顺序值越小，执行越早

---

### 2. getName()

```java
@Override
public String getName() {
    return AwareTypeEnum.PERFORMANCE_MONITOR_AWARE.getName();
}
```

**作用**: 返回感知器的名称

**返回值**: `"monitor"`

**含义**:
- 用于标识和配置感知器
- 在线程池配置中可以通过 `aware-names: [monitor]` 来启用此感知器

---

### 3. execute(Executor executor, Runnable r)

```java
@Override
public void execute(Executor executor, Runnable r) {
    if (TRUE_STR.equals(System.getProperty(DTP_EXECUTE_ENHANCED, TRUE_STR))) {
        Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> p.startTask(r));
    }
}
```

**作用**: 感知到任务提交事件，启动任务性能统计

**触发时机**: 当任务通过 `executor.execute(r)` 提交到线程池时

**执行流程**:
1. **检查开关**: 检查系统属性 `DTP_EXECUTE_ENHANCED` 是否为 `true`（默认为 `true`）
   - 如果为 `false`，则跳过性能监控，避免性能开销
   - 可以通过 `-DDTP_EXECUTE_ENHANCED=false` 关闭性能监控

2. **获取统计提供者**: 从 `statProviders` 映射中获取该线程池对应的 `ThreadPoolStatProvider`
   - `statProviders` 是在 `register()` 方法中注册的
   - 如果找不到对应的提供者，说明该线程池未注册，跳过处理

3. **启动任务计时**: 调用 `p.startTask(r)` 开始计时
   - 记录任务提交时间：`stopWatchMap.put(r, System.currentTimeMillis())`
   - 将任务和当前时间戳存入 `stopWatchMap`，用于后续计算任务执行时间

**意义**:
- 标记任务的开始时间点
- 为后续计算任务响应时间（RT）做准备
- 这是性能监控的起点

---

### 4. afterExecute(Executor executor, Runnable r, Throwable t)

```java
@Override
public void afterExecute(Executor executor, Runnable r, Throwable t) {
    Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> p.completeTask(r));
}
```

**作用**: 感知到任务执行完成事件，完成任务性能统计

**触发时机**: 当线程执行完任务后（无论成功还是异常）

**执行流程**:
1. **获取统计提供者**: 从 `statProviders` 中获取对应的 `ThreadPoolStatProvider`

2. **完成任务统计**: 调用 `p.completeTask(r)` 完成统计
   - 从 `stopWatchMap` 中移除任务，获取开始时间：`stopWatchMap.remove(r)`
   - 计算响应时间：`rt = System.currentTimeMillis() - startTime`
   - 将响应时间记录到性能提供者：`performanceProvider.completeTask(rt)`
   - 性能提供者使用 `MMAPCounter`（内存映射计数器）记录响应时间
   - 统计指标包括：TPS、最大/最小/平均响应时间、TP50/TP75/TP90/TP95/TP99/TP999

**统计的指标**:
- **TPS**: 每秒任务数（Tasks Per Second）
- **最大响应时间**: 所有任务中的最大执行时间
- **最小响应时间**: 所有任务中的最小执行时间
- **平均响应时间**: 所有任务的平均执行时间
- **TPXX**: 响应时间的百分位数（如 TP99 表示 99% 的任务执行时间小于该值）

**意义**:
- 记录任务的完整执行时间
- 为性能分析和监控提供数据
- 这些指标会被定期采集并上报到监控系统

---

### 5. afterReject(Runnable r, Executor executor)

```java
@Override
public void afterReject(Runnable r, Executor executor) {
    Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> p.completeTask(r));
}
```

**作用**: 感知到任务被拒绝事件，也完成任务统计（将被拒绝的任务也计入统计）

**触发时机**: 当任务被线程池拒绝策略拒绝后

**执行流程**:
- 与 `afterExecute()` 相同的处理逻辑
- 调用 `p.completeTask(r)` 完成任务统计

**特殊说明**:
- 被拒绝的任务也会被计入性能统计
- 这样可以统计到所有提交的任务，包括被拒绝的
- 被拒绝的任务的响应时间是从提交到被拒绝的时间

**意义**:
- 确保统计的完整性，包括被拒绝的任务
- 可以分析拒绝率对性能的影响
- 提供更全面的性能数据

---

## 完整工作流程

### 正常任务执行流程

```
1. 任务提交
   └─> execute(executor, r)
       └─> startTask(r)  // 记录开始时间

2. 任务执行
   └─> (线程池执行任务)

3. 任务完成
   └─> afterExecute(executor, r, t)
       └─> completeTask(r)  // 计算响应时间，记录到性能统计
```

### 任务被拒绝流程

```
1. 任务提交
   └─> execute(executor, r)
       └─> startTask(r)  // 记录开始时间

2. 任务被拒绝
   └─> (拒绝策略拒绝任务)

3. 拒绝后处理
   └─> afterReject(r, executor)
       └─> completeTask(r)  // 计算从提交到拒绝的时间，记录到性能统计
```

## 性能统计的数据流向

```
PerformanceMonitorAware
    │
    ├─> ThreadPoolStatProvider.startTask()
    │   └─> stopWatchMap.put(r, startTime)  // 记录开始时间
    │
    └─> ThreadPoolStatProvider.completeTask()
        ├─> stopWatchMap.remove(r)  // 获取开始时间
        ├─> rt = currentTime - startTime  // 计算响应时间
        └─> PerformanceProvider.completeTask(rt)
            └─> MMAPCounter.add(rt)  // 记录响应时间到内存映射计数器
                │
                └─> 定期采集
                    └─> PerformanceSnapshot
                        ├─> TPS (每秒任务数)
                        ├─> 最大/最小/平均响应时间
                        └─> TP50/TP75/TP90/TP95/TP99/TP999
```

## 关键设计点

### 1. 性能开关

通过系统属性 `DTP_EXECUTE_ENHANCED` 控制是否启用性能监控：
- **启用**: 默认启用，记录所有任务的性能数据
- **关闭**: 可以通过 `-DDTP_EXECUTE_ENHANCED=false` 关闭，避免性能开销

### 2. 使用 Optional 和 ifPresent

```java
Optional.ofNullable(statProviders.get(executor)).ifPresent(p -> p.startTask(r));
```

- 安全地处理可能为 null 的情况
- 如果线程池未注册，不会抛出异常，只是跳过处理

### 3. 被拒绝任务也统计

`afterReject()` 方法也会调用 `completeTask()`，确保：
- 所有提交的任务都被统计
- 包括被拒绝的任务
- 提供更全面的性能数据

### 4. 使用 MMAPCounter

性能统计使用内存映射计数器（MMAPCounter）：
- 高性能的计数器实现
- 支持高并发场景
- 可以计算百分位数（TPXX）

## 使用场景

1. **性能监控**: 实时监控线程池任务的执行性能
2. **性能分析**: 分析任务响应时间分布，找出性能瓶颈
3. **容量规划**: 根据 TPS 和响应时间数据规划线程池容量
4. **告警触发**: 当响应时间超过阈值时触发告警

## 注意事项

1. **性能开销**: 性能监控会有一定的性能开销，如果不需要可以关闭
2. **内存占用**: `stopWatchMap` 会占用一定内存，任务完成后会自动清理
3. **统计精度**: 使用毫秒级精度，对于微秒级任务可能不够精确
4. **并发安全**: 使用 `ConcurrentHashMap` 保证并发安全

