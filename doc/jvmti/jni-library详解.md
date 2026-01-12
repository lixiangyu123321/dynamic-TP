# jni-library.cpp 详解

## 文件位置

```
jvmti/jvmti-build/src/main/native/src/jni-library.cpp
```

## 概述

`jni-library.cpp` 是 JVMTI 模块的原生库（Native Library）实现，使用 C++ 编写。它通过 JNI（Java Native Interface）和 JVMTI（JVM Tool Interface）技术实现 Java 代码无法完成的功能，如获取 JVM 堆中某个类的所有存活实例、强制触发 GC 等。

## 文件结构

该文件是一个 C++ 源文件，包含：
- JNI 接口实现
- JVMTI Agent 初始化
- 堆对象遍历逻辑
- GC 强制触发功能

## 核心头文件

```cpp
#include <stdio.h>
#include <jni.h>        // JNI 头文件
#include <jni_md.h>     // JNI 平台相关定义
#include <jvmti.h>      // JVMTI 头文件
#include "org_dromara_dynamictp_jvmti_JVMTI.h"  // JNI 生成的头文件
```

## 全局变量

### jvmtiEnv

```cpp
static jvmtiEnv *jvmti;
```

**作用**：JVMTI 环境指针，用于调用 JVMTI API

**生命周期**：在 Agent 初始化时设置，整个进程生命周期有效

### tagCounter

```cpp
static jlong tagCounter = 0;
```

**作用**：Tag 计数器，为每个对象生成唯一的标记

**使用场景**：在遍历堆对象时，为每个匹配的对象分配唯一的 tag

### LimitCounter 结构体

```cpp
struct LimitCounter {
    jint currentCounter;  // 当前计数器值
    jint limitValue;      // 限制值

    void init(jint limit) {
        currentCounter = 0;
        limitValue = limit;
    }

    void countDown() {
        currentCounter++;
    }

    bool allow() {
        if (limitValue < 0) {
            return true;  // 负数表示不限制
        }
        return limitValue > currentCounter;
    }
};
```

**作用**：用于限制获取的实例数量

**方法说明**：
- `init(limit)`：初始化计数器，设置限制值
- `countDown()`：计数器加一
- `allow()`：检查是否允许继续处理（未达到限制）

### limitCounter

```cpp
static LimitCounter limitCounter = {0, 0};
```

**作用**：全局限制计数器实例

**注意**：每次调用 `IterateOverInstancesOfClass` 前需要重新初始化

## 核心函数

### init_agent(JavaVM *vm, void *reserved)

```cpp
extern "C"
int init_agent(JavaVM *vm, void *reserved) {
    /* Get JVMTI environment */
    jint rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_2);
    if (rc != JNI_OK) {
        fprintf(stderr, "ERROR: dynamic-tp Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
        return -1;
    }

    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;
    jvmtiError error = jvmti->AddCapabilities(&capabilities);
    if (error) {
        fprintf(stderr, "ERROR: dynamic-tp JVMTI AddCapabilities failed!%u\n", error);
        return JNI_FALSE;
    }

    return JNI_OK;
}
```

**功能**：初始化 JVMTI Agent

**参数**：
- `vm`：Java 虚拟机指针
- `reserved`：保留参数

**返回值**：
- `JNI_OK`：初始化成功
- 其他：初始化失败

**执行流程**：
1. 获取 JVMTI 环境（`GetEnv`）
2. 设置 JVMTI 能力（`can_tag_objects`）
3. 添加能力到 JVMTI 环境

**关键能力**：
- `can_tag_objects`：允许为对象打标签，用于标记和检索对象

### Agent_OnLoad(JavaVM *vm, char *options, void *reserved)

```cpp
extern "C" JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    return init_agent(vm, reserved);
}
```

**功能**：Agent 加载时调用（在 JVM 启动时）

**参数**：
- `vm`：Java 虚拟机指针
- `options`：Agent 选项字符串
- `reserved`：保留参数

**返回值**：初始化结果

**使用场景**：通过 `-agentpath` 或 `-javaagent` 参数加载 Agent

### Agent_OnAttach(JavaVM *vm, char *options, void *reserved)

```cpp
extern "C" JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    return init_agent(vm, reserved);
}
```

**功能**：Agent 附加时调用（在运行时动态附加）

**参数**：与 `Agent_OnLoad` 相同

**返回值**：初始化结果

**使用场景**：通过 `Attach API` 在运行时动态加载 Agent

### JNI_OnLoad(JavaVM *vm, void *reserved)

```cpp
extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    init_agent(vm, reserved);
    return JNI_VERSION_1_6;
}
```

**功能**：JNI 库加载时调用

**参数**：
- `vm`：Java 虚拟机指针
- `reserved`：保留参数

**返回值**：JNI 版本号（`JNI_VERSION_1_6`）

**使用场景**：通过 `System.load()` 或 `System.loadLibrary()` 加载原生库时

**注意**：这是 DynamicTp 使用的主要加载方式

### getTag()

```cpp
extern "C"
jlong getTag() {
    return ++tagCounter;
}
```

**功能**：生成唯一的 tag 值

**返回值**：递增的 tag 值

**使用场景**：为堆对象分配唯一标记

### HeapObjectCallback()

```cpp
extern "C"
jvmtiIterationControl JNICALL
HeapObjectCallback(jlong class_tag, jlong size, jlong *tag_ptr, void *user_data) {
    jlong *data = static_cast<jlong *>(user_data);
    *tag_ptr = *data;

    limitCounter.countDown();
    if (limitCounter.allow()) {
        return JVMTI_ITERATION_CONTINUE;
    } else {
        return JVMTI_ITERATION_ABORT;
    }
}
```

**功能**：堆对象遍历回调函数

**参数**：
- `class_tag`：类的 tag
- `size`：对象大小
- `tag_ptr`：对象的 tag 指针（用于设置 tag）
- `user_data`：用户数据（包含要设置的 tag 值）

**返回值**：
- `JVMTI_ITERATION_CONTINUE`：继续遍历
- `JVMTI_ITERATION_ABORT`：停止遍历

**执行流程**：
1. 从 `user_data` 获取要设置的 tag 值
2. 为当前对象设置 tag
3. 计数器加一
4. 检查是否达到限制
5. 如果未达到限制，继续遍历；否则停止遍历

### Java_org_dromara_dynamictp_jvmti_JVMTI_getInstances0()

```cpp
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_dromara_dynamictp_jvmti_JVMTI_getInstances0(JNIEnv *env, jclass thisClass, jclass klass, jint limit) {
    jlong tag = getTag();
    limitCounter.init(limit);
    jvmtiError error = jvmti->IterateOverInstancesOfClass(klass, JVMTI_HEAP_OBJECT_EITHER,
                                               HeapObjectCallback, &tag);
    if (error) {
        printf("ERROR: dynamic-tp JVMTI IterateOverInstancesOfClass failed!%u\n", error);
        return NULL;
    }

    jint count = 0;
    jobject *instances;
    error = jvmti->GetObjectsWithTags(1, &tag, &count, &instances, NULL);
    if (error) {
        printf("ERROR: dynamic-tp JVMTI GetObjectsWithTags failed!%u\n", error);
        return NULL;
    }

    jobjectArray array = env->NewObjectArray(count, klass, NULL);
    //add element to array
    for (int i = 0; i < count; i++) {
        env->SetObjectArrayElement(array, i, instances[i]);
    }
    jvmti->Deallocate(reinterpret_cast<unsigned char *>(instances));
    return array;
}
```

**功能**：JNI 方法，获取指定类的所有实例

**参数**：
- `env`：JNI 环境
- `thisClass`：Java 类对象（JVMTI 类）
- `klass`：要查找实例的类对象
- `limit`：实例数量限制

**返回值**：对象数组，包含所有匹配的实例

**执行流程**：
1. 生成唯一的 tag
2. 初始化限制计数器
3. 遍历堆中指定类的所有实例，为匹配的对象设置 tag
4. 获取所有带有指定 tag 的对象
5. 创建 Java 对象数组
6. 将对象复制到数组中
7. 释放 JVMTI 分配的内存
8. 返回数组

**关键 JVMTI API**：
- `IterateOverInstancesOfClass`：遍历指定类的所有实例
- `GetObjectsWithTags`：获取带有指定 tag 的所有对象
- `Deallocate`：释放 JVMTI 分配的内存

### Java_org_dromara_dynamictp_jvmti_JVMTI_forceGc()

```cpp
extern "C"
JNIEXPORT void JNICALL
Java_org_dromara_dynamictp_jvmti_JVMTI_forceGc(JNIEnv *env, jclass thisClass) {
    jvmti->ForceGarbageCollection();
}
```

**功能**：JNI 方法，强制触发垃圾回收

**参数**：
- `env`：JNI 环境
- `thisClass`：Java 类对象（JVMTI 类）

**返回值**：无

**关键 JVMTI API**：
- `ForceGarbageCollection`：强制触发 GC

**注意**：这是一个同步操作，会阻塞直到 GC 完成

## 工作原理

### 1. 初始化流程

```
原生库加载 (System.load)
    ↓
JNI_OnLoad 被调用
    ↓
init_agent 初始化 JVMTI
    ↓
获取 JVMTI 环境
    ↓
添加 can_tag_objects 能力
    ↓
初始化完成
```

### 2. 获取实例流程

```
Java 调用 getInstances0
    ↓
JNI 调用 C++ 函数
    ↓
生成唯一 tag
    ↓
初始化 limitCounter
    ↓
IterateOverInstancesOfClass 遍历堆
    ↓
对每个匹配的对象调用 HeapObjectCallback
    ↓
回调函数为对象设置 tag
    ↓
检查限制，决定是否继续
    ↓
GetObjectsWithTags 获取所有标记的对象
    ↓
创建 Java 数组
    ↓
复制对象到数组
    ↓
返回数组
```

### 3. 对象标记机制

1. **生成 Tag**：为每次调用生成唯一的 tag
2. **遍历堆**：使用 `IterateOverInstancesOfClass` 遍历
3. **标记对象**：在回调函数中为匹配的对象设置 tag
4. **检索对象**：使用 `GetObjectsWithTags` 获取所有标记的对象

**优点**：
- 避免重复遍历
- 支持限制数量
- 高效的对象检索

## 技术细节

### 1. JVMTI 能力

**can_tag_objects**：
- 允许为对象打标签
- 用于标记和检索对象
- 必需的 JVMTI 能力

### 2. 堆遍历模式

**JVMTI_HEAP_OBJECT_EITHER**：
- 遍历所有类型的对象（可达和不可达）
- 包括已标记为回收的对象

**其他选项**：
- `JVMTI_HEAP_OBJECT_TAGGED`：只遍历已标记的对象
- `JVMTI_HEAP_OBJECT_UNTAGGED`：只遍历未标记的对象

### 3. 内存管理

- **JVMTI 分配**：`GetObjectsWithTags` 返回的内存由 JVMTI 分配
- **必须释放**：使用 `Deallocate` 释放，避免内存泄漏
- **Java 对象**：JNI 创建的 Java 对象由 GC 管理

### 4. 线程安全

- JNI 调用是线程安全的
- 使用静态变量需要小心（当前实现使用同步的原生方法）

## 编译要求

### 1. 依赖的头文件

- `jni.h`：JNI 标准头文件（JDK 提供）
- `jvmti.h`：JVMTI 头文件（JDK 提供）
- 生成的 JNI 头文件（通过 `javah` 或 `javac -h` 生成）

### 2. 编译器

- GCC（Linux）
- Clang（macOS）
- MSVC（Windows）

### 3. 链接库

- JVM 库（动态链接）
- 标准 C++ 库

### 4. 编译标志

```bash
# Linux
g++ -shared -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
    jni-library.cpp -o libJniLibrary-x64.so

# macOS
clang++ -shared -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin \
    jni-library.cpp -o libJniLibrary.dylib

# Windows
cl /LD /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" \
    jni-library.cpp /Fe:libJniLibrary-x64.dll
```

## 注意事项

### 1. JVMTI 版本

- 使用 `JVMTI_VERSION_1_2`
- 确保 JVM 支持该版本

### 2. 错误处理

- 所有 JVMTI 调用都应该检查返回值
- 错误时返回 `NULL` 或适当的错误码

### 3. 性能影响

- 堆遍历可能很耗时
- 可能触发 GC，影响应用性能
- 使用限制可以减少遍历时间

### 4. 内存泄漏

- 必须释放 JVMTI 分配的内存
- Java 对象引用需要正确管理

## 相关文件

- **JVMTI.java**：Java 接口定义
- **生成的 JNI 头文件**：`org_dromara_dynamictp_jvmti_JVMTI.h`
- **编译脚本**：Maven 构建脚本（在 `jvmti-build` 模块中）

## 调试技巧

### 1. 添加日志

```cpp
printf("DEBUG: IterateOverInstancesOfClass called, class: %p\n", klass);
```

### 2. 检查返回值

```cpp
if (error != JVMTI_ERROR_NONE) {
    char *error_name;
    jvmti->GetErrorName(error, &error_name);
    printf("JVMTI Error: %s\n", error_name);
    jvmti->Deallocate((unsigned char *)error_name);
}
```

### 3. 使用 JVMTI 事件

可以注册 JVMTI 事件监听器，跟踪对象创建和回收

## 扩展功能

可以添加的功能：
1. 对象大小统计
2. 对象引用关系分析
3. 内存泄漏检测
4. 堆转储功能

