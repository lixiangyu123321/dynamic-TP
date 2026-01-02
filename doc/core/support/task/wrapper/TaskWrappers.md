# TaskWrappers

## 概述

`TaskWrappers` 是任务包装器管理器，采用单例模式管理所有可用的任务包装器。它负责加载、注册和获取任务包装器。

## 核心作用

1. **包装器管理**: 统一管理所有任务包装器
2. **SPI 加载**: 通过 SPI 机制加载扩展包装器
3. **按名获取**: 根据名称获取指定的包装器列表

## 核心属性

```java
private static final List<TaskWrapper> TASK_WRAPPERS = Lists.newArrayList();
```

**说明**: 静态列表，存储所有可用的任务包装器

## 核心方法

### getInstance()

**作用**: 获取单例实例

**实现**: 使用内部类实现单例模式

```java
public static TaskWrappers getInstance() {
    return TaskWrappersHolder.INSTANCE;
}

private static class TaskWrappersHolder {
    private static final TaskWrappers INSTANCE = new TaskWrappers();
}
```

---

### 构造方法（私有）

**作用**: 初始化包装器列表

**实现流程**:

1. **加载 SPI 包装器**:
   ```java
   List<TaskWrapper> loadedWrappers = ExtensionServiceLoader.get(TaskWrapper.class);
   if (CollectionUtils.isNotEmpty(loadedWrappers)) {
       TASK_WRAPPERS.addAll(loadedWrappers);
   }
   ```

2. **添加内置包装器**:
   ```java
   TASK_WRAPPERS.add(new TtlTaskWrapper());
   TASK_WRAPPERS.add(new MdcTaskWrapper());
   ```

**说明**: 
- 首先加载通过 SPI 注册的包装器
- 然后添加框架内置的包装器（TTL 和 MDC）

---

### getByNames(Set<String> names)

**作用**: 根据名称集合获取包装器列表

**实现**:
```java
public List<TaskWrapper> getByNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
        return Collections.emptyList();
    }
    return TASK_WRAPPERS.stream()
            .filter(t -> StringUtil.containsIgnoreCase(t.name(), names))
            .collect(toList());
}
```

**说明**:
- 如果名称为空，返回空列表
- 使用流式处理，过滤出名称匹配的包装器
- 名称匹配不区分大小写

**使用场景**: 根据配置的包装器名称获取包装器列表

---

### register(TaskWrapper taskWrapper)

**作用**: 注册新的任务包装器

**实现**:
```java
public static void register(TaskWrapper taskWrapper) {
    Set<String> names = TASK_WRAPPERS.stream()
            .map(TaskWrapper::name)
            .collect(Collectors.toSet());
    if (names.contains(taskWrapper.name())) {
        return;  // 已存在，不重复注册
    }
    TASK_WRAPPERS.add(taskWrapper);
}
```

**说明**:
- 检查包装器名称是否已存在
- 如果已存在，不重复注册
- 如果不存在，添加到列表

**使用场景**: 编程式注册自定义包装器

## 包装器加载顺序

1. **SPI 包装器**: 通过 SPI 机制加载的扩展包装器
2. **内置包装器**: 框架内置的包装器（TtlTaskWrapper、MdcTaskWrapper）

## 使用场景

### 1. 获取配置的包装器

```java
Set<String> wrapperNames = Set.of("ttl", "mdc");
List<TaskWrapper> wrappers = TaskWrappers.getInstance().getByNames(wrapperNames);
```

### 2. 注册自定义包装器

```java
TaskWrapper customWrapper = new CustomTaskWrapper();
TaskWrappers.register(customWrapper);
```

### 3. 框架内部使用

框架在创建线程池时使用：

```java
Set<String> wrapperNames = executor.getTaskWrapperNames();
List<TaskWrapper> wrappers = TaskWrappers.getInstance().getByNames(wrapperNames);
executor.setTaskWrappers(wrappers);
```

## 设计特点

### 1. 单例模式

使用内部类实现线程安全的单例模式：

```java
private static class TaskWrappersHolder {
    private static final TaskWrappers INSTANCE = new TaskWrappers();
}
```

### 2. SPI 机制

支持通过 SPI 机制加载扩展包装器，提高可扩展性。

### 3. 名称去重

注册时检查名称，避免重复注册。

### 4. 大小写不敏感

名称匹配不区分大小写，提高易用性。

## 注意事项

1. **初始化时机**: 单例在首次调用 `getInstance()` 时初始化
2. **线程安全**: 使用静态 final 列表，初始化后不再修改（注册除外）
3. **名称唯一性**: 包装器名称应该唯一，避免冲突
4. **SPI 优先级**: SPI 加载的包装器在内置包装器之前

