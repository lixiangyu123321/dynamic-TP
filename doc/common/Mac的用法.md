# Mac 类用法详解

## 概述

`Mac`（Message Authentication Code，消息认证码）是 Java 加密扩展（JCE）中的一个核心类，用于生成消息认证码。MAC 是一种用于验证消息完整性和身份认证的加密技术，它使用密钥和哈希算法来生成一个固定长度的认证码。

### 核心定位

- **消息完整性验证**: 确保消息在传输过程中未被篡改
- **身份认证**: 验证消息发送者的身份
- **基于密钥的哈希**: 使用密钥和哈希算法生成认证码
- **单向加密**: 只能生成，不能反向解密

### MAC 与普通哈希的区别

| 特性 | 普通哈希（MD5、SHA-256） | MAC（HmacSHA256） |
|------|------------------------|-------------------|
| 密钥 | 不需要 | 需要密钥 |
| 安全性 | 较低，易受攻击 | 较高，需要密钥才能验证 |
| 用途 | 数据完整性检查 | 数据完整性 + 身份认证 |
| 防篡改 | 较弱 | 强（需要密钥） |

---

## 核心概念

### 1. MAC 算法

MAC 算法通常基于哈希函数，常见的算法包括：

- **HmacSHA1**: 基于 SHA-1 的 MAC 算法
- **HmacSHA256**: 基于 SHA-256 的 MAC 算法（推荐）
- **HmacSHA384**: 基于 SHA-384 的 MAC 算法
- **HmacSHA512**: 基于 SHA-512 的 MAC 算法
- **HmacMD5**: 基于 MD5 的 MAC 算法（不推荐，安全性较低）

### 2. 工作流程

```
1. 获取 Mac 实例（指定算法）
   ↓
2. 初始化 Mac（设置密钥）
   ↓
3. 更新数据（可选，可以多次调用）
   ↓
4. 生成 MAC（调用 doFinal）
   ↓
5. 重置 Mac（可选，用于下一次计算）
```

---

## 基础用法

### 1. 获取 Mac 实例

#### 方式1：使用算法名称（推荐）

```java
import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;

// 获取 HmacSHA256 算法的 Mac 实例
Mac mac = Mac.getInstance("HmacSHA256");
```

#### 方式2：指定提供者

```java
import java.security.Provider;
import java.security.NoSuchProviderException;

// 指定提供者
Mac mac = Mac.getInstance("HmacSHA256", "SunJCE");
```

#### 方式3：使用 Provider 对象

```java
import java.security.Provider;

Provider provider = Security.getProvider("SunJCE");
Mac mac = Mac.getInstance("HmacSHA256", provider);
```

**源码解析**:

```java
// Mac.java
public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
    // 1. 获取所有支持该算法的服务提供者
    List<Provider.Service> services = GetInstance.getServices("Mac", algorithm);
    Iterator<Provider.Service> iterator = services.iterator();
    
    // 2. 遍历提供者，找到第一个可用的
    while (iterator.hasNext()) {
        Provider.Service service = iterator.next();
        if (JceSecurity.canUseProvider(service.getProvider())) {
            // 3. 创建延迟初始化的 Mac 实例
            return new Mac(service, iterator, algorithm);
        }
    }
    
    // 4. 如果没有找到，抛出异常
    throw new NoSuchAlgorithmException("Algorithm " + algorithm + " not available");
}
```

### 2. 初始化 Mac

#### 使用 SecretKeySpec 初始化

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.nio.charset.StandardCharsets;

// 1. 获取 Mac 实例
Mac mac = Mac.getInstance("HmacSHA256");

// 2. 创建密钥规范
String secret = "my-secret-key";
SecretKeySpec secretKey = new SecretKeySpec(
    secret.getBytes(StandardCharsets.UTF_8), 
    "HmacSHA256"
);

// 3. 初始化 Mac
mac.init(secretKey);
```

#### 使用 Key 对象初始化

```java
import javax.crypto.Mac;
import java.security.Key;
import java.security.InvalidKeyException;

Mac mac = Mac.getInstance("HmacSHA256");
Key key = ...; // 从 KeyStore 或其他地方获取
mac.init(key);
```

#### 带算法参数初始化

```java
import javax.crypto.Mac;
import java.security.AlgorithmParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

Mac mac = Mac.getInstance("HmacSHA256");
Key key = ...;
AlgorithmParameterSpec params = ...;
mac.init(key, params);
```

**源码解析**:

```java
// Mac.java
public final void init(Key key) throws InvalidKeyException {
    try {
        if (this.spi != null) {
            // 如果已经选择了提供者，直接初始化
            this.spi.engineInit(key, null);
        } else {
            // 否则，延迟选择提供者
            this.chooseProvider(key, null);
        }
    } catch (InvalidAlgorithmParameterException e) {
        throw new InvalidKeyException("init() failed", e);
    }
    
    this.initialized = true;
}
```

### 3. 更新数据

#### 更新单个字节

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 更新单个字节
mac.update((byte) 0x01);
```

#### 更新字节数组

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 更新整个字节数组
byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
mac.update(data);
```

#### 更新字节数组的一部分

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
// 更新从索引 0 开始，长度为 5 的字节
mac.update(data, 0, 5);
```

#### 更新 ByteBuffer

```java
import java.nio.ByteBuffer;

Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

ByteBuffer buffer = ByteBuffer.wrap("Hello, World!".getBytes(StandardCharsets.UTF_8));
mac.update(buffer);
```

#### 多次更新

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 可以多次调用 update，效果等同于一次性传入所有数据
mac.update("Hello, ".getBytes(StandardCharsets.UTF_8));
mac.update("World!".getBytes(StandardCharsets.UTF_8));
```

**源码解析**:

```java
// Mac.java
public final void update(byte[] input) throws IllegalStateException {
    this.chooseFirstProvider();  // 延迟初始化提供者
    if (!this.initialized) {
        throw new IllegalStateException("MAC not initialized");
    }
    if (input != null) {
        this.spi.engineUpdate(input, 0, input.length);
    }
}
```

### 4. 生成 MAC

#### 方式1：使用 doFinal() 生成 MAC

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);
mac.update(data);

// 生成 MAC（会自动重置）
byte[] macBytes = mac.doFinal();
```

#### 方式2：使用 doFinal(byte[]) 更新并生成

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 更新数据并生成 MAC（一步完成）
byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
byte[] macBytes = mac.doFinal(data);
```

#### 方式3：将 MAC 写入指定数组

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);
mac.update(data);

// 将 MAC 写入指定数组的指定位置
byte[] output = new byte[mac.getMacLength()];
mac.doFinal(output, 0);
```

**源码解析**:

```java
// Mac.java
public final byte[] doFinal() throws IllegalStateException {
    this.chooseFirstProvider();
    if (!this.initialized) {
        throw new IllegalStateException("MAC not initialized");
    }
    
    // 1. 生成 MAC
    byte[] result = this.spi.engineDoFinal();
    
    // 2. 重置 Mac（重要：用于下一次计算）
    this.spi.engineReset();
    
    return result;
}

public final byte[] doFinal(byte[] input) throws IllegalStateException {
    this.chooseFirstProvider();
    if (!this.initialized) {
        throw new IllegalStateException("MAC not initialized");
    }
    
    // 先更新数据，再生成 MAC
    this.update(input);
    return this.doFinal();
}
```

### 5. 重置 Mac

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 第一次计算
mac.update(data1);
byte[] mac1 = mac.doFinal();

// 重置后可以用于下一次计算
mac.reset();
mac.update(data2);
byte[] mac2 = mac.doFinal();
```

**注意**: `doFinal()` 方法会自动调用 `reset()`，所以通常不需要手动调用。

---

## 完整示例

### 示例1：生成 HmacSHA256 签名（钉钉签名）

```java
// DingSignUtil.java
public static String dingSign(String secret, long timestamp) {
    // 1. 拼接待签名字符串
    String stringToSign = timestamp + "\n" + secret;
    
    try {
        // 2. 获取 Mac 实例
        Mac mac = Mac.getInstance("HmacSHA256");
        
        // 3. 初始化 Mac（使用密钥）
        mac.init(new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        ));
        
        // 4. 更新数据并生成 MAC
        byte[] signData = mac.doFinal(
            stringToSign.getBytes(StandardCharsets.UTF_8)
        );
        
        // 5. Base64 编码并 URL 编码
        String base64Sign = new String(Base64.encodeBase64(signData));
        return URLEncoder.encode(base64Sign, StandardCharsets.UTF_8.name());
    } catch (Exception e) {
        log.error("DynamicTp, cal ding sign error", e);
        return "";
    }
}
```

**使用场景**: 钉钉机器人 Webhook 签名生成

### 示例2：生成 HmacSHA256 签名（飞书签名）

```java
// LarkNotifier.java
protected String genSign(String secret, Long timestamp) 
        throws NoSuchAlgorithmException, InvalidKeyException {
    // 1. 拼接待签名字符串
    String stringToSign = timestamp + "\n" + secret;
    
    // 2. 获取 Mac 实例
    Mac mac = Mac.getInstance("HmacSHA256");
    
    // 3. 初始化 Mac（注意：这里使用 stringToSign 作为密钥）
    mac.init(new SecretKeySpec(
        stringToSign.getBytes(StandardCharsets.UTF_8), 
        "HmacSHA256"
    ));
    
    // 4. 生成 MAC（传入空字节数组）
    byte[] signData = mac.doFinal(new byte[]{});
    
    // 5. Base64 编码
    return new String(Base64.encodeBase64(signData));
}
```

**使用场景**: 飞书机器人 Webhook 签名生成

**注意**: 飞书和钉钉的签名算法略有不同：
- **钉钉**: 使用 `secret` 作为密钥，对 `timestamp + "\n" + secret` 进行签名
- **飞书**: 使用 `timestamp + "\n" + secret` 作为密钥，对空字节数组进行签名

### 示例3：验证消息完整性

```java
public class MessageVerifier {
    private static final String ALGORITHM = "HmacSHA256";
    private final String secretKey;
    
    public MessageVerifier(String secretKey) {
        this.secretKey = secretKey;
    }
    
    /**
     * 生成消息的 MAC
     */
    public byte[] generateMac(byte[] message) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), 
            ALGORITHM
        );
        mac.init(keySpec);
        return mac.doFinal(message);
    }
    
    /**
     * 验证消息的 MAC
     */
    public boolean verifyMac(byte[] message, byte[] receivedMac) throws Exception {
        byte[] calculatedMac = generateMac(message);
        return Arrays.equals(calculatedMac, receivedMac);
    }
    
    /**
     * 验证消息的 MAC（Base64 编码）
     */
    public boolean verifyMacBase64(byte[] message, String receivedMacBase64) throws Exception {
        byte[] receivedMac = Base64.getDecoder().decode(receivedMacBase64);
        return verifyMac(message, receivedMac);
    }
}
```

### 示例4：流式处理大数据

```java
public class StreamingMacGenerator {
    public byte[] generateMacForLargeData(String secret, InputStream dataStream) 
            throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        mac.init(keySpec);
        
        // 分块读取并更新
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = dataStream.read(buffer)) != -1) {
            mac.update(buffer, 0, bytesRead);
        }
        
        // 生成最终的 MAC
        return mac.doFinal();
    }
}
```

---

## 核心方法详解

### 1. 静态方法

#### getInstance(String algorithm)

```java
public static final Mac getInstance(String algorithm) 
        throws NoSuchAlgorithmException
```

**功能**: 获取指定算法的 Mac 实例

**参数**:
- `algorithm`: 算法名称（如 "HmacSHA256"）

**返回**: Mac 实例

**异常**: 
- `NoSuchAlgorithmException`: 如果算法不存在

**源码解析**:

```java
public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
    // 1. 获取所有支持该算法的服务提供者
    List<Provider.Service> services = GetInstance.getServices("Mac", algorithm);
    Iterator<Provider.Service> iterator = services.iterator();
    
    // 2. 遍历提供者，找到第一个可用的
    Provider.Service service;
    do {
        if (!iterator.hasNext()) {
            throw new NoSuchAlgorithmException("Algorithm " + algorithm + " not available");
        }
        service = iterator.next();
    } while (!JceSecurity.canUseProvider(service.getProvider()));
    
    // 3. 创建延迟初始化的 Mac 实例
    return new Mac(service, iterator, algorithm);
}
```

**延迟初始化机制**:
- Mac 实例创建时不会立即选择提供者
- 只有在调用 `init()`、`update()` 或 `doFinal()` 时才会选择提供者
- 这样可以提高性能，避免不必要的提供者选择

#### getInstance(String algorithm, String provider)

```java
public static final Mac getInstance(String algorithm, String provider) 
        throws NoSuchAlgorithmException, NoSuchProviderException
```

**功能**: 从指定提供者获取 Mac 实例

#### getInstance(String algorithm, Provider provider)

```java
public static final Mac getInstance(String algorithm, Provider provider) 
        throws NoSuchAlgorithmException
```

**功能**: 从指定 Provider 对象获取 Mac 实例

### 2. 实例方法

#### init(Key key)

```java
public final void init(Key key) throws InvalidKeyException
```

**功能**: 使用密钥初始化 Mac

**参数**:
- `key`: 密钥对象（通常是 `SecretKeySpec`）

**异常**:
- `InvalidKeyException`: 如果密钥无效

**源码解析**:

```java
public final void init(Key key) throws InvalidKeyException {
    try {
        if (this.spi != null) {
            // 如果已经选择了提供者，直接初始化
            this.spi.engineInit(key, null);
        } else {
            // 否则，延迟选择提供者
            this.chooseProvider(key, null);
        }
    } catch (InvalidAlgorithmParameterException e) {
        throw new InvalidKeyException("init() failed", e);
    }
    
    this.initialized = true;
}
```

#### init(Key key, AlgorithmParameterSpec params)

```java
public final void init(Key key, AlgorithmParameterSpec params) 
        throws InvalidKeyException, InvalidAlgorithmParameterException
```

**功能**: 使用密钥和算法参数初始化 Mac

#### update(byte input)

```java
public final void update(byte input) throws IllegalStateException
```

**功能**: 更新单个字节

#### update(byte[] input)

```java
public final void update(byte[] input) throws IllegalStateException
```

**功能**: 更新整个字节数组

#### update(byte[] input, int offset, int len)

```java
public final void update(byte[] input, int offset, int len) 
        throws IllegalStateException
```

**功能**: 更新字节数组的一部分

**参数**:
- `input`: 输入字节数组
- `offset`: 起始偏移量
- `len`: 长度

**异常**:
- `IllegalArgumentException`: 如果参数无效
- `IllegalStateException`: 如果 Mac 未初始化

#### update(ByteBuffer input)

```java
public final void update(ByteBuffer input)
```

**功能**: 更新 ByteBuffer 中的数据

#### doFinal()

```java
public final byte[] doFinal() throws IllegalStateException
```

**功能**: 完成 MAC 计算并返回结果（会自动重置）

**返回**: MAC 字节数组

**源码解析**:

```java
public final byte[] doFinal() throws IllegalStateException {
    this.chooseFirstProvider();
    if (!this.initialized) {
        throw new IllegalStateException("MAC not initialized");
    }
    
    // 1. 生成 MAC
    byte[] result = this.spi.engineDoFinal();
    
    // 2. 重置 Mac（重要：用于下一次计算）
    this.spi.engineReset();
    
    return result;
}
```

#### doFinal(byte[] input)

```java
public final byte[] doFinal(byte[] input) throws IllegalStateException
```

**功能**: 更新数据并完成 MAC 计算

**等价于**:
```java
update(input);
return doFinal();
```

#### doFinal(byte[] output, int outOffset)

```java
public final void doFinal(byte[] output, int outOffset) 
        throws ShortBufferException, IllegalStateException
```

**功能**: 完成 MAC 计算并将结果写入指定数组

**参数**:
- `output`: 输出数组
- `outOffset`: 输出偏移量

**异常**:
- `ShortBufferException`: 如果输出数组太小

#### reset()

```java
public final void reset()
```

**功能**: 重置 Mac，用于下一次计算

**注意**: `doFinal()` 会自动调用 `reset()`，通常不需要手动调用

#### getMacLength()

```java
public final int getMacLength()
```

**功能**: 获取 MAC 的长度（字节数）

**返回**: MAC 长度

**示例**:
```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);
int length = mac.getMacLength();  // 通常为 32（SHA-256 输出 256 位 = 32 字节）
```

#### getProvider()

```java
public final Provider getProvider()
```

**功能**: 获取当前使用的提供者

#### getAlgorithm()

```java
public final String getAlgorithm()
```

**功能**: 获取算法名称

#### clone()

```java
public final Object clone() throws CloneNotSupportedException
```

**功能**: 克隆 Mac 实例

**注意**: 只有实现了 `Cloneable` 接口的 MacSpi 才能被克隆

---

## 常见算法

### HmacSHA256（推荐）

- **输出长度**: 32 字节（256 位）
- **安全性**: 高
- **性能**: 良好
- **推荐场景**: 大多数应用场景

```java
Mac mac = Mac.getInstance("HmacSHA256");
```

### HmacSHA512

- **输出长度**: 64 字节（512 位）
- **安全性**: 非常高
- **性能**: 较慢
- **推荐场景**: 需要更高安全性的场景

```java
Mac mac = Mac.getInstance("HmacSHA512");
```

### HmacSHA1

- **输出长度**: 20 字节（160 位）
- **安全性**: 中等（已不推荐）
- **性能**: 较快
- **推荐场景**: 兼容旧系统

```java
Mac mac = Mac.getInstance("HmacSHA1");
```

### HmacMD5

- **输出长度**: 16 字节（128 位）
- **安全性**: 低（不推荐）
- **性能**: 快
- **推荐场景**: 仅用于兼容，不推荐新项目使用

```java
Mac mac = Mac.getInstance("HmacMD5");
```

---

## 最佳实践

### 1. 算法选择

```java
// ✅ 推荐：使用 HmacSHA256
Mac mac = Mac.getInstance("HmacSHA256");

// ❌ 不推荐：使用 HmacMD5（安全性低）
Mac mac = Mac.getInstance("HmacMD5");
```

### 2. 密钥管理

```java
// ✅ 推荐：使用强密钥
String secret = "your-strong-secret-key-at-least-32-characters";
SecretKeySpec keySpec = new SecretKeySpec(
    secret.getBytes(StandardCharsets.UTF_8), 
    "HmacSHA256"
);

// ❌ 不推荐：使用弱密钥
String weakSecret = "123456";
```

### 3. 异常处理

```java
// ✅ 推荐：捕获并处理异常
try {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(secretKey);
    byte[] macBytes = mac.doFinal(data);
} catch (NoSuchAlgorithmException e) {
    log.error("Algorithm not available", e);
} catch (InvalidKeyException e) {
    log.error("Invalid key", e);
} catch (IllegalStateException e) {
    log.error("MAC not initialized", e);
}
```

### 4. 资源管理

```java
// ✅ 推荐：每次使用创建新实例（Mac 不是线程安全的）
public byte[] generateMac(byte[] data) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(secretKey);
    return mac.doFinal(data);
}

// ❌ 不推荐：复用同一个实例（线程不安全）
private Mac mac = Mac.getInstance("HmacSHA256");
```

### 5. 编码一致性

```java
// ✅ 推荐：统一使用 UTF-8 编码
String data = "Hello, World!";
byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
mac.update(bytes);

// ❌ 不推荐：混用不同编码
byte[] bytes1 = data.getBytes(StandardCharsets.UTF_8);
byte[] bytes2 = data.getBytes(StandardCharsets.ISO_8859_1);
```

### 6. Base64 编码

```java
// ✅ 推荐：使用 Base64 编码传输 MAC
byte[] macBytes = mac.doFinal(data);
String macBase64 = Base64.getEncoder().encodeToString(macBytes);

// 解码
byte[] decodedMac = Base64.getDecoder().decode(macBase64);
```

---

## 注意事项

### 1. 线程安全

- **Mac 对象不是线程安全的**
- 每个线程应该使用自己的 Mac 实例
- 不要在多个线程间共享同一个 Mac 实例

```java
// ❌ 错误：多线程共享同一个 Mac 实例
private static Mac mac = Mac.getInstance("HmacSHA256");

// ✅ 正确：每个线程创建自己的实例
public byte[] generateMac(byte[] data) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(secretKey);
    return mac.doFinal(data);
}
```

### 2. 初始化顺序

- **必须先调用 `init()` 才能调用 `update()` 或 `doFinal()`**
- 未初始化的 Mac 会抛出 `IllegalStateException`

```java
// ❌ 错误：未初始化就使用
Mac mac = Mac.getInstance("HmacSHA256");
mac.update(data);  // 抛出 IllegalStateException

// ✅ 正确：先初始化
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);
mac.update(data);
```

### 3. 自动重置

- **`doFinal()` 会自动调用 `reset()`**
- 调用 `doFinal()` 后，Mac 会被重置，可以用于下一次计算
- 通常不需要手动调用 `reset()`

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(secretKey);

// 第一次计算
byte[] mac1 = mac.doFinal(data1);

// 可以直接用于下一次计算（已自动重置）
byte[] mac2 = mac.doFinal(data2);
```

### 4. 密钥长度

- **不同算法对密钥长度有不同要求**
- HmacSHA256 建议密钥长度至少 32 字节
- 密钥太短会影响安全性

```java
// ✅ 推荐：使用足够长的密钥
String secret = "your-strong-secret-key-at-least-32-characters";

// ❌ 不推荐：密钥太短
String weakSecret = "123456";
```

### 5. 延迟初始化

- **Mac 实例创建时不会立即选择提供者**
- 只有在调用 `init()`、`update()` 或 `doFinal()` 时才会选择提供者
- 这是性能优化，避免不必要的提供者选择

---

## 在项目中的实际应用

### 1. 钉钉签名生成（DingSignUtil）

```java
// DingSignUtil.java
public static String dingSign(String secret, long timestamp) {
    // 1. 拼接待签名字符串
    String stringToSign = timestamp + "\n" + secret;
    
    try {
        // 2. 获取 Mac 实例
        Mac mac = Mac.getInstance("HmacSHA256");
        
        // 3. 初始化 Mac（使用 secret 作为密钥）
        mac.init(new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        ));
        
        // 4. 更新数据并生成 MAC
        byte[] signData = mac.doFinal(
            stringToSign.getBytes(StandardCharsets.UTF_8)
        );
        
        // 5. Base64 编码并 URL 编码
        String base64Sign = new String(Base64.encodeBase64(signData));
        return URLEncoder.encode(base64Sign, StandardCharsets.UTF_8.name());
    } catch (Exception e) {
        log.error("DynamicTp, cal ding sign error", e);
        return "";
    }
}
```

**特点**:
- 使用 `secret` 作为密钥
- 对 `timestamp + "\n" + secret` 进行签名
- 结果进行 Base64 和 URL 编码

### 2. 飞书签名生成（LarkNotifier）

```java
// LarkNotifier.java
protected String genSign(String secret, Long timestamp) 
        throws NoSuchAlgorithmException, InvalidKeyException {
    // 1. 拼接待签名字符串
    String stringToSign = timestamp + "\n" + secret;
    
    // 2. 获取 Mac 实例
    Mac mac = Mac.getInstance("HmacSHA256");
    
    // 3. 初始化 Mac（注意：使用 stringToSign 作为密钥）
    mac.init(new SecretKeySpec(
        stringToSign.getBytes(StandardCharsets.UTF_8), 
        "HmacSHA256"
    ));
    
    // 4. 生成 MAC（传入空字节数组）
    byte[] signData = mac.doFinal(new byte[]{});
    
    // 5. Base64 编码
    return new String(Base64.encodeBase64(signData));
}
```

**特点**:
- 使用 `timestamp + "\n" + secret` 作为密钥
- 对空字节数组进行签名
- 结果进行 Base64 编码

**与钉钉的区别**:
- 钉钉：`secret` 作为密钥，对 `timestamp + "\n" + secret` 签名
- 飞书：`timestamp + "\n" + secret` 作为密钥，对空字节数组签名

---

## 常见问题

### Q1: 为什么 `doFinal()` 会自动重置？

**A**: `doFinal()` 方法在生成 MAC 后会自动调用 `reset()`，这样 Mac 实例可以用于下一次计算，无需手动重置。

### Q2: Mac 是线程安全的吗？

**A**: 不是。Mac 对象不是线程安全的，每个线程应该使用自己的 Mac 实例。

### Q3: 如何选择 MAC 算法？

**A**: 
- **推荐**: HmacSHA256（平衡了安全性和性能）
- **高安全性**: HmacSHA512
- **兼容性**: HmacSHA1（不推荐新项目使用）
- **不推荐**: HmacMD5（安全性低）

### Q4: 密钥长度有什么要求？

**A**: 
- HmacSHA256 建议密钥长度至少 32 字节
- 密钥太短会影响安全性
- 密钥可以比算法要求的长度更长

### Q5: 如何验证 MAC？

**A**: 重新计算 MAC 并与接收到的 MAC 进行比较：

```java
byte[] calculatedMac = mac.doFinal(data);
boolean isValid = Arrays.equals(calculatedMac, receivedMac);
```

---

## 总结

`Mac` 类是 Java 加密扩展中用于生成消息认证码的核心类，主要特点：

1. **消息完整性验证**: 确保消息在传输过程中未被篡改
2. **身份认证**: 验证消息发送者的身份
3. **基于密钥**: 使用密钥和哈希算法生成认证码
4. **算法丰富**: 支持多种 MAC 算法（HmacSHA256、HmacSHA512 等）

### 核心优势

- **安全性高**: 基于密钥的哈希，难以伪造
- **性能良好**: 基于哈希函数，计算速度快
- **易于使用**: API 简洁，支持链式调用
- **标准化**: 符合 JCE 标准，跨平台兼容

### 适用场景

- **API 签名**: 验证 API 请求的合法性
- **消息完整性**: 确保消息未被篡改
- **身份认证**: 验证消息发送者身份
- **Webhook 签名**: 钉钉、飞书等 Webhook 签名生成

通过 `Mac` 类，可以快速、安全地实现消息认证码的生成和验证，是 Java 开发中常用的加密工具。

