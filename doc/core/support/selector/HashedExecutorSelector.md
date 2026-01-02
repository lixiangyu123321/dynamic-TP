# HashedExecutorSelector

## 概述

`HashedExecutorSelector` 是哈希执行器选择器，根据选择参数的哈希值从执行器列表中选择一个执行器。它保证相同的参数总是选择相同的执行器。

## 核心作用

1. **一致性哈希**: 保证相同参数选择相同执行器
2. **路由功能**: 支持按参数路由到特定执行器
3. **负载均衡**: 通过哈希实现相对均匀的负载分配

## 实现

```java
public class HashedExecutorSelector implements ExecutorSelector {
    
    @Override
    public Executor select(List<Executor> executors, Object arg) {
        int idx = arg.hashCode() % executors.size();
        if (idx < 0) {
            idx = Math.abs(idx);
        }
        return executors.get(idx);
    }
}
```

## 工作原理

### 1. 计算索引

```java
int idx = arg.hashCode() % executors.size();
```

**说明**:
- 计算参数的哈希值
- 对执行器数量取模
- 得到执行器索引

### 2. 处理负数

```java
if (idx < 0) {
    idx = Math.abs(idx);
}
```

**说明**: 如果哈希值为负数，取绝对值

### 3. 选择执行器

```java
return executors.get(idx);
```

## 使用场景

### 1. 按用户路由

```java
List<Executor> executors = Arrays.asList(executor1, executor2, executor3);
HashedExecutorSelector selector = new HashedExecutorSelector();

// 相同用户 ID 总是选择相同的执行器
String userId = "user123";
Executor executor = selector.select(executors, userId);
```

### 2. 按订单路由

```java
// 相同订单 ID 总是选择相同的执行器
String orderId = "order456";
Executor executor = selector.select(executors, orderId);
```

### 3. 保证顺序

如果需要保证相同实体的任务在同一个线程池中执行（保证顺序），可以使用哈希选择：

```java
// 相同订单的任务在同一个线程池中执行
Executor executor = selector.select(executors, orderId);
executor.execute(() -> processOrder(orderId));
```

## 特点

### 1. 一致性

相同参数总是选择相同的执行器，保证一致性。

### 2. 负载均衡

通过哈希实现相对均匀的负载分配。

### 3. 简单高效

实现简单，性能高。

## 注意事项

1. **哈希冲突**: 不同参数可能哈希到同一个执行器
2. **执行器数量变化**: 执行器数量变化时，路由会发生变化
3. **参数类型**: 参数必须有合理的 `hashCode()` 实现

