# Priority

## 概述

`Priority` 是优先级接口，用于标识支持优先级的任务。优先级值越小，优先级越高。

## 核心作用

1. **优先级标识**: 标识支持优先级的任务
2. **优先级值**: 提供优先级值
3. **常量定义**: 定义最高和最低优先级常量

## 接口定义

```java
public interface Priority {
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;  // 最高优先级
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;      // 最低优先级
    
    int getPriority();  // 获取优先级值
}
```

## 常量说明

### HIGHEST_PRECEDENCE

- **值**: `Integer.MIN_VALUE`
- **说明**: 最高优先级

---

### LOWEST_PRECEDENCE

- **值**: `Integer.MAX_VALUE`
- **说明**: 最低优先级

## 使用场景

### 1. 实现优先级任务

```java
public class PriorityTask implements Runnable, Priority {
    private int priority;
    
    public PriorityTask(int priority) {
        this.priority = priority;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public void run() {
        // 任务逻辑
    }
}
```

### 2. 使用优先级执行器

```java
PriorityDtpExecutor executor = new PriorityDtpExecutor(...);

// 提交优先级任务
executor.execute(new PriorityTask(Priority.HIGHEST_PRECEDENCE));
executor.execute(new PriorityTask(10));
executor.execute(new PriorityTask(Priority.LOWEST_PRECEDENCE));
```

## 设计特点

### 1. 值越小优先级越高

与 Servlet 的 `load-on-startup` 值类似，值越小优先级越高。

### 2. 常量定义

提供最高和最低优先级常量，方便使用。

### 3. 灵活扩展

可以定义任意优先级值。

## 注意事项

1. **优先级值**: 值越小，优先级越高
2. **相同优先级**: 相同优先级的任务执行顺序不确定
3. **范围**: 优先级值可以是任意整数

