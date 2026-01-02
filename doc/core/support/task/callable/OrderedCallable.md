# OrderedCallable

## 概述

`OrderedCallable` 是有序 Callable 接口，继承自 `Ordered` 和 `Callable`。它用于 `OrderedDtpExecutor`，确保相同 `hashKey` 的 Callable 任务按顺序执行。

## 核心作用

1. **顺序保证**: 保证相同 `hashKey` 的 Callable 任务按提交顺序执行
2. **接口组合**: 组合 `Ordered` 和 `Callable` 接口
3. **有序执行**: 用于有序线程池

## 接口定义

```java
public interface OrderedCallable<C> extends Ordered, Callable<C> {
}
```

## 继承的方法

### 来自 Ordered 接口

- `hashKey()`: 返回哈希键，用于路由到同一个线程

### 来自 Callable 接口

- `call()`: 执行任务并返回结果

## 使用场景

### 1. 有序线程池

在 `OrderedDtpExecutor` 中使用：

```java
public class MyOrderedCallable implements OrderedCallable<String> {
    
    private final String orderId;
    
    public MyOrderedCallable(String orderId) {
        this.orderId = orderId;
    }
    
    @Override
    public Object hashKey() {
        return orderId;  // 相同订单 ID 的任务在同一个线程执行
    }
    
    @Override
    public String call() throws Exception {
        // 处理订单并返回结果
        return processOrder(orderId);
    }
}

// 使用
OrderedDtpExecutor executor = ...;
Future<String> future = executor.submit(new MyOrderedCallable("order123"));
```

### 2. 保证顺序

需要保证相同实体的任务按顺序执行并返回结果：

```java
public class UserCallable implements OrderedCallable<User> {
    
    private final String userId;
    
    @Override
    public Object hashKey() {
        return userId;
    }
    
    @Override
    public User call() throws Exception {
        return getUserInfo(userId);
    }
}
```

## 工作原理

与 `OrderedRunnable` 相同，`OrderedDtpExecutor` 会根据 `hashKey()` 将任务路由到特定线程，保证相同 `hashKey` 的任务顺序执行。

## 与 OrderedRunnable 的区别

| 特性 | OrderedRunnable | OrderedCallable |
|------|----------------|-----------------|
| **返回结果** | 无 | 有 |
| **异常处理** | 抛出异常 | 返回异常（在 Future 中） |
| **使用方式** | `execute()` | `submit()` |

## 注意事项

1. **hashKey 选择**: 应该选择能够唯一标识任务组的键
2. **顺序保证**: 只保证相同 `hashKey` 的任务顺序执行
3. **返回值**: 可以通过 `Future` 获取返回值

