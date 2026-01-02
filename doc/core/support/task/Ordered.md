# Ordered

## 概述

`Ordered` 是有序接口，用于标识需要按顺序执行的任务。它定义了 `hashKey()` 方法，用于将任务路由到特定的线程，保证相同 `hashKey` 的任务按顺序执行。

## 核心作用

1. **顺序标识**: 标识任务需要按顺序执行
2. **路由键**: 提供哈希键用于任务路由
3. **有序执行**: 用于有序线程池（OrderedDtpExecutor）

## 接口定义

```java
public interface Ordered {
    Object hashKey();
}
```

## 核心方法

### hashKey()

**作用**: 返回哈希键，用于任务路由

**返回**: 哈希键对象

**说明**: 
- 相同 `hashKey` 的任务会被路由到同一个线程
- 在同一个线程中，任务按提交顺序执行
- 不同 `hashKey` 的任务可能在不同线程并行执行

## 使用场景

### 1. 有序线程池

在 `OrderedDtpExecutor` 中使用：

```java
public class OrderTask implements Ordered, Runnable {
    
    private final String orderId;
    
    public OrderTask(String orderId) {
        this.orderId = orderId;
    }
    
    @Override
    public Object hashKey() {
        return orderId;  // 相同订单 ID 的任务在同一个线程执行
    }
    
    @Override
    public void run() {
        // 处理订单
    }
}
```

### 2. 保证顺序

需要保证相同实体的任务按顺序执行：

```java
public class UserTask implements Ordered, Runnable {
    
    private final String userId;
    
    @Override
    public Object hashKey() {
        return userId;  // 相同用户的任务在同一个线程执行
    }
    
    @Override
    public void run() {
        // 处理用户任务
    }
}
```

## 工作原理

`OrderedDtpExecutor` 的工作流程：

1. **获取 hashKey**: 从任务中获取 `hashKey()`
2. **计算索引**: `index = hashKey.hashCode() % threadCount`
3. **路由到线程**: 将任务路由到对应的线程
4. **顺序执行**: 在同一个线程中，任务按提交顺序执行

## 实现类

- `OrderedRunnable`: 有序 Runnable 接口
- `OrderedCallable`: 有序 Callable 接口

## 注意事项

1. **hashKey 选择**: 应该选择能够唯一标识任务组的键
2. **哈希冲突**: 不同 hashKey 可能哈希到同一个线程
3. **性能影响**: 有序执行可能影响性能，需要权衡

