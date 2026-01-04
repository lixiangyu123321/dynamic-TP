# Optional 使用指南

## 概述

`Optional` 是 Java 8 引入的一个容器类，用于表示可能不存在的值。它旨在解决 `null` 引用带来的问题，提供更优雅的空值处理方式。

### 核心定位

- **空值容器**: 用于包装可能为 null 的值
- **空值安全**: 避免 `NullPointerException`
- **函数式编程**: 支持链式调用和函数式操作
- **API 设计**: 明确表示返回值可能不存在

### 主要特性

1. **类型安全**: 编译时类型检查，避免运行时异常
2. **函数式风格**: 支持 map、filter、flatMap 等函数式操作
3. **链式调用**: 支持流畅的链式 API
4. **明确语义**: 在 API 中明确表示返回值可能为空

## 核心方法

### 1. 创建 Optional

#### Optional.of(T value)

创建一个包含非 null 值的 Optional，如果值为 null 会抛出 `NullPointerException`。

```java
// 值不为 null
Optional<String> optional = Optional.of("hello");
System.out.println(optional.get());  // "hello"

// 值为 null，抛出异常
Optional<String> optional = Optional.of(null);  // 抛出 NullPointerException
```

**使用场景**: 确定值不为 null 时使用。

#### Optional.ofNullable(T value)

创建一个可能为 null 的 Optional，如果值为 null 返回 `Optional.empty()`。

```java
// 值不为 null
Optional<String> optional = Optional.ofNullable("hello");
System.out.println(optional.isPresent());  // true

// 值为 null
Optional<String> optional = Optional.ofNullable(null);
System.out.println(optional.isPresent());  // false
```

**使用场景**: 值可能为 null 时使用（最常用）。

#### Optional.empty()

创建一个空的 Optional。

```java
Optional<String> optional = Optional.empty();
System.out.println(optional.isPresent());  // false
```

**使用场景**: 明确表示没有值。

### 2. 判断和获取值

#### isPresent()

判断 Optional 是否包含值。

```java
Optional<String> optional = Optional.of("hello");
if (optional.isPresent()) {
    System.out.println(optional.get());
}
```

#### isEmpty() (Java 11+)

判断 Optional 是否为空（与 `!isPresent()` 等价）。

```java
Optional<String> optional = Optional.empty();
if (optional.isEmpty()) {
    System.out.println("No value");
}
```

#### get()

获取值，如果为空会抛出 `NoSuchElementException`。

```java
Optional<String> optional = Optional.of("hello");
String value = optional.get();  // "hello"

Optional<String> empty = Optional.empty();
String value = empty.get();  // 抛出 NoSuchElementException
```

**注意**: 应该先检查 `isPresent()` 或使用其他安全方法。

#### orElse(T other)

如果值存在则返回，否则返回默认值。

```java
Optional<String> optional = Optional.of("hello");
String value = optional.orElse("default");  // "hello"

Optional<String> empty = Optional.empty();
String value = empty.orElse("default");  // "default"
```

**特点**: 即使值存在，也会执行默认值的创建（如果默认值是方法调用）。

#### orElseGet(Supplier<? extends T> supplier)

如果值存在则返回，否则通过 Supplier 获取默认值。

```java
Optional<String> optional = Optional.of("hello");
String value = optional.orElseGet(() -> "default");  // "hello"

Optional<String> empty = Optional.empty();
String value = empty.orElseGet(() -> "default");  // "default"
```

**优势**: 只有在值不存在时才执行 Supplier，性能更好。

**性能对比**:
```java
// orElse: 即使值存在也会执行 expensiveOperation()
String value = optional.orElse(expensiveOperation());

// orElseGet: 只有值不存在时才执行 expensiveOperation()
String value = optional.orElseGet(() -> expensiveOperation());
```

#### orElseThrow()

如果值存在则返回，否则抛出 `NoSuchElementException`。

```java
Optional<String> optional = Optional.of("hello");
String value = optional.orElseThrow();  // "hello"

Optional<String> empty = Optional.empty();
String value = empty.orElseThrow();  // 抛出 NoSuchElementException
```

#### orElseThrow(Supplier<? extends X> exceptionSupplier)

如果值存在则返回，否则抛出指定异常。

```java
Optional<String> optional = Optional.empty();
String value = optional.orElseThrow(() -> new IllegalArgumentException("Value not found"));
```

### 3. 函数式操作

#### ifPresent(Consumer<? super T> consumer)

如果值存在，执行 Consumer。

```java
Optional<String> optional = Optional.of("hello");
optional.ifPresent(value -> System.out.println(value));  // 打印 "hello"

Optional<String> empty = Optional.empty();
empty.ifPresent(value -> System.out.println(value));  // 不执行
```

#### ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) (Java 9+)

如果值存在执行 action，否则执行 emptyAction。

```java
Optional<String> optional = Optional.of("hello");
optional.ifPresentOrElse(
    value -> System.out.println(value),
    () -> System.out.println("No value")
);  // 打印 "hello"

Optional<String> empty = Optional.empty();
empty.ifPresentOrElse(
    value -> System.out.println(value),
    () -> System.out.println("No value")
);  // 打印 "No value"
```

#### map(Function<? super T, ? extends U> mapper)

如果值存在，应用 Function 进行转换，返回新的 Optional。

```java
Optional<String> optional = Optional.of("hello");
Optional<Integer> length = optional.map(String::length);  // Optional[5]

Optional<String> empty = Optional.empty();
Optional<Integer> length = empty.map(String::length);  // Optional.empty
```

**特点**: 
- 如果值为空，返回 `Optional.empty()`
- 如果 mapper 返回 null，会抛出 `NullPointerException`

#### flatMap(Function<? super T, Optional<U>> mapper)

如果值存在，应用 Function 进行转换，然后扁平化。

```java
Optional<String> optional = Optional.of("hello");
Optional<Character> firstChar = optional.flatMap(s -> 
    s.isEmpty() ? Optional.empty() : Optional.of(s.charAt(0))
);  // Optional['h']
```

**与 map 的区别**:
```java
// map: Function 返回普通值，自动包装成 Optional
Optional<Optional<String>> nested = optional.map(s -> Optional.of(s.toUpperCase()));
// 结果：Optional[Optional["HELLO"]]

// flatMap: Function 返回 Optional，直接展开
Optional<String> flat = optional.flatMap(s -> Optional.of(s.toUpperCase()));
// 结果：Optional["HELLO"]
```

#### filter(Predicate<? super T> predicate)

如果值存在且满足条件，返回该 Optional，否则返回空。

```java
Optional<String> optional = Optional.of("hello");
Optional<String> filtered = optional.filter(s -> s.length() > 3);  // Optional["hello"]

Optional<String> filtered2 = optional.filter(s -> s.length() > 10);  // Optional.empty

Optional<String> empty = Optional.empty();
Optional<String> filtered3 = empty.filter(s -> s.length() > 3);  // Optional.empty
```

### 4. 流式操作 (Java 9+)

#### stream()

将 Optional 转换为 Stream。

```java
Optional<String> optional = Optional.of("hello");
List<String> list = optional.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());  // ["HELLO"]

Optional<String> empty = Optional.empty();
List<String> list2 = empty.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());  // []
```

## 在项目中的使用

### 1. NotifyHelper.getPlatform()

```java
public static Optional<NotifyPlatform> getPlatform(String platformId) {
    Map<String, NotifyPlatform> platformMap = getAllPlatforms();
    return Optional.ofNullable(platformMap.get(platformId));
}
```

**说明**: 
- 使用 `Optional.ofNullable()` 包装可能为 null 的值
- 返回 `Optional<NotifyPlatform>` 明确表示可能不存在

**使用方式**:
```java
Optional<NotifyPlatform> platform = NotifyHelper.getPlatform(platformId);
if (platform.isPresent()) {
    NotifyPlatform p = platform.get();
    // 使用平台
}

// 或者使用 orElse
NotifyPlatform platform = NotifyHelper.getPlatform(platformId)
    .orElse(getDefaultPlatform());

// 或者使用 orElseGet
NotifyPlatform platform = NotifyHelper.getPlatform(platformId)
    .orElseGet(() -> createDefaultPlatform());
```

### 2. MicroMeterCollector.getTags()

```java
tags.add(Tag.of(
    POOL_ALIAS_TAG, 
    Optional.ofNullable(poolStats.getPoolAliasName())
        .orElse(poolStats.getPoolName())
));
```

**说明**:
- 使用 `Optional.ofNullable()` 处理可能为 null 的值
- 使用 `orElse()` 提供默认值（别名不存在时使用名称）

### 3. AbstractDtpNotifier.buildAlarmContent()

```java
Optional.ofNullable(lastAlarmTime).orElse(UNKNOWN)
```

**说明**:
- 处理可能为 null 的最后告警时间
- 如果为 null，使用 "UNKNOWN" 作为默认值

### 4. ExecutorAdapter.getRejectedExecutionHandlerType()

```java
return Optional.ofNullable(getRejectedExecutionHandler())
    .map(RejectedExecutionHandler::getClass)
    .map(Class::getSimpleName)
    .orElse("Unknown");
```

**说明**:
- 使用 `map()` 进行链式转换
- 如果任何步骤为 null，返回默认值

## 使用场景

### 1. 方法返回值

**推荐**: 当方法可能返回 null 时，返回 `Optional<T>`。

```java
// ✅ 推荐：明确表示可能为空
public Optional<User> findUserById(Long id) {
    User user = userRepository.findById(id);
    return Optional.ofNullable(user);
}

// ❌ 不推荐：直接返回 null
public User findUserById(Long id) {
    return userRepository.findById(id);  // 可能为 null
}
```

**使用**:
```java
Optional<User> user = findUserById(1L);
user.ifPresent(u -> System.out.println(u.getName()));
```

### 2. 空值检查和默认值

```java
// ✅ 推荐：使用 Optional
String name = Optional.ofNullable(user.getName())
    .orElse("Unknown");

// ❌ 不推荐：使用 if-else
String name;
if (user.getName() != null) {
    name = user.getName();
} else {
    name = "Unknown";
}
```

### 3. 链式转换

```java
// ✅ 推荐：使用 map 进行链式转换
Optional<String> upperName = Optional.ofNullable(user)
    .map(User::getName)
    .map(String::toUpperCase);

// ❌ 不推荐：嵌套的 if-else
String upperName = null;
if (user != null && user.getName() != null) {
    upperName = user.getName().toUpperCase();
}
```

### 4. 条件过滤

```java
// ✅ 推荐：使用 filter
Optional<User> adultUser = Optional.ofNullable(user)
    .filter(u -> u.getAge() >= 18);

// ❌ 不推荐：使用 if
User adultUser = null;
if (user != null && user.getAge() >= 18) {
    adultUser = user;
}
```

### 5. 异常处理

```java
// ✅ 推荐：使用 orElseThrow
String name = Optional.ofNullable(user)
    .map(User::getName)
    .orElseThrow(() -> new IllegalArgumentException("User name is required"));

// ❌ 不推荐：手动检查
if (user == null || user.getName() == null) {
    throw new IllegalArgumentException("User name is required");
}
String name = user.getName();
```

## 最佳实践

### 1. 不要滥用 Optional

```java
// ❌ 不推荐：作为字段类型
public class User {
    private Optional<String> name;  // 不推荐
}

// ✅ 推荐：字段可以为 null，方法返回 Optional
public class User {
    private String name;  // 可以为 null
    
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }
}
```

### 2. 不要使用 Optional 作为参数

```java
// ❌ 不推荐：Optional 作为参数
public void process(Optional<String> name) {
    // ...
}

// ✅ 推荐：参数可以为 null
public void process(String name) {
    Optional.ofNullable(name).ifPresent(n -> {
        // 处理
    });
}
```

### 3. 优先使用 orElseGet 而不是 orElse

```java
// ⚠️ 性能问题：即使值存在也会执行 expensiveOperation()
String value = optional.orElse(expensiveOperation());

// ✅ 推荐：只有值不存在时才执行
String value = optional.orElseGet(() -> expensiveOperation());
```

### 4. 使用 map 和 flatMap 进行链式转换

```java
// ✅ 推荐：链式转换
Optional<String> result = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase);

// ❌ 不推荐：嵌套的 if-else
String result = null;
if (user != null && user.getAddress() != null 
    && user.getAddress().getCity() != null) {
    result = user.getAddress().getCity().toUpperCase();
}
```

### 5. 结合 Stream API 使用

```java
List<String> names = users.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// 或者
List<String> names = users.stream()
    .map(u -> Optional.ofNullable(u.getName()))
    .filter(Optional::isPresent)
    .map(Optional::get)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### 6. 避免嵌套 Optional

```java
// ❌ 不推荐：嵌套 Optional
Optional<Optional<String>> nested = Optional.ofNullable(getNestedOptional());

// ✅ 推荐：使用 flatMap
Optional<String> flat = Optional.ofNullable(getNestedOptional())
    .flatMap(Function.identity());
```

## 常见模式

### 1. 空值检查和默认值

```java
// 模式 1: orElse
String value = Optional.ofNullable(value).orElse("default");

// 模式 2: orElseGet
String value = Optional.ofNullable(value).orElseGet(() -> getDefault());

// 模式 3: orElseThrow
String value = Optional.ofNullable(value)
    .orElseThrow(() -> new IllegalArgumentException("Value required"));
```

### 2. 链式调用

```java
// 模式 1: map
Optional<String> result = Optional.ofNullable(user)
    .map(User::getName)
    .map(String::toUpperCase);

// 模式 2: flatMap
Optional<String> result = Optional.ofNullable(user)
    .flatMap(u -> u.getOptionalName())
    .map(String::toUpperCase);
```

### 3. 条件执行

```java
// 模式 1: ifPresent
Optional.ofNullable(value).ifPresent(v -> process(v));

// 模式 2: ifPresentOrElse (Java 9+)
Optional.ofNullable(value).ifPresentOrElse(
    v -> process(v),
    () -> handleEmpty()
);
```

### 4. 过滤

```java
Optional<User> adultUser = Optional.ofNullable(user)
    .filter(u -> u.getAge() >= 18);
```

## 常见错误

### 1. 使用 get() 前不检查

```java
// ❌ 错误：可能抛出 NoSuchElementException
String value = optional.get();

// ✅ 正确：先检查或使用其他方法
String value = optional.orElse("default");
```

### 2. 使用 Optional.of() 处理可能为 null 的值

```java
// ❌ 错误：如果 value 为 null 会抛出异常
Optional<String> optional = Optional.of(value);

// ✅ 正确：使用 ofNullable
Optional<String> optional = Optional.ofNullable(value);
```

### 3. 使用 == 或 equals 比较 Optional

```java
// ❌ 错误：比较 Optional 对象本身
if (optional == Optional.empty()) { }

// ✅ 正确：使用 isPresent() 或 isEmpty()
if (optional.isEmpty()) { }
```

### 4. 在集合中使用 Optional

```java
// ❌ 不推荐：集合中存储 Optional
List<Optional<String>> list = ...;

// ✅ 推荐：集合中存储实际值，使用 null 表示不存在
List<String> list = ...;
```

## 性能考虑

### 1. orElse vs orElseGet

```java
// orElse: 总是执行默认值创建
String value = optional.orElse(expensiveOperation());

// orElseGet: 只有值不存在时才执行
String value = optional.orElseGet(() -> expensiveOperation());
```

**建议**: 如果默认值创建成本高，使用 `orElseGet`。

### 2. Optional 的开销

Optional 是一个对象，会有额外的内存开销。在性能敏感的代码中，如果确定值不会为 null，可以考虑直接使用值。

```java
// 性能敏感场景
if (value != null) {
    process(value);
}

// 非性能敏感场景
Optional.ofNullable(value).ifPresent(this::process);
```

## 总结

Optional 是 Java 8 引入的重要特性，用于优雅地处理空值：

1. **创建**: 使用 `Optional.ofNullable()` 处理可能为 null 的值
2. **获取**: 使用 `orElse()`、`orElseGet()`、`orElseThrow()` 安全获取值
3. **转换**: 使用 `map()` 和 `flatMap()` 进行链式转换
4. **过滤**: 使用 `filter()` 进行条件过滤
5. **执行**: 使用 `ifPresent()` 执行操作

**使用原则**:
- ✅ 方法返回值使用 Optional
- ✅ 使用函数式方法（map、filter、flatMap）
- ❌ 不要作为字段类型
- ❌ 不要作为方法参数
- ❌ 不要滥用，简单场景可以直接使用 null 检查

合理使用 Optional 可以让代码更加清晰、安全、易读。

