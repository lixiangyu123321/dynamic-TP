# Filter 设计对比

## 概述

本文档介绍 DynamicTp 项目中 Filter 的设计，并与 Spring 框架中的 Filter 进行对比，帮助理解两种 Filter 的设计理念和使用场景。

## DynamicTp 中的 Filter 设计

### 1. Filter 接口

```java
public interface Filter<T> {
    /**
     * Filter order.
     * @return int val
     */
    int getOrder();
    
    /**
     * Do filter.
     * @param context context
     * @param nextInvoker next invoker
     */
    void doFilter(T context, Invoker<T> nextInvoker);
}
```

**核心特点**:
- **泛型设计**: 支持不同类型的上下文
- **顺序控制**: 通过 `getOrder()` 控制执行顺序
- **责任链模式**: 通过 `nextInvoker` 实现责任链传递
- **显式传递**: 需要显式调用 `nextInvoker.invoke()` 继续传递

### 2. NotifyFilter 接口

```java
public interface NotifyFilter extends Filter<BaseNotifyCtx> {
    /**
     * If supports this type.
     * @param notifyType notifyType
     * @return true if supported, else false
     */
    default boolean supports(NotifyTypeEnum notifyType) {
        return true;
    }
}
```

**扩展功能**:
- **类型支持**: 通过 `supports()` 方法控制是否支持特定通知类型
- **默认实现**: 默认支持所有类型

### 3. 内置 Filter 实现

#### BaseAlarmFilter

```java
public class BaseAlarmFilter implements NotifyFilter {
    @Override
    public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
        // 1. 检查基础条件
        if (!satisfyBaseCondition(notifyItem, executorWrapper)) {
            return;  // 中断责任链
        }
        
        // 2. 增加告警计数
        AlarmCounter.incAlarmCount(threadPoolName, notifyItem.getType());
        
        // 3. 判断是否达到阈值
        if (alarmInfo.getCount() < notifyItem.getCount()) {
            return;  // 未达到阈值，中断责任链
        }
        
        // 4. 继续传递
        nextInvoker.invoke(context);
    }
    
    @Override
    public int getOrder() {
        return 0;  // 最先执行
    }
}
```

#### SilentCheckFilter

```java
public class SilentCheckFilter implements NotifyFilter {
    @Override
    public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
        if (isSilent(context)) {
            return;  // 在静默期内，中断责任链
        }
        nextInvoker.invoke(context);  // 继续传递
    }
    
    @Override
    public int getOrder() {
        return 5;  // 在 BaseAlarmFilter 之后执行
    }
}
```

### 4. 责任链构建

```java
public static InvokerChain<BaseNotifyCtx> getAlarmInvokerChain() {
    // 1. 收集所有过滤器
    Collection<NotifyFilter> alarmFilters = ...;
    
    // 2. 过滤和排序
    alarmFilters = alarmFilters.stream()
        .filter(x -> x.supports(NotifyTypeEnum.ALARM))
        .sorted(Comparator.comparing(Filter::getOrder))
        .collect(Collectors.toList());
    
    // 3. 构建责任链
    return InvokerChainFactory.buildInvokerChain(
        new AlarmInvoker(),  // 终点
        alarmFilters.toArray(new NotifyFilter[0])
    );
}
```

### 5. 执行流程

```
1. chain.proceed(context)
   ↓
2. Filter1.doFilter(context, nextInvoker)
   ↓
3. 如果通过，调用 nextInvoker.invoke(context)
   ↓
4. Filter2.doFilter(context, nextInvoker)
   ↓
5. ...（继续传递）
   ↓
6. AlarmInvoker.invoke(context)  // 最终执行
```

## Spring 框架中的 Filter

### 1. Servlet Filter

#### Filter 接口

```java
public interface Filter {
    default void init(FilterConfig filterConfig) throws ServletException {}
    
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
        throws IOException, ServletException;
    
    default void destroy() {}
}
```

**核心特点**:
- **Servlet 规范**: 基于 Servlet 规范
- **请求/响应**: 处理 HTTP 请求和响应
- **FilterChain**: 通过 `chain.doFilter()` 继续传递
- **生命周期**: 支持 `init()` 和 `destroy()` 方法

#### 使用示例

```java
@Component
@Order(1)
public class LoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        // 前置处理
        long startTime = System.currentTimeMillis();
        
        // 继续传递
        chain.doFilter(request, response);
        
        // 后置处理
        long duration = System.currentTimeMillis() - startTime;
        log.info("Request processed in {} ms", duration);
    }
}
```

### 2. HandlerInterceptor

#### HandlerInterceptor 接口

```java
public interface HandlerInterceptor {
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        return true;
    }
    
    default void postHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler, ModelAndView modelAndView) throws Exception {}
    
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) throws Exception {}
}
```

**核心特点**:
- **Spring MVC**: 专门用于 Spring MVC
- **三个时机**: preHandle、postHandle、afterCompletion
- **返回值控制**: `preHandle` 返回 `false` 可以中断处理
- **异常处理**: `afterCompletion` 可以处理异常

#### 使用示例

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 认证检查
        if (!isAuthenticated(request)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;  // 中断处理
        }
        return true;  // 继续处理
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler, ModelAndView modelAndView) {
        // 后置处理
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) {
        // 清理资源
    }
}
```

### 3. OncePerRequestFilter

```java
public abstract class OncePerRequestFilter extends GenericFilterBean {
    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        // 确保每个请求只执行一次
        if (!shouldNotFilter(request)) {
            doFilterInternal(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }
    
    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException;
}
```

**特点**:
- **防止重复执行**: 确保每个请求只执行一次
- **抽象方法**: 子类实现 `doFilterInternal()`

## 设计对比

### 1. 接口设计对比

| 特性 | DynamicTp Filter | Servlet Filter | HandlerInterceptor |
|------|-----------------|---------------|-------------------|
| **泛型支持** | ✅ 支持泛型 `<T>` | ❌ 不支持 | ❌ 不支持 |
| **顺序控制** | ✅ `getOrder()` | ✅ `@Order` 注解 | ✅ `OrderComparator` |
| **传递方式** | ✅ 显式传递 `nextInvoker` | ✅ `chain.doFilter()` | ✅ 返回值控制 |
| **生命周期** | ❌ 不支持 | ✅ `init()` / `destroy()` | ❌ 不支持 |
| **异常处理** | ⚠️ 需要手动处理 | ⚠️ 需要手动处理 | ✅ `afterCompletion()` |

### 2. 执行流程对比

#### DynamicTp Filter

```
请求 → Filter1 → Filter2 → Filter3 → Invoker → 响应
       ↓         ↓         ↓
     过滤逻辑   过滤逻辑   过滤逻辑   执行逻辑
```

**特点**:
- 显式传递：需要调用 `nextInvoker.invoke()`
- 可以中断：直接 `return` 即可中断
- 无后置处理：只有前置过滤

#### Servlet Filter

```
请求 → Filter1 → Filter2 → Filter3 → Servlet → 响应
       ↓         ↓         ↓
     前置处理   前置处理   前置处理
       ↑         ↑         ↑
     后置处理   后置处理   后置处理
```

**特点**:
- 双向处理：可以在 `chain.doFilter()` 前后处理
- 显式传递：需要调用 `chain.doFilter()`
- 可以中断：不调用 `chain.doFilter()` 即可中断

#### HandlerInterceptor

```
请求 → preHandle → Handler → postHandle → afterCompletion
       ↓                      ↓            ↓
     前置处理                后置处理      清理
```

**特点**:
- 三个时机：preHandle、postHandle、afterCompletion
- 返回值控制：`preHandle` 返回 `false` 中断
- 异常处理：`afterCompletion` 可以处理异常

### 3. 使用场景对比

#### DynamicTp Filter

**适用场景**:
- ✅ 业务逻辑过滤（如告警过滤、通知过滤）
- ✅ 条件检查（如静默期检查、计数检查）
- ✅ 责任链处理
- ✅ 非 HTTP 请求处理

**示例**:
```java
// 告警过滤链
BaseAlarmFilter → SilentCheckFilter → AlarmInvoker
```

#### Servlet Filter

**适用场景**:
- ✅ HTTP 请求过滤（如认证、日志、CORS）
- ✅ 请求/响应处理
- ✅ 跨所有 Servlet 的处理
- ✅ 需要双向处理的场景

**示例**:
```java
// HTTP 请求过滤链
LoggingFilter → AuthFilter → CORSFilter → DispatcherServlet
```

#### HandlerInterceptor

**适用场景**:
- ✅ Spring MVC 请求拦截
- ✅ 需要访问 Handler 和 ModelAndView
- ✅ 需要异常处理
- ✅ 需要后置处理（如日志、统计）

**示例**:
```java
// Spring MVC 拦截链
AuthInterceptor → LoggingInterceptor → Handler → postHandle → afterCompletion
```

### 4. 扩展性对比

#### DynamicTp Filter

```java
// 扩展方式：实现 NotifyFilter 接口
@Component
public class CustomFilter implements NotifyFilter {
    @Override
    public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
        // 自定义过滤逻辑
        if (shouldFilter(context)) {
            return;
        }
        nextInvoker.invoke(context);
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public boolean supports(NotifyTypeEnum notifyType) {
        return notifyType == NotifyTypeEnum.ALARM;
    }
}
```

**特点**:
- 自动发现：通过 Spring Bean 自动发现
- 类型过滤：通过 `supports()` 控制参与类型
- 顺序控制：通过 `getOrder()` 控制顺序

#### Servlet Filter

```java
// 扩展方式：实现 Filter 接口
@Component
@Order(1)
public class CustomFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        // 自定义过滤逻辑
        chain.doFilter(request, response);
    }
}
```

**特点**:
- 自动注册：通过 `@Component` 自动注册
- 顺序控制：通过 `@Order` 注解控制顺序
- 全局生效：对所有请求生效

#### HandlerInterceptor

```java
// 扩展方式：实现 HandlerInterceptor 接口
@Component
public class CustomInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 自定义拦截逻辑
        return true;
    }
}

// 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CustomInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**");
    }
}
```

**特点**:
- 路径匹配：可以指定拦截路径
- 三个时机：支持前置、后置、完成处理
- 灵活配置：可以排除特定路径

## 代码示例对比

### DynamicTp Filter 示例

```java
// 1. 定义过滤器
@Component
public class RateLimitFilter implements NotifyFilter {
    @Override
    public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
        if (isRateLimited(context)) {
            log.warn("Rate limit exceeded");
            return;  // 中断责任链
        }
        nextInvoker.invoke(context);  // 继续传递
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public boolean supports(NotifyTypeEnum notifyType) {
        return notifyType == NotifyTypeEnum.ALARM;
    }
}

// 2. 使用（自动构建责任链）
InvokerChain<BaseNotifyCtx> chain = NotifyFilterBuilder.getAlarmInvokerChain();
chain.proceed(context);
```

### Servlet Filter 示例

```java
// 1. 定义过滤器
@Component
@Order(1)
public class LoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);  // 继续传递
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("Request processed in {} ms", duration);
        }
    }
}

// 2. 使用（自动注册到 FilterChain）
// Spring Boot 自动注册，无需手动调用
```

### HandlerInterceptor 示例

```java
// 1. 定义拦截器
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isAuthenticated(request)) {
            response.setStatus(401);
            return false;  // 中断处理
        }
        return true;  // 继续处理
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler, ModelAndView modelAndView) {
        // 后置处理
    }
}

// 2. 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }
}
```

## 设计模式应用

### DynamicTp Filter

**应用模式**:
- **责任链模式**: Filter → Filter → Invoker
- **模板方法模式**: Filter 接口定义模板，子类实现具体逻辑

**优势**:
- 解耦过滤逻辑和执行逻辑
- 支持动态组合过滤器
- 易于扩展和维护

### Servlet Filter

**应用模式**:
- **责任链模式**: Filter → Filter → Servlet
- **装饰器模式**: 可以包装请求和响应

**优势**:
- 标准 Servlet 规范
- 支持双向处理
- 广泛支持

### HandlerInterceptor

**应用模式**:
- **拦截器模式**: 在 Handler 执行前后拦截
- **模板方法模式**: 三个方法定义处理时机

**优势**:
- 专门用于 Spring MVC
- 支持三个处理时机
- 可以访问 Handler 和 ModelAndView

## 选择建议

### 使用 DynamicTp Filter 的场景

1. **业务逻辑过滤**: 如告警过滤、通知过滤
2. **非 HTTP 请求**: 如内部服务调用、事件处理
3. **责任链处理**: 需要多个过滤器顺序处理
4. **类型过滤**: 需要根据类型选择过滤器

### 使用 Servlet Filter 的场景

1. **HTTP 请求过滤**: 如认证、日志、CORS
2. **全局处理**: 需要处理所有 HTTP 请求
3. **双向处理**: 需要在请求前后都处理
4. **标准规范**: 需要遵循 Servlet 规范

### 使用 HandlerInterceptor 的场景

1. **Spring MVC 拦截**: 专门用于 Spring MVC
2. **Handler 访问**: 需要访问 Handler 和 ModelAndView
3. **异常处理**: 需要统一的异常处理
4. **路径匹配**: 需要根据路径选择拦截器

## 总结

### DynamicTp Filter 的特点

1. **业务导向**: 专门用于业务逻辑过滤
2. **责任链模式**: 通过 Invoker 实现责任链
3. **类型支持**: 通过 `supports()` 控制参与类型
4. **显式传递**: 需要显式调用 `nextInvoker.invoke()`

### Spring Filter 的特点

1. **HTTP 导向**: 专门用于 HTTP 请求处理
2. **标准规范**: 遵循 Servlet 规范
3. **双向处理**: 支持前置和后置处理
4. **自动注册**: Spring Boot 自动注册

### 设计理念差异

| 方面 | DynamicTp Filter | Spring Filter |
|------|-----------------|---------------|
| **设计目标** | 业务逻辑过滤 | HTTP 请求过滤 |
| **适用场景** | 内部服务、事件处理 | Web 请求处理 |
| **传递方式** | 显式传递 Invoker | 显式传递 FilterChain |
| **处理时机** | 前置过滤 | 前置+后置处理 |
| **类型支持** | 支持类型过滤 | 全局生效 |

两种 Filter 设计各有优势，适用于不同的场景。DynamicTp Filter 更适合业务逻辑的过滤和处理，而 Spring Filter 更适合 HTTP 请求的处理和拦截。

