# HTTP 请求代理机制

## 概述

**HTTP 代理（HTTP Proxy）** 是一种网络中间件，位于客户端和目标服务器之间，作为两者之间的中介。客户端通过代理服务器发送 HTTP 请求，代理服务器转发请求到目标服务器，并将响应返回给客户端。

### 核心定位

- **网络中介**: 作为客户端和服务器之间的中间层
- **请求转发**: 转发客户端的 HTTP 请求到目标服务器
- **响应返回**: 将服务器的响应返回给客户端
- **功能增强**: 可以提供缓存、过滤、认证、负载均衡等功能

### 主要特性

1. **透明转发**: 对客户端和服务器透明，无需修改应用代码
2. **功能增强**: 支持缓存、压缩、安全过滤等功能
3. **访问控制**: 可以控制哪些请求可以通过
4. **负载均衡**: 可以将请求分发到多个服务器
5. **协议支持**: 支持 HTTP、HTTPS、SOCKS 等协议

## 代理类型

### 1. 正向代理（Forward Proxy）

正向代理是客户端使用的代理，客户端知道代理的存在，并主动配置使用代理。

```
客户端 → 正向代理 → 目标服务器
```

**特点**:
- 客户端知道代理的存在
- 客户端需要配置代理地址
- 目标服务器不知道真实的客户端地址
- 常用于突破网络限制、访问控制

**使用场景**:
- 企业内网访问外网
- 突破地域限制
- 隐藏客户端真实 IP
- 访问控制和安全审计

### 2. 反向代理（Reverse Proxy）

反向代理是服务器端使用的代理，客户端不知道代理的存在，直接访问代理服务器。

```
客户端 → 反向代理 → 后端服务器
```

**特点**:
- 客户端不知道代理的存在
- 客户端直接访问代理服务器
- 后端服务器不知道真实的客户端地址
- 常用于负载均衡、SSL 终止、缓存

**使用场景**:
- 负载均衡
- SSL/TLS 终止
- 静态资源缓存
- 安全防护（WAF）
- 统一入口

### 3. 透明代理（Transparent Proxy）

透明代理对客户端和服务器都是透明的，客户端不需要配置，请求被自动转发到代理。

```
客户端 → 透明代理 → 目标服务器
```

**特点**:
- 客户端不需要配置
- 客户端不知道代理的存在
- 通常由网络设备（如路由器、防火墙）实现
- 常用于内容过滤、流量监控

**使用场景**:
- 企业网络内容过滤
- 流量监控和分析
- 带宽管理
- 安全审计

## 代理协议

### 1. HTTP 代理

HTTP 代理是最常见的代理类型，支持 HTTP 和 HTTPS 协议。

**工作原理**:
1. 客户端发送 HTTP 请求到代理服务器
2. 代理服务器解析请求，提取目标 URL
3. 代理服务器转发请求到目标服务器
4. 代理服务器接收响应并返回给客户端

**请求格式**:
```
GET http://example.com/path HTTP/1.1
Host: example.com
Proxy-Connection: keep-alive
```

### 2. HTTPS 代理

HTTPS 代理支持 HTTPS 协议，可以处理加密的 HTTPS 连接。

**CONNECT 方法**:
```
CONNECT example.com:443 HTTP/1.1
Host: example.com
Proxy-Connection: keep-alive
```

**工作原理**:
1. 客户端发送 CONNECT 请求到代理
2. 代理建立到目标服务器的 TCP 连接
3. 代理在客户端和目标服务器之间转发数据
4. 客户端和目标服务器直接进行 TLS 握手

### 3. SOCKS 代理

SOCKS 代理工作在传输层，可以代理任何基于 TCP 的协议。

**SOCKS 版本**:
- **SOCKS4**: 支持 TCP 连接，不支持认证
- **SOCKS5**: 支持 TCP 和 UDP，支持多种认证方式

**特点**:
- 工作在传输层，不解析应用层协议
- 可以代理任何基于 TCP/UDP 的协议
- 性能更好，开销更小

## 工作原理

### HTTP 代理工作流程

```
┌─────────┐         ┌─────────┐         ┌─────────┐
│ 客户端  │────────▶│  代理   │────────▶│ 服务器  │
└─────────┘         └─────────┘         └─────────┘
     │                   │                   │
     │  1. 发送请求      │                   │
     │─────────────────▶│                   │
     │                   │  2. 转发请求      │
     │                   │─────────────────▶│
     │                   │  3. 接收响应      │
     │                   │◀─────────────────│
     │  4. 返回响应      │                   │
     │◀─────────────────│                   │
```

### 详细步骤

1. **客户端发送请求**
   - 客户端构造 HTTP 请求
   - 请求发送到代理服务器（不是目标服务器）
   - 请求中包含完整的目标 URL

2. **代理解析请求**
   - 代理服务器解析 HTTP 请求
   - 提取目标服务器地址和路径
   - 检查代理规则和策略

3. **代理转发请求**
   - 代理服务器建立到目标服务器的连接
   - 转发请求到目标服务器
   - 可能修改请求头（如添加 X-Forwarded-For）

4. **代理接收响应**
   - 代理服务器接收目标服务器的响应
   - 可能进行缓存、压缩等处理
   - 记录日志和统计信息

5. **代理返回响应**
   - 代理服务器将响应返回给客户端
   - 客户端收到响应，就像直接访问目标服务器

### HTTPS 代理工作流程

```
┌─────────┐         ┌─────────┐         ┌─────────┐
│ 客户端  │────────▶│  代理   │────────▶│ 服务器  │
└─────────┘         └─────────┘         └─────────┘
     │                   │                   │
     │  1. CONNECT       │                   │
     │─────────────────▶│                   │
     │                   │  2. TCP 连接      │
     │                   │─────────────────▶│
     │  3. TLS 握手      │                   │
     │◀─────────────────▶│◀─────────────────▶│
     │  4. 加密数据      │                   │
     │◀─────────────────▶│◀─────────────────▶│
```

## 代理功能

### 1. 缓存

代理服务器可以缓存响应，减少对目标服务器的请求。

**缓存策略**:
- **Cache-Control**: 根据响应头决定是否缓存
- **Expires**: 根据过期时间决定缓存有效性
- **ETag**: 使用实体标签验证缓存有效性

**优势**:
- 减少网络流量
- 提高响应速度
- 减轻服务器负载

### 2. 负载均衡

反向代理可以将请求分发到多个后端服务器。

**负载均衡算法**:
- **轮询（Round Robin）**: 依次分发请求
- **加权轮询（Weighted Round Robin）**: 根据权重分发
- **最少连接（Least Connections）**: 分发到连接数最少的服务器
- **IP 哈希（IP Hash）**: 根据客户端 IP 哈希分发

### 3. SSL/TLS 终止

反向代理可以处理 SSL/TLS 连接，后端服务器使用普通 HTTP。

**优势**:
- 简化后端服务器配置
- 集中管理证书
- 提高性能（减少加密/解密开销）

### 4. 访问控制

代理可以控制哪些请求可以通过。

**控制方式**:
- **IP 白名单/黑名单**: 根据客户端 IP 控制
- **URL 过滤**: 根据请求 URL 过滤
- **内容过滤**: 根据请求/响应内容过滤
- **认证**: 要求客户端提供认证信息

### 5. 请求/响应修改

代理可以修改请求和响应。

**修改内容**:
- **请求头**: 添加、删除、修改请求头
- **响应头**: 添加、删除、修改响应头
- **URL 重写**: 修改请求 URL
- **内容替换**: 替换响应内容

### 6. 日志和监控

代理可以记录请求和响应信息。

**记录内容**:
- 请求 URL、方法、头部
- 响应状态码、大小、时间
- 客户端 IP、用户代理
- 处理时间、错误信息

## 在 DynamicTp 中的应用

### AbstractHttpNotifier

DynamicTp 在发送 HTTP 通知时支持代理配置：

```java
public abstract class AbstractHttpNotifier extends AbstractNotifier {
    
    @Override
    protected void send0(NotifyPlatform platform, String content) {
        val url = buildUrl(platform);
        val msgBody = buildMsgBody(platform, content);
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
        
        HttpResponse response = request.execute();
        // ...
    }
}
```

### 配置示例

```yaml
dynamictp:
  platforms:
    - platform: ding
      urlKey: your-webhook-url
      receivers: user1,user2
      # HTTP 代理配置
      proxyType: HTTP                    # 代理类型：DIRECT, HTTP, SOCKS
      proxyHost: proxy.example.com       # 代理服务器地址
      proxyPort: 8080                    # 代理服务器端口
      timeout: 3000                      # 请求超时时间（毫秒）
```

### 支持的代理类型

```java
public enum Type {
    /**
     * 直接连接，不使用代理
     */
    DIRECT,
    
    /**
     * HTTP 代理
     */
    HTTP,
    
    /**
     * SOCKS 代理
     */
    SOCKS
}
```

## 代码示例

### 1. Java 使用 HTTP 代理

```java
import java.net.*;
import java.io.*;

public class HttpProxyExample {
    
    public static void main(String[] args) throws Exception {
        // 创建代理对象
        Proxy proxy = new Proxy(
            Proxy.Type.HTTP,
            new InetSocketAddress("proxy.example.com", 8080)
        );
        
        // 创建 URL 连接
        URL url = new URL("http://example.com/api");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        
        // 设置请求属性
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        // 如果需要认证
        String auth = "username:password";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
        
        // 发送请求
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        
        // 读取响应
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
    }
}
```

### 2. OkHttp 使用代理

```java
import okhttp3.*;

public class OkHttpProxyExample {
    
    public static void main(String[] args) throws Exception {
        // 创建代理对象
        Proxy proxy = new Proxy(
            Proxy.Type.HTTP,
            new InetSocketAddress("proxy.example.com", 8080)
        );
        
        // 创建 OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder()
            .proxy(proxy)
            .proxyAuthenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) {
                    String credential = Credentials.basic("username", "password");
                    return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
                }
            })
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        
        // 创建请求
        Request request = new Request.Builder()
            .url("http://example.com/api")
            .build();
        
        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response Code: " + response.code());
            System.out.println("Response Body: " + response.body().string());
        }
    }
}
```

### 3. Apache HttpClient 使用代理

```java
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ApacheHttpClientProxyExample {
    
    public static void main(String[] args) throws Exception {
        // 创建代理主机
        HttpHost proxy = new HttpHost("proxy.example.com", 8080);
        
        // 创建 HttpClient
        CloseableHttpClient client = HttpClients.custom()
            .setProxy(proxy)
            .build();
        
        // 创建请求
        HttpGet request = new HttpGet("http://example.com/api");
        
        // 发送请求
        try (CloseableHttpResponse response = client.execute(request)) {
            System.out.println("Status Code: " + response.getStatusLine().getStatusCode());
            // 处理响应
        }
    }
}
```

### 4. Spring RestTemplate 使用代理

```java
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class RestTemplateProxyExample {
    
    public static void main(String[] args) {
        // 创建代理
        Proxy proxy = new Proxy(
            Proxy.Type.HTTP,
            new InetSocketAddress("proxy.example.com", 8080)
        );
        
        // 创建请求工厂
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(proxy);
        
        // 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 发送请求
        String response = restTemplate.getForObject(
            "http://example.com/api",
            String.class
        );
        System.out.println(response);
    }
}
```

## 常见代理服务器

### 1. Nginx

Nginx 可以作为反向代理服务器。

**配置示例**:
```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://backend-server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 2. Apache HTTP Server

Apache 可以作为正向和反向代理。

**配置示例**:
```apache
ProxyPass /api http://backend-server:8080/api
ProxyPassReverse /api http://backend-server:8080/api
```

### 3. Squid

Squid 是专业的代理服务器，支持缓存和访问控制。

**配置示例**:
```
http_port 3128
cache_dir ufs /var/spool/squid 100 16 256
acl localnet src 192.168.0.0/16
http_access allow localnet
```

### 4. HAProxy

HAProxy 是高性能的负载均衡器和反向代理。

**配置示例**:
```
frontend web
    bind *:80
    default_backend servers

backend servers
    balance roundrobin
    server server1 192.168.1.10:8080 check
    server server2 192.168.1.11:8080 check
```

## 代理认证

### HTTP 基本认证

```java
// 设置代理认证
String auth = "username:password";
String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
```

### SOCKS5 认证

```java
// SOCKS5 支持多种认证方式
// - 无认证
// - 用户名/密码认证
// - GSSAPI 认证
```

## 最佳实践

### 1. 代理选择

- **企业内网**: 使用正向代理访问外网
- **负载均衡**: 使用反向代理分发请求
- **安全防护**: 使用反向代理作为安全网关

### 2. 性能优化

- **连接池**: 使用连接池复用连接
- **缓存**: 启用缓存减少请求
- **压缩**: 启用压缩减少传输数据

### 3. 安全配置

- **认证**: 启用代理认证
- **加密**: 使用 HTTPS 保护数据传输
- **访问控制**: 配置 IP 白名单/黑名单
- **日志**: 记录访问日志用于审计

### 4. 错误处理

- **超时设置**: 合理设置连接和读取超时
- **重试机制**: 实现请求重试机制
- **降级策略**: 代理不可用时使用直连

## 常见问题

### 1. 代理连接失败

**问题**: 无法连接到代理服务器

**可能原因**:
- 代理服务器地址或端口错误
- 网络连接问题
- 防火墙阻止连接

**解决方法**:
- 检查代理配置
- 测试网络连接
- 检查防火墙规则

### 2. 代理认证失败

**问题**: 代理认证失败

**可能原因**:
- 用户名或密码错误
- 认证方式不正确
- 代理不支持该认证方式

**解决方法**:
- 验证用户名和密码
- 检查认证方式
- 查看代理服务器日志

### 3. HTTPS 连接问题

**问题**: 通过代理访问 HTTPS 失败

**可能原因**:
- 代理不支持 CONNECT 方法
- SSL 证书验证失败
- 代理配置错误

**解决方法**:
- 检查代理是否支持 HTTPS
- 配置证书验证
- 检查代理配置

## 总结

HTTP 代理是一种重要的网络中间件，提供了请求转发、缓存、负载均衡、安全防护等功能。在 DynamicTp 中，代理主要用于发送 HTTP 通知时支持企业内网环境。理解代理的工作原理和配置方法，有助于更好地使用和管理网络请求。

