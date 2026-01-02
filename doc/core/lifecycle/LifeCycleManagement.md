# LifeCycleManagement

## 概述

`LifeCycleManagement` 是生命周期管理接口，定义了组件生命周期管理的方法。它提供了启动、停止、运行状态检查等功能。

## 核心作用

1. **生命周期定义**: 定义组件的生命周期方法
2. **状态管理**: 提供运行状态检查
3. **自动启动**: 支持自动启动配置

## 接口定义

```java
public interface LifeCycleManagement {
    void start();                    // 启动组件
    void stop();                     // 停止组件
    boolean isRunning();             // 检查是否运行中
    void stop(Runnable callback);    // 停止组件（带回调）
    boolean isAutoStartup();         // 是否自动启动
    int getPhase();                  // 获取阶段
    void shutdownInternal();         // 内部关闭
}
```

## 核心方法

### start()

**作用**: 启动组件

**说明**: 执行组件的初始化逻辑

---

### stop()

**作用**: 停止组件

**说明**: 执行组件的清理逻辑

---

### stop(Runnable callback)

**作用**: 停止组件（带回调）

**说明**: 停止组件后执行回调

---

### isRunning()

**作用**: 检查是否运行中

**返回**: `true` 表示运行中，`false` 表示已停止

---

### isAutoStartup()

**作用**: 是否自动启动

**返回**: `true` 表示自动启动，`false` 表示手动启动

---

### getPhase()

**作用**: 获取阶段

**返回**: 阶段值，值越大越后执行

---

### shutdownInternal()

**作用**: 内部关闭

**说明**: 关闭组件内部的资源

## 实现类

### DtpLifecycle

`DtpLifecycle` 实现了 `LifeCycleManagement` 接口：

```java
public class DtpLifecycle implements LifeCycleManagement {
    @Override
    public void start() {
        // 初始化执行器
    }
    
    @Override
    public void stop() {
        // 销毁执行器
    }
    
    // ...
}
```

## 使用场景

### 1. Spring 集成

在 Spring 环境中，实现 `LifeCycleManagement` 的组件会被自动管理：

```java
@Component
public class MyLifecycle implements LifeCycleManagement {
    @Override
    public void start() {
        // 启动逻辑
    }
    
    @Override
    public void stop() {
        // 停止逻辑
    }
    
    // ...
}
```

## 设计特点

### 1. 生命周期管理

提供完整的生命周期管理方法。

### 2. 状态检查

支持运行状态检查。

### 3. 阶段控制

通过 `getPhase()` 控制启动和关闭顺序。

## 注意事项

1. **线程安全**: 实现类需要保证线程安全
2. **资源释放**: 停止时需要正确释放资源
3. **阶段顺序**: 通过 `getPhase()` 控制执行顺序

