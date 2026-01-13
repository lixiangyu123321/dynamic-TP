/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.dynamictp.starter.common.monitor;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import lombok.val;
import org.apache.commons.collections4.MapUtils;
import org.dromara.dynamictp.common.entity.JvmStats;
import org.dromara.dynamictp.common.entity.Metrics;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.converter.ExecutorConverter;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.aware.MetricsAware;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

/**
 * DtpEndpoint related
 * XXX 定义了一个 Spring Boot Actuator 的自定义端点（Endpoint），
 * XXX 核心作用是：对外暴露一个名为 dynamictp 的监控端点，用于统一获取动态线程池（DTP）的运行指标、
 * XXX 自定义扩展指标，以及 JVM 内存相关的统计信息，方便监控系统（如 Prometheus、监控面板）采集这些核心数据。
 *
 * XXX 先理解 Spring Boot Actuator Endpoint 的定位
 * Spring Boot Actuator 是 Spring Boot 提供的监控 / 运维能力，@Endpoint 注解用于定义自定义监控端点：
 * @Endpoint(id = "dynamictp")：声明这个端点的唯一标识是 dynamictp，访问路径通常是 /actuator/dynamictp（默认需要开启 Actuator 端点暴露）；
 * @ReadOperation：声明这个方法是只读操作 （对应 HTTP GET 请求），用于获取监控数据，不会修改系统状态；
 * 这个端点的核心价值是：把分散的线程池、JVM 指标聚合起来，对外提供统一的监控数据接口。
 * @author yanhom
 * @since 1.0.0
 **/
@Endpoint(id = "dynamictp")
public class DtpEndpoint {

    @ReadOperation
    public List<Metrics> invoke() {
        // 1. 初始化指标列表，用于存放所有监控数据
        List<Metrics> metricsList = Lists.newArrayList();

        // 2. 第一步：收集所有动态线程池的核心指标
        // DtpRegistry：动态线程池注册表，存储所有已注册的线程池
        DtpRegistry.getAllExecutorNames().forEach(x -> {
            // 根据线程池名称获取包装类（包含线程池实例+配置+运行状态）
            ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper(x);
            // 转换为标准化的Metrics对象（包含核心线程数、活跃数、队列大小等）
            metricsList.add(ExecutorConverter.toMetrics(wrapper));
        });

        // 3. 第二步：收集自定义扩展的线程池指标（适配MetricsAware接口的Bean）
        // 获取所有实现了MetricsAware接口的Spring Bean（用户自定义的指标收集器）
        val handlerMap = ContextManagerHelper.getBeansOfType(MetricsAware.class);
        if (MapUtils.isNotEmpty(handlerMap)) {
            // 遍历所有自定义指标收集器，添加它们的多线程池统计数据
            handlerMap.forEach((k, v) -> metricsList.addAll(v.getMultiPoolStats()));
        }

        // 4. 第三步：收集JVM内存相关指标
        JvmStats jvmStats = new JvmStats();
        Runtime runtime = Runtime.getRuntime();
        // 格式化内存大小为可读格式（如1024MB → 1GB）
        // JVM最大可用内存
        jvmStats.setMaxMemory(FileUtil.readableFileSize(runtime.maxMemory()));
        // JVM已分配内存
        jvmStats.setTotalMemory(FileUtil.readableFileSize(runtime.totalMemory()));
        // JVM空闲内存
        jvmStats.setFreeMemory(FileUtil.readableFileSize(runtime.freeMemory()));
        // 计算JVM可用内存（最大内存 - 已分配 + 空闲）
        jvmStats.setUsableMemory(FileUtil.readableFileSize(runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()));
        metricsList.add(jvmStats);

        // 5. 返回所有聚合的监控指标
        return metricsList;
    }
}
