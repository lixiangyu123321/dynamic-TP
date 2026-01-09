# Micrometer 对接 Prometheus 完整示例

## 示例概述

本示例演示如何在 Spring Boot 应用中使用 Micrometer 将 DynamicTp 的线程池指标导出到 Prometheus，实现完整的监控体系。

## 项目结构

```
my-application/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       └── Application.java
│       └── resources/
│           └── application.yml
└── prometheus/
    └── prometheus.yml
```

## 1. 依赖配置

### Maven 依赖 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-application</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- DynamicTp Spring Boot Starter -->
        <dependency>
            <groupId>org.dromara.dynamictp</groupId>
            <artifactId>dynamic-tp-spring-boot-starter</artifactId>
            <version>1.1.0</version>
        </dependency>

        <!-- Micrometer Prometheus Registry -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Micrometer Core (通常由 Spring Boot 管理) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**依赖说明**:
- `spring-boot-starter-actuator`: Spring Boot 监控端点支持
- `dynamic-tp-spring-boot-starter`: DynamicTp 核心依赖
- `micrometer-registry-prometheus`: Micrometer 的 Prometheus 注册表实现
- `micrometer-core`: Micrometer 核心库（通常由 Spring Boot 自动管理版本）

## 2. Spring Boot 应用配置

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: my-application

# DynamicTp 配置
spring:
  dynamic:
    tp:
      # 是否启用 DynamicTp
      enabled: true
      
      # 是否开启监控指标采集
      enabledCollect: true
      
      # 监控数据采集器类型，使用 micrometer
      collectorTypes: micrometer
      
      # 监控采集间隔（秒）
      monitorInterval: 5
      
      # 线程池配置
      executors:
        - threadPoolName: dtpExecutor1
          threadPoolAliasName: 异步任务线程池
          corePoolSize: 10
          maximumPoolSize: 20
          keepAliveTime: 60
          queueCapacity: 200
          queueType: LinkedBlockingQueue
          rejectHandlerType: CallerRunsPolicy
          
        - threadPoolName: dtpExecutor2
          threadPoolAliasName: 定时任务线程池
          corePoolSize: 5
          maximumPoolSize: 10
          keepAliveTime: 60
          queueCapacity: 100

# Spring Boot Actuator 配置
management:
  # 端点配置
  endpoints:
    web:
      # 暴露的端点
      exposure:
        include: prometheus,metrics,health,info
      # 端点基础路径（可选）
      base-path: /actuator
      
  # 指标导出配置
  metrics:
    export:
      prometheus:
        # 启用 Prometheus 导出
        enabled: true
        # 指标描述（可选，默认启用）
        descriptions: true
        # 步长（可选，默认 1 分钟）
        step: 1m
        
    # 指标标签配置
    tags:
      application: ${spring事件驱动模型的使用.md.application.name}
      environment: ${spring事件驱动模型的使用.md.profiles.active:default}
      
  # 端点详细配置
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
```

**配置说明**:

#### DynamicTp 配置部分

- `enabled: true`: 启用 DynamicTp 功能
- `enabledCollect: true`: 开启监控指标采集
- `collectorTypes: micrometer`: 使用 Micrometer 作为指标收集器
- `monitorInterval: 5`: 每 5 秒采集一次指标
- `executors`: 定义线程池配置，支持多个线程池

#### Actuator 配置部分

- `endpoints.web.exposure.include`: 暴露的端点列表
  - `prometheus`: Prometheus 格式的指标端点
  - `metrics`: 通用指标端点
  - `health`: 健康检查端点
- `metrics.export.prometheus.enabled: true`: 启用 Prometheus 导出
- `metrics.tags`: 为所有指标添加全局标签

## 3. 应用主类

### Application.java

```java
package com.example;

import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootApplication
@RestController
public class Application {

    @Resource
    private DtpExecutor dtpExecutor1;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 测试接口，使用动态线程池执行任务
     */
    @GetMapping("/test")
    public String test() {
        dtpExecutor1.execute(() -> {
            try {
                // 模拟业务处理
                Thread.sleep(100);
                System.out.println("任务执行完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return "任务已提交到线程池";
    }

    /**
     * 获取线程池信息
     */
    @GetMapping("/pool-info")
    public String getPoolInfo() {
        ThreadPoolExecutor executor = dtpExecutor1.getThreadPoolExecutor();
        return String.format(
            "核心线程数: %d, 最大线程数: %d, 当前线程数: %d, 活跃线程数: %d, 队列大小: %d",
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size()
        );
    }
}
```

**代码说明**:
- 使用 `@Resource` 注入 DynamicTp 管理的线程池
- 提供测试接口验证线程池功能
- 提供信息查询接口查看线程池状态

## 4. Prometheus 配置

### prometheus.yml

```yaml
# 全局配置
global:
  # 默认采集间隔
  scrape_interval: 15s
  
  # 告警规则评估间隔
  evaluation_interval: 15s
  
  # 外部标签，会添加到所有时间序列
  external_labels:
    cluster: 'production'
    environment: 'prod'

# 告警管理器配置
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093  # 如果使用 Alertmanager，取消注释

# 告警规则文件
rule_files:
  # - "alerts.yml"  # 如果使用告警规则，取消注释

# 采集配置
scrape_configs:
  # Prometheus 自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
        labels:
          instance: prometheus

  # DynamicTp 应用监控
  - job_name: 'dynamictp-app'
    # 指标路径（Spring Boot Actuator 的 Prometheus 端点）
    metrics_path: '/actuator/prometheus'
    
    # 采集间隔（可以覆盖全局配置）
    scrape_interval: 5s
    
    # 采集超时时间
    scrape_timeout: 10s
    
    # 静态目标配置
    static_configs:
      - targets:
          # 应用地址和端口
          - 'localhost:8080'
        labels:
          app: 'my-application'
          environment: 'development'
          
  # 如果有多个应用实例，可以这样配置
  # - job_name: 'dynamictp-app'
  #   metrics_path: '/actuator/prometheus'
  #   static_configs:
  #     - targets:
  #         - '192.168.1.10:8080'
  #         - '192.168.1.11:8080'
  #         - '192.168.1.12:8080'
  #       labels:
  #         app: 'my-application'
  #         environment: 'production'
```

**配置说明**:

#### 全局配置

- `scrape_interval: 15s`: 默认每 15 秒采集一次指标
- `evaluation_interval: 15s`: 每 15 秒评估一次告警规则
- `external_labels`: 添加到所有时间序列的外部标签

#### 采集配置

- `job_name`: 作业名称，会作为 `job` 标签添加到指标中
- `metrics_path`: 指标端点路径，Spring Boot Actuator 默认为 `/actuator/prometheus`
- `scrape_interval`: 采集间隔，可以覆盖全局配置
- `static_configs.targets`: 目标应用地址列表
- `static_configs.labels`: 添加到该作业所有指标的标签

## 5. 启动和验证

### 5.1 启动应用

```bash
# 使用 Maven 启动
mvn spring事件驱动模型的使用.md-boot:run

# 或打包后启动
mvn clean package
java -jar target/my-application-1.0.0.jar
```

### 5.2 验证指标端点

访问 Prometheus 格式的指标端点：

```bash
curl http://localhost:8080/actuator/prometheus
```

**预期输出示例**:

```
# HELP thread_pool_current_size Current thread pool size
# TYPE thread_pool_current_size gauge
thread_pool_current_size{app_name="my-application",thread_pool_alias="异步任务线程池",thread_pool_name="dtpExecutor1"} 10.0
thread_pool_current_size{app_name="my-application",thread_pool_alias="定时任务线程池",thread_pool_name="dtpExecutor2"} 5.0

# HELP thread_pool_active_count Active thread count
# TYPE thread_pool_active_count gauge
thread_pool_active_count{app_name="my-application",thread_pool_alias="异步任务线程池",thread_pool_name="dtpExecutor1"} 3.0

# HELP thread_pool_queue_size Queue size
# TYPE thread_pool_queue_size gauge
thread_pool_queue_size{app_name="my-application",thread_pool_alias="异步任务线程池",thread_pool_name="dtpExecutor1"} 5.0

# HELP thread_pool_reject_count Reject count
# TYPE thread_pool_reject_count gauge
thread_pool_reject_count{app_name="my-application",thread_pool_alias="异步任务线程池",thread_pool_name="dtpExecutor1"} 0.0

# ... 更多指标
```

### 5.3 启动 Prometheus

#### 使用 Docker 启动

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus:latest
```

#### 使用二进制文件启动

```bash
# 下载 Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.40.0/prometheus-2.40.0.linux-amd64.tar.gz
tar xvfz prometheus-2.40.0.linux-amd64.tar.gz
cd prometheus-2.40.0.linux-amd64

# 启动
./prometheus --config.file=prometheus.yml
```

### 5.4 访问 Prometheus Web UI

打开浏览器访问: `http://localhost:9090`

#### 验证目标状态

1. 点击 **Status** → **Targets**
2. 查看 `dynamictp-app` 作业的状态
3. 状态应为 **UP**（绿色）

#### 查询指标

在 **Graph** 页面输入以下 PromQL 查询：

```promql
# 查询所有线程池的当前线程数
thread_pool_current_size

# 查询特定线程池的活跃线程数
thread_pool_active_count{thread_pool_name="dtpExecutor1"}

# 查询线程池使用率
thread_pool_current_size / thread_pool_maximum_size

# 查询队列使用率
thread_pool_queue_size / thread_pool_queue_capacity
```

## 6. 完整示例代码

### 6.1 测试控制器

```java
package com.example.controller;

import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/api")
public class TestController {

    @Resource
    private DtpExecutor dtpExecutor1;

    @Resource
    private DtpExecutor dtpExecutor2;

    /**
     * 提交任务到线程池
     */
    @GetMapping("/submit-task")
    public Map<String, Object> submitTask() {
        Map<String, Object> result = new HashMap<>();
        
        // 提交多个任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            dtpExecutor1.execute(() -> {
                try {
                    System.out.println("执行任务: " + taskId);
                    Thread.sleep(1000); // 模拟业务处理
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        result.put("status", "success");
        result.put("message", "已提交 10 个任务到线程池");
        return result;
    }

    /**
     * 获取线程池统计信息
     */
    @GetMapping("/pool-stats")
    public Map<String, Object> getPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        
        ThreadPoolExecutor executor1 = dtpExecutor1.getThreadPoolExecutor();
        ThreadPoolExecutor executor2 = dtpExecutor2.getThreadPoolExecutor();
        
        Map<String, Object> pool1 = new HashMap<>();
        pool1.put("corePoolSize", executor1.getCorePoolSize());
        pool1.put("maximumPoolSize", executor1.getMaximumPoolSize());
        pool1.put("poolSize", executor1.getPoolSize());
        pool1.put("activeCount", executor1.getActiveCount());
        pool1.put("queueSize", executor1.getQueue().size());
        pool1.put("completedTaskCount", executor1.getCompletedTaskCount());
        
        Map<String, Object> pool2 = new HashMap<>();
        pool2.put("corePoolSize", executor2.getCorePoolSize());
        pool2.put("maximumPoolSize", executor2.getMaximumPoolSize());
        pool2.put("poolSize", executor2.getPoolSize());
        pool2.put("activeCount", executor2.getActiveCount());
        pool2.put("queueSize", executor2.getQueue().size());
        pool2.put("completedTaskCount", executor2.getCompletedTaskCount());
        
        stats.put("dtpExecutor1", pool1);
        stats.put("dtpExecutor2", pool2);
        
        return stats;
    }
}
```

## 7. 常用 PromQL 查询示例

### 7.1 基础查询

```promql
# 查询所有线程池的当前线程数
thread_pool_current_size

# 查询特定线程池的指标
thread_pool_current_size{thread_pool_name="dtpExecutor1"}

# 多标签过滤
thread_pool_active_count{thread_pool_name="dtpExecutor1", app_name="my-application"}

# 正则匹配
thread_pool_current_size{thread_pool_name=~"dtp.*"}
```

### 7.2 聚合查询

```promql
# 所有线程池的平均当前线程数
avg(thread_pool_current_size)

# 按线程池名称分组求平均值
avg(thread_pool_current_size) by (thread_pool_name)

# 所有线程池的最大活跃线程数
max(thread_pool_active_count)

# 所有线程池的线程总数
sum(thread_pool_current_size)
```

### 7.3 计算查询

```promql
# 线程池使用率
thread_pool_current_size / thread_pool_maximum_size

# 队列使用率
thread_pool_queue_size / thread_pool_queue_capacity * 100

# 拒绝率（每秒）
rate(thread_pool_reject_count[5m])
```

### 7.4 时间范围查询

```promql
# 过去 5 分钟的平均值
avg_over_time(thread_pool_current_size[5m])

# 过去 1 小时的最大值
max_over_time(thread_pool_active_count[1h])
```

## 8. 告警规则示例

### alerts.yml

```yaml
groups:
  - name: thread_pool_alerts
    interval: 30s
    rules:
      # 线程池拒绝率过高
      - alert: ThreadPoolRejectHigh
        expr: rate(thread_pool_reject_count[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "线程池拒绝率过高"
          description: "线程池 {{ $labels.thread_pool_name }} 在过去 5 分钟内拒绝率超过 10/秒"

      # 线程池队列使用率过高
      - alert: ThreadPoolQueueFull
        expr: thread_pool_queue_size / thread_pool_queue_capacity > 0.8
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "线程池队列使用率过高"
          description: "线程池 {{ $labels.thread_pool_name }} 队列使用率超过 80%，当前: {{ $value | humanizePercentage }}"

      # 线程池 TP99 响应时间过高
      - alert: ThreadPoolTp99High
        expr: thread_pool_completed_task_time_tp99 > 1000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "线程池 TP99 响应时间过高"
          description: "线程池 {{ $labels.thread_pool_name }} TP99 响应时间超过 1000ms，当前: {{ $value }}ms"
```

在 `prometheus.yml` 中引用：

```yaml
rule_files:
  - "alerts.yml"
```

## 9. 故障排查

### 9.1 指标端点无法访问

**问题**: 访问 `http://localhost:8080/actuator/prometheus` 返回 404

**解决方法**:
1. 检查 `management.endpoints.web.exposure.include` 是否包含 `prometheus`
2. 检查 `management.metrics.export.prometheus.enabled` 是否为 `true`
3. 检查依赖是否正确引入 `micrometer-registry-prometheus`

### 9.2 Prometheus 无法采集指标

**问题**: Prometheus Targets 页面显示状态为 DOWN

**解决方法**:
1. 检查应用是否正常运行
2. 检查 `metrics_path` 配置是否正确（应为 `/actuator/prometheus`）
3. 检查网络连接，确保 Prometheus 可以访问应用
4. 查看 Prometheus 日志获取详细错误信息

### 9.3 指标数据缺失

**问题**: Prometheus 中查询不到指标

**解决方法**:
1. 确认 `collectorTypes` 配置为 `micrometer`
2. 确认 `enabledCollect` 为 `true`
3. 检查应用日志是否有错误信息
4. 等待一段时间后再次查询（指标采集有延迟）

## 10. 总结

通过以上配置，可以实现：

1. ✅ DynamicTp 线程池指标自动采集
2. ✅ 指标通过 Micrometer 导出为 Prometheus 格式
3. ✅ Prometheus 自动拉取和存储指标
4. ✅ 通过 PromQL 查询和分析指标
5. ✅ 配置告警规则监控异常情况

这样就完成了从应用指标采集到监控告警的完整链路。

