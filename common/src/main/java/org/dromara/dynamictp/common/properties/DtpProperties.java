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

package org.dromara.dynamictp.common.properties;

import com.google.common.collect.Lists;
import lombok.Data;
import org.dromara.dynamictp.common.em.CollectorTypeEnum;
import org.dromara.dynamictp.common.entity.DtpExecutorProps;
import org.dromara.dynamictp.common.entity.NotifyPlatform;
import org.dromara.dynamictp.common.entity.TpExecutorProps;

import java.util.List;

/**
 * XXX 全局配置
 * 动态线程池（Dynamic Thread Pool, DTP）核心配置类
 * 作用：统一管理动态线程池的所有配置项（开关、监控、注册中心、各组件线程池等）
 * 设计：单例模式（Holder 静态内部类）+ Lombok @Data（自动生成get/set/toString等方法）
 */
@Data
public class DtpProperties {

    /**
     * 私有构造方法：禁止外部实例化（保证单例）
     * 配合静态内部类 Holder 实现懒加载、线程安全的单例模式
     */
    private DtpProperties() { }

    /**
     * 核心开关：是否启用动态线程池功能
     * 默认值：true（开启），设为false则整个DTP框架不生效
     */
    private boolean enabled = true;

    /**
     * 部署环境标识：如 dev/test/prod
     * 若未配置，会读取系统变量/配置文件中的 "APP.ENV" 作为默认值
     */
    private String env;

    /**
     * 启动banner开关：是否在应用启动时打印DTP框架的banner标识
     * 默认值：true（打印），设为false可关闭启动日志中的banner
     */
    private boolean enabledBanner = true;

    /**
     * 监控指标收集开关：是否开启线程池指标（如活跃数、队列长度、耗时等）收集
     * 默认值：true（开启），关闭后不会采集任何线程池监控数据
     */
    private boolean enabledCollect = true;

    /**
     * 监控指标收集器类型列表
     * 默认值：[MICROMETER]（基于Micrometer框架收集指标，可对接Prometheus/Grafana）
     * 可选值：logging（日志输出）、micrometer（指标采集）、alert（告警推送）等（由CollectorTypeEnum定义）
     */
    private List<String> collectorTypes = Lists.newArrayList(CollectorTypeEnum.MICROMETER.name());

    /**
     * 监控日志存储路径：仅对 "logging" 类型收集器生效
     * 若配置，指标会输出到该路径的日志文件；未配置则输出到应用默认日志
     */
    private String logPath;

    /**
     * 配置文件类型：针对Zookeeper/ETCD等配置中心的配置格式
     * 可选值：json/yaml/properties，用于解析配置中心中的线程池配置
     */
    private String configType;

    /**
     * 监控采集间隔：单位为秒（s）
     * 默认值：5秒，即每5秒采集一次线程池的监控指标
     */
    private int monitorInterval = 5;

    /**
     * 告警通知平台配置列表：如钉钉、企业微信、飞书、邮件等
     * 每个NotifyPlatform对应一个告警渠道的配置（webhook、密钥、模板等）
     */
    private List<NotifyPlatform> platforms;

    /**
     * Zookeeper配置：用于从ZK读取/更新线程池动态配置
     * 嵌套类封装ZK的连接、节点、版本等信息
     */
    private Zookeeper zookeeper;

    /**
     * Etcd配置：用于从ETCD读取/更新线程池动态配置
     * 嵌套类封装ETCD的连接、认证、超时等信息
     */
    private Etcd etcd;

    /**
     * 线程池全局配置：对所有自定义线程池生效的默认配置
     * 如核心线程数、最大线程数、队列类型、拒绝策略等通用配置
     */
    private DtpExecutorProps globalExecutorProps;

    /**
     * 自定义线程池配置列表：业务自定义的ThreadPoolExecutor配置
     * 每个DtpExecutorProps对应一个独立的线程池（如orderExecutor、payExecutor）
     */
    private List<DtpExecutorProps> executors;

    /**
     * Tomcat容器工作线程池配置：用于动态调整Tomcat的worker线程池参数
     * 如核心线程数、最大线程数、空闲超时时间等
     */
    private TpExecutorProps tomcatTp;

    /**
     * Jetty容器线程池配置：用于动态调整Jetty服务器的线程池参数
     */
    private TpExecutorProps jettyTp;

    /**
     * Undertow容器线程池配置：用于动态调整Undertow服务器的IO线程/工作线程参数
     */
    private TpExecutorProps undertowTp;

    /**
     * Dubbo框架线程池配置列表：支持多组Dubbo Provider/Consumer线程池动态调整
     */
    private List<TpExecutorProps> dubboTp;

    /**
     * Hystrix熔断框架线程池配置列表：动态调整Hystrix命令执行的线程池参数
     */
    private List<TpExecutorProps> hystrixTp;

    /**
     * RocketMQ消息队列线程池配置列表：动态调整生产者/消费者的线程池参数
     */
    private List<TpExecutorProps> rocketMqTp;

    /**
     * Grpc框架线程池配置列表：动态调整Grpc服务端/客户端的线程池参数
     */
    private List<TpExecutorProps> grpcTp;

    /**
     * Motan RPC框架服务端线程池配置列表：动态调整Motan服务器的线程池参数
     */
    private List<TpExecutorProps> motanTp;

    /**
     * Okhttp3客户端线程池配置列表：动态调整Okhttp3的连接池/执行线程池参数
     */
    private List<TpExecutorProps> okhttp3Tp;

    /**
     * Brpc框架线程池配置列表：动态调整Brpc服务端/客户端的线程池参数
     */
    private List<TpExecutorProps> brpcTp;

    /**
     * Tars RPC框架线程池配置列表：动态调整Tars服务的线程池参数
     */
    private List<TpExecutorProps> tarsTp;

    /**
     * SOFA RPC框架线程池配置列表：动态调整SOFA服务端/客户端的线程池参数
     */
    private List<TpExecutorProps> sofaTp;

    /**
     * RabbitMQ消息队列线程池配置列表：动态调整生产者/消费者的线程池参数
     */
    private List<TpExecutorProps> rabbitmqTp;

    /**
     * Liteflow规则引擎线程池配置列表：动态调整规则执行的线程池参数
     */
    private List<TpExecutorProps> liteflowTp;

    /**
     * Thrift RPC框架线程池配置列表：动态调整Thrift服务端/客户端的线程池参数
     */
    private List<TpExecutorProps> thriftTp;

    /**
     * 获取DtpProperties单例实例（懒加载、线程安全）
     * 基于静态内部类Holder实现，JVM类加载机制保证线程安全，且懒加载（调用时才初始化）
     * @return DtpProperties单例对象
     */
    public static DtpProperties getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 嵌套静态类：Zookeeper配置项封装
     * 作用：集中管理ZK连接和配置读取的相关参数
     */
    @Data
    public static class Zookeeper {

        /** ZK集群连接串：如 127.0.0.1:2181,127.0.0.1:2182 */
        private String zkConnectStr;

        /** 配置版本号：用于配置中心的配置版本控制（如灰度发布、回滚） */
        private String configVersion;

        /** ZK根节点：线程池配置的根路径，如 /dtp/config */
        private String rootNode;

        /** ZK子节点：当前应用的配置节点，如 /dtp/config/app-xxx */
        private String node;

        /** 配置键名：节点中存储配置的key，如 dtp-config */
        private String configKey;
    }

    /**
     * 嵌套静态类：Etcd配置项封装
     * 作用：集中管理ETCD连接、认证和配置读取的相关参数
     */
    @Data
    public static class Etcd {

        /** ETCD集群端点：如 http://127.0.0.1:2379,http://127.0.0.1:2380 */
        private String endpoints;

        /** ETCD认证用户名：仅当authEnable=true时生效 */
        private String user;

        /** ETCD认证密码：仅当authEnable=true时生效 */
        private String password;

        /** 配置编码格式：默认UTF-8，用于解析ETCD中的配置内容 */
        private String charset = "UTF-8";

        /** ETCD认证开关：是否启用用户名密码认证 */
        private boolean authEnable = false;

        /** ETCD授权方式：默认ssl，用于HTTPS连接的授权验证 */
        private String authority = "ssl";

        /** 配置键名：ETCD中存储线程池配置的key */
        private String key;

        /** ETCD连接超时时间：单位毫秒（ms），默认30秒 */
        private long timeout = 30000L;
    }

    /**
     * 静态内部类Holder：实现DtpProperties的单例模式
     * 设计：JVM类加载时静态内部类不会立即初始化，调用getInstance时才加载，保证懒加载；
     *      类加载由JVM保证线程安全，无需加锁。
     */
    private static class Holder {
        /** 单例实例：final保证一旦初始化不可修改，确保单例唯一性 */
        private static final DtpProperties INSTANCE = new DtpProperties();
    }
}