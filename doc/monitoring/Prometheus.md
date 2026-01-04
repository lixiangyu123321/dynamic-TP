# Prometheus 监控系统

## 概述

**Prometheus** 是一款开源的系统监控和告警工具包，最初由 SoundCloud 开发。它采用**拉取（Pull）模式**采集指标数据，通过强大的查询语言 **PromQL** 进行数据查询和分析，广泛应用于容器化环境和微服务架构的监控场景。

### 核心定位

- **时间序列数据库**: 专门用于存储时间序列数据
- **拉取模式**: 主动从目标应用拉取指标，而非应用推送
- **多维数据模型**: 通过指标名称和标签（Label）标识时间序列
- **强大的查询语言**: PromQL 支持复杂的数据查询和聚合

### 主要特性

1. **多维数据模型**: 通过指标名称和键值对标签标识时间序列
2. **灵活的查询语言**: PromQL 支持实时查询和聚合
3. **不依赖分布式存储**: 单节点即可运行
4. **拉取模式**: 通过 HTTP 主动拉取指标
5. **多种服务发现**: 支持静态配置、文件、Consul、Kubernetes 等
6. **告警系统**: 内置 Alertmanager 支持告警规则和通知
7. **丰富的可视化**: 支持 Grafana 等可视化工具

## 核心概念

### 1. 时间序列（Time Series）

时间序列是一系列按时间排序的数据点，每个数据点包含：
- **时间戳（Timestamp）**: 数据采集的时间
- **值（Value）**: 指标的值（通常是浮点数）

示例：
```
thread_pool_current_size{thread_pool_name="dtpExecutor1"} 10.0 @ 1640995200
thread_pool_current_size{thread_pool_name="dtpExecutor1"} 12.0 @ 1640995210
thread_pool_current_size{thread_pool_name="dtpExecutor1"} 15.0 @ 1640995220
```

### 2. 指标（Metric）

指标是时间序列的标识，由两部分组成：
- **指标名称（Metric Name）**: 描述指标的含义，如 `thread_pool_current_size`
- **标签（Labels）**: 键值对，用于区分不同的时间序列，如 `thread_pool_name="dtpExecutor1"`

### 3. 标签（Label）

标签是键值对，用于对指标进行多维度分类：

```promql
thread_pool_current_size{
  thread_pool_name="dtpExecutor1",
  app_name="my-app",
  thread_pool_alias="异步线程池"
}
```

**标签的作用**:
- 区分不同的时间序列
- 支持灵活的查询和过滤
- 支持聚合和分组

### 4. 指标类型

Prometheus 定义了四种指标类型：

#### Counter（计数器）

单调递增的计数器，只能增加或重置为 0，如请求总数、错误总数。

```promql
thread_pool_reject_count{thread_pool_name="dtpExecutor1"}
```

#### Gauge（仪表）

可以任意上下变化的数值，如当前线程数、队列大小。

```promql
thread_pool_current_size{thread_pool_name="dtpExecutor1"}
thread_pool_queue_size{thread_pool_name="dtpExecutor1"}
```

#### Histogram（直方图）

对观测值进行采样，并在可配置的桶中计数，用于统计分布。

#### Summary（摘要）

类似于 Histogram，但计算的是分位数。

### 5. 作业（Job）和实例（Instance）

- **Job**: 一组功能相同的实例，如 `dynamictp-app`
- **Instance**: 被监控的目标，通常是 `host:port`，如 `192.168.1.5:8080`

## 架构和工作原理

### 架构组件

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  Application│─────▶│  Prometheus   │─────▶│  Grafana    │
│  (Exporter) │      │   Server     │      │  Dashboard  │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ Alertmanager │
                     └──────────────┘
```

### 工作流程

1. **指标暴露**: 应用通过 HTTP 端点暴露指标（如 `/actuator/prometheus`）
2. **指标拉取**: Prometheus 定期从配置的目标拉取指标
3. **数据存储**: 指标数据存储在 Prometheus 的时间序列数据库中
4. **查询分析**: 通过 PromQL 查询语言进行数据查询和分析
5. **告警评估**: 根据告警规则评估并触发告警
6. **可视化**: 通过 Grafana 等工具进行数据可视化

### 拉取模式

Prometheus 采用**拉取（Pull）模式**：

- **优势**:
  - 集中管理采集目标
  - 应用无需知道监控系统地址
  - 更容易扩展和故障排查
- **劣势**:
  - 需要应用暴露 HTTP 端点
  - 不适合短生命周期的任务（如批处理作业）

## 在 DynamicTp 中的集成

### 1. 启用 Micrometer 收集器

DynamicTp 通过 Micrometer 将指标导出到 Prometheus：

```yaml
spring:
  dynamic:
    tp:
      enabled: true
      enabledCollect: true
      collectorTypes: micrometer              # 使用 Micrometer 收集器
      monitorInterval: 5                      # 采集间隔
```

### 2. 配置 Spring Boot Actuator

启用 Prometheus 端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,metrics,health
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3. 添加依赖

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 4. 访问指标端点

应用启动后，可以通过以下端点访问 Prometheus 格式的指标：

```bash
curl http://localhost:8080/actuator/prometheus
```

输出示例：
```
# HELP thread_pool_current_size Current thread pool size
# TYPE thread_pool_current_size gauge
thread_pool_current_size{app_name="my-app",thread_pool_alias="异步线程池",thread_pool_name="dtpExecutor1"} 10.0
thread_pool_current_size{app_name="my-app",thread_pool_alias="异步线程池",thread_pool_name="dtpExecutor2"} 8.0

# HELP thread_pool_active_count Active thread count
# TYPE thread_pool_active_count gauge
thread_pool_active_count{app_name="my-app",thread_pool_alias="异步线程池",thread_pool_name="dtpExecutor1"} 5.0
```

## Prometheus 配置

### 1. 安装 Prometheus

#### Docker 方式

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

#### 二进制方式

```bash
# 下载
wget https://github.com/prometheus/prometheus/releases/download/v2.40.0/prometheus-2.40.0.linux-amd64.tar.gz

# 解压
tar xvfz prometheus-2.40.0.linux-amd64.tar.gz
cd prometheus-2.40.0.linux-amd64

# 运行
./prometheus --config.file=prometheus.yml
```

### 2. 配置文件

创建 `prometheus.yml` 配置文件：

```yaml
# 全局配置
global:
  scrape_interval: 15s          # 默认采集间隔
  evaluation_interval: 15s      # 告警规则评估间隔
  external_labels:               # 外部标签
    cluster: 'production'
    environment: 'prod'

# 告警配置
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

# 告警规则文件
rule_files:
  - "alerts.yml"

# 采集配置
scrape_configs:
  # Prometheus 自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # DynamicTp 应用监控
  - job_name: 'dynamictp-app'
    metrics_path: '/actuator/prometheus'    # 指标路径
    scrape_interval: 5s                      # 采集间隔
    static_configs:
      - targets:
          - '192.168.1.5:8080'               # 应用地址
          - '192.168.1.6:8080'
        labels:
          app: 'dynamictp'
          environment: 'production'
```

### 3. Kubernetes 服务发现配置

在 Kubernetes 环境中，可以使用服务发现自动发现目标：

```yaml
scrape_configs:
  - job_name: 'dynamictp-k8s'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      # 只采集有 prometheus.io/scrape 注解的 Pod
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      # 使用注解中的路径
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      # 使用注解中的端口
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### 4. 启动 Prometheus

```bash
./prometheus --config.file=prometheus.yml
```

访问 `http://localhost:9090` 查看 Prometheus Web UI。

## PromQL 查询语言

PromQL 是 Prometheus 的查询语言，支持强大的数据查询和聚合。

### 基础查询

#### 选择器（Selector）

选择特定的时间序列：

```promql
# 查询所有线程池的当前线程数
thread_pool_current_size

# 查询特定线程池的当前线程数
thread_pool_current_size{thread_pool_name="dtpExecutor1"}

# 多标签过滤
thread_pool_current_size{thread_pool_name="dtpExecutor1", app_name="my-app"}

# 标签匹配操作符
thread_pool_current_size{thread_pool_name=~"dtp.*"}  # 正则匹配
thread_pool_current_size{thread_pool_name!="dtpExecutor1"}  # 不等于
```

#### 范围查询

查询一段时间内的数据：

```promql
# 查询过去 5 分钟的数据
thread_pool_current_size[5m]

# 查询过去 1 小时的数据
thread_pool_current_size[1h]
```

### 聚合操作

#### 聚合函数

```promql
# 平均值
avg(thread_pool_current_size)

# 最大值
max(thread_pool_current_size)

# 最小值
min(thread_pool_current_size)

# 总和
sum(thread_pool_current_size)

# 计数
count(thread_pool_current_size)

# 标准差
stddev(thread_pool_current_size)
```

#### 分组聚合

```promql
# 按线程池名称分组求平均值
avg(thread_pool_current_size) by (thread_pool_name)

# 按应用名称和线程池名称分组
sum(thread_pool_reject_count) by (app_name, thread_pool_name)
```

### 数学运算

```promql
# 计算线程池使用率
thread_pool_current_size / thread_pool_maximum_size

# 计算队列使用率
thread_pool_queue_size / thread_pool_queue_capacity * 100

# 计算拒绝率（每秒）
rate(thread_pool_reject_count[5m])
```

### 速率函数

```promql
# 计算每秒速率（适用于 Counter）
rate(thread_pool_reject_count[5m])

# 计算每秒增量
irate(thread_pool_reject_count[5m])

# 计算时间范围内的增量
increase(thread_pool_reject_count[1h])
```

### 常用查询示例

#### 线程池使用率

```promql
# 当前线程数 / 最大线程数
thread_pool_current_size / thread_pool_maximum_size
```

#### 队列使用率

```promql
# 队列大小 / 队列容量
thread_pool_queue_size / thread_pool_queue_capacity
```

#### 平均任务执行时间

```promql
# 按线程池分组
avg(thread_pool_completed_task_time_avg) by (thread_pool_name)
```

#### TP99 分位数

```promql
# 查询特定线程池的 TP99
thread_pool_completed_task_time_tp99{thread_pool_name="dtpExecutor1"}
```

#### 拒绝率趋势

```promql
# 计算过去 5 分钟的平均拒绝率
rate(thread_pool_reject_count[5m])
```

#### Top N 查询

```promql
# 查询拒绝次数最多的 5 个线程池
topk(5, thread_pool_reject_count)
```

## 告警配置

### 1. 告警规则文件

创建 `alerts.yml` 告警规则文件：

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
          description: "线程池 {{ $labels.thread_pool_name }} 队列使用率超过 80%"

      # 线程池 TP99 响应时间过高
      - alert: ThreadPoolTp99High
        expr: thread_pool_completed_task_time_tp99 > 1000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "线程池 TP99 响应时间过高"
          description: "线程池 {{ $labels.thread_pool_name }} TP99 响应时间超过 1000ms"

      # 线程池活跃线程数接近最大值
      - alert: ThreadPoolActiveThreadsHigh
        expr: thread_pool_active_count / thread_pool_maximum_size > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "线程池活跃线程数接近最大值"
          description: "线程池 {{ $labels.thread_pool_name }} 活跃线程数达到最大值的 90%"

      # 线程池执行超时
      - alert: ThreadPoolRunTimeout
        expr: increase(thread_pool_run_timeout_count[5m]) > 5
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "线程池执行超时"
          description: "线程池 {{ $labels.thread_pool_name }} 在过去 5 分钟内执行超时次数超过 5 次"
```

### 2. 在 Prometheus 配置中引用

```yaml
rule_files:
  - "alerts.yml"
```

### 3. Alertmanager 配置

创建 `alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'thread_pool_name']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'web.hook'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://127.0.0.1:5001/'

  - name: 'critical-alerts'
    webhook_configs:
      - url: 'http://127.0.0.1:5001/critical'

  - name: 'warning-alerts'
    webhook_configs:
      - url: 'http://127.0.0.1:5001/warning'
```

## Grafana 集成

### 1. 添加 Prometheus 数据源

在 Grafana 中添加 Prometheus 数据源：

1. 进入 **Configuration** → **Data Sources**
2. 点击 **Add data source**
3. 选择 **Prometheus**
4. 配置 URL: `http://prometheus:9090`
5. 点击 **Save & Test**

### 2. 创建仪表板

#### 线程池监控面板

**当前线程数**:
```promql
thread_pool_current_size{thread_pool_name="dtpExecutor1"}
```

**活跃线程数**:
```promql
thread_pool_active_count{thread_pool_name="dtpExecutor1"}
```

**队列大小**:
```promql
thread_pool_queue_size{thread_pool_name="dtpExecutor1"}
```

**线程池使用率**:
```promql
thread_pool_current_size / thread_pool_maximum_size
```

**TP99 响应时间**:
```promql
thread_pool_completed_task_time_tp99{thread_pool_name="dtpExecutor1"}
```

**拒绝率**:
```promql
rate(thread_pool_reject_count[5m]){thread_pool_name="dtpExecutor1"}
```

### 3. 导入预定义仪表板

DynamicTp 提供了预定义的 Grafana 仪表板，可以直接导入：

1. 下载仪表板 JSON 文件（位于 `example/metric/` 目录）
2. 在 Grafana 中点击 **Import**
3. 上传 JSON 文件
4. 选择 Prometheus 数据源
5. 点击 **Import**

## 最佳实践

### 1. 指标命名规范

- 使用小写字母和下划线
- 使用有意义的名称
- 遵循命名约定：`<namespace>_<metric>_<unit>`

示例：
```
thread_pool_current_size
thread_pool_queue_size
thread_pool_reject_count
```

### 2. 标签使用建议

- **不要过度使用标签**: 每个标签组合都会创建新的时间序列
- **使用有意义的标签**: 如应用名称、环境、线程池名称等
- **避免高基数标签**: 如用户 ID、请求 ID 等会导致指标爆炸

### 3. 采集频率

- **生产环境**: 建议 5-15 秒采集一次
- **开发环境**: 可以设置更短的间隔（1-5 秒）
- **高负载环境**: 可以适当增加间隔（15-30 秒）

### 4. 存储和保留

- **本地存储**: Prometheus 默认保留 15 天数据
- **长期存储**: 使用 Thanos 或 Cortex 进行长期存储
- **数据压缩**: 定期清理旧数据，避免磁盘空间不足

### 5. 告警规则设计

- **避免告警风暴**: 合理设置 `for` 持续时间
- **分级告警**: 使用不同严重级别（warning、critical）
- **告警聚合**: 使用 `group_by` 对相关告警进行分组
- **告警抑制**: 配置告警抑制规则，避免重复告警

### 6. 性能优化

- **指标数量**: 控制指标和标签的数量，避免指标爆炸
- **查询优化**: 使用范围查询和聚合函数减少查询开销
- **记录规则**: 使用记录规则预计算常用查询
- **采样**: 对于高频指标，考虑采样

### 7. 高可用部署

- **多实例**: 部署多个 Prometheus 实例
- **联邦**: 使用联邦模式聚合多个 Prometheus 实例
- **远程写入**: 使用远程写入将数据备份到长期存储

## 常见问题

### 1. 指标不显示

**可能原因**:
- Prometheus 配置错误
- 应用端点无法访问
- 指标路径不正确

**解决方法**:
- 检查 Prometheus 配置中的 `targets` 和 `metrics_path`
- 验证应用端点是否可访问：`curl http://app:8080/actuator/prometheus`
- 检查 Prometheus 日志

### 2. 指标数据缺失

**可能原因**:
- 采集间隔过长
- 应用重启导致数据中断
- 网络问题

**解决方法**:
- 调整 `scrape_interval`
- 检查应用健康状态
- 检查网络连接

### 3. 查询性能问题

**可能原因**:
- 查询范围过大
- 指标数量过多
- 复杂聚合查询

**解决方法**:
- 缩小查询时间范围
- 使用记录规则预计算
- 优化查询表达式

## 相关资源

- **Prometheus 官方文档**: https://prometheus.io/docs
- **PromQL 查询语言**: https://prometheus.io/docs/prometheus/latest/querying/basics/
- **Grafana 文档**: https://grafana.com/docs
- **Micrometer Prometheus**: https://micrometer.io/docs/registry/prometheus
- **DynamicTp Micrometer**: [Micrometer.md](./Micrometer.md)

## 总结

Prometheus 作为强大的监控系统，与 DynamicTp 的 Micrometer 收集器完美集成，提供了全面的线程池监控能力。通过 PromQL 查询语言、告警规则和 Grafana 可视化，可以构建完整的监控和告警体系，帮助及时发现和解决线程池相关问题。

