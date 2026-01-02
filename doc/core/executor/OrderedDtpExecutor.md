# OrderedDtpExecutor

## 概述

`OrderedDtpExecutor` 是有序线程池执行器，继承自 `DtpExecutor`。它可以确保相同 key 的任务按照提交顺序执行，适用于需要并行处理提高吞吐量，同时需要保证任务按一定顺序执行的场景。

## 核心作用

1. **有序执行**: 保证相同 key 的任务按提交顺序执行
2. **并行处理**: 不同 key 的任务可以并行执行
3. **吞吐量提升**: 通过并行处理提高吞吐量

## 核心属性

```java
private final ExecutorSelector selector = new HashedExecutorSelector();  // 执行器选择器
private final List<Executor> childExecutors = Lists.newArrayList();      // 子执行器列表
```

## 工作原理

### 1. 子执行器机制

`OrderedDtpExecutor` 内部维护多个子执行器（`ChildExecutor`），数量等于核心线程数：

```java
for (int i = 0; i < corePoolSize; i++) {
    ChildExecutor childExecutor = new ChildExecutor(workQueue.size() + workQueue.remainingCapacity());
    childExecutors.add(childExecutor);
}
```

### 2. 任务路由

根据任务的 `hashKey` 选择子执行器：

```java
private void doOrderedExecute(Runnable command, Object hashKey) {
    Executor executor = selector.select(childExecutors, hashKey);  // 根据 hashKey 选择执行器
    executor.execute(command);
}
```

**说明**: 相同 `hashKey` 的任务会被路由到同一个子执行器，保证顺序执行。

### 3. 无序任务

如果任务没有 `hashKey`，直接提交到父执行器：

```java
private void doUnorderedExecute(Runnable command) {
    super.execute(command);  // 直接提交到父执行器
}
```

## 核心方法

### execute(Runnable command)

**作用**: 执行任务

**实现**:
```java
@Override
public void execute(Runnable command) {
    if (command instanceof Ordered) {
        doOrderedExecute(command, ((Ordered) command).hashKey());  // 有序执行
    } else {
        doUnorderedExecute(command);  // 无序执行
    }
}
```

---

### execute(Runnable command, Object hashKey)

**作用**: 执行任务（指定 hashKey）

**实现**:
```java
public void execute(Runnable command, Object hashKey) {
    if (Objects.nonNull(hashKey)) {
        doOrderedExecute(command, hashKey);
    } else {
        doUnorderedExecute(command);
    }
}
```

---

### ChildExecutor

**作用**: 子执行器，负责执行有序任务

**特点**:
- 单线程执行（保证顺序）
- 使用队列存储任务
- 支持任务增强和感知器

**实现**:
```java
private final class ChildExecutor implements Executor, Runnable {
    private final BlockingQueue<Runnable> taskQueue;
    private boolean running;
    
    @Override
    public void execute(Runnable command) {
        command = getEnhancedTask(command, getTaskWrappers());  // 任务增强
        taskQueue.add(command);
        if (!running) {
            running = true;
            doUnorderedExecute(this);  // 提交自身作为任务
        }
    }
    
    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        Runnable task;
        while ((task = getTask()) != null) {
            onBeforeExecute(thread, task);
            try {
                task.run();
            } finally {
                onAfterExecute(task, thrown);
            }
        }
    }
}
```

## 使用场景

### 1. 有序任务执行

```java
OrderedDtpExecutor executor = new OrderedDtpExecutor(10, 20, 60, TimeUnit.SECONDS, 
    new LinkedBlockingQueue<>(200));

// 相同 userId 的任务按顺序执行
executor.execute(() -> processOrder(userId, order1), userId);
executor.execute(() -> processOrder(userId, order2), userId);
executor.execute(() -> processOrder(userId, order3), userId);
```

### 2. 使用 Ordered 接口

```java
public class OrderTask implements Runnable, Ordered {
    private String userId;
    
    @Override
    public Object hashKey() {
        return userId;  // 返回 hashKey
    }
    
    @Override
    public void run() {
        // 任务逻辑
    }
}

executor.execute(new OrderTask());
```

## 设计特点

### 1. 哈希路由

使用哈希算法将任务路由到子执行器，相同 key 的任务总是路由到同一个执行器。

### 2. 单线程执行

每个子执行器是单线程的，保证任务顺序执行。

### 3. 并行处理

不同 key 的任务可以并行执行，提高吞吐量。

## 注意事项

1. **子执行器数量**: 等于核心线程数
2. **队列容量**: 子执行器的队列容量等于父执行器的队列容量
3. **任务统计**: 任务统计包括子执行器的统计

