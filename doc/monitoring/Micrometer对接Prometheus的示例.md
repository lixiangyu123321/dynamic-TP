# Micrometer 对接 Prometheus 完整实战示例
本次示例基于 **Spring Boot 2.x/3.x**（Micrometer 与 Spring Boot Actuator 深度集成，最简配置），实现步骤如下：

## 一、环境准备
1.  开发工具：IDEA
2.  构建工具：Maven/Gradle（此处以 Maven 为例）
3.  额外环境（用于验证）：Prometheus 服务、Grafana 服务（可通过 Docker 快速部署）

## 二、步骤1：创建 Spring Boot 项目并添加依赖
创建一个普通 Spring Boot 项目，在 `pom.xml` 中添加以下核心依赖（无需额外引入 Micrometer 核心包，Actuator 已集成）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version> <!-- 兼容 2.x 所有版本，3.x 版本同样适用，只需修改此版本 -->
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>micrometer-prometheus-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>micrometer-prometheus-demo</name>
    
    <dependencies>
        <!-- 1. Web 依赖：提供简单接口用于测试自定义指标 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- 2. Actuator 依赖：暴露监控端点，集成 Micrometer -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- 3. Micrometer 对接 Prometheus 的适配器依赖 -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
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

## 三、步骤2：配置 application.yml（暴露 Prometheus 端点）
在 `src/main/resources` 下创建/修改 `application.yml`，配置 Actuator 暴露 `/actuator/prometheus` 端点（Prometheus 会通过该端点拉取指标）：

```yaml
server:
  port: 8080 # 应用端口，后续 Prometheus 配置需用到

# Spring Boot Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health # 暴露 prometheus 端点（核心）和 health 端点（用于检查应用状态）
  metrics:
    tags:
      application: micrometer-prometheus-demo # 全局指标标签（可选），用于区分不同应用
    export:
      prometheus:
        enabled: true # 启用 Prometheus 指标导出（默认已启用，显式配置更清晰）
```

## 四、步骤3：编写自定义业务指标
创建一个业务服务类，实现 3 种核心指标（Counter 计数器、Gauge 仪表盘、Timer 计时器），演示自定义指标采集：

```java
package com.example.demo.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BusinessMetricService {
    // 1. 计数器：统计用户注册次数
    private final Counter userRegisterCounter;
    
    // 2. 仪表盘：统计当前在线用户数（可增可减）
    private final AtomicInteger onlineUserCount;
    
    // 3. 计时器：统计订单处理耗时
    private final Timer orderProcessTimer;
    
    private final Random random = new Random();

    // 构造函数注入 MeterRegistry（Spring Boot 自动配置，直接注入即可）
    public BusinessMetricService(MeterRegistry meterRegistry) {
        // 初始化计数器，添加标签（区分注册渠道）
        this.userRegisterCounter = meterRegistry.counter(
                "business.user.register.count", // 指标名称（建议带业务前缀，避免冲突）
                "channel", "web" // 自定义标签
        );
        
        // 初始化在线用户数（AtomicInteger 保证线程安全），并注册为 Gauge 指标
        this.onlineUserCount = new AtomicInteger(0);
        Gauge.builder("business.online.user.count", onlineUserCount, AtomicInteger::get)
                .description("当前在线用户数")
                .tag("env", "prod")
                .register(meterRegistry);
        
        // 初始化计时器，配置分位数（P95、P99），便于后续分析耗时分布
        this.orderProcessTimer = Timer.builder("business.order.process.time")
                .description("订单处理耗时统计")
                .tag("type", "normal")
                .publishPercentiles(0.95, 0.99) // 输出 P95、P99 分位数
                .register(meterRegistry);
    }

    // ------------- 业务方法：用户注册（触发 Counter 递增）-------------
    public void userRegister() {
        // 计数器递增（每次调用该方法，指标值 +1）
        userRegisterCounter.increment();
        // 模拟业务逻辑
        System.out.println("用户注册成功，注册次数累计：" + (long) userRegisterCounter.count());
    }

    // ------------- 业务方法：用户上线/下线（修改 Gauge 数值）-------------
    public void userOnline() {
        onlineUserCount.incrementAndGet();
        System.out.println("用户上线，当前在线人数：" + onlineUserCount.get());
    }

    public void userOffline() {
        if (onlineUserCount.get() > 0) {
            onlineUserCount.decrementAndGet();
        }
        System.out.println("用户下线，当前在线人数：" + onlineUserCount.get());
    }

    // ------------- 业务方法：处理订单（统计耗时，两种方式：编程式 + 注解式）-------------
    // 方式1：编程式（手动包裹耗时逻辑，灵活度高）
    public void processOrderProgrammatically() {
        // 用 Timer 包裹耗时业务逻辑
        orderProcessTimer.record(() -> {
            try {
                // 模拟订单处理耗时（50-500 毫秒）
                long sleepTime = random.nextInt(450) + 50;
                Thread.sleep(sleepTime);
                System.out.println("订单处理完成（编程式），耗时：" + sleepTime + " 毫秒");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // 方式2：注解式（@Timed，无需手动包裹，零侵入，需配合 Spring Boot）
    // 注意：@Timed 注解默认需要启用 AOP，Spring Boot 中已自动配置
    @Timed(value = "business.order.process.annotation.time", 
           description = "订单处理耗时（注解式）", 
           percentiles = {0.95, 0.99})
    public void processOrderAnnotation() {
        try {
            long sleepTime = random.nextInt(450) + 50;
            Thread.sleep(sleepTime);
            System.out.println("订单处理完成（注解式），耗时：" + sleepTime + " 毫秒");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 五、步骤4：创建接口用于触发指标变化
创建一个 Controller，提供 HTTP 接口，方便通过浏览器/Postman 触发业务方法，生成指标数据：

```java
package com.example.demo.controller;

import com.example.demo.service.BusinessMetricService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final BusinessMetricService businessMetricService;

    public BusinessController(BusinessMetricService businessMetricService) {
        this.businessMetricService = businessMetricService;
    }

    // 用户注册接口
    @GetMapping("/user/register")
    public String userRegister() {
        businessMetricService.userRegister();
        return "用户注册请求已处理";
    }

    // 用户上线接口
    @GetMapping("/user/online")
    public String userOnline() {
        businessMetricService.userOnline();
        return "用户上线请求已处理";
    }

    // 用户下线接口
    @GetMapping("/user/offline")
    public String userOffline() {
        businessMetricService.userOffline();
        return "用户下线请求已处理";
    }

    // 订单处理（编程式）
    @GetMapping("/order/process/program")
    public String processOrderProgram() {
        businessMetricService.processOrderProgrammatically();
        return "订单处理（编程式）请求已处理";
    }

    // 订单处理（注解式）
    @GetMapping("/order/process/annotation")
    public String processOrderAnnotation() {
        businessMetricService.processOrderAnnotation();
        return "订单处理（注解式）请求已处理";
    }
}
```

## 六、步骤5：启动应用并验证指标端点
1.  启动 Spring Boot 应用，访问 `http://localhost:8080/actuator/prometheus`
2.  若能看到大量文本格式的指标数据（包含我们自定义的 `business.*` 前缀指标），说明端点暴露成功，示例如下（部分截取）：

```
# HELP business_user_register_count_total 当前用户注册次数
# TYPE business_user_register_count_total counter
business_user_register_count_total{application="micrometer-prometheus-demo",channel="web",} 3.0

# HELP business_online_user_count 当前在线用户数
# TYPE business_online_user_count gauge
business_online_user_count{application="micrometer-prometheus-demo",env="prod",} 2.0

# HELP business_order_process_time_seconds 订单处理耗时统计
# TYPE business_order_process_time_seconds summary
business_order_process_time_seconds_count{application="micrometer-prometheus-demo",type="normal",} 5.0
business_order_process_time_seconds_sum{application="micrometer-prometheus-demo",type="normal",} 1.234567
```

## 七、步骤6：配置 Prometheus 拉取应用指标
1.  找到 Prometheus 配置文件 `prometheus.yml`（Docker 部署的话，通常在挂载目录下）
2.  添加以下 `scrape_configs` 配置，让 Prometheus 定期拉取我们的应用指标：

```yaml
global:
  scrape_interval: 15s # 全局拉取间隔，每 15 秒拉取一次指标

scrape_configs:
  # 配置 1：拉取 Spring Boot 应用指标
  - job_name: "micrometer-prometheus-demo" # 任务名称，自定义
    scrape_interval: 5s # 该任务的拉取间隔（覆盖全局配置，更快看到指标变化）
    static_configs:
      - targets: ["localhost:8080"] # 替换为你的应用 IP + 端口（Docker 部署需填写宿主 IP 或容器内网 IP）
    metrics_path: "/actuator/prometheus" # 指标拉取路径（固定）
```

3.  重启 Prometheus 服务，访问 Prometheus UI（默认 `http://localhost:9090`）
4.  在「Graph」页面的搜索框中输入自定义指标名称（如 `business_user_register_count_total`），点击「Execute」，即可看到指标数据的变化曲线。

## 八、关键验证与补充说明
1.  **触发指标变化**：通过访问以下接口，生成指标数据：
    - 注册用户：`http://localhost:8080/api/business/user/register`（多次访问，计数器递增）
    - 用户上线：`http://localhost:8080/api/business/user/online`（多次访问，在线人数增加）
    - 处理订单：`http://localhost:8080/api/business/order/process/program`（触发计时器统计）
2.  **指标格式说明**：Micrometer 导出到 Prometheus 的指标会自动转换格式（如驼峰命名转下划线、Counter 指标自动添加 `_total` 后缀），这是 Prometheus 的标准规范，不影响使用。
3.  **Grafana 可视化（可选）**：若需更美观的仪表盘，可在 Grafana 中添加 Prometheus 数据源，然后导入现成的 Spring Boot 监控面板（模板 ID：4701 或 12856），即可看到自定义指标和默认指标（JVM、HTTP 请求等）的可视化展示。

## 九、总结
本次示例实现了 Micrometer 与 Prometheus 的完整对接，核心要点：
1.  核心依赖：`spring-boot-starter-actuator` + `micrometer-registry-prometheus`
2.  关键配置：暴露 `/actuator/prometheus` 端点
3.  自定义指标：通过 `MeterRegistry` 注入，创建 Counter、Gauge、Timer 三种核心指标
4.  Prometheus 配置：通过 `scrape_configs` 拉取应用指标端点

按照以上步骤，即可快速在 Spring Boot 应用中实现指标采集并对接 Prometheus，满足生产环境的监控需求。