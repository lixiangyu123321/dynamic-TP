# DtpBannerPrinter

## 概述

`DtpBannerPrinter` 是 Banner 打印器，用于在框架启动时打印框架的 Banner 信息。它会在 Spring 上下文刷新时自动打印 Banner。

## 核心作用

1. **Banner 打印**: 在框架启动时打印 Banner
2. **版本信息**: 显示框架版本信息
3. **链接信息**: 显示官网、GitHub、Gitee 等链接

## 核心属性

```java
private static final String NAME = " :: Dynamic Thread Pool :: ";
private static final String SITE = " :: https://dynamictp.cn ::";
private static final String GITHUB_REPO = " :: https://github.com/dromara/dynamic-tp ::";
private static final String GITEE_REPO = " :: https://gitee.com/dromara/dynamic-tp ::";
private static final String BANNER = "..." // ASCII 艺术字
```

## 核心方法

### 构造方法

```java
public DtpBannerPrinter() {
    EventBusManager.register(this);
}
```

**说明**: 注册到事件总线，监听上下文刷新事件

---

### onBannerPrintEvent(CustomContextRefreshedEvent event)

**作用**: 处理上下文刷新事件

**实现**:
```java
@Subscribe
public void onBannerPrintEvent(CustomContextRefreshedEvent event) {
    printBanner();
}
```

**说明**: 当 Spring 上下文刷新时，自动调用 `printBanner()`

---

### printBanner()

**作用**: 打印 Banner

**实现**:
```java
public static void printBanner() {
    boolean enable = Boolean.parseBoolean(
        ContextManagerHelper.getEnvironmentProperty(
            DynamicTpConst.BANNER_ENABLED_PROP, "true"
        )
    );
    if (enable) {
        log.info(BANNER + "\n" + NAME + "\n :: {} :: \n" + SITE + "\n" + GITHUB_REPO + "\n" + GITEE_REPO,
                VersionUtil.getVersion());
    }
}
```

**说明**:
- 检查是否启用 Banner（通过配置项 `spring.dynamic.tp.banner-enabled`，默认为 `true`）
- 如果启用，打印 Banner、名称、版本、链接等信息

## Banner 内容

打印的 Banner 包含：

1. **ASCII 艺术字**: "Dynamic Thread Pool" 的 ASCII 艺术字
2. **框架名称**: ":: Dynamic Thread Pool ::"
3. **版本信息**: 框架版本号
4. **官网链接**: https://dynamictp.cn
5. **GitHub 链接**: https://github.com/dromara/dynamic-tp
6. **Gitee 链接**: https://gitee.com/dromara/dynamic-tp

## 配置方式

### 启用 Banner（默认）

```yaml
spring:
  dynamic:
    tp:
      banner-enabled: true
```

### 禁用 Banner

```yaml
spring:
  dynamic:
    tp:
      banner-enabled: false
```

或通过系统属性：

```bash
-Dspring.dynamic.tp.banner-enabled=false
```

## 使用场景

### 1. 框架启动

框架启动时自动打印 Banner，显示框架信息。

### 2. 版本确认

通过 Banner 可以快速确认框架版本。

## 设计特点

### 1. 事件驱动

通过事件总线监听上下文刷新事件，自动触发打印。

### 2. 可配置

支持通过配置启用或禁用 Banner。

### 3. 静态方法

`printBanner()` 是静态方法，可以手动调用。

## 注意事项

1. **默认启用**: Banner 默认启用，可以通过配置禁用
2. **日志级别**: Banner 使用 INFO 级别日志输出
3. **事件触发**: 只有在 Spring 上下文刷新时才会自动打印

