# Immutable 不可变对象系列

## 概述

**Immutable（不可变对象）** 是指一旦创建后就不能被修改的对象。在 Java 中，不可变对象具有以下特点：
- 对象状态在创建后不能被改变
- 所有字段都是 final 的
- 没有提供修改状态的方法
- 如果包含可变对象的引用，需要深度防御性复制

### 核心优势

1. **线程安全**: 不可变对象天然线程安全，无需同步
2. **简化并发**: 多线程环境下无需考虑状态变化
3. **避免副作用**: 不会意外修改对象状态
4. **易于理解**: 对象状态不会变化，更容易理解和维护
5. **缓存友好**: 可以安全地缓存和重用

## Immutable 原理

### 1. 不可变性的实现方式

#### 方式一：final 字段 + 无 setter 方法

```java
public final class ImmutablePoint {
    private final int x;
    private final int y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    // 没有 setter 方法
    // 所有字段都是 final
}
```

#### 方式二：防御性复制

如果包含可变对象的引用，需要进行防御性复制：

```java
public final class ImmutablePerson {
    private final String name;
    private final List<String> hobbies;  // 可变对象
    
    public ImmutablePerson(String name, List<String> hobbies) {
        this.name = name;
        // 防御性复制：创建新的不可变列表
        this.hobbies = Collections.unmodifiableList(new ArrayList<>(hobbies));
    }
    
    public List<String> getHobbies() {
        // 返回不可变视图，防止外部修改
        return hobbies;
    }
}
```

#### 方式三：使用不可变集合

```java
import com.google.common.collect.ImmutableList;

public final class ImmutablePerson {
    private final String name;
    private final ImmutableList<String> hobbies;
    
    public ImmutablePerson(String name, List<String> hobbies) {
        this.name = name;
        this.hobbies = ImmutableList.copyOf(hobbies);
    }
    
    public ImmutableList<String> getHobbies() {
        return hobbies;
    }
}
```

### 2. 不可变性的保证

#### 字段声明

```java
// 所有字段都应该是 final
private final String name;
private final int age;
private final List<String> tags;
```

#### 构造方法

```java
// 构造方法中完成所有初始化
public ImmutablePerson(String name, int age, List<String> tags) {
    this.name = name;
    this.age = age;
    this.tags = ImmutableList.copyOf(tags);  // 防御性复制
}
```

#### 方法设计

```java
// 不提供修改方法
// 如果需要"修改"，返回新对象
public ImmutablePerson withAge(int newAge) {
    return new ImmutablePerson(this.name, newAge, this.tags);
}
```

### 3. 线程安全性

不可变对象天然线程安全：

```java
// 多个线程可以安全地共享同一个不可变对象
ImmutableList<String> list = ImmutableList.of("a", "b", "c");

// 线程 1
Thread t1 = new Thread(() -> {
    list.forEach(System.out::println);  // 安全
});

// 线程 2
Thread t2 = new Thread(() -> {
    list.forEach(System.out::println);  // 安全
});

// 无需同步，因为对象不可变
```

## Java 标准库的不可变集合

### Collections.unmodifiable* 方法

Java 标准库提供了 `Collections.unmodifiable*` 方法创建不可变视图：

#### Collections.unmodifiableList

```java
List<String> mutableList = new ArrayList<>();
mutableList.add("a");
mutableList.add("b");

// 创建不可变视图
List<String> unmodifiableList = Collections.unmodifiableList(mutableList);

// 可以读取
System.out.println(unmodifiableList.get(0));  // "a"

// 不能修改（抛出 UnsupportedOperationException）
// unmodifiableList.add("c");  // 抛出异常

// 注意：原始列表的修改会影响视图
mutableList.add("c");
System.out.println(unmodifiableList.size());  // 3（已变化）
```

**特点**:
- 返回的是视图，不是真正的不可变集合
- 原始集合的修改会影响视图
- 适合作为返回值，防止外部修改

#### Collections.unmodifiableMap

```java
Map<String, Integer> mutableMap = new HashMap<>();
mutableMap.put("a", 1);
mutableMap.put("b", 2);

// 创建不可变视图
Map<String, Integer> unmodifiableMap = Collections.unmodifiableMap(mutableMap);

// 可以读取
System.out.println(unmodifiableMap.get("a"));  // 1

// 不能修改
// unmodifiableMap.put("c", 3);  // 抛出异常
```

#### Collections.unmodifiableSet

```java
Set<String> mutableSet = new HashSet<>();
mutableSet.add("a");
mutableSet.add("b");

// 创建不可变视图
Set<String> unmodifiableSet = Collections.unmodifiableSet(mutableSet);

// 可以读取
System.out.println(unmodifiableSet.contains("a"));  // true

// 不能修改
// unmodifiableSet.add("c");  // 抛出异常
```

### Collections.empty* 方法

创建空的不可变集合：

```java
// 空列表
List<String> emptyList = Collections.emptyList();
// emptyList.add("a");  // 抛出异常

// 空映射
Map<String, Integer> emptyMap = Collections.emptyMap();
// emptyMap.put("a", 1);  // 抛出异常

// 空集合
Set<String> emptySet = Collections.emptySet();
// emptySet.add("a");  // 抛出异常
```

### Collections.singleton* 方法

创建包含单个元素的不可变集合：

```java
// 单元素列表
List<String> singletonList = Collections.singletonList("a");
// singletonList.add("b");  // 抛出异常

// 单元素映射
Map<String, Integer> singletonMap = Collections.singletonMap("a", 1);
// singletonMap.put("b", 2);  // 抛出异常

// 单元素集合
Set<String> singletonSet = Collections.singleton("a");
// singletonSet.add("b");  // 抛出异常
```

### 在项目中的使用

```java
// StreamUtil.java
public static <I, T> List<I> fetchProperty(Collection<T> data, Function<T, I> mapping) {
    if (CollectionUtils.isEmpty(data)) {
        return Collections.emptyList();  // 返回空不可变列表
    }
    return data.stream().map(mapping).collect(Collectors.toList());
}

public static <P, O> Map<O, P> toMap(Collection<P> coll, Function<P, O> key) {
    if (CollectionUtils.isEmpty(coll)) {
        return Collections.emptyMap();  // 返回空不可变映射
    }
    return coll.stream().collect(Collectors.toMap(key, Function.identity()));
}
```

## Guava Immutable 系列

Google Guava 提供了真正的不可变集合实现，比 `Collections.unmodifiable*` 更安全。

### ImmutableList

#### 创建方式

```java
import com.google.common.collect.ImmutableList;

// 方式 1: of() 方法
ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");

// 方式 2: copyOf() 方法
List<String> mutableList = new ArrayList<>();
mutableList.add("a");
mutableList.add("b");
ImmutableList<String> list2 = ImmutableList.copyOf(mutableList);

// 方式 3: Builder 模式
ImmutableList<String> list3 = ImmutableList.<String>builder()
    .add("a")
    .add("b")
    .add("c")
    .build();

// 方式 4: 从数组创建
String[] array = {"a", "b", "c"};
ImmutableList<String> list4 = ImmutableList.copyOf(array);
```

#### 特点

- **真正的不可变**: 创建后完全不可修改
- **线程安全**: 天然线程安全
- **性能优化**: 针对不可变场景优化
- **空值处理**: `of()` 方法不接受 null，`copyOf()` 会抛出异常如果包含 null

#### 在项目中的使用

```java
// DynamicTpConst.java
public static final List<NotifyItemEnum> SCHEDULE_NOTIFY_ITEMS = 
    ImmutableList.of(
        NotifyItemEnum.LIVENESS,
        NotifyItemEnum.CAPACITY,
        NotifyItemEnum.REJECT
    );
```

### ImmutableSet

```java
import com.google.common.collect.ImmutableSet;

// 创建不可变集合
ImmutableSet<String> set1 = ImmutableSet.of("a", "b", "c");

// 从可变集合创建
Set<String> mutableSet = new HashSet<>();
mutableSet.add("a");
mutableSet.add("b");
ImmutableSet<String> set2 = ImmutableSet.copyOf(mutableSet);

// Builder 模式
ImmutableSet<String> set3 = ImmutableSet.<String>builder()
    .add("a")
    .add("b")
    .add("c")
    .build();
```

### ImmutableMap

```java
import com.google.common.collect.ImmutableMap;

// 创建不可变映射
ImmutableMap<String, Integer> map1 = ImmutableMap.of(
    "a", 1,
    "b", 2,
    "c", 3
);

// 从可变映射创建
Map<String, Integer> mutableMap = new HashMap<>();
mutableMap.put("a", 1);
mutableMap.put("b", 2);
ImmutableMap<String, Integer> map2 = ImmutableMap.copyOf(mutableMap);

// Builder 模式（推荐用于多个键值对）
ImmutableMap<String, Integer> map3 = ImmutableMap.<String, Integer>builder()
    .put("a", 1)
    .put("b", 2)
    .put("c", 3)
    .build();
```

### ImmutableMultimap

```java
import com.google.common.collect.ImmutableMultimap;

// 创建不可变多值映射
ImmutableMultimap<String, Integer> multimap = ImmutableMultimap.<String, Integer>builder()
    .put("a", 1)
    .put("a", 2)
    .put("b", 3)
    .build();

// 获取值
Collection<Integer> values = multimap.get("a");  // [1, 2]
```

### ImmutableBiMap

```java
import com.google.common.collect.ImmutableBiMap;

// 创建不可变双向映射
ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of(
    "one", 1,
    "two", 2,
    "three", 3
);

// 可以通过值查找键
String key = biMap.inverse().get(1);  // "one"
```

## Apache Commons Lang ImmutablePair

### ImmutablePair 概述

`ImmutablePair` 是 Apache Commons Lang 提供的不可变键值对实现。

### 使用方式

```java
import org.apache.commons.lang3.tuple.ImmutablePair;

// 创建不可变键值对
ImmutablePair<String, Integer> pair = new ImmutablePair<>("name", 100);

// 或者使用静态方法
ImmutablePair<String, Integer> pair2 = ImmutablePair.of("name", 100);

// 获取键和值
String key = pair.getLeft();    // "name"
Integer value = pair.getRight(); // 100

// 不能修改
// pair.setLeft("newName");  // 没有 setter 方法
```

### 在项目中的使用

```java
// DtpDingNotifier.java
private Pair<String, String> getColors() {
    return new ImmutablePair<>(
        DingNotifyConst.WARNING_COLOR,
        DingNotifyConst.CONTENT_COLOR
    );
}

// DtpLarkNotifier.java
private Pair<String, String> getColors() {
    return new ImmutablePair<>(
        LarkNotifyConst.WARNING_COLOR,
        LarkNotifyConst.COMMENT_COLOR
    );
}
```

### 与其他 Pair 实现的对比

| 特性 | ImmutablePair | Pair | MutablePair |
|------|--------------|------|-------------|
| **不可变性** | ✅ 完全不可变 | ⚠️ 取决于实现 | ❌ 可变 |
| **线程安全** | ✅ 天然线程安全 | ⚠️ 取决于实现 | ❌ 需要同步 |
| **推荐使用** | ✅ 推荐 | ⚠️ 不推荐 | ❌ 不推荐 |

## 使用场景

### 1. 常量定义

```java
// 定义不可变常量集合
public static final ImmutableList<String> SUPPORTED_LANGUAGES = 
    ImmutableList.of("Java", "Python", "JavaScript");

public static final ImmutableMap<String, String> CONFIG_DEFAULTS = 
    ImmutableMap.of(
        "timeout", "3000",
        "retry", "3"
    );
```

### 2. 方法返回值

```java
// 返回不可变集合，防止外部修改
public List<String> getTags() {
    return ImmutableList.copyOf(internalTags);
}

// 返回不可变映射
public Map<String, Integer> getCounts() {
    return ImmutableMap.copyOf(internalCounts);
}
```

### 3. 多线程共享

```java
// 多个线程可以安全地共享不可变对象
ImmutableList<String> sharedList = ImmutableList.of("a", "b", "c");

// 线程 1
executor1.execute(() -> {
    sharedList.forEach(System.out::println);  // 安全
});

// 线程 2
executor2.execute(() -> {
    sharedList.forEach(System.out::println);  // 安全
});
```

### 4. 缓存键

```java
// 使用不可变对象作为缓存键
ImmutableList<String> cacheKey = ImmutableList.of("user", "123", "profile");
cache.put(cacheKey, userProfile);
```

### 5. 配置对象

```java
// 配置对象应该是不可变的
public final class AppConfig {
    private final String appName;
    private final ImmutableList<String> allowedHosts;
    private final ImmutableMap<String, String> properties;
    
    public AppConfig(String appName, List<String> hosts, Map<String, String> props) {
        this.appName = appName;
        this.allowedHosts = ImmutableList.copyOf(hosts);
        this.properties = ImmutableMap.copyOf(props);
    }
    
    // 只有 getter，没有 setter
}
```

## 最佳实践

### 1. 优先使用 Guava Immutable

```java
// ✅ 推荐：使用 Guava Immutable
ImmutableList<String> list = ImmutableList.of("a", "b", "c");

// ⚠️ 不推荐：使用 Collections.unmodifiableList
List<String> list = Collections.unmodifiableList(new ArrayList<>());
```

**原因**:
- Guava Immutable 是真正的不可变
- 性能更好
- API 更丰富

### 2. 防御性复制

```java
// ✅ 正确：防御性复制
public ImmutablePerson(String name, List<String> hobbies) {
    this.name = name;
    this.hobbies = ImmutableList.copyOf(hobbies);  // 复制
}

// ❌ 错误：直接引用
public ImmutablePerson(String name, List<String> hobbies) {
    this.name = name;
    this.hobbies = hobbies;  // 危险：外部可以修改
}
```

### 3. 返回不可变视图

```java
// ✅ 推荐：返回不可变集合
public List<String> getTags() {
    return ImmutableList.copyOf(internalTags);
}

// ⚠️ 可以：返回不可变视图（如果原始集合不会变化）
public List<String> getTags() {
    return Collections.unmodifiableList(internalTags);
}
```

### 4. 使用 Builder 模式

```java
// ✅ 推荐：使用 Builder 模式（多个元素时）
ImmutableList<String> list = ImmutableList.<String>builder()
    .add("a")
    .add("b")
    .add("c")
    .build();

// ✅ 也可以：使用 of() 方法（少量元素时）
ImmutableList<String> list = ImmutableList.of("a", "b", "c");
```

### 5. 避免 null 值

```java
// ❌ 错误：ImmutableList.of() 不接受 null
ImmutableList<String> list = ImmutableList.of("a", null, "c");  // 抛出异常

// ✅ 正确：使用 copyOf()，但会抛出异常如果包含 null
List<String> mutable = Arrays.asList("a", null, "c");
ImmutableList<String> list = ImmutableList.copyOf(mutable);  // 抛出异常

// ✅ 正确：过滤 null
List<String> mutable = Arrays.asList("a", null, "c");
ImmutableList<String> list = mutable.stream()
    .filter(Objects::nonNull)
    .collect(ImmutableList.toImmutableList());
```

## 性能考虑

### 1. 内存占用

- **ImmutableList**: 针对不可变场景优化，内存占用更小
- **Collections.unmodifiableList**: 只是包装，内存占用与原集合相同

### 2. 创建性能

```java
// 少量元素：of() 方法最快
ImmutableList.of("a", "b", "c");

// 大量元素：Builder 模式更高效
ImmutableList.<String>builder()
    .addAll(largeList)
    .build();
```

### 3. 访问性能

- 不可变集合的访问性能与普通集合相同
- 由于不需要同步，多线程环境下性能更好

## 常见问题

### 1. Collections.unmodifiableList 不是真正的不可变

```java
List<String> mutable = new ArrayList<>();
mutable.add("a");
List<String> unmodifiable = Collections.unmodifiableList(mutable);

// 原始列表的修改会影响视图
mutable.add("b");
System.out.println(unmodifiable.size());  // 2（已变化）

// 解决方案：使用 Guava ImmutableList
ImmutableList<String> immutable = ImmutableList.copyOf(mutable);
mutable.add("c");
System.out.println(immutable.size());  // 2（不变）
```

### 2. 包含可变对象的不可变集合

```java
// ❌ 错误：集合不可变，但元素可变
List<StringBuilder> mutable = new ArrayList<>();
mutable.add(new StringBuilder("a"));
ImmutableList<StringBuilder> immutable = ImmutableList.copyOf(mutable);

// 可以修改元素内容
immutable.get(0).append("b");  // 元素内容被修改

// ✅ 正确：深度不可变
List<String> strings = Arrays.asList("a", "b");
ImmutableList<String> immutable = ImmutableList.copyOf(strings);
// String 是不可变的，所以整个集合真正不可变
```

### 3. 性能 vs 安全性

```java
// 如果确定原始集合不会被修改，可以使用视图
List<String> internal = new ArrayList<>();
// ... 填充数据后不再修改
return Collections.unmodifiableList(internal);  // 性能更好

// 如果不确定，使用真正的不可变集合
return ImmutableList.copyOf(internal);  // 更安全
```

## 总结

Immutable 系列提供了创建和使用不可变对象的标准方式：

1. **Java 标准库**: `Collections.unmodifiable*` 提供不可变视图
2. **Guava**: `ImmutableList`、`ImmutableSet`、`ImmutableMap` 等提供真正的不可变集合
3. **Apache Commons**: `ImmutablePair` 提供不可变键值对

**选择建议**:
- **常量定义**: 使用 Guava Immutable
- **方法返回值**: 使用 Guava Immutable 或 Collections.unmodifiable*
- **多线程共享**: 使用 Guava Immutable
- **性能敏感**: 根据场景选择，Guava Immutable 通常性能更好

不可变对象是函数式编程和并发编程的重要概念，合理使用可以大大提高代码的安全性和可维护性。

