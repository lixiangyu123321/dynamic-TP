# 并行流（Parallel Stream）与 Spliterator 详解

## 概述

并行流（Parallel Stream）是 Java 8 Stream API 提供的一种并行处理数据的方式。它利用 `Spliterator` 将数据分割成多个部分，在不同的线程上并行处理，最后合并结果，从而充分利用多核 CPU 的性能优势。

### 核心概念

- **并行流**：将数据流分割成多个子流，在多个线程上并行处理
- **Spliterator**：并行流的基础，负责数据分割和遍历
- **Fork-Join 框架**：并行流底层使用的并行处理框架
- **线程池**：并行流默认使用 `ForkJoinPool.commonPool()`

### 并行流 vs 顺序流

| 特性 | 顺序流 | 并行流 |
|------|--------|--------|
| 执行方式 | 单线程顺序执行 | 多线程并行执行 |
| 性能 | 适合小数据量 | 适合大数据量计算密集型任务 |
| 可预测性 | 执行顺序可预测 | 执行顺序可能不可预测 |
| 线程安全 | 不需要考虑线程安全 | 需要考虑线程安全 |
| 开销 | 无并行开销 | 有分割、调度、合并开销 |

## 创建并行流

### 1. 从 Collection 创建

```java
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// 方式1：直接创建并行流
Stream<Integer> parallelStream = list.parallelStream();

// 方式2：从顺序流转换为并行流
Stream<Integer> parallelStream = list.stream().parallel();

// 方式3：从并行流转换为顺序流
Stream<Integer> sequentialStream = list.parallelStream().sequential();
```

### 2. 从数组创建

```java
int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

// 方式1：使用 Arrays.stream()
IntStream parallelStream = Arrays.stream(array).parallel();

// 方式2：使用 Stream.of()
Stream<int[]> stream = Stream.of(array).parallel(); // 注意：这是 Stream<int[]>
```

### 3. 从 Spliterator 创建

```java
List<String> list = Arrays.asList("a", "b", "c", "d", "e");

// 获取 Spliterator
Spliterator<String> spliterator = list.spliterator();

// 使用 StreamSupport 创建并行流
Stream<String> parallelStream = StreamSupport.stream(spliterator, true);
// true = 并行流，false = 顺序流

// 使用并行流
parallelStream
    .map(String::toUpperCase)
    .forEach(System.out::println);
```

### 4. 从其他数据源创建

```java
// 从 Stream.iterate() 创建
Stream<Integer> parallelStream = Stream.iterate(0, n -> n + 1)
    .limit(100)
    .parallel();

// 从 Stream.generate() 创建
Stream<Double> parallelStream = Stream.generate(Math::random)
    .limit(1000)
    .parallel();

// 从 Pattern 创建
Pattern pattern = Pattern.compile(",");
Stream<String> parallelStream = pattern.splitAsStream("a,b,c,d,e")
    .parallel();
```

## 并行流的工作原理

### 整体流程

```
数据源 → Spliterator → 分割 → 多个子任务 → ForkJoinPool → 并行执行 → 合并结果
```

### 详细步骤

#### 1. 获取 Spliterator

```java
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

// 调用 Collection.parallelStream()
Stream<Integer> stream = list.parallelStream();

// 内部实现（简化版）
public default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
}

// spliterator() 方法返回该集合的 Spliterator
public Spliterator<E> spliterator() {
    // 不同的集合有不同的实现
    // ArrayList: 返回 ArraySpliterator
    // HashSet: 返回 HashMap.KeySpliterator
    // ...
}
```

#### 2. 分割数据（trySplit）

```java
// 并行流内部使用 Fork/Join 框架
// 框架会调用 Spliterator.trySplit() 来分割数据

Spliterator<Integer> original = list.spliterator();
Spliterator<Integer> firstHalf = original.trySplit();
Spliterator<Integer> secondHalf = original;

// 继续分割，直到数据量足够小或无法分割
// 通常分割到每个线程处理合适数量的元素（例如几千个）
```

#### 3. 并行执行

```java
// ForkJoinPool 使用工作窃取（work-stealing）算法
// 每个线程处理自己分配到的数据块

// 线程1处理 firstHalf
firstHalf.forEachRemaining(element -> process(element));

// 线程2处理 secondHalf
secondHalf.forEachRemaining(element -> process(element));

// 如果线程1先完成，可以"窃取"其他线程的任务
```

#### 4. 合并结果

```java
// 根据操作类型合并结果
// reduce 操作：合并各个线程的计算结果
// collect 操作：合并各个线程的收集结果
// forEach 操作：各个线程独立执行，无需合并
```

## Spliterator 如何支持并行流

### 1. 数据分割

```java
// 并行流通过 trySplit() 分割数据
public class ArraySpliterator<T> implements Spliterator<T> {
    private final T[] array;
    private int index;
    private final int fence; // 结束位置
    
    @Override
    public Spliterator<T> trySplit() {
        int lo = index, mid = (lo + fence) >>> 1; // 从中间分割
        return (lo >= mid)
            ? null  // 无法分割
            : new ArraySpliterator<>(array, lo, index = mid, characteristics);
    }
}
```

**分割策略**：
- **ArrayList**：从中间分割，高效且平衡
- **LinkedList**：通常不分割（返回 null），因为分割成本高
- **HashSet**：根据哈希桶分割

### 2. 特性标志影响并行行为

```java
// SIZED 特性：知道确切大小，可以优化并行策略
if (spliterator.hasCharacteristics(Spliterator.SIZED)) {
    long size = spliterator.estimateSize();
    // 可以根据大小决定是否并行，以及分割策略
}

// ORDERED 特性：需要保持顺序
if (spliterator.hasCharacteristics(Spliterator.ORDERED)) {
    // 并行处理时需要保持顺序，可能影响性能
}

// CONCURRENT 特性：可以并发修改
if (spliterator.hasCharacteristics(Spliterator.CONCURRENT)) {
    // 不需要额外的同步
}
```

### 3. 自定义 Spliterator 支持并行流

```java
public class CustomCollection<E> implements Collection<E> {
    private final List<E> elements;
    
    @Override
    public Spliterator<E> spliterator() {
        return new CustomSpliterator<>(elements, 0, elements.size());
    }
    
    static class CustomSpliterator<E> implements Spliterator<E> {
        private final List<E> list;
        private int start;
        private int end;
        
        CustomSpliterator(List<E> list, int start, int end) {
            this.list = list;
            this.start = start;
            this.end = end;
        }
        
        @Override
        public Spliterator<E> trySplit() {
            int size = end - start;
            if (size < 2) {
                return null; // 太小，无法分割
            }
            
            int mid = start + size / 2;
            CustomSpliterator<E> prefix = 
                new CustomSpliterator<>(list, start, mid);
            start = mid;
            return prefix;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            if (start < end) {
                action.accept(list.get(start++));
                return true;
            }
            return false;
        }
        
        @Override
        public long estimateSize() {
            return end - start;
        }
        
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | 
                   Spliterator.SIZED | 
                   Spliterator.SUBSIZED;
        }
    }
}

// 使用自定义集合的并行流
CustomCollection<String> collection = new CustomCollection<>();
collection.addAll(Arrays.asList("a", "b", "c", "d", "e"));

// 现在可以使用并行流
collection.parallelStream()
    .map(String::toUpperCase)
    .forEach(System.out::println);
```

## 常用并行流操作

### 1. 过滤和映射

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// 并行过滤偶数并映射为平方
List<Integer> result = numbers.parallelStream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .collect(Collectors.toList());

System.out.println(result); // [4, 16, 36, 64, 100]
```

### 2. 归约（Reduce）

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// 并行求和
int sum = numbers.parallelStream()
    .reduce(0, Integer::sum);

// 或使用 mapToInt
int sum = numbers.parallelStream()
    .mapToInt(Integer::intValue)
    .sum();

System.out.println(sum); // 55
```

### 3. 查找和匹配

```java
List<String> words = Arrays.asList("apple", "banana", "cherry", "date");

// 并行查找第一个匹配的元素
Optional<String> first = words.parallelStream()
    .filter(s -> s.startsWith("b"))
    .findFirst();

// 并行检查是否有匹配的元素
boolean anyMatch = words.parallelStream()
    .anyMatch(s -> s.length() > 5);

// 并行检查是否全部匹配
boolean allMatch = words.parallelStream()
    .allMatch(s -> s.length() > 3);
```

### 4. 收集（Collect）

```java
List<Person> people = Arrays.asList(
    new Person("Alice", 25),
    new Person("Bob", 30),
    new Person("Charlie", 25),
    new Person("David", 30)
);

// 并行分组
Map<Integer, List<Person>> groupedByAge = people.parallelStream()
    .collect(Collectors.groupingBy(Person::getAge));

// 并行收集到 Set
Set<String> names = people.parallelStream()
    .map(Person::getName)
    .collect(Collectors.toSet());

// 并行收集统计信息
IntSummaryStatistics stats = people.parallelStream()
    .mapToInt(Person::getAge)
    .summaryStatistics();
```

### 5. 排序

```java
List<Integer> numbers = Arrays.asList(5, 2, 8, 1, 9, 3);

// 并行排序（注意：排序在并行流中可能效率不高）
List<Integer> sorted = numbers.parallelStream()
    .sorted()
    .collect(Collectors.toList());

// 对于大数据集，可以使用并行流排序
// 对于小数据集，顺序流排序可能更快
```

### 6. 去重

```java
List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 5);

// 并行去重
List<Integer> distinct = numbers.parallelStream()
    .distinct()
    .collect(Collectors.toList());

System.out.println(distinct); // [1, 2, 3, 4, 5]
```

## 性能优化技巧

### 1. 选择合适的并行度

```java
// 默认使用 ForkJoinPool.commonPool()
// 可以通过系统属性调整并行度
System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");

// 或者使用自定义的 ForkJoinPool
ForkJoinPool customPool = new ForkJoinPool(8);

List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

customPool.submit(() -> {
    numbers.parallelStream()
        .map(n -> n * n)
        .forEach(System.out::println);
}).join();

customPool.shutdown();
```

### 2. 避免不必要的装箱拆箱

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 不好：频繁装箱拆箱
int sum = numbers.parallelStream()
    .map(n -> n * n)  // Integer -> Integer
    .reduce(0, Integer::sum);  // Integer -> int

// 好：使用原始类型流
int sum = numbers.parallelStream()
    .mapToInt(n -> n * n)  // Integer -> int
    .sum();  // 直接在 int 上操作
```

### 3. 使用无状态操作

```java
// 无状态操作：filter, map, flatMap, peek
// 可以很好地并行化

List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 这些操作可以高效并行
List<Integer> result = numbers.parallelStream()
    .filter(n -> n % 2 == 0)  // 无状态
    .map(n -> n * n)  // 无状态
    .collect(Collectors.toList());

// 有状态操作：sorted, distinct, limit, skip
// 并行化效果可能不好，可能降低性能
```

### 4. 避免顺序依赖

```java
List<String> words = Arrays.asList("apple", "banana", "cherry");

// 不好：依赖顺序
List<String> result = words.parallelStream()
    .sorted()  // 排序会影响并行性能
    .limit(2)  // limit 也会影响并行性能
    .collect(Collectors.toList());

// 如果不需要顺序，考虑使用无序流
List<String> result = words.parallelStream()
    .unordered()  // 明确声明无序，可能提高性能
    .distinct()
    .collect(Collectors.toList());
```

### 5. 合并操作的性能

```java
// collect 操作的合并器性能很重要
List<String> words = Arrays.asList("a", "b", "c", "d", "e");

// 使用高效的合并器
Set<String> result = words.parallelStream()
    .collect(Collectors.toCollection(HashSet::new));  // HashSet 合并快

// 避免使用 ArrayList 的 collect（合并慢）
List<String> slow = words.parallelStream()
    .collect(Collectors.toList());  // ArrayList 合并慢
```

## 何时使用并行流

### 适合使用并行流的场景

#### 1. 大数据集的计算密集型任务

```java
// 计算密集型任务，适合并行处理
List<Integer> numbers = IntStream.range(0, 1_000_000)
    .boxed()
    .collect(Collectors.toList());

long sum = numbers.parallelStream()
    .mapToLong(n -> {
        // 计算密集型操作
        long result = 0;
        for (int i = 0; i < 1000; i++) {
            result += n * i;
        }
        return result;
    })
    .sum();
```

#### 2. 独立的数据处理

```java
// 每个元素的处理是独立的
List<File> files = Arrays.asList(new File("file1.txt"), new File("file2.txt"));

List<String> contents = files.parallelStream()
    .map(file -> {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    })
    .collect(Collectors.toList());
```

#### 3. 可以分而治之的问题

```java
// 可以分割成独立子问题
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// 求最大值
Optional<Integer> max = numbers.parallelStream()
    .max(Integer::compareTo);

// 求总和
int sum = numbers.parallelStream()
    .reduce(0, Integer::sum);
```

### 不适合使用并行流的场景

#### 1. 小数据集

```java
// 数据集太小，并行开销大于收益
List<Integer> smallList = Arrays.asList(1, 2, 3, 4, 5);

// 顺序流更快
int sum = smallList.stream()
    .mapToInt(Integer::intValue)
    .sum();
```

#### 2. 顺序依赖的操作

```java
// 依赖顺序的操作
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// sorted, limit, skip 等操作在并行流中可能效率不高
List<Integer> result = numbers.parallelStream()
    .sorted()  // 排序在并行流中可能更慢
    .limit(3)  // limit 需要等待前面的元素
    .collect(Collectors.toList());
```

#### 3. 有状态的操作

```java
// 有状态的操作不适合并行
AtomicInteger counter = new AtomicInteger(0);

List<String> words = Arrays.asList("a", "b", "c");

// 错误：并行执行时状态不安全
words.parallelStream()
    .forEach(word -> {
        int count = counter.incrementAndGet();
        System.out.println(word + ": " + count);  // 结果不确定
    });
```

#### 4. I/O 密集型任务

```java
// I/O 操作通常不适合并行流
// 应该使用异步 I/O 或专门的 I/O 线程池

List<URL> urls = Arrays.asList(url1, url2, url3);

// 可能不是最优选择
List<String> contents = urls.parallelStream()
    .map(url -> {
        try {
            return url.openStream();  // I/O 操作
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    })
    .collect(Collectors.toList());
```

## 线程安全和注意事项

### 1. 非线程安全的操作

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 错误：使用非线程安全的集合
List<Integer> result = new ArrayList<>();  // 非线程安全
numbers.parallelStream()
    .forEach(result::add);  // 可能导致数据丢失或错误

// 正确：使用线程安全的收集器
List<Integer> result = numbers.parallelStream()
    .collect(Collectors.toList());  // 线程安全
```

### 2. 外部变量的线程安全

```java
List<String> words = Arrays.asList("apple", "banana", "cherry");

// 错误：共享可变状态
StringBuilder sb = new StringBuilder();  // 非线程安全
words.parallelStream()
    .forEach(sb::append);  // 可能导致数据损坏

// 正确：使用线程安全的收集
String result = words.parallelStream()
    .collect(Collectors.joining());  // 线程安全
```

### 3. 使用并发集合

```java
List<String> words = Arrays.asList("a", "b", "c", "d", "e");

// 使用 ConcurrentHashMap 收集
Map<String, Integer> result = words.parallelStream()
    .collect(Collectors.toConcurrentMap(
        word -> word,
        word -> word.length(),
        (a, b) -> a + b  // 合并函数（在并行时使用）
    ));
```

## 调试和监控

### 1. 检查并行执行

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

numbers.parallelStream()
    .peek(n -> {
        System.out.println("处理 " + n + " 在线程 " + Thread.currentThread().getName());
    })
    .map(n -> n * n)
    .forEach(n -> {
        System.out.println("结果 " + n + " 在线程 " + Thread.currentThread().getName());
    });
```

### 2. 性能测量

```java
List<Integer> numbers = IntStream.range(0, 10_000_000)
    .boxed()
    .collect(Collectors.toList());

// 顺序流
long start = System.currentTimeMillis();
long sum1 = numbers.stream()
    .mapToLong(n -> n * n)
    .sum();
long sequentialTime = System.currentTimeMillis() - start;

// 并行流
start = System.currentTimeMillis();
long sum2 = numbers.parallelStream()
    .mapToLong(n -> n * n)
    .sum();
long parallelTime = System.currentTimeMillis() - start;

System.out.println("顺序流时间: " + sequentialTime + "ms");
System.out.println("并行流时间: " + parallelTime + "ms");
System.out.println("加速比: " + (double) sequentialTime / parallelTime);
```

## 项目中的实际应用

### VariableLinkedBlockingQueue 的并行流支持

在 `common/src/main/java/org/dromara/dynamictp/common/queue/VariableLinkedBlockingQueue.java` 中，实现了自定义的 `LBQSpliterator`，使得队列支持并行流：

```java
// 获取队列的并行流
VariableLinkedBlockingQueue<String> queue = new VariableLinkedBlockingQueue<>();
queue.add("task1");
queue.add("task2");
queue.add("task3");

// 使用并行流处理队列中的元素
queue.parallelStream()
    .map(String::toUpperCase)
    .forEach(System.out::println);

// 并行流内部使用 LBQSpliterator
// LBQSpliterator 实现了：
// 1. trySplit() - 批量分割策略
// 2. tryAdvance() - 线程安全的元素访问
// 3. characteristics() - ORDERED | NONNULL | CONCURRENT
```

**关键特性**：
- **CONCURRENT**：支持并发修改，在遍历过程中队列可以被其他线程修改
- **ORDERED**：保持元素的插入顺序
- **NONNULL**：保证不包含 null 元素
- **批量分割**：使用批量数组收集元素，提高分割效率

## 最佳实践

### 1. 性能测试

在使用并行流之前，先进行性能测试：

```java
// 测试并行流是否真的更快
public static <T> void benchmark(List<T> data, 
                                 Function<Stream<T>, Long> operation) {
    // 顺序流
    long sequentialTime = measure(() -> operation.apply(data.stream()));
    
    // 并行流
    long parallelTime = measure(() -> operation.apply(data.parallelStream()));
    
    System.out.println("顺序流: " + sequentialTime + "ms");
    System.out.println("并行流: " + parallelTime + "ms");
    System.out.println("加速比: " + (double) sequentialTime / parallelTime);
}

private static long measure(Runnable task) {
    long start = System.currentTimeMillis();
    task.run();
    return System.currentTimeMillis() - start;
}
```

### 2. 使用合适的数据结构

```java
// ArrayList 的并行性能通常最好（随机访问、支持高效分割）
List<Integer> arrayList = new ArrayList<>(numbers);

// LinkedList 的并行性能较差（不支持高效分割）
List<Integer> linkedList = new LinkedList<>(numbers);

// HashSet 的并行性能取决于数据分布
Set<Integer> hashSet = new HashSet<>(numbers);
```

### 3. 避免不必要的并行转换

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 不好：频繁切换并行/顺序
numbers.parallelStream()
    .filter(n -> n % 2 == 0)
    .sequential()  // 切换到顺序流
    .map(n -> n * n)
    .parallel()  // 又切换回并行流（开销大）
    .collect(Collectors.toList());

// 好：保持一种模式
numbers.parallelStream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .collect(Collectors.toList());
```

### 4. 使用合适的终端操作

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 适合并行的操作：reduce, collect
long sum = numbers.parallelStream()
    .reduce(0, Integer::sum);

// 可能不适合并行的操作：forEach（如果顺序不重要，可以考虑）
numbers.parallelStream()
    .forEach(System.out::println);  // 输出顺序不确定
```

## 常见问题

### 1. 并行流不总是更快

**原因**：
- 数据量太小，并行开销大于收益
- 操作本身很快，并行化收益不明显
- 数据分割或合并成本高

**解决**：
- 先进行性能测试
- 确保数据集足够大（通常 > 10,000 元素）
- 确保操作是计算密集型的

### 2. 并行流的顺序不确定

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// 并行流的处理顺序可能不确定
numbers.parallelStream()
    .forEach(System.out::println);  // 可能不是 1, 2, 3, 4, 5

// 如果需要保持顺序，使用有序操作
numbers.parallelStream()
    .sorted()  // 会保持顺序，但可能影响性能
    .forEach(System.out::println);
```

### 3. 共享状态的问题

```java
// 错误示例
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
int sum = 0;  // 共享变量

numbers.parallelStream()
    .forEach(n -> sum += n);  // 线程不安全！

// 正确做法
int sum = numbers.parallelStream()
    .reduce(0, Integer::sum);  // 线程安全
```

## 总结

并行流是 Java 8 提供的强大工具，它通过 `Spliterator` 实现数据的并行处理：

1. **工作原理**：
   - 使用 `Spliterator` 分割数据
   - 使用 `ForkJoinPool` 并行执行
   - 合并各个线程的结果

2. **使用场景**：
   - 大数据集的计算密集型任务
   - 可以分而治之的问题
   - 独立的数据处理

3. **性能考虑**：
   - 数据集要足够大
   - 操作要是计算密集型的
   - 避免有状态和顺序依赖的操作

4. **线程安全**：
   - 使用线程安全的收集器
   - 避免共享可变状态
   - 确保 Spliterator 的特性设置正确

5. **在 DynamicTp 项目中的应用**：
   - `VariableLinkedBlockingQueue` 通过自定义 `LBQSpliterator` 支持并行流
   - 实现了线程安全的并行遍历
   - 使用批量分割策略提高性能

**关键要点**：
- 并行流通过 `Spliterator` 实现并行处理
- 正确设置 `Spliterator` 的特性可以提高并行性能
- 在使用并行流前要进行性能测试
- 注意线程安全和顺序问题
- 合理选择使用并行流的场景

## 参考资源

- [Oracle Java Documentation - Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)
- [Oracle Java Documentation - Spliterator](https://docs.oracle.com/javase/8/docs/api/java/util/Spliterator.html)
- [Oracle Java Documentation - StreamSupport](https://docs.oracle.com/javase/8/docs/api/java/util/stream/StreamSupport.html)
- [Oracle Java Documentation - ForkJoinPool](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html)

