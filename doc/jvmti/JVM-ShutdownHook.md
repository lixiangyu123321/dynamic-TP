# JVM 关闭钩子（Shutdown Hook）详解

## 概述

JVM 关闭钩子（Shutdown Hook）是 Java 提供的一种机制，允许应用程序在 JVM 关闭前执行清理工作。当 JVM 收到关闭信号（如用户按下 Ctrl+C、系统关闭、调用 `System.exit()` 等）时，会在关闭前执行所有已注册的关闭钩子。

### 核心概念

- **关闭钩子**：通过 `Runtime.addShutdownHook()` 注册的线程，JVM 关闭时会执行该线程的 `run()` 方法
- **执行时机**：JVM 正常关闭时，在所有非守护线程结束之后执行
- **执行顺序**：多个钩子的执行顺序是不确定的，不能依赖执行顺序
- **执行时间**：钩子执行时间不能过长，否则可能导致 JVM 无法正常退出

## 核心 API

### Runtime.addShutdownHook()

```java
public void addShutdownHook(Thread hook)
```

**功能说明**：
- 注册一个在 JVM 关闭时执行的线程
- 钩子线程必须是一个未启动的线程
- 同一个钩子只能注册一次

**参数**：
- `hook`：要注册的关闭钩子线程（必须是未启动的线程）

**异常**：
- `IllegalArgumentException`：如果钩子已经启动，或者已经注册过
- `IllegalStateException`：如果 JVM 已经开始关闭流程

**返回值**：无

### Runtime.removeShutdownHook()

```java
public boolean removeShutdownHook(Thread hook)
```

**功能说明**：
- 取消之前注册的关闭钩子
- 必须在 JVM 关闭流程开始之前调用才有效

**参数**：
- `hook`：要移除的关闭钩子线程

**返回值**：
- `true`：成功移除
- `false`：钩子未注册或已经执行

## 使用场景

### 1. 资源清理

关闭钩子最常见的用途是确保资源在 JVM 退出前被正确释放：

```java
public class ResourceManager {
    private static FileLock lock;
    
    static {
        // 初始化资源
        lock = acquireFileLock();
        
        // 注册关闭钩子，确保资源释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (lock != null) {
                    lock.release();
                    System.out.println("File lock released");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
```

### 2. 线程池优雅关闭

确保线程池在 JVM 退出前正确关闭，避免线程泄漏：

```java
public class ExecutorManager {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    static {
        // 注册关闭钩子，优雅关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down executor...");
            executor.shutdown();
            try {
                // 等待任务完成，最多等待 30 秒
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    System.out.println("Executor did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Executor shutdown complete");
        }));
    }
}
```

### 3. 定时任务关闭

关闭定时任务线程池，防止 JVM 无法退出：

```java
public class SchedulerManager {
    private static final ScheduledExecutorService scheduler = 
        Executors.newSingleThreadScheduledExecutor();
    
    static {
        scheduler.scheduleAtFixedRate(() -> {
            // 定时任务逻辑
        }, 0, 1, TimeUnit.SECONDS);
        
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(
            new Thread(scheduler::shutdown)
        );
    }
}
```

### 4. 数据持久化

在 JVM 退出前保存关键数据：

```java
public class DataManager {
    private static Map<String, Object> cache = new ConcurrentHashMap<>();
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 保存缓存数据到磁盘
                saveCacheToDisk(cache);
                System.out.println("Cache saved successfully");
            } catch (Exception e) {
                System.err.println("Failed to save cache: " + e.getMessage());
            }
        }));
    }
}
```

### 5. 网络连接关闭

关闭网络连接、释放端口等：

```java
public class ServerManager {
    private static ServerSocket serverSocket;
    
    static {
        try {
            serverSocket = new ServerSocket(8080);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        System.out.println("Server socket closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 项目中的实际使用

### MemoryLimitCalculator 中的使用

在 `common/src/main/java/org/dromara/dynamictp/common/queue/MemoryLimitCalculator.java` 中，使用关闭钩子确保定时任务线程池正确关闭：

```java
public class MemoryLimitCalculator {
    private static final ScheduledExecutorService SCHEDULER = 
        Executors.newSingleThreadScheduledExecutor();
    
    static {
        // 启动定时任务
        SCHEDULER.scheduleWithFixedDelay(
            MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);
        
        // 注册JVM关闭钩子：JVM退出时优雅关闭定时任务线程池
        // 避免线程池残留导致JVM无法正常退出
        Runtime.getRuntime().addShutdownHook(
            new Thread(SCHEDULER::shutdown)
        );
    }
}
```

**设计要点**：
- 使用单线程定时任务线程池，每 50ms 刷新一次内存值
- 通过关闭钩子确保线程池在 JVM 退出时被关闭
- 使用 `shutdown()` 而不是 `shutdownNow()`，允许正在执行的任务完成

## 执行时机和顺序

### 执行时机

关闭钩子在以下情况下会被执行：

1. **正常关闭**：
   - 所有非守护线程执行完毕
   - 调用 `System.exit()`
   - 最后一个非守护线程结束

2. **异常关闭**：
   - 收到 `SIGTERM` 信号（Unix/Linux）
   - 收到 `SIGINT` 信号（Ctrl+C）
   - 系统关闭

3. **强制关闭**：
   - `kill -9` 或 `kill -KILL`（不会执行钩子）
   - `Runtime.halt()` 方法（不会执行钩子）

### 执行顺序

**重要**：多个关闭钩子的执行顺序是**不确定的**，不应该依赖执行顺序。

如果需要有序执行，可以在一个钩子中组织逻辑：

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 按顺序执行清理任务
    step1();
    step2();
    step3();
}));
```

## 注意事项和最佳实践

### 1. 钩子必须快速执行

关闭钩子不应该执行耗时操作，否则可能导致：
- JVM 无法及时退出
- 系统资源无法释放
- 用户体验差

**推荐做法**：
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 设置超时时间
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> future = executor.submit(() -> {
        // 清理逻辑
    });
    
    try {
        future.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
    } finally {
        executor.shutdown();
    }
}));
```

### 2. 异常处理

钩子中的异常不应传播，应该被捕获并记录：

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        // 清理逻辑
    } catch (Exception e) {
        // 记录日志，不要抛出异常
        System.err.println("Error in shutdown hook: " + e.getMessage());
        e.printStackTrace();
    }
}));
```

### 3. 避免死锁

钩子不应该等待其他线程，否则可能导致死锁：

```java
// 错误的做法
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 等待主线程结束 - 可能导致死锁
    mainThread.join();
}));

// 正确的做法
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 直接执行清理逻辑，不等待其他线程
    cleanup();
}));
```

### 4. 守护线程和关闭钩子

关闭钩子可以在守护线程中注册，但执行时是在非守护线程结束之后：

```java
Thread daemonThread = new Thread(() -> {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Shutdown hook executed");
    }));
    // 守护线程逻辑
});
daemonThread.setDaemon(true);
daemonThread.start();
```

### 5. 使用 ThreadLocal 的注意事项

钩子执行时，ThreadLocal 的值可能已经丢失，应避免依赖 ThreadLocal：

```java
// 不推荐在钩子中使用 ThreadLocal
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    String value = threadLocal.get(); // 可能为 null
}));
```

### 6. 钩子注册时机

钩子应该在应用程序初始化时注册，而不是在关闭时：

```java
// 正确的做法：在初始化时注册
public class Application {
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanup();
        }));
    }
}

// 错误的做法：在关闭时注册（可能已经来不及）
public class Application {
    public void shutdown() {
        // 此时注册可能已经来不及
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanup();
        }));
    }
}
```

## 常见问题和陷阱

### 1. 钩子未执行

**原因**：
- JVM 被强制终止（`kill -9`）
- 调用了 `Runtime.halt()`
- JVM 崩溃（如内存溢出）

**解决**：
- 无法完全避免，但可以通过正常关闭流程减少发生概率

### 2. 钩子执行时间过长

**问题**：
- JVM 无法及时退出
- 系统资源无法释放

**解决**：
- 设置超时时间
- 异步执行耗时操作
- 使用 `shutdownNow()` 强制终止

### 3. 重复注册钩子

**问题**：
```java
Thread hook = new Thread(() -> System.out.println("Hook"));
Runtime.getRuntime().addShutdownHook(hook);
Runtime.getRuntime().addShutdownHook(hook); // 抛出 IllegalArgumentException
```

**解决**：
```java
private static final Thread SHUTDOWN_HOOK = new Thread(() -> {
    // 钩子逻辑
});

static {
    Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
}
```

### 4. 钩子中注册新钩子

**问题**：在钩子执行时注册新钩子不会被执行

**解决**：所有钩子都应该在应用程序初始化时注册

### 5. 钩子中调用 System.exit()

**问题**：可能导致死循环或不可预期的行为

**解决**：避免在钩子中调用 `System.exit()`

## 高级用法

### 1. 钩子管理器

创建一个管理类来统一管理所有关闭钩子：

```java
public class ShutdownHookManager {
    private static final List<Runnable> hooks = new ArrayList<>();
    private static boolean registered = false;
    
    public static synchronized void addHook(Runnable hook) {
        if (!registered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (Runnable h : hooks) {
                    try {
                        h.run();
                    } catch (Exception e) {
                        System.err.println("Error executing hook: " + e.getMessage());
                    }
                }
            }));
            registered = true;
        }
        hooks.add(hook);
    }
    
    public static synchronized void removeHook(Runnable hook) {
        hooks.remove(hook);
    }
}
```

### 2. 有序执行的钩子

如果需要有序执行，可以使用优先级：

```java
public class PrioritizedShutdownHook {
    private static final Map<Integer, List<Runnable>> hooks = new TreeMap<>();
    private static boolean registered = false;
    
    public static synchronized void addHook(int priority, Runnable hook) {
        hooks.computeIfAbsent(priority, k -> new ArrayList<>()).add(hook);
        
        if (!registered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (List<Runnable> hookList : hooks.values()) {
                    for (Runnable h : hookList) {
                        try {
                            h.run();
                        } catch (Exception e) {
                            System.err.println("Error executing hook: " + e.getMessage());
                        }
                    }
                }
            }));
            registered = true;
        }
    }
}
```

### 3. 带超时的钩子执行

```java
public class TimeoutShutdownHook {
    public static void addHook(Runnable hook, long timeout, TimeUnit unit) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(hook);
            
            try {
                future.get(timeout, unit);
            } catch (TimeoutException e) {
                future.cancel(true);
                System.err.println("Shutdown hook timeout");
            } catch (Exception e) {
                System.err.println("Error in shutdown hook: " + e.getMessage());
            } finally {
                executor.shutdown();
            }
        }));
    }
}
```

## 性能考虑

### 1. 钩子数量

- 钩子数量过多可能影响关闭性能
- 建议合并相关钩子，减少钩子数量

### 2. 钩子执行时间

- 每个钩子应该快速执行（建议 < 1 秒）
- 耗时操作应该异步执行或设置超时

### 3. 内存占用

- 钩子持有大量对象可能导致内存无法及时释放
- 在钩子中避免创建大对象

## 与 Spring 框架的集成

在 Spring 应用中，通常使用 `@PreDestroy` 或实现 `DisposableBean` 来处理资源清理，但在某些场景下仍需要使用关闭钩子：

```java
@Component
public class ResourceComponent implements DisposableBean {
    
    @PostConstruct
    public void init() {
        // Spring Bean 初始化后注册钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanup();
        }));
    }
    
    @Override
    public void destroy() {
        // Spring 容器关闭时也会调用
        cleanup();
    }
    
    private void cleanup() {
        // 清理逻辑
    }
}
```

## 测试关闭钩子

### 单元测试

```java
@Test
public void testShutdownHook() throws Exception {
    AtomicBoolean hookExecuted = new AtomicBoolean(false);
    
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        hookExecuted.set(true);
    }));
    
    // 触发 JVM 关闭（仅在测试中）
    Runtime.getRuntime().exit(0);
    
    // 注意：exit() 会终止 JVM，实际测试中应该使用其他方法
}
```

### 集成测试

```java
@Test
public void testShutdownHookIntegration() {
    // 使用 ProcessBuilder 启动新的 JVM 进程
    ProcessBuilder pb = new ProcessBuilder(
        "java", "-cp", classpath, "TestApplication"
    );
    
    Process process = pb.start();
    // 终止进程，观察钩子是否执行
    process.destroy();
    
    // 检查输出日志，确认钩子执行
}
```

## 总结

JVM 关闭钩子是一种强大的机制，用于在 JVM 退出前执行清理工作。正确使用关闭钩子可以：

1. **确保资源释放**：防止资源泄漏
2. **优雅关闭**：给应用程序机会完成关键操作
3. **数据持久化**：保存重要数据
4. **连接清理**：关闭网络连接、释放端口

**关键要点**：
- 钩子必须快速执行
- 做好异常处理
- 避免死锁和依赖执行顺序
- 在初始化时注册钩子
- 设置超时机制防止阻塞

**在 DynamicTp 项目中的应用**：
- `MemoryLimitCalculator` 使用关闭钩子确保定时任务线程池正确关闭
- 防止线程池残留导致 JVM 无法正常退出
- 保证资源清理的可靠性

## 参考资源

- [Oracle Java Documentation - Shutdown Hooks](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-)
- [Java Language Specification - Shutdown Hooks](https://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.8)
- [Effective Java - Item 9: Prefer try-with-resources to try-finally](https://www.oracle.com/java/technologies/javase/java-tutorial-writing-finalizers-and-cleanup-methods.html)

