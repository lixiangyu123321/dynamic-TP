# NativeUtil 详解

## 文件位置

```
jvmti/jvmti-runtime/src/main/java/org/dromara/dynamictp/jvmti/NativeUtil.java
```

## 概述

`NativeUtil` 是用于从 JAR 包中加载原生库（Native Library）的工具类。它解决了在打包成 JAR 文件时，原生库文件无法直接从 JAR 中加载的问题。通过将原生库从 JAR 包中提取到临时目录，然后使用 `System.load()` 加载，实现了原生库的自动加载。

## 类声明

```java
public class NativeUtil {
    // ...
}
```

**设计特点**：
- **工具类**：使用私有构造函数，防止实例化
- **静态方法**：所有方法都是静态方法
- **临时目录管理**：自动创建和管理临时目录

## 核心常量

```java
private static final String NATIVE_FOLDER_PATH_PREFIX = "dtp_native_";
```

**作用**：临时目录名称前缀，用于创建唯一命名的临时目录。

## 核心属性

```java
private static File temporaryDir;
```

**作用**：存储临时目录的引用，避免重复创建。

**特点**：
- 静态变量，整个 JVM 生命周期中共享
- 延迟初始化，只在第一次使用时创建
- 设置为退出时删除（`deleteOnExit()`）

## 核心方法

### loadLibraryFromJar(String filename)

```java
public static void loadLibraryFromJar(String filename) throws IOException
```

**功能**：从 JAR 包中加载原生库文件

**参数**：
- `filename`：原生库文件名（如 `libJniLibrary-x64.so`）

**异常**：
- `IllegalArgumentException`：如果文件名为空
- `IOException`：如果读取或写入文件失败
- `FileNotFoundException`：如果 JAR 包中找不到文件

**实现流程**：

1. **参数验证**：
```java
if (StringUtils.isBlank(filename)) {
    throw new IllegalArgumentException("The filename cannot be null");
}
```

2. **创建临时目录**（如果不存在）：
```java
if (temporaryDir == null) {
    temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX);
    temporaryDir.deleteOnExit();
}
```

3. **创建临时文件**：
```java
File temp = new File(temporaryDir, filename);
```

4. **从 JAR 包复制文件到临时目录**：
```java
try (InputStream is = NativeUtil.class.getClassLoader().getResourceAsStream(filename)) {
    assert is != null;
    Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
} catch (IOException e) {
    Files.delete(temp.toPath());
    throw e;
} catch (NullPointerException e) {
    Files.delete(temp.toPath());
    throw new FileNotFoundException("File " + filename + " was not found inside JAR.");
}
```

5. **加载原生库**：
```java
try {
    System.load(temp.getAbsolutePath());
} finally {
    temp.deleteOnExit();
}
```

**完整实现**：
```java
public static void loadLibraryFromJar(String filename) throws IOException {
    if (StringUtils.isBlank(filename)) {
        throw new IllegalArgumentException("The filename cannot be null");
    }

    if (temporaryDir == null) {
        temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX);
        temporaryDir.deleteOnExit();
    }

    File temp = new File(temporaryDir, filename);
    try (InputStream is = NativeUtil.class.getClassLoader().getResourceAsStream(filename)) {
        assert is != null;
        Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
        Files.delete(temp.toPath());
        throw e;
    } catch (NullPointerException e) {
        Files.delete(temp.toPath());
        throw new FileNotFoundException("File " + filename + " was not found inside JAR.");
    }

    try {
        System.load(temp.getAbsolutePath());
    } finally {
        temp.deleteOnExit();
    }
}
```

**关键点**：
- 使用 `try-with-resources` 确保 InputStream 关闭
- 使用 `StandardCopyOption.REPLACE_EXISTING` 覆盖已存在的文件
- 异常处理时删除临时文件，避免残留
- 设置临时文件退出时删除（`deleteOnExit()`）

### createTempDirectory(String prefix)

```java
private static File createTempDirectory(String prefix) throws IOException
```

**功能**：创建临时目录

**参数**：
- `prefix`：目录名称前缀

**返回值**：创建的临时目录 File 对象

**实现**：
```java
private static File createTempDirectory(String prefix) throws IOException {
    String tempDir = System.getProperty("java.io.tmpdir");
    File generatedDir = new File(tempDir, prefix + System.nanoTime());
    if (!generatedDir.mkdir()) {
        throw new IOException("Failed to create temp directory " + generatedDir.getName());
    }
    return generatedDir;
}
```

**实现逻辑**：
1. 获取系统临时目录路径（`java.io.tmpdir`）
2. 创建目录名：`{prefix}{nanoTime}`（使用纳秒时间戳确保唯一性）
3. 创建目录，如果失败抛出异常
4. 返回目录 File 对象

**特点**：
- 使用 `System.nanoTime()` 确保目录名唯一
- 在系统临时目录下创建
- 如果创建失败，抛出 `IOException`

## 工作原理

### 1. JAR 包中原生库的限制

Java 的 `System.loadLibrary()` 和 `System.load()` 方法只能加载文件系统中的文件，无法直接从 JAR 包中加载。

**问题**：
- JAR 包是压缩文件，其中的文件不是独立的文件系统文件
- 原生库需要是独立的可执行文件

**解决方案**：
1. 从 JAR 包中读取原生库文件（通过 `ClassLoader.getResourceAsStream()`）
2. 写入临时目录（解压到文件系统）
3. 从临时目录加载（使用 `System.load()`）

### 2. 加载流程

```
调用 loadLibraryFromJar(filename)
    ↓
检查临时目录是否存在
    ↓ (不存在)
创建临时目录 (createTempDirectory)
    ↓
从 JAR 包读取文件 (getResourceAsStream)
    ↓
写入临时目录 (Files.copy)
    ↓
加载原生库 (System.load)
    ↓
设置退出时删除 (deleteOnExit)
    ↓
完成
```

### 3. 文件位置

**JAR 包中的位置**：
```
jvmti-runtime/src/main/resources/
    ├── libJniLibrary-x64.dll
    ├── libJniLibrary-x64.so
    └── libJniLibrary.dylib
```

**临时目录的位置**：
```
{java.io.tmpdir}/dtp_native_{nanoTime}/
    ├── libJniLibrary-x64.dll
    ├── libJniLibrary-x64.so
    └── libJniLibrary.dylib
```

## 设计特点

### 1. 自动清理

- 临时目录和文件都设置了 `deleteOnExit()`
- JVM 退出时自动删除，避免磁盘空间浪费

### 2. 异常安全

- 异常处理时删除已创建的临时文件
- 使用 `try-with-resources` 确保资源释放

### 3. 幂等性

- 多次调用会覆盖已存在的文件（`REPLACE_EXISTING`）
- 临时目录只创建一次

### 4. 线程安全

- 静态变量 `temporaryDir` 的并发访问问题（虽然通常只在初始化时访问）
- `System.load()` 本身是线程安全的

## 使用场景

### 1. JVMTI 初始化

在 `JVMTI` 类的静态初始化块中使用：

```java
static {
    try {
        NativeUtil.loadLibraryFromJar(JVMTIUtil.detectLibName());
        AVAILABLE.set(true);
    } catch (Throwable t) {
        log.error("JVMTI initialization failed!", t);
    }
}
```

### 2. 动态加载原生库

```java
try {
    String libName = "libJniLibrary-x64.so";
    NativeUtil.loadLibraryFromJar(libName);
    System.out.println("原生库加载成功");
} catch (FileNotFoundException e) {
    System.err.println("JAR 包中找不到原生库文件");
} catch (IOException e) {
    System.err.println("加载原生库失败: " + e.getMessage());
}
```

## 注意事项

### 1. 文件路径

- 原生库文件必须在 Classpath 的根目录或 `resources` 目录下
- 文件名必须与 JAR 包中的文件名完全匹配（包括大小写）

### 2. 临时目录权限

- 需要系统临时目录的写权限
- 如果无法创建临时目录，会抛出 `IOException`

### 3. 文件系统限制

- 某些操作系统对临时目录的文件数量有限制
- 临时文件可能被系统清理策略删除（虽然设置了 `deleteOnExit()`）

### 4. 安全性

- 从 JAR 包加载的文件是受信任的
- 但临时文件可能被其他进程访问（取决于操作系统权限）

### 5. 性能考虑

- 每次调用都会复制文件（虽然有缓存机制）
- 如果频繁调用，可能影响性能

## 错误处理

### 常见错误

1. **文件不存在**：
```
FileNotFoundException: File libJniLibrary-x64.so was not found inside JAR.
```
**原因**：JAR 包中找不到指定的原生库文件
**解决**：检查文件名是否正确，文件是否在 resources 目录下

2. **无法创建临时目录**：
```
IOException: Failed to create temp directory dtp_native_xxx
```
**原因**：没有临时目录的写权限或磁盘空间不足
**解决**：检查系统临时目录权限和磁盘空间

3. **加载失败**：
```
UnsatisfiedLinkError: ...
```
**原因**：原生库加载失败（架构不匹配、依赖缺失等）
**解决**：检查原生库是否与当前平台匹配

## 相关类

- **JVMTIUtil**：检测原生库文件名
- **JVMTI**：使用 `NativeUtil` 加载原生库
- **OSUtils**：平台检测（间接使用）

## 替代方案

### 1. 使用 System.loadLibrary()

需要将原生库放在系统库路径（如 `LD_LIBRARY_PATH`）：
```java
System.loadLibrary("JniLibrary"); // 不需要 .so 后缀
```

**缺点**：需要手动配置库路径，不够灵活。

### 2. 手动解压

手动将原生库解压到指定目录，然后加载：
```java
// 手动解压到 /path/to/libs/
System.load("/path/to/libs/libJniLibrary-x64.so");
```

**缺点**：不够自动化，需要手动操作。

### 3. 使用第三方库

使用 `jnr-ffi`、`JNA` 等第三方库：
```java
// 使用 JNA
NativeLibrary.getInstance("JniLibrary");
```

**缺点**：增加依赖，可能影响性能。

## 最佳实践

### 1. 错误处理

```java
try {
    NativeUtil.loadLibraryFromJar(libName);
} catch (FileNotFoundException e) {
    log.warn("原生库文件不存在，某些功能可能不可用");
} catch (IOException e) {
    log.error("加载原生库失败", e);
}
```

### 2. 检查加载结果

```java
try {
    NativeUtil.loadLibraryFromJar(libName);
    // 验证加载是否成功（如果原生库有测试方法）
} catch (Exception e) {
    // 降级处理
}
```

### 3. 清理临时文件（可选）

虽然设置了 `deleteOnExit()`，但如果需要立即清理：

```java
// 注意：需要保存 temporaryDir 的引用
// 当前实现中 temporaryDir 是私有的，无法外部访问
```

## 改进建议

1. **线程安全**：考虑使用 `synchronized` 或 `ConcurrentHashMap` 保护 `temporaryDir`
2. **缓存机制**：检查文件是否已加载，避免重复加载
3. **路径验证**：验证临时目录的可写性
4. **日志记录**：记录加载过程和临时文件位置

