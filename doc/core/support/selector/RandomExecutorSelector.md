# RandomExecutorSelector

## 概述

`RandomExecutorSelector` 是随机执行器选择器，从执行器列表中随机选择一个执行器。它使用 `ThreadLocalRandom` 实现线程安全的随机选择。

## 核心作用

1. **随机选择**: 随机选择一个执行器
2. **负载均衡**: 实现均匀的负载分配
3. **简单高效**: 实现简单，性能高

## 实现

```java
public class RandomExecutorSelector implements ExecutorSelector {
    
    @Override
    public Executor select(List<Executor> executors, Object arg) {
        return executors.get(ThreadLocalRandom.current().nextInt(executors.size()));
    }
}
```

## 工作原理

### 1. 生成随机索引

```java
ThreadLocalRandom.current().nextInt(executors.size())
```

**说明**:
- 使用 `ThreadLocalRandom` 生成随机数
- 范围是 `[0, executors.size())`
- `ThreadLocalRandom` 是线程安全的，性能优于 `Random`

### 2. 选择执行器

```java
return executors.get(idx);
```

## 使用场景

### 1. 均匀负载分配

```java
List<Executor> executors = Arrays.asList(executor1, executor2, executor3);
RandomExecutorSelector selector = new RandomExecutorSelector();

// 随机选择，实现负载均衡
Executor executor = selector.select(executors, null);
```

### 2. 无状态任务

对于无状态任务，不需要保证路由一致性，可以使用随机选择：

```java
// 任务之间没有关联，可以随机分配
Executor executor = selector.select(executors, null);
executor.execute(() -> processTask());
```

## 特点

### 1. 负载均衡

随机选择可以实现相对均匀的负载分配。

### 2. 线程安全

使用 `ThreadLocalRandom`，线程安全且性能好。

### 3. 简单高效

实现简单，性能开销小。

## 与 HashedExecutorSelector 的对比

| 特性 | RandomExecutorSelector | HashedExecutorSelector |
|------|----------------------|----------------------|
| **选择方式** | 随机 | 哈希 |
| **一致性** | 无 | 有 |
| **适用场景** | 无状态任务 | 需要路由的任务 |
| **负载均衡** | 均匀 | 相对均匀 |

## 注意事项

1. **无一致性**: 相同参数可能选择不同的执行器
2. **负载均衡**: 在大量任务下才能实现均匀分配
3. **参数忽略**: `arg` 参数被忽略，不影响选择结果

