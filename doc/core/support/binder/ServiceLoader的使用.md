# ServiceLoader 详解与使用指南
`ServiceLoader` 是 Java 内置的**服务提供者加载框架**（位于 `java.util` 包下），核心作用是实现「服务接口」与「服务实现类」的解耦，支持**SPI（Service Provider Interface）机制**——即开发者只需定义标准服务接口，第三方实现者提供具体实现，`ServiceLoader` 可自动扫描、加载所有符合规范的服务实现，无需硬编码依赖。

## 一、核心设计思想
`ServiceLoader` 采用「约定优于配置」的设计，核心要素分为 3 部分：
1.  **服务接口（Service Interface）**：定义标准的服务功能规范，由开发者统一制定。
2.  **服务实现类（Service Provider）**：第三方实现者针对服务接口编写的具体实现，必须提供**无参公共构造方法**（`public` 无参构造），否则 `ServiceLoader` 无法实例化。
3.  **配置文件**：在项目 `META-INF/services/` 目录下，创建以「服务接口全限定类名」命名的文件，文件内容写入「服务实现类的全限定类名」（多个实现类每行一个），`ServiceLoader` 会通过该配置文件扫描加载实现类。

## 二、完整使用步骤（分步实操）
下面以「定义一个“消息发送服务”，提供邮件、短信两种实现」为例，演示 `ServiceLoader` 的完整使用流程。

### 步骤 1：定义服务接口（统一规范）
首先创建服务接口，明确服务的核心功能：
```java
// 服务接口：消息发送服务
package com.example.demo.service;

public interface MessageSender {
    // 核心方法：发送消息
    void send(String content);
}
```

### 步骤 2：编写服务实现类（具体实现）
针对服务接口，编写 2 个具体实现类，且都提供无参公共构造方法（默认无参构造即可，无需额外编写）。

#### 实现类 1：邮件消息发送
```java
// 邮件消息实现类
package com.example.demo.service.impl;

import com.example.demo.service.MessageSender;

public class EmailMessageSender implements MessageSender {
    // 无参公共构造（默认存在，无需显式编写）
    public EmailMessageSender() {}

    @Override
    public void send(String content) {
        System.out.println("【邮件发送】：" + content);
    }
}
```

#### 实现类 2：短信消息发送
```java
// 短信消息实现类
package com.example.demo.service.impl;

import com.example.demo.service.MessageSender;

public class SmsMessageSender implements MessageSender {
    // 无参公共构造（默认存在，无需显式编写）
    public SmsMessageSender() {}

    @Override
    public void send(String content) {
        System.out.println("【短信发送】：" + content);
    }
}
```

### 步骤 3：创建 SPI 配置文件（关键约定）
这是 `ServiceLoader` 能扫描到实现类的核心步骤，严格遵循目录结构和命名规范：

1.  在项目的 `resources` 目录下，创建多级目录：`META-INF/services/`（目录名必须完全一致，大小写敏感）。
2.  在 `services` 目录下，创建一个**文件**，文件名必须是「服务接口的全限定类名」，此处为 `com.example.demo.service.MessageSender`（无后缀名）。
3.  打开该文件，写入所有服务实现类的全限定类名，**一个类名占一行**，示例内容：
    ```
    com.example.demo.service.impl.EmailMessageSender
    com.example.demo.service.impl.SmsMessageSender
    ```

    项目目录结构最终如下（以 Maven 项目为例）：
    ```
    src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── demo
    │   │               ├── service
    │   │               │   └── MessageSender.java
    │   │               └── service
    │   │                   └── impl
    │   │                       ├── EmailMessageSender.java
    │   │                       └── SmsMessageSender.java
    │   └── resources
    │       └── META-INF
    │           └── services
    │               └── com.example.demo.service.MessageSender
    ```

### 步骤 4：使用 ServiceLoader 加载并调用服务
通过 `ServiceLoader` 的静态方法 `load()` 加载服务接口，再通过迭代器（或增强 for 循环）遍历所有加载成功的服务实现类，调用其方法。

```java
package com.example.demo;

import com.example.demo.service.MessageSender;
import java.util.ServiceLoader;

public class ServiceLoaderDemo {
    public static void main(String[] args) {
        // 1. 加载服务接口：ServiceLoader.load(服务接口.class)
        ServiceLoader<MessageSender> serviceLoader = ServiceLoader.load(MessageSender.class);

        // 2. 遍历所有加载成功的服务实现类（ServiceLoader 实现了 Iterable 接口，支持增强 for 循环）
        System.out.println("===== 开始加载并执行消息发送服务 =====");
        for (MessageSender messageSender : serviceLoader) {
            // 3. 调用服务实现类的核心方法
            messageSender.send("Hello, ServiceLoader!");
        }
    }
}
```

### 步骤 5：运行结果（验证效果）
运行 `ServiceLoaderDemo`，控制台会输出两个实现类的执行结果，说明 `ServiceLoader` 成功加载了所有符合规范的服务实现：
```
===== 开始加载并执行消息发送服务 =====
【邮件发送】：Hello, ServiceLoader!
【短信发送】：Hello, ServiceLoader!
```

## 三、核心API与关键特性
### 1. 核心 API 说明
| 方法 | 作用 |
|------|------|
| `ServiceLoader.load(Class<S> service)` | 核心静态方法，加载指定服务接口的所有实现类，返回 `ServiceLoader<S>` 实例 |
| `ServiceLoader.load(Class<S> service, ClassLoader loader)` | 重载方法，指定类加载器加载服务（适用于自定义类加载场景，如模块化项目） |
| `iterator()` | 返回迭代器，遍历所有加载成功的服务实现类（增强 for 循环底层调用该方法） |
| `reload()` | 重新加载服务实现类（刷新配置，重新扫描 `META-INF/services/` 目录） |

### 2. 关键特性
1.  **延迟加载（懒加载）**：`ServiceLoader.load()` 方法并不会立即加载所有服务实现类，而是在遍历迭代器时，才会逐个实例化服务实现类，节省资源。
2.  **支持 Iterable 接口**：`ServiceLoader` 实现了 `Iterable` 接口，可直接使用增强 for 循环遍历，使用简洁。
3.  **必须提供无参公共构造**：服务实现类如果没有 `public` 无参构造方法，`ServiceLoader` 在实例化时会抛出 `InstantiationException` 异常。
4.  **配置文件严格规范**：目录（`META-INF/services/`）、文件名（服务接口全限定类名）、文件内容（实现类全限定类名）必须完全符合约定，否则无法加载。
5.  **线程不安全**：`ServiceLoader` 本身不是线程安全的，如果多个线程同时遍历或修改（如 `reload()`），需要手动加锁保证线程安全。

## 四、常见使用场景
1.  **Java 内置标准实现**：Java 自身的很多功能都基于 `ServiceLoader` 实现，例如 `java.sql.Driver`（数据库驱动加载）、`java.nio.charset.Charset`（字符集加载）。
2.  **第三方框架扩展**：很多开源框架通过 `ServiceLoader` 实现插件化扩展，例如 Spring、Dubbo、MyBatis 等，允许开发者通过 SPI 机制自定义扩展实现。
3.  **模块化项目解耦**：在大型项目中，通过 `ServiceLoader` 实现“接口定义”与“实现分离”，降低模块间的依赖耦合，便于后续替换或新增实现类（无需修改核心代码，只需添加实现类和配置文件）。

## 五、注意事项与常见问题
1.  **配置文件路径错误**：最常见问题是目录创建错误（如写成 `META-INF/service/` 少一个 `s`），或文件名与服务接口全限定类名不一致，导致 `ServiceLoader` 无法扫描到配置文件，最终遍历不到实现类。
2.  **实现类无无参公共构造**：如果实现类编写了带参构造，且未显式提供无参构造，`ServiceLoader` 实例化时会抛出 `java.lang.InstantiationException` 异常。
3.  **类加载器问题**：在模块化项目（Java 9+ Module）或自定义类加载器场景下，直接使用 `ServiceLoader.load(Class<S>)` 可能无法加载到实现类，此时需要指定类加载器，使用 `ServiceLoader.load(Class<S>, ClassLoader loader)` 重载方法。
4.  **无法加载外部 Jar 中的实现**：如果服务实现类打包在外部 Jar 中，只需确保该 Jar 中包含 `META-INF/services/` 目录及对应配置文件，即可被主项目的 `ServiceLoader` 扫描加载，无需额外配置。

## 总结
1.  `ServiceLoader` 是 Java 内置 SPI 框架，核心实现「服务接口」与「服务实现」的解耦，无需硬编码依赖。
2.  核心使用流程：定义服务接口 → 编写实现类（无参公共构造）→ 创建 `META-INF/services/` 配置文件 → 用 `ServiceLoader.load()` 加载并遍历调用。
3.  关键约定：配置文件目录、文件名、内容必须严格符合规范，否则无法正常加载。
4.  优势是支持懒加载、插件化扩展，劣势是线程不安全、配置繁琐，适用于模块化、可扩展的项目场景。