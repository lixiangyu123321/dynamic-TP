# OrderedRunnable

## 概述

`OrderedRunnable` 是有序任务接口，继承自 `Ordered` 和 `Runnable`。它用于 `OrderedDtpExecutor`，确保相同 `hashKey` 的任务按顺序执行。

## 核心作用

1. **顺序保证**: 保证相同 `hashKey` 的任务按提交顺序执行
2. **接口组合**: 组合 `Ordered` 和 `Runnable` 接口
3. **有序执行**: 用于有序线程池

## 接口定义

```java
public interface OrderedRunnable extends Ordered, Runnable {
}
```

## 继承的方法

### 来自 Ordered 接口

- `hashKey()`: 返回哈希键，用于路由到同一个线程

### 来自 Runnable 接口

- `run()`: 执行任务

## 使用场景

### 1. 有序线程池

在 `OrderedDtpExecutor` 中使用：

```java
public class MyOrderedTask implements OrderedRunnable {
    
    private final String orderId;
    
    public MyOrderedTask(String orderId) {
        this.orderId = orderId;
    }
    
    @Override
    public Object hashKey() {
        return orderId;  // 相同订单 ID 的任务在同一个线程执行
    }
    
    @Override
    public void run() {
        // 处理订单
        processOrder(orderId);
    }
}

// 使用
OrderedDtpExecutor executor = ...;
executor.execute(new MyOrderedTask("order123"));  // 保证相同订单的任务顺序执行
```

### 2. 保证顺序

需要保证相同实体的任务按顺序执行：

```java
public class UserTask implements OrderedRunnable {
    
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

`OrderedDtpExecutor` 会根据 `hashKey()` 的哈希值将任务路由到特定的线程，保证相同 `hashKey` 的任务在同一个线程中按顺序执行。

## 注意事项

1. **hashKey 选择**: `hashKey()` 应该返回能够唯一标识任务组的键
2. **顺序保证**: 只保证相同 `hashKey` 的任务顺序执行，不同 `hashKey` 的任务可能并行执行
3. **性能影响**: 有序执行可能影响性能，需要权衡

