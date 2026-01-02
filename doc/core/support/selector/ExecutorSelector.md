# ExecutorSelector

## 概述

`ExecutorSelector` 是执行器选择器接口，用于从多个执行器中选择一个执行器来执行任务。它提供了不同的选择策略，如哈希选择、随机选择等。

## 核心作用

1. **执行器选择**: 从多个执行器中选择一个
2. **负载均衡**: 提供不同的选择策略，实现负载均衡
3. **可扩展**: 支持自定义选择策略

## 接口定义

```java
public interface ExecutorSelector {
    Executor select(List<Executor> executors, Object arg);
}
```

## 核心方法

### select(List<Executor> executors, Object arg)

**作用**: 从执行器列表中选择一个执行器

**参数**:
- `executors`: 执行器列表
- `arg`: 选择参数（用于哈希选择等策略）

**返回**: 选中的执行器

## 内置实现

### 1. HashedExecutorSelector

**策略**: 哈希选择

**实现**: 根据 `arg` 的哈希值选择执行器

**适用场景**: 需要保证相同参数选择相同执行器（如按用户 ID 路由）

---

### 2. RandomExecutorSelector

**策略**: 随机选择

**实现**: 随机选择一个执行器

**适用场景**: 需要均匀分配负载

## 使用场景

### 1. 多线程池路由

```java
List<Executor> executors = Arrays.asList(executor1, executor2, executor3);
ExecutorSelector selector = new HashedExecutorSelector();
Executor selected = selector.select(executors, userId);  // 根据用户 ID 选择
```

### 2. 负载均衡

```java
ExecutorSelector selector = new RandomExecutorSelector();
Executor selected = selector.select(executors, null);  // 随机选择
```

## 自定义实现

```java
public class CustomExecutorSelector implements ExecutorSelector {
    
    @Override
    public Executor select(List<Executor> executors, Object arg) {
        // 自定义选择逻辑
        // 例如：轮询、最少连接等
        return executors.get(0);
    }
}
```

