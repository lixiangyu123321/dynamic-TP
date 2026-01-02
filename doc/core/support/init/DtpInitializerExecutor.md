# DtpInitializerExecutor

## 概述

`DtpInitializerExecutor` 是动态线程池初始化器执行器，负责执行所有注册的 `DtpInitializer`。它确保初始化器只执行一次，并按顺序执行。

## 核心作用

1. **初始化执行**: 执行所有注册的初始化器
2. **单次执行**: 确保初始化只执行一次
3. **有序执行**: 按顺序执行初始化器

## 核心方法

### init(Object... args)

**作用**: 执行所有初始化器

**实现**:
```java
public static void init(Object... args) {
    if (!INITIALIZED.compareAndSet(false, true)) {
        return;  // 已经初始化过，直接返回
    }
    List<DtpInitializer> loadedInitializers = ExtensionServiceLoader.get(DtpInitializer.class);
    if (CollectionUtils.isEmpty(loadedInitializers)) {
        return;  // 没有初始化器，直接返回
    }
    loadedInitializers.sort(Comparator.comparingInt(DtpInitializer::getOrder));
    loadedInitializers.forEach(i -> i.init(args));
}
```

**执行流程**:

1. **检查初始化状态**:
   ```java
   if (!INITIALIZED.compareAndSet(false, true)) {
       return;  // 已经初始化，直接返回
   }
   ```
   - 使用 `AtomicBoolean` 确保只执行一次
   - 使用 `compareAndSet` 保证原子性

2. **加载初始化器**:
   ```java
   List<DtpInitializer> loadedInitializers = ExtensionServiceLoader.get(DtpInitializer.class);
   ```
   - 通过 SPI 机制加载所有初始化器

3. **排序**:
   ```java
   loadedInitializers.sort(Comparator.comparingInt(DtpInitializer::getOrder));
   ```
   - 按 `getOrder()` 排序，顺序值越小越先执行

4. **执行**:
   ```java
   loadedInitializers.forEach(i -> i.init(args));
   ```
   - 依次执行每个初始化器

## 使用场景

### 1. 框架启动

框架在启动时调用：

```java
DtpInitializerExecutor.init();
```

### 2. 带参数初始化

传递初始化参数：

```java
DtpInitializerExecutor.init(applicationContext, config);
```

## 设计特点

### 1. 单次执行

使用 `AtomicBoolean` 确保初始化只执行一次：

```java
private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
```

### 2. 有序执行

按 `getOrder()` 排序，保证执行顺序。

### 3. SPI 机制

通过 SPI 机制加载初始化器，支持扩展。

## 注意事项

1. **单次执行**: 初始化只会执行一次，后续调用会直接返回
2. **执行顺序**: 通过 `getOrder()` 控制执行顺序
3. **异常处理**: 初始化器中的异常不会中断其他初始化器的执行

