# ExtensionServiceLoader

## 概述

`ExtensionServiceLoader` 是统一的 ServiceLoader 辅助类，用于通过 Java SPI（Service Provider Interface）机制加载扩展服务。它提供了缓存机制，避免重复加载，提高了性能和易用性。

## 核心作用

1. **SPI 加载**: 通过 Java SPI 机制加载扩展服务
2. **缓存机制**: 缓存已加载的服务，避免重复加载
3. **统一接口**: 提供统一的加载接口，简化 SPI 使用
4. **性能优化**: 通过缓存减少重复加载的开销

## 类结构

```java
public class ExtensionServiceLoader {
    // 缓存已加载的服务
    private static final Map<Class<?>, List<?>> EXTENSION_MAP = new ConcurrentHashMap<>();
    
    // 私有构造方法，防止实例化
    private ExtensionServiceLoader() { }
    
    // 加载所有服务
    public static <T> List<T> get(Class<T> clazz)
    
    // 加载第一个服务
    public static <T> T getFirst(Class<T> clazz)
    
    // 内部加载方法
    private static <T> List<T> load(Class<T> clazz)
}
```

## 核心方法

### 1. get(Class<T> clazz)

**作用**: 加载指定接口的所有实现类

**参数**: `clazz` - SPI 接口类

**返回**: 实现类列表

**实现流程**:

```java
public static <T> List<T> get(Class<T> clazz) {
    // 1. 从缓存中获取
    List<T> services = (List<T>) EXTENSION_MAP.get(clazz);
    
    // 2. 如果缓存中没有，则加载
    if (CollectionUtils.isEmpty(services)) {
        services = load(clazz);
        
        // 3. 如果加载到服务，放入缓存
        if (CollectionUtils.isNotEmpty(services)) {
            EXTENSION_MAP.put(clazz, services);
        }
    }
    
    // 4. 返回服务列表
    return services;
}
```

**特点**:
- **缓存机制**: 首次加载后缓存，后续直接返回缓存结果
- **线程安全**: 使用 `ConcurrentHashMap` 保证线程安全
- **空值处理**: 如果没有找到服务，返回空列表

---

### 2. getFirst(Class<T> clazz)

**作用**: 加载指定接口的第一个实现类

**参数**: `clazz` - SPI 接口类

**返回**: 第一个实现类，如果没有则返回 `null`

**实现**:

```java
public static <T> T getFirst(Class<T> clazz) {
    List<T> services = get(clazz);
    return CollectionUtils.isEmpty(services) ? null : services.get(0);
}
```

**特点**:
- **便捷方法**: 当只需要一个实现时，使用此方法更简单
- **返回第一个**: 返回列表中的第一个实现
- **空值处理**: 如果没有找到服务，返回 `null`

---

### 3. load(Class<T> clazz)

**作用**: 内部方法，实际执行 SPI 加载

**实现**:

```java
private static <T> List<T> load(Class<T> clazz) {
    // 1. 使用 ServiceLoader 加载
    ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
    
    // 2. 遍历并收集所有实现
    List<T> services = new ArrayList<>();
    for (T service : serviceLoader) {
        services.add(service);
    }
    
    // 3. 返回服务列表
    return services;
}
```

**说明**:
- 使用 Java 标准的 `ServiceLoader` 机制
- 遍历所有实现并收集到列表中
- 私有方法，只能通过 `get()` 或 `getFirst()` 调用

## 使用场景

### 场景 1: 加载任务包装器

在 `TaskWrappers` 中加载扩展的任务包装器：

```java
public class TaskWrappers {
    private TaskWrappers() {
        // 通过 SPI 加载扩展的任务包装器
        List<TaskWrapper> loadedWrappers = ExtensionServiceLoader.get(TaskWrapper.class);
        if (CollectionUtils.isNotEmpty(loadedWrappers)) {
            TASK_WRAPPERS.addAll(loadedWrappers);
        }
        
        // 添加内置包装器
        TASK_WRAPPERS.add(new TtlTaskWrapper());
        TASK_WRAPPERS.add(new MdcTaskWrapper());
    }
}
```

**SPI 配置文件**: `META-INF/services/org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper`

---

### 场景 2: 加载配置绑定器

在 `BinderHelper` 中加载配置绑定器：

```java
private static PropertiesBinder getBinder() {
    // 先从单例获取
    PropertiesBinder binder = Singleton.INST.get(PropertiesBinder.class);
    if (Objects.nonNull(binder)) {
        return binder;
    }
    
    // 通过 SPI 加载第一个实现
    final PropertiesBinder loadedFirstBinder = ExtensionServiceLoader.getFirst(PropertiesBinder.class);
    if (Objects.isNull(loadedFirstBinder)) {
        log.error("No SPI for PropertiesBinder.");
        return null;
    }
    
    // 缓存到单例
    Singleton.INST.single(PropertiesBinder.class, loadedFirstBinder);
    return loadedFirstBinder;
}
```

**说明**: 使用 `getFirst()` 获取第一个实现，通常配置绑定器只有一个实现。

---

### 场景 3: 加载感知器

在 `AwareManager` 中加载扩展的感知器：

```java
static {
    // 添加内置感知器
    EXECUTOR_AWARE_LIST.add(new PerformanceMonitorAware());
    EXECUTOR_AWARE_LIST.add(new TaskTimeoutAware());
    EXECUTOR_AWARE_LIST.add(new TaskRejectAware());
    
    // 通过 SPI 加载扩展的感知器
    List<ExecutorAware> serviceLoader = ExtensionServiceLoader.get(ExecutorAware.class);
    EXECUTOR_AWARE_LIST.addAll(serviceLoader);
    
    // 按顺序排序
    EXECUTOR_AWARE_LIST.sort(Comparator.comparingInt(ExecutorAware::getOrder));
}
```

**说明**: 加载所有扩展的感知器，并与内置感知器合并。

---

### 场景 4: 加载初始化器

在 `DtpInitializerExecutor` 中加载初始化器：

```java
public static void init(Object... args) {
    // 通过 SPI 加载所有初始化器
    List<DtpInitializer> loadedInitializers = ExtensionServiceLoader.get(DtpInitializer.class);
    if (CollectionUtils.isEmpty(loadedInitializers)) {
        return;
    }
    
    // 按顺序排序
    loadedInitializers.sort(Comparator.comparingInt(DtpInitializer::getOrder));
    
    // 执行初始化
    loadedInitializers.forEach(i -> i.init(args));
}
```

---

### 场景 5: 加载上下文管理器

在 `ContextManagerHelper` 中加载上下文管理器：

```java
static {
    // 通过 SPI 加载第一个上下文管理器
    contextManager = ExtensionServiceLoader.getFirst(ContextManager.class);
    if (contextManager == null) {
        contextManager = new NullContextManager();
        throw new IllegalStateException("No ContextManager implementation found");
    }
}
```

## SPI 配置方式

### 1. 创建 SPI 接口

```java
public interface MyExtension {
    void doSomething();
}
```

### 2. 实现接口

```java
public class MyExtensionImpl implements MyExtension {
    @Override
    public void doSomething() {
        // 实现逻辑
    }
}
```

### 3. 创建 SPI 配置文件

在 `src/main/resources/META-INF/services/` 目录下创建文件：

**文件名**: `org.dromara.dynamictp.common.plugin.MyExtension`

**文件内容**:
```
com.example.MyExtensionImpl
```

### 4. 使用 ExtensionServiceLoader 加载

```java
// 加载所有实现
List<MyExtension> extensions = ExtensionServiceLoader.get(MyExtension.class);

// 或加载第一个实现
MyExtension extension = ExtensionServiceLoader.getFirst(MyExtension.class);
```

## 设计特点

### 1. 缓存机制

使用 `ConcurrentHashMap` 缓存已加载的服务：

- **性能优化**: 避免重复加载，提高性能
- **线程安全**: 使用 `ConcurrentHashMap` 保证线程安全
- **内存效率**: 每个接口只加载一次，减少内存占用

### 2. 统一接口

提供统一的加载接口，简化 SPI 使用：

- **简单易用**: 不需要直接使用 `ServiceLoader`
- **类型安全**: 通过泛型保证类型安全
- **空值处理**: 自动处理空值情况

### 3. 支持多实现

`get()` 方法返回所有实现，支持多个扩展：

```java
// 加载所有实现
List<MyExtension> extensions = ExtensionServiceLoader.get(MyExtension.class);
for (MyExtension ext : extensions) {
    ext.doSomething();
}
```

### 4. 支持单实现

`getFirst()` 方法返回第一个实现，适用于单例场景：

```java
// 加载第一个实现
MyExtension extension = ExtensionServiceLoader.getFirst(MyExtension.class);
if (extension != null) {
    extension.doSomething();
}
```

## 与 Java ServiceLoader 的区别

| 特性 | ExtensionServiceLoader | Java ServiceLoader |
|------|----------------------|-------------------|
| **缓存** | ✅ 支持缓存 | ❌ 不支持缓存 |
| **易用性** | ✅ 简单易用 | ⚠️ 需要手动遍历 |
| **性能** | ✅ 性能更好（缓存） | ⚠️ 每次都要加载 |
| **返回类型** | ✅ List 或单个对象 | ⚠️ Iterator |
| **空值处理** | ✅ 自动处理 | ⚠️ 需要手动处理 |

## 注意事项

### 1. SPI 配置文件位置

SPI 配置文件必须放在 `META-INF/services/` 目录下，文件名必须是接口的全限定名。

### 2. 类加载器

`ServiceLoader.load()` 使用当前线程的上下文类加载器，确保类加载器正确。

### 3. 缓存时机

服务在首次调用 `get()` 或 `getFirst()` 时加载并缓存，后续调用直接返回缓存结果。

### 4. 线程安全

`ExtensionServiceLoader` 是线程安全的，可以在多线程环境中使用。

### 5. 空值处理

- `get()` 方法：如果没有找到服务，返回空列表（不会返回 `null`）
- `getFirst()` 方法：如果没有找到服务，返回 `null`

## 最佳实践

### 1. 使用 get() 加载多个实现

当可能有多个实现时，使用 `get()` 方法：

```java
List<MyExtension> extensions = ExtensionServiceLoader.get(MyExtension.class);
for (MyExtension ext : extensions) {
    // 处理每个实现
}
```

### 2. 使用 getFirst() 加载单例实现

当只需要一个实现时，使用 `getFirst()` 方法：

```java
MyExtension extension = ExtensionServiceLoader.getFirst(MyExtension.class);
if (extension != null) {
    // 使用实现
}
```

### 3. 结合内置实现使用

通常与内置实现结合使用：

```java
// 添加内置实现
list.add(new BuiltinImpl());

// 加载扩展实现
List<Extension> extensions = ExtensionServiceLoader.get(Extension.class);
list.addAll(extensions);
```

### 4. 按顺序排序

如果实现有顺序要求，加载后排序：

```java
List<MyExtension> extensions = ExtensionServiceLoader.get(MyExtension.class);
extensions.sort(Comparator.comparingInt(MyExtension::getOrder));
```

## 总结

`ExtensionServiceLoader` 是框架中 SPI 机制的统一入口，提供了：

- ✅ **缓存机制**: 避免重复加载，提高性能
- ✅ **统一接口**: 简化 SPI 使用
- ✅ **类型安全**: 通过泛型保证类型安全
- ✅ **线程安全**: 支持多线程环境
- ✅ **易用性**: 提供 `get()` 和 `getFirst()` 两种方法

它是框架扩展机制的基础，为框架的可扩展性提供了重要支持。

