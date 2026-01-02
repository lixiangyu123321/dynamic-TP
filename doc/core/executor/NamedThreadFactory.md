# NamedThreadFactory

## 概述

`NamedThreadFactory` 是命名线程工厂，用于为线程池创建有意义的线程名称。它实现了 `ThreadFactory` 接口，提供线程命名、守护线程设置、优先级设置等功能。

## 核心作用

1. **线程命名**: 为线程提供有意义的名称，便于调试和监控
2. **守护线程**: 支持设置线程是否为守护线程
3. **优先级设置**: 支持设置线程优先级

## 核心属性

```java
private String namePrefix;              // 线程名称前缀
private final ThreadGroup group;        // 线程组
private final boolean daemon;           // 是否为守护线程
private final Integer priority;         // 线程优先级
private final AtomicInteger seq = new AtomicInteger(1);  // 线程序号
```

## 核心方法

### 构造方法

#### NamedThreadFactory(String namePrefix)

**作用**: 创建线程工厂（默认非守护线程，正常优先级）

```java
public NamedThreadFactory(String namePrefix) {
    this(namePrefix, false, Thread.NORM_PRIORITY);
}
```

---

#### NamedThreadFactory(String namePrefix, boolean daemon)

**作用**: 创建线程工厂（指定是否为守护线程）

```java
public NamedThreadFactory(String namePrefix, boolean daemon) {
    this(namePrefix, daemon, Thread.NORM_PRIORITY);
}
```

---

#### NamedThreadFactory(String namePrefix, boolean daemon, int priority)

**作用**: 创建线程工厂（完整参数）

```java
public NamedThreadFactory(String namePrefix, boolean daemon, int priority) {
    this.daemon = daemon;
    this.priority = priority;
    SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    this.namePrefix = namePrefix;
}
```

**说明**:
- 如果存在 `SecurityManager`，使用其线程组
- 否则使用当前线程的线程组

---

### newThread(Runnable r)

**作用**: 创建新线程

**实现**:
```java
@Override
public Thread newThread(Runnable r) {
    String name = namePrefix + "-" + seq.getAndIncrement();
    Thread t = new Thread(group, r, name);
    t.setDaemon(daemon);
    t.setPriority(priority);
    return t;
}
```

**线程名称格式**: `{namePrefix}-{序号}`

**示例**: `dtp-1`, `dtp-2`, `myPool-1`, `myPool-2`

---

### getNamePrefix() / setNamePrefix(String namePrefix)

**作用**: 获取/设置线程名称前缀

**说明**: 支持动态修改前缀，用于配置刷新

## 使用场景

### 1. 创建线程池时使用

```java
DtpExecutor executor = new DtpExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(200),
    new NamedThreadFactory("myPool"),  // 使用命名线程工厂
    new AbortPolicy()
);
```

### 2. 配置刷新时修改前缀

```java
NamedThreadFactory factory = (NamedThreadFactory) executor.getThreadFactory();
factory.setNamePrefix("newPrefix");  // 修改前缀
// 新创建的线程名称会使用新前缀
```

## 线程名称示例

```java
NamedThreadFactory factory = new NamedThreadFactory("dtp");

// 创建的线程名称：
// dtp-1
// dtp-2
// dtp-3
// ...
```

## 设计特点

### 1. 自动编号

使用 `AtomicInteger` 自动为线程编号，保证唯一性。

### 2. 线程安全

使用 `AtomicInteger` 保证线程安全。

### 3. 可配置

支持设置守护线程和优先级。

## 注意事项

1. **线程组**: 线程组由 `SecurityManager` 或当前线程决定
2. **序号递增**: 序号从 1 开始，每次递增
3. **前缀修改**: 修改前缀只影响新创建的线程

