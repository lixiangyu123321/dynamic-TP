# Hutool HttpRequest 用法详解

## 概述

Hutool 的 `HttpRequest` 是一个简单易用的 HTTP 请求工具类，封装了 Java 原生的 `HttpURLConnection`，提供了链式调用的 API，支持 GET、POST、PUT、DELETE 等多种 HTTP 方法，以及超时设置、代理配置、请求头设置等功能。

### 核心定位

- **简单易用**: 链式调用，API 简洁
- **功能丰富**: 支持多种 HTTP 方法、超时、代理、请求头等
- **轻量级**: 基于 JDK 原生 `HttpURLConnection`，无需额外依赖
- **灵活配置**: 支持各种请求参数和响应处理

## 依赖引入

### Maven

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-http</artifactId>
    <version>5.8.23</version>
</dependency>
```

### Gradle

```gradle
implementation 'cn.hutool:hutool-http:5.8.23'
```

## 基础用法

### 1. GET 请求

#### 简单 GET 请求

```java
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

// 发送 GET 请求
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .execute();
    
// 获取响应状态码
int statusCode = response.getStatus();
// 获取响应体
String body = response.body();
```

#### 带参数的 GET 请求

```java
// 方式1：URL 中直接拼接参数
HttpResponse response = HttpRequest.get("https://api.example.com/users?id=1&name=test")
    .execute();

// 方式2：使用 form() 方法添加参数
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .form("id", "1")
    .form("name", "test")
    .execute();

// 方式3：使用 Map 批量添加参数
Map<String, Object> params = new HashMap<>();
params.put("id", "1");
params.put("name", "test");
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .form(params)
    .execute();
```

### 2. POST 请求

#### JSON 格式 POST 请求

```java
// 方式1：使用 body() 方法直接设置 JSON 字符串
String jsonBody = "{\"name\":\"test\",\"age\":18}";
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .body(jsonBody)
    .execute();

// 方式2：使用 body() 方法设置对象（自动序列化为 JSON）
User user = new User("test", 18);
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .body(JsonUtil.toJson(user))  // 需要先序列化
    .execute();
```

#### Form 表单 POST 请求

```java
// 方式1：使用 form() 方法
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .form("name", "test")
    .form("age", "18")
    .execute();

// 方式2：使用 Map 批量添加
Map<String, Object> formData = new HashMap<>();
formData.put("name", "test");
formData.put("age", "18");
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .form(formData)
    .execute();
```

#### 文件上传

```java
// 上传文件
HttpResponse response = HttpRequest.post("https://api.example.com/upload")
    .form("file", new File("path/to/file.txt"))
    .execute();

// 上传多个文件
HttpResponse response = HttpRequest.post("https://api.example.com/upload")
    .form("file1", new File("path/to/file1.txt"))
    .form("file2", new File("path/to/file2.txt"))
    .execute();
```

### 3. PUT 请求

```java
String jsonBody = "{\"name\":\"test\",\"age\":20}";
HttpResponse response = HttpRequest.put("https://api.example.com/users/1")
    .body(jsonBody)
    .execute();
```

### 4. DELETE 请求

```java
HttpResponse response = HttpRequest.delete("https://api.example.com/users/1")
    .execute();
```

---

## 高级配置

### 1. 超时设置

#### 连接超时和读取超时

```java
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .setConnectionTimeout(5000)  // 连接超时：5秒
    .setReadTimeout(10000)       // 读取超时：10秒
    .body(jsonBody)
    .execute();
```

**说明**:
- `setConnectionTimeout(int timeout)`: 设置连接超时时间（毫秒）
- `setReadTimeout(int timeout)`: 设置读取超时时间（毫秒）

#### 在项目中的使用

```java
// AbstractHttpNotifier.java
HttpRequest request = HttpRequest.post(url)
    .setConnectionTimeout(platform.getTimeout())  // 连接超时
    .setReadTimeout(platform.getTimeout())        // 读取超时
    .body(msgBody);
```

### 2. 请求头设置

#### 设置 Content-Type

```java
// 方式1：使用 header() 方法
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .header("Content-Type", "application/json")
    .body(jsonBody)
    .execute();

// 方式2：使用 contentType() 方法（推荐）
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .contentType("application/json")
    .body(jsonBody)
    .execute();
```

#### 设置多个请求头

```java
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer token123")
    .header("User-Agent", "MyApp/1.0")
    .body(jsonBody)
    .execute();
```

#### 设置常用请求头

```java
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .contentType("application/json")           // Content-Type
    .header("Authorization", "Bearer token")    // 认证头
    .header("X-Request-Id", "123456")          // 自定义头
    .body(jsonBody)
    .execute();
```

### 3. 代理设置

#### HTTP 代理

```java
import java.net.InetSocketAddress;
import java.net.Proxy;

HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080)))
    .body(jsonBody)
    .execute();
```

#### SOCKS 代理

```java
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.example.com", 1080)))
    .body(jsonBody)
    .execute();
```

#### 在项目中的使用

```java
// AbstractHttpNotifier.java
HttpRequest request = HttpRequest.post(url)
    .setConnectionTimeout(platform.getTimeout())
    .setReadTimeout(platform.getTimeout())
    .body(msgBody);

// 如果配置了代理，设置代理
if (platform.getProxyType() != Proxy.Type.DIRECT) {
    request.setProxy(new Proxy(
        platform.getProxyType(), 
        new InetSocketAddress(platform.getProxyHost(), platform.getProxyPort())
    ));
}
```

### 4. 请求体设置

#### JSON 请求体

```java
// 方式1：直接设置 JSON 字符串
String jsonBody = "{\"name\":\"test\",\"age\":18}";
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .body(jsonBody)
    .execute();

// 方式2：使用对象（需要先序列化）
User user = new User("test", 18);
String jsonBody = JsonUtil.toJson(user);
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .body(jsonBody)
    .execute();
```

#### Form 表单请求体

```java
// 方式1：使用 form() 方法
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .form("name", "test")
    .form("age", "18")
    .execute();

// 方式2：使用 Map
Map<String, Object> formData = new HashMap<>();
formData.put("name", "test");
formData.put("age", "18");
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .form(formData)
    .execute();
```

#### XML 请求体

```java
String xmlBody = "<?xml version=\"1.0\"?><user><name>test</name></user>";
HttpResponse response = HttpRequest.post("https://api.example.com/users")
    .contentType("application/xml")
    .body(xmlBody)
    .execute();
```

### 5. 响应处理

#### 获取响应状态码

```java
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .execute();
    
int statusCode = response.getStatus();
if (statusCode == 200) {
    // 处理成功响应
} else {
    // 处理错误响应
}
```

#### 获取响应体

```java
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .execute();

// 方式1：获取字符串响应体
String body = response.body();

// 方式2：获取字节数组响应体
byte[] bytes = response.bodyBytes();

// 方式3：获取输入流
InputStream inputStream = response.bodyStream();
```

#### 获取响应头

```java
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .execute();

// 获取单个响应头
String contentType = response.header("Content-Type");

// 获取所有响应头
Map<String, List<String>> headers = response.headers();
```

#### 判断响应是否成功

```java
HttpResponse response = HttpRequest.get("https://api.example.com/users")
    .execute();

// 方式1：检查状态码
if (response.getStatus() == 200) {
    // 成功
}

// 方式2：使用 isOk() 方法（状态码 200-299）
if (response.isOk()) {
    // 成功
}
```

---

## 完整示例

### 示例1：发送钉钉通知（项目中的实际使用）

```java
// AbstractHttpNotifier.java
@Override
protected void send0(NotifyPlatform platform, String content) {
    // 1. 构建 URL
    String url = buildUrl(platform);
    
    // 2. 构建消息体
    String msgBody = buildMsgBody(platform, content);
    
    // 3. 创建 POST 请求
    HttpRequest request = HttpRequest.post(url)
        .setConnectionTimeout(platform.getTimeout())  // 连接超时
        .setReadTimeout(platform.getTimeout())        // 读取超时
        .body(msgBody);                                // 请求体
    
    // 4. 设置代理（如果配置了）
    if (platform.getProxyType() != Proxy.Type.DIRECT) {
        request.setProxy(new Proxy(
            platform.getProxyType(), 
            new InetSocketAddress(platform.getProxyHost(), platform.getProxyPort())
        ));
    }
    
    // 5. 执行请求
    HttpResponse response = request.execute();
    
    // 6. 处理响应
    if (Objects.nonNull(response)) {
        log.info("DynamicTp notify, {} send success, response: {}, request: {}",
            platform(), response.body(), msgBody);
    }
}
```

### 示例2：调用 REST API

```java
public class UserService {
    
    /**
     * 获取用户信息
     */
    public User getUser(Long id) {
        HttpResponse response = HttpRequest.get("https://api.example.com/users/" + id)
            .setConnectionTimeout(5000)
            .setReadTimeout(10000)
            .execute();
        
        if (response.isOk()) {
            String json = response.body();
            return JsonUtil.toBean(json, User.class);
        } else {
            throw new RuntimeException("Failed to get user: " + response.getStatus());
        }
    }
    
    /**
     * 创建用户
     */
    public User createUser(User user) {
        String jsonBody = JsonUtil.toJson(user);
        HttpResponse response = HttpRequest.post("https://api.example.com/users")
            .contentType("application/json")
            .setConnectionTimeout(5000)
            .setReadTimeout(10000)
            .body(jsonBody)
            .execute();
        
        if (response.isOk()) {
            String json = response.body();
            return JsonUtil.toBean(json, User.class);
        } else {
            throw new RuntimeException("Failed to create user: " + response.getStatus());
        }
    }
    
    /**
     * 更新用户
     */
    public User updateUser(Long id, User user) {
        String jsonBody = JsonUtil.toJson(user);
        HttpResponse response = HttpRequest.put("https://api.example.com/users/" + id)
            .contentType("application/json")
            .setConnectionTimeout(5000)
            .setReadTimeout(10000)
            .body(jsonBody)
            .execute();
        
        if (response.isOk()) {
            String json = response.body();
            return JsonUtil.toBean(json, User.class);
        } else {
            throw new RuntimeException("Failed to update user: " + response.getStatus());
        }
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        HttpResponse response = HttpRequest.delete("https://api.example.com/users/" + id)
            .setConnectionTimeout(5000)
            .setReadTimeout(10000)
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("Failed to delete user: " + response.getStatus());
        }
    }
}
```

### 示例3：文件上传

```java
public class FileService {
    
    /**
     * 上传文件
     */
    public String uploadFile(File file) {
        HttpResponse response = HttpRequest.post("https://api.example.com/upload")
            .form("file", file)
            .form("description", "My file")
            .setConnectionTimeout(30000)  // 文件上传需要更长的超时时间
            .setReadTimeout(60000)
            .execute();
        
        if (response.isOk()) {
            String json = response.body();
            UploadResponse uploadResponse = JsonUtil.toBean(json, UploadResponse.class);
            return uploadResponse.getFileUrl();
        } else {
            throw new RuntimeException("Failed to upload file: " + response.getStatus());
        }
    }
}
```

### 示例4：带认证的请求

```java
public class AuthenticatedApiClient {
    
    private String token;
    
    public AuthenticatedApiClient(String token) {
        this.token = token;
    }
    
    /**
     * 调用需要认证的 API
     */
    public String callApi(String url) {
        HttpResponse response = HttpRequest.get(url)
            .header("Authorization", "Bearer " + token)
            .header("X-Request-Id", UUID.randomUUID().toString())
            .setConnectionTimeout(5000)
            .setReadTimeout(10000)
            .execute();
        
        if (response.isOk()) {
            return response.body();
        } else {
            throw new RuntimeException("API call failed: " + response.getStatus());
        }
    }
}
```

---

## 常用方法总结

### 请求方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `HttpRequest.get(url)` | GET 请求 | `HttpRequest.get("https://api.example.com")` |
| `HttpRequest.post(url)` | POST 请求 | `HttpRequest.post("https://api.example.com")` |
| `HttpRequest.put(url)` | PUT 请求 | `HttpRequest.put("https://api.example.com")` |
| `HttpRequest.delete(url)` | DELETE 请求 | `HttpRequest.delete("https://api.example.com")` |
| `HttpRequest.patch(url)` | PATCH 请求 | `HttpRequest.patch("https://api.example.com")` |

### 配置方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `setConnectionTimeout(int)` | 设置连接超时 | `.setConnectionTimeout(5000)` |
| `setReadTimeout(int)` | 设置读取超时 | `.setReadTimeout(10000)` |
| `header(String, String)` | 设置请求头 | `.header("Authorization", "Bearer token")` |
| `contentType(String)` | 设置 Content-Type | `.contentType("application/json")` |
| `setProxy(Proxy)` | 设置代理 | `.setProxy(new Proxy(...))` |
| `body(String)` | 设置请求体 | `.body(jsonBody)` |
| `form(String, Object)` | 添加表单参数 | `.form("name", "test")` |
| `form(Map)` | 批量添加表单参数 | `.form(formData)` |

### 响应方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `execute()` | 执行请求 | `request.execute()` |
| `getStatus()` | 获取状态码 | `response.getStatus()` |
| `isOk()` | 判断是否成功 | `response.isOk()` |
| `body()` | 获取响应体（字符串） | `response.body()` |
| `bodyBytes()` | 获取响应体（字节数组） | `response.bodyBytes()` |
| `bodyStream()` | 获取响应体（输入流） | `response.bodyStream()` |
| `header(String)` | 获取响应头 | `response.header("Content-Type")` |
| `headers()` | 获取所有响应头 | `response.headers()` |

---

## 最佳实践

### 1. 超时设置

- **连接超时**: 建议设置为 5-10 秒
- **读取超时**: 根据接口响应时间设置，建议 10-30 秒
- **文件上传**: 需要更长的超时时间（30-60 秒）

```java
// ✅ 推荐：根据场景设置合适的超时时间
HttpRequest.post(url)
    .setConnectionTimeout(5000)   // 连接超时：5秒
    .setReadTimeout(10000)        // 读取超时：10秒
    .body(jsonBody)
    .execute();
```

### 2. 异常处理

```java
// ✅ 推荐：捕获异常并处理
try {
    HttpResponse response = HttpRequest.post(url)
        .setConnectionTimeout(5000)
        .setReadTimeout(10000)
        .body(jsonBody)
        .execute();
    
    if (response.isOk()) {
        // 处理成功响应
        String body = response.body();
    } else {
        // 处理错误响应
        log.error("Request failed with status: {}", response.getStatus());
    }
} catch (Exception e) {
    log.error("Request exception", e);
    // 处理异常
}
```

### 3. 请求头设置

```java
// ✅ 推荐：设置必要的请求头
HttpRequest.post(url)
    .contentType("application/json")           // Content-Type
    .header("Authorization", "Bearer token")   // 认证头
    .header("User-Agent", "MyApp/1.0")        // User-Agent
    .body(jsonBody)
    .execute();
```

### 4. 响应验证

```java
// ✅ 推荐：验证响应状态
HttpResponse response = HttpRequest.get(url).execute();
if (response.isOk()) {
    String body = response.body();
    // 处理响应
} else {
    log.error("Request failed: status={}, body={}", 
        response.getStatus(), response.body());
    throw new RuntimeException("Request failed");
}
```

### 5. 代理配置

```java
// ✅ 推荐：条件性设置代理
HttpRequest request = HttpRequest.post(url)
    .setConnectionTimeout(5000)
    .setReadTimeout(10000)
    .body(jsonBody);

if (needProxy) {
    request.setProxy(new Proxy(
        Proxy.Type.HTTP, 
        new InetSocketAddress(proxyHost, proxyPort)
    ));
}

HttpResponse response = request.execute();
```

---

## 注意事项

### 1. 线程安全

- `HttpRequest` 对象不是线程安全的，每次请求应该创建新的实例
- `HttpResponse` 对象也不是线程安全的

```java
// ✅ 正确：每次创建新实例
HttpResponse response = HttpRequest.post(url).execute();

// ❌ 错误：复用同一个实例
HttpRequest request = HttpRequest.post(url);
// 多线程使用 request 会有问题
```

### 2. 资源释放

- `HttpResponse` 的 `bodyStream()` 方法返回的输入流需要手动关闭
- 使用 `body()` 或 `bodyBytes()` 方法会自动处理资源释放

```java
// ✅ 推荐：使用 body() 方法（自动处理资源）
String body = response.body();

// ⚠️ 注意：使用 bodyStream() 需要手动关闭
try (InputStream is = response.bodyStream()) {
    // 处理输入流
}
```

### 3. 字符编码

- 默认使用 UTF-8 编码
- 可以通过 `charset()` 方法指定编码

```java
HttpResponse response = HttpRequest.get(url)
    .charset("GBK")  // 指定编码
    .execute();
```

### 4. 重定向

- 默认自动跟随重定向（最多 3 次）
- 可以通过 `setFollowRedirects(boolean)` 控制

```java
HttpResponse response = HttpRequest.get(url)
    .setFollowRedirects(false)  // 不跟随重定向
    .execute();
```

---

## 在项目中的实际应用

### AbstractHttpNotifier

DynamicTp 框架中使用 `HttpRequest` 发送 HTTP 通知：

```java
@Slf4j
public abstract class AbstractHttpNotifier extends AbstractNotifier {
    
    @Override
    protected void send0(NotifyPlatform platform, String content) {
        // 1. 构建 URL 和消息体
        String url = buildUrl(platform);
        String msgBody = buildMsgBody(platform, content);
        
        // 2. 创建 POST 请求并设置超时
        HttpRequest request = HttpRequest.post(url)
            .setConnectionTimeout(platform.getTimeout())
            .setReadTimeout(platform.getTimeout())
            .body(msgBody);
        
        // 3. 设置代理（如果配置了）
        if (platform.getProxyType() != Proxy.Type.DIRECT) {
            request.setProxy(new Proxy(
                platform.getProxyType(), 
                new InetSocketAddress(platform.getProxyHost(), platform.getProxyPort())
            ));
        }
        
        // 4. 执行请求并处理响应
        HttpResponse response = request.execute();
        if (Objects.nonNull(response)) {
            log.info("DynamicTp notify, {} send success, response: {}, request: {}",
                platform(), response.body(), msgBody);
        }
    }
}
```

**特点**:
- 支持超时配置
- 支持代理配置
- 统一的错误处理和日志记录

---

## 总结

Hutool 的 `HttpRequest` 是一个功能强大且易于使用的 HTTP 客户端工具，主要特点：

1. **简单易用**: 链式调用，API 简洁直观
2. **功能丰富**: 支持多种 HTTP 方法、超时、代理、请求头等
3. **轻量级**: 基于 JDK 原生实现，无需额外依赖
4. **灵活配置**: 支持各种请求参数和响应处理

### 核心优势

- **链式调用**: 代码简洁，易于阅读
- **自动处理**: 自动处理编码、重定向等
- **类型安全**: 提供类型安全的方法
- **扩展性强**: 支持自定义配置

### 适用场景

- REST API 调用
- 文件上传下载
- 第三方服务集成
- 通知消息发送
- 数据采集

通过 `HttpRequest`，可以快速、简洁地实现各种 HTTP 请求功能，是 Java 开发中常用的 HTTP 客户端工具。

