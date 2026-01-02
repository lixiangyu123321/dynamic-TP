# Benchmark 模块

## 模块概述

`benchmark` 模块是 DynamicTp 框架的性能基准测试模块，使用 JMH（Java Microbenchmark Harness）框架进行性能测试，评估框架的性能表现。

## 主要功能

### 1. 性能测试

- **线程池性能测试**: 测试动态线程池的性能表现
- **对比测试**: 对比动态线程池与原生线程池的性能差异
- **压力测试**: 在不同负载下测试线程池性能

### 2. 测试场景

- **任务提交性能**: 测试任务提交的性能
- **任务执行性能**: 测试任务执行的性能
- **配置刷新性能**: 测试配置刷新的性能影响
- **监控采集性能**: 测试监控采集的性能影响

### 3. JMH 集成

- 使用 JMH 框架进行微基准测试
- 提供标准的基准测试方法
- 支持多种测试模式

## 核心功能

### 1. 性能基准测试

使用 JMH 框架进行性能测试：

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ThreadPoolBenchmark {
    
    private DtpExecutor executor;
    
    @Setup
    public void setup() {
        executor = new DtpExecutor(
            10, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200)
        );
    }
    
    @Benchmark
    public void executeTask() {
        executor.execute(() -> {
            // 测试任务
        });
    }
}
```

### 2. 对比测试

对比动态线程池与原生线程池的性能：

- 测试相同配置下的性能差异
- 评估框架的性能开销
- 验证框架的性能优化效果

## 使用方式

### 1. 运行基准测试

```bash
mvn clean install
java -jar benchmark/target/benchmarks.jar
```

### 2. 指定测试类

```bash
java -jar benchmark/target/benchmarks.jar ThreadPoolBenchmark
```

### 3. 自定义参数

```bash
java -jar benchmark/target/benchmarks.jar \
    -wi 5 -w 1s -i 5 -r 1s \
    -f 3 \
    -jvmArgs "-Xmx2g -Xms2g"
```

参数说明：

- `-wi`: 预热迭代次数
- `-w`: 预热时间
- `-i`: 测试迭代次数
- `-r`: 测试时间
- `-f`: Fork 次数
- `-jvmArgs`: JVM 参数

## 测试指标

### 1. 吞吐量（Throughput）

测试单位时间内完成的任务数量：

- 任务提交吞吐量
- 任务执行吞吐量

### 2. 延迟（Latency）

测试任务从提交到执行完成的延迟：

- 平均延迟
- P50、P90、P99 延迟

### 3. 资源占用

测试线程池的资源占用：

- 内存占用
- CPU 占用

## 依赖关系

- `dynamic-tp-core`: 依赖核心模块
- `dynamic-tp-extension-agent`: 依赖 Agent 扩展
- `jmh-core`: JMH 核心库
- `jmh-generator-annprocess`: JMH 注解处理器

## 测试场景

### 1. 基础性能测试

测试动态线程池的基础性能：

- 任务提交性能
- 任务执行性能
- 线程池创建和销毁性能

### 2. 功能性能测试

测试框架功能的性能影响：

- 配置刷新性能影响
- 监控采集性能影响
- 告警检查性能影响

### 3. 对比测试

对比不同实现的性能：

- 动态线程池 vs 原生线程池
- 不同队列类型的性能对比
- 不同拒绝策略的性能对比

## 注意事项

1. **测试环境**: 确保测试环境稳定，避免外部因素影响
2. **JVM 参数**: 合理设置 JVM 参数，确保测试结果准确
3. **预热时间**: 充分预热，确保 JIT 编译完成
4. **多次运行**: 多次运行测试，取平均值
5. **结果分析**: 结合实际情况分析测试结果

## 测试报告

测试完成后会生成测试报告，包含：

- 测试结果数据
- 性能指标对比
- 性能分析建议

## 使用场景

1. **性能评估**: 评估框架的性能表现
2. **性能优化**: 通过测试发现性能瓶颈，进行优化
3. **版本对比**: 对比不同版本的性能差异
4. **配置优化**: 通过测试找到最优配置

## 示例测试

### 任务提交性能测试

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TaskSubmitBenchmark {
    
    @Benchmark
    public void submitTask(DtpExecutorState state) {
        state.executor.execute(() -> {
            // 空任务
        });
    }
}
```

### 配置刷新性能测试

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RefreshBenchmark {
    
    @Benchmark
    public void refreshConfig(DtpExecutorState state) {
        DtpProperties properties = new DtpProperties();
        // 设置配置
        state.executor.refresh(properties);
    }
}
```

## 最佳实践

1. **独立测试**: 每个功能独立测试，避免相互影响
2. **充分预热**: 确保充分预热，JIT 编译完成
3. **多次运行**: 多次运行测试，取稳定结果
4. **环境一致**: 保持测试环境一致，确保结果可比较
5. **结果验证**: 验证测试结果的合理性

