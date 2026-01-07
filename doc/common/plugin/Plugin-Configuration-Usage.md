# Plugin 插件系统配置与使用指南

## 概述

DynamicTp 提供了一个基于动态代理的插件（拦截器）系统，允许开发者对线程池执行器的方法进行拦截和增强。插件系统采用 SPI（Service Provider Interface）机制自动发现和注册拦截器，支持方法级别的精确拦截。

### 核心定位

- **方法拦截**: 对目标对象的方法执行进行拦截和增强
- **动态代理**: 基于 ByteBuddy 实现动态代理
- **SPI 机制**: 自动发现和注册拦截器
- **灵活配置**: 支持按需启用拦截器

---

## 核心类说明

### 1. DtpInterceptor（拦截器接口）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpInterceptor.java`

**作用**: 定义拦截器的核心接口，所有拦截器必须实现此接口。

**核心方法**:

```java
public interface DtpInterceptor {
    /**
     * 拦截方法：对目标方法的执行过程进行拦截处理
     */
    Object intercept(DtpInvocation invocation) throws Throwable;
    
    /**
     * 默认方法：为目标对象创建代理对象
     */
    default Object plugin(Object target) {
        return DtpInterceptorProxyFactory.enhance(target, this);
    }
    
    /**
     * 重载方法：支持指定方法参数类型和参数
     */
    default Object plugin(Object target, Class<?>[] argumentTypes, Object[] arguments) {
        return DtpInterceptorProxyFactory.enhance(target, argumentTypes, arguments, this);
    }
}
```

### 2. DtpIntercepts（拦截器注解）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpIntercepts.java`

**作用**: 标注拦截器类，指定拦截器名称和要拦截的方法签名。

**属性**:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DtpIntercepts {
    /**
     * 拦截器名称
     */
    String name();
    
    /**
     * 方法签名数组
     */
    DtpSignature[] signatures();
}
```

### 3. DtpSignature（方法签名注解）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpSignature.java`

**作用**: 定义要拦截的方法签名，包括类、方法名和参数类型。

**属性**:

```java
public @interface DtpSignature {
    /**
     * 目标类
     */
    Class<?> clazz();
    
    /**
     * 方法名
     */
    String method();
    
    /**
     * 方法参数类型数组
     */
    Class<?>[] args();
}
```

### 4. DtpInvocation（方法调用上下文）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpInvocation.java`

**作用**: 封装方法调用的上下文信息，包括目标对象、方法和参数。

**核心方法**:

```java
public class DtpInvocation {
    private final Object target;      // 目标对象
    private final Method method;        // 方法对象
    private final Object[] args;       // 方法参数
    
    /**
     * 执行目标方法
     */
    public Object proceed() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }
}
```

### 5. DtpInvocationHandler（调用处理器）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpInvocationHandler.java`

**作用**: 实现 `InvocationHandler` 接口，处理代理对象的方法调用。

**核心逻辑**:

```java
public class DtpInvocationHandler implements InvocationHandler {
    private final Object target;
    private final DtpInterceptor interceptor;
    private final Map<Class<?>, Set<Method>> signatureMap;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Set<Method> methods = signatureMap.get(method.getDeclaringClass());
        // 如果方法在签名映射中，则拦截
        if (CollectionUtils.isNotEmpty(methods) && methods.contains(method)) {
            return interceptor.intercept(new DtpInvocation(target, method, args));
        }
        // 否则直接调用原方法
        return method.invoke(target, args);
    }
}
```

### 6. DtpInterceptorProxyFactory（代理工厂）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpInterceptorProxyFactory.java`

**作用**: 使用 ByteBuddy 创建动态代理类。

**核心方法**:

```java
public class DtpInterceptorProxyFactory {
    /**
     * 增强目标对象
     */
    public static Object enhance(Object target, DtpInterceptor interceptor) {
        return enhance(target, null, null, interceptor);
    }
    
    /**
     * 增强目标对象（支持指定构造参数）
     */
    public static Object enhance(Object target, Class<?>[] argumentTypes, 
                                 Object[] arguments, DtpInterceptor interceptor) {
        // 1. 获取方法签名映射
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        
        // 2. 检查目标类是否在签名映射中
        if (!signatureMap.containsKey(target.getClass())) {
            return target;
        }
        
        // 3. 使用 ByteBuddy 创建代理类
        Class<?> proxyClass = new ByteBuddy()
            .subclass(target.getClass())
            .name(String.format("%s$ByteBuddy$%s", target.getClass().getName(), UUIDUtil.genUuid(5)))
            .method(ElementMatchers.any())
            .intercept(InvocationHandlerAdapter.of(new DtpInvocationHandler(target, interceptor, signatureMap)))
            .make()
            .load(DtpInterceptorProxyFactory.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
            .getLoaded();
        
        // 4. 创建代理实例
        if (Objects.isNull(argumentTypes) || Objects.isNull(arguments)) {
            return proxyClass.getDeclaredConstructor().newInstance();
        }
        return proxyClass.getDeclaredConstructor(argumentTypes).newInstance(arguments);
    }
}
```

### 7. DtpInterceptorRegistry（拦截器注册表）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/DtpInterceptorRegistry.java`

**作用**: 管理所有拦截器的注册和查找，提供代理对象的创建方法。

**核心功能**:

```java
@Slf4j
public class DtpInterceptorRegistry {
    /**
     * 维护所有已注册的拦截器
     */
    private static final Map<String, DtpInterceptor> INTERCEPTORS = Maps.newConcurrentMap();
    
    /**
     * 静态初始化：自动加载 SPI 拦截器
     */
    static {
        List<DtpInterceptor> loadedInterceptors = ExtensionServiceLoader.get(DtpInterceptor.class);
        if (CollectionUtils.isNotEmpty(loadedInterceptors)) {
            loadedInterceptors.forEach(x -> {
                DtpIntercepts interceptsAnno = x.getClass().getAnnotation(DtpIntercepts.class);
                if (Objects.nonNull(interceptsAnno)) {
                    String name = StringUtils.isBlank(interceptsAnno.name()) 
                        ? x.getClass().getSimpleName() 
                        : interceptsAnno.name();
                    INTERCEPTORS.put(name, x);
                }
            });
        }
    }
    
    /**
     * 手动注册拦截器
     */
    public static void register(String name, DtpInterceptor dtpInterceptor) {
        log.info("DynamicTp register DtpInterceptor, name: {}, interceptor: {}", name, dtpInterceptor);
        INTERCEPTORS.put(name, dtpInterceptor);
    }
    
    /**
     * 获取所有拦截器
     */
    public static Map<String, DtpInterceptor> getInterceptors() {
        return Collections.unmodifiableMap(INTERCEPTORS);
    }
    
    /**
     * 使用所有拦截器增强目标对象
     */
    public static Object pluginAll(Object target) {
        return plugin(target, INTERCEPTORS.keySet());
    }
    
    /**
     * 使用指定的拦截器增强目标对象
     */
    public static Object plugin(Object target, Set<String> interceptors) {
        Collection<DtpInterceptor> filterInterceptors = getInterceptors(interceptors);
        for (DtpInterceptor interceptor : filterInterceptors) {
            target = interceptor.plugin(target);
        }
        return target;
    }
}
```

### 8. PluginException（插件异常）

**位置**: `common/src/main/java/org/dromara/dynamictp/common/plugin/PluginException.java`

**作用**: 插件相关的异常类。

---

## 配置方式

### 方式1：SPI 自动发现（推荐）

#### 步骤1：实现拦截器接口

```java
package com.example.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.plugin.DtpInterceptor;
import org.dromara.dynamictp.common.plugin.DtpIntercepts;
import org.dromara.dynamictp.common.plugin.DtpInvocation;
import org.dromara.dynamictp.common.plugin.DtpSignature;
import org.dromara.dynamictp.core.executor.DtpExecutor;

/**
 * 示例拦截器：拦截 DtpExecutor 的 execute 方法
 */
@DtpIntercepts(
    name = "testExecuteInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}
        )
    }
)
@Slf4j
public class TestExecuteInterceptor implements DtpInterceptor {
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        DtpExecutor dtpExecutor = (DtpExecutor) invocation.getTarget();
        String method = invocation.getMethod().getName();
        Object[] args = invocation.getArgs();
        
        log.info("TestExecuteInterceptor: dtpExecutor: {}, method: {}, args: {}",
            dtpExecutor.getThreadPoolName(), method, args);
        
        // 执行前置逻辑
        beforeExecute(dtpExecutor, method, args);
        
        try {
            // 执行原方法
            Object result = invocation.proceed();
            
            // 执行后置逻辑
            afterExecute(dtpExecutor, method, args, result);
            
            return result;
        } catch (Throwable e) {
            // 异常处理
            onException(dtpExecutor, method, args, e);
            throw e;
        }
    }
    
    private void beforeExecute(DtpExecutor executor, String method, Object[] args) {
        log.info("Before execute: {}", method);
    }
    
    private void afterExecute(DtpExecutor executor, String method, Object[] args, Object result) {
        log.info("After execute: {}", method);
    }
    
    private void onException(DtpExecutor executor, String method, Object[] args, Throwable e) {
        log.error("Exception in execute: {}", method, e);
    }
}
```

#### 步骤2：创建 SPI 配置文件

在 `src/main/resources/META-INF/services/` 目录下创建文件：

**文件名**: `org.dromara.dynamictp.common.plugin.DtpInterceptor`

**文件内容**:
```
com.example.interceptor.TestExecuteInterceptor
```

**说明**:
- 文件名必须是接口的完全限定名
- 文件内容是实现类的完全限定名（每行一个）
- 可以配置多个实现类

#### 步骤3：在 DtpExecutor 中配置插件名称

**YAML 配置**:

```yaml
dynamictp:
  executors:
    - threadPoolName: dtpExecutor1
      corePoolSize: 5
      maximumPoolSize: 10
      pluginNames:
        - testExecuteInterceptor
```

**Properties 配置**:

```properties
dynamictp.executors[0].threadPoolName=dtpExecutor1
dynamictp.executors[0].corePoolSize=5
dynamictp.executors[0].maximumPoolSize=10
dynamictp.executors[0].pluginNames[0]=testExecuteInterceptor
```

**Java 代码配置**:

```java
@Bean
public DtpExecutor dtpExecutor() {
    DtpExecutor executor = new DtpExecutor();
    executor.setThreadPoolName("dtpExecutor1");
    executor.setCorePoolSize(5);
    executor.setMaximumPoolSize(10);
    
    // 配置插件名称
    Set<String> pluginNames = Sets.newHashSet("testExecuteInterceptor");
    executor.setPluginNames(pluginNames);
    
    return executor;
}
```

### 方式2：手动注册

如果不想使用 SPI 机制，可以手动注册拦截器：

```java
import org.dromara.dynamictp.common.plugin.DtpInterceptorRegistry;
import com.example.interceptor.TestExecuteInterceptor;

// 手动注册拦截器
DtpInterceptorRegistry.register("testExecuteInterceptor", new TestExecuteInterceptor());
```

**注意**: 手动注册需要在 Spring Bean 初始化之前完成，建议在 `@PostConstruct` 方法或配置类中注册。

---

## 使用示例

### 示例1：拦截 execute 方法

```java
@DtpIntercepts(
    name = "executeInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}
        )
    }
)
@Slf4j
public class ExecuteInterceptor implements DtpInterceptor {
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        DtpExecutor executor = (DtpExecutor) invocation.getTarget();
        Runnable task = (Runnable) invocation.getArgs()[0];
        
        log.info("Execute task in thread pool: {}", executor.getThreadPoolName());
        
        // 执行原方法
        return invocation.proceed();
    }
}
```

### 示例2：拦截多个方法

```java
@DtpIntercepts(
    name = "multiMethodInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}
        ),
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "submit", 
            args = {Runnable.class}
        ),
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "submit", 
            args = {Callable.class}
        )
    }
)
@Slf4j
public class MultiMethodInterceptor implements DtpInterceptor {
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        Object[] args = invocation.getArgs();
        
        log.info("Intercept method: {}, args count: {}", methodName, args.length);
        
        // 根据方法名执行不同逻辑
        switch (methodName) {
            case "execute":
                return handleExecute(invocation);
            case "submit":
                return handleSubmit(invocation);
            default:
                return invocation.proceed();
        }
    }
    
    private Object handleExecute(DtpInvocation invocation) throws Throwable {
        // 执行前逻辑
        log.info("Before execute");
        Object result = invocation.proceed();
        log.info("After execute");
        return result;
    }
    
    private Object handleSubmit(DtpInvocation invocation) throws Throwable {
        // 执行前逻辑
        log.info("Before submit");
        Object result = invocation.proceed();
        log.info("After submit");
        return result;
    }
}
```

### 示例3：方法执行时间统计

```java
@DtpIntercepts(
    name = "timingInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}
        )
    }
)
@Slf4j
public class TimingInterceptor implements DtpInterceptor {
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = invocation.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Method {} executed in {} ms", 
                invocation.getMethod().getName(), duration);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Method {} failed after {} ms", 
                invocation.getMethod().getName(), duration, e);
            throw e;
        }
    }
}
```

### 示例4：任务执行前验证

```java
@DtpIntercepts(
    name = "validationInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}
        )
    }
)
@Slf4j
public class ValidationInterceptor implements DtpInterceptor {
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        DtpExecutor executor = (DtpExecutor) invocation.getTarget();
        Runnable task = (Runnable) invocation.getArgs()[0];
        
        // 验证线程池状态
        if (executor.isShutdown()) {
            throw new IllegalStateException("Thread pool is shutdown");
        }
        
        // 验证任务
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        
        // 执行原方法
        return invocation.proceed();
    }
}
```

### 示例5：在测试中使用

```java
@Slf4j
public class InterceptTest {
    
    public static class TestA {
        public void execute() {
            log.info("execute");
        }
        
        public void beforeExecute() {
            log.info("beforeExecute");
        }
        
        public void afterExecute() {
            log.info("afterExecute");
        }
    }
    
    @Test
    public void test() {
        // 创建拦截器
        AInterceptorTest interceptor = new AInterceptorTest();
        
        // 手动注册拦截器
        DtpInterceptorRegistry.register("TestAInterceptor", interceptor);
        
        // 创建目标对象
        TestA testA = new TestA();
        
        // 使用拦截器增强对象
        TestA enhanced = (TestA) DtpInterceptorRegistry.plugin(
            testA, 
            Sets.newHashSet("TestAInterceptor")
        );
        
        // 验证代理类
        Assert.assertTrue(enhanced.getClass().getSimpleName()
            .startsWith("InterceptTest$TestA$ByteBuddy$"));
        
        // 调用方法（会被拦截）
        enhanced.execute();
        enhanced.beforeExecute();
        enhanced.afterExecute();
    }
}
```

---

## 在 Spring 中的集成

### DtpPostProcessor

Spring 集成中，`DtpPostProcessor` 会自动处理 `DtpExecutor` Bean，应用配置的拦截器：

```java
// DtpPostProcessor.java
private Object registerAndReturnDtp(Object bean) {
    DtpExecutor dtpExecutor = (DtpExecutor) bean;
    
    // 1. 获取构造参数
    Object[] args = ConstructorUtil.buildTpExecutorConstructorArgs(dtpExecutor);
    Class<?>[] argTypes = ConstructorUtil.buildTpExecutorConstructorArgTypes();
    
    // 2. 获取配置的插件名称
    Set<String> pluginNames = dtpExecutor.getPluginNames();
    
    // 3. 使用拦截器增强 Bean
    DtpExecutor enhancedBean = (DtpExecutor) DtpInterceptorRegistry.plugin(
        bean, pluginNames, argTypes, args
    );
    
    // 4. 特殊处理 EagerDtpExecutor
    if (enhancedBean instanceof EagerDtpExecutor) {
        ((TaskQueue) enhancedBean.getQueue())
            .setExecutor((EagerDtpExecutor) enhancedBean);
    }
    
    // 5. 注册到 DtpRegistry
    DtpRegistry.registerExecutor(ExecutorWrapper.of(enhancedBean), REGISTER_SOURCE);
    
    return enhancedBean;
}
```

**工作流程**:

1. Spring 容器初始化 `DtpExecutor` Bean
2. `DtpPostProcessor` 拦截 Bean 初始化
3. 从 `DtpExecutor` 获取 `pluginNames` 配置
4. 使用 `DtpInterceptorRegistry.plugin()` 应用拦截器
5. 返回增强后的代理对象

---

## 配置说明

### 全局配置

可以在全局配置中设置默认的插件名称：

```yaml
dynamictp:
  # 全局配置
  executors:
    - threadPoolName: commonExecutor
      corePoolSize: 5
      maximumPoolSize: 10
      pluginNames:
        - testExecuteInterceptor
        - timingInterceptor
```

### 单个线程池配置

每个线程池可以独立配置插件：

```yaml
dynamictp:
  executors:
    - threadPoolName: executor1
      corePoolSize: 5
      maximumPoolSize: 10
      pluginNames:
        - testExecuteInterceptor
    
    - threadPoolName: executor2
      corePoolSize: 10
      maximumPoolSize: 20
      pluginNames:
        - timingInterceptor
        - validationInterceptor
```

### 插件名称匹配

插件名称支持大小写不敏感匹配：

```java
// 配置
pluginNames:
  - testExecuteInterceptor

// 以下名称都能匹配
- testExecuteInterceptor
- TestExecuteInterceptor
- TESTEXECUTEINTERCEPTOR
- testexecuteinterceptor
```

---

## 最佳实践

### 1. 拦截器命名

- **使用有意义的名称**: 名称应该清晰表达拦截器的用途
- **遵循命名规范**: 使用驼峰命名，如 `executeTimingInterceptor`
- **避免冲突**: 确保拦截器名称唯一

```java
@DtpIntercepts(
    name = "executeTimingInterceptor",  // ✅ 推荐
    signatures = {...}
)
```

### 2. 方法签名定义

- **精确匹配**: 使用完整的参数类型数组
- **支持重载**: 为每个重载方法定义单独的签名
- **类型安全**: 使用 `Class<?>` 而不是字符串

```java
@DtpIntercepts(
    name = "multiMethodInterceptor",
    signatures = {
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "execute", 
            args = {Runnable.class}  // ✅ 精确指定参数类型
        ),
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "submit", 
            args = {Runnable.class}   // ✅ 支持重载
        ),
        @DtpSignature(
            clazz = DtpExecutor.class, 
            method = "submit", 
            args = {Callable.class}    // ✅ 另一个重载
        )
    }
)
```

### 3. 异常处理

- **不要吞掉异常**: 除非有特殊需求，否则应该重新抛出异常
- **记录异常日志**: 在捕获异常时记录详细信息
- **保持异常类型**: 不要改变异常类型

```java
@Override
public Object intercept(DtpInvocation invocation) throws Throwable {
    try {
        // 前置逻辑
        beforeExecute();
        
        // 执行原方法
        Object result = invocation.proceed();
        
        // 后置逻辑
        afterExecute(result);
        
        return result;
    } catch (Throwable e) {
        // ✅ 记录异常
        log.error("Interceptor error", e);
        // ✅ 重新抛出异常
        throw e;
    }
}
```

### 4. 性能考虑

- **避免耗时操作**: 拦截器会在每次方法调用时执行，避免耗时操作
- **使用缓存**: 对于重复计算的结果，使用缓存
- **异步处理**: 对于非关键路径的逻辑，考虑异步处理

```java
@Override
public Object intercept(DtpInvocation invocation) throws Throwable {
    // ❌ 不推荐：每次调用都创建对象
    // Object expensiveObject = createExpensiveObject();
    
    // ✅ 推荐：使用缓存
    Object cachedObject = getCachedObject();
    
    return invocation.proceed();
}
```

### 5. 线程安全

- **无状态设计**: 拦截器应该是无状态的
- **避免共享可变状态**: 不要使用共享的可变状态
- **使用线程安全的数据结构**: 如果必须共享状态，使用线程安全的数据结构

```java
// ✅ 推荐：无状态拦截器
@Slf4j
public class StatelessInterceptor implements DtpInterceptor {
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        // 只使用局部变量
        String methodName = invocation.getMethod().getName();
        return invocation.proceed();
    }
}

// ❌ 不推荐：有状态拦截器
@Slf4j
public class StatefulInterceptor implements DtpInterceptor {
    private int count = 0;  // ❌ 共享可变状态
    
    @Override
    public Object intercept(DtpInvocation invocation) throws Throwable {
        count++;  // ❌ 线程不安全
        return invocation.proceed();
    }
}
```

---

## 注意事项

### 1. 拦截器顺序

多个拦截器会按照配置的顺序依次应用：

```java
// 配置
pluginNames:
  - interceptor1
  - interceptor2
  - interceptor3

// 执行顺序
interceptor1 -> interceptor2 -> interceptor3 -> 原方法
```

### 2. 代理类生成

- **类名格式**: 代理类名格式为 `原类名$ByteBuddy$UUID`
- **类加载器**: 使用 `INJECTION` 策略，将类注入到当前类加载器
- **性能影响**: 首次创建代理类会有性能开销，后续使用无影响

### 3. 方法匹配

- **精确匹配**: 方法签名必须完全匹配（类、方法名、参数类型）
- **不支持通配符**: 不支持方法名或参数类型的通配符
- **继承关系**: 只匹配声明类，不匹配继承的方法

### 4. SPI 配置

- **文件位置**: 必须在 `META-INF/services/` 目录下
- **文件命名**: 文件名必须是接口的完全限定名
- **文件内容**: 每行一个实现类的完全限定名
- **编码格式**: 文件必须使用 UTF-8 编码

### 5. 异常处理

- **不要捕获所有异常**: 只捕获需要处理的异常
- **保持异常链**: 使用 `initCause()` 保持异常链
- **记录上下文**: 在异常日志中记录足够的上下文信息

---

## 常见问题

### Q1: 拦截器没有被调用？

**A**: 检查以下几点：
1. 拦截器是否正确实现 `DtpInterceptor` 接口
2. 是否添加了 `@DtpIntercepts` 注解
3. 方法签名是否完全匹配
4. SPI 配置文件是否正确
5. `pluginNames` 配置是否正确

### Q2: 如何调试拦截器？

**A**: 
1. 在 `intercept()` 方法中添加日志
2. 检查 `DtpInterceptorRegistry.getInterceptors()` 是否包含你的拦截器
3. 验证方法签名是否匹配

### Q3: 拦截器可以修改返回值吗？

**A**: 可以。`intercept()` 方法的返回值会作为原方法的返回值：

```java
@Override
public Object intercept(DtpInvocation invocation) throws Throwable {
    Object result = invocation.proceed();
    // 可以修改返回值
    return modifyResult(result);
}
```

### Q4: 如何跳过原方法执行？

**A**: 不调用 `invocation.proceed()` 即可：

```java
@Override
public Object intercept(DtpInvocation invocation) throws Throwable {
    // 不调用 proceed()，直接返回
    return null;  // 或返回其他值
}
```

### Q5: 拦截器可以访问原对象吗？

**A**: 可以。通过 `invocation.getTarget()` 获取：

```java
@Override
public Object intercept(DtpInvocation invocation) throws Throwable {
    Object target = invocation.getTarget();
    // 可以访问原对象的字段和方法
    return invocation.proceed();
}
```

---

## 总结

DynamicTp 的插件系统提供了强大的方法拦截能力，主要特点：

1. **基于动态代理**: 使用 ByteBuddy 实现动态代理
2. **SPI 自动发现**: 支持 SPI 机制自动发现和注册拦截器
3. **精确方法匹配**: 支持精确的方法签名匹配
4. **灵活配置**: 支持按需启用拦截器
5. **易于扩展**: 接口简单，易于实现自定义拦截器

### 核心优势

- **非侵入式**: 不需要修改目标类代码
- **灵活配置**: 支持运行时配置拦截器
- **高性能**: 基于 ByteBuddy，性能优异
- **易于使用**: API 简洁，易于理解和使用

### 适用场景

- **方法执行时间统计**
- **方法执行前后处理**
- **异常处理和日志记录**
- **参数验证和转换**
- **性能监控和指标收集**

通过插件系统，可以轻松实现对线程池执行器方法的拦截和增强，满足各种业务需求。

