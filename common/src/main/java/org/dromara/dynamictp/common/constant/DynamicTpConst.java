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

package org.dromara.dynamictp.common.constant;

import com.google.common.collect.ImmutableList;
import org.dromara.dynamictp.common.em.NotifyItemEnum;

import java.util.List;

/**
 * DynamicTpConst related
 *
 * @author yanhom
 * @since 1.0.0
 **/
public final class DynamicTpConst {

    /**
     * 私有构造方法
     * 目的：禁止外部通过 new DynamicTpConst() 实例化该类（常量类无需实例化，仅需访问静态常量）
     */
    private DynamicTpConst() { }

    /**
     * 动态线程池核心配置前缀
     * 对应配置文件（application.yml/application.properties）中的根节点，例如：dynamictp.enabled=true
     */
    public static final String MAIN_PROPERTIES_PREFIX = "dynamictp";

    /**
     * 动态线程池总开关配置项
     * 配置格式：dynamictp.enabled=true/false
     * 作用：控制整个动态线程池功能是否启用
     */
    public static final String DTP_ENABLED_PROP = MAIN_PROPERTIES_PREFIX + ".enabled";

    /**
     * 动态线程池启动横幅（Banner）开关配置项
     * 配置格式：dynamictp.enabledBanner=true/false
     * 作用：控制项目启动时，是否在控制台打印 DynamicTP 的专属启动横幅
     */
    public static final String BANNER_ENABLED_PROP = MAIN_PROPERTIES_PREFIX + ".enabledBanner";

    /**
     * 当前服务器的可用处理器核心数
     * 来源：通过 Runtime 类获取 JVM 可用的 CPU 核心数，常用于线程池核心线程数、最大线程数的默认值计算
     */
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    /**
     * 配置变更日志展示格式模板
     * 作用：当动态线程池配置发生变更时，用于格式化打印「旧值 => 新值」的日志，提高可读性
     * 示例：日志输出 "corePoolSize => 10 => 20"（核心线程数从 10 变更为 20）
     */
    public static final String PROPERTIES_CHANGE_SHOW_STYLE = "%s => %s";

    /**
     * 未知值占位符
     * 作用：当某些配置项、参数值无法获取或为空时，用于填充默认占位，避免 null 导致的日志混乱或逻辑异常
     */
    public static final String UNKNOWN = "---";

    /**
     * 链路追踪ID字段名
     * 作用：在动态线程池的任务包装器中，用于传递分布式链路追踪的 traceId，保证链路追踪的完整性
     */
    public static final String TRACE_ID = "traceId";

    /**
     * 动态线程池全局配置前缀
     * 配置格式：dynamictp.globalExecutorProps.xxx=xxx
     * 作用：配置所有动态线程池的公共全局属性（如默认拒绝策略、默认超时时间等），所有线程池都会继承该配置
     */
    public static final String GLOBAL_CONFIG_PREFIX = MAIN_PROPERTIES_PREFIX + ".globalExecutorProps.";

    /**
     * 动态线程池列表配置前缀（左部分）
     * 配置格式：dynamictp.executors[0].threadPoolName=myDtpExecutor
     * 作用：配置多个具体的动态线程池实例，[] 用于指定数组下标，对应多个线程池配置
     */
    public static final String EXECUTORS_CONFIG_PREFIX = MAIN_PROPERTIES_PREFIX + ".executors[";

    /**
     * 应用名称配置键
     * 作用：用于获取当前应用的名称（通常从环境变量或配置文件中读取），用于动态线程池的监控、告警标识
     * TODO 修改
     */
    public static final String APP_NAME_KEY = "APP.NAME";

    /**
     * 应用端口配置键
     * 作用：用于获取当前应用的端口号，用于动态线程池的监控、告警标识，方便定位具体实例
     * TODO 修改
     */
    public static final String APP_PORT_KEY = "APP.PORT";

    /**
     * 应用环境配置键
     * 作用：用于获取当前应用的运行环境（如 dev/test/prod），用于动态线程池的环境隔离，避免跨环境配置污染
     */
    public static final String APP_ENV_KEY = "APP.ENV";

    /**
     * -------------- 以下为 动态线程池单个线程池实例的配置字段常量 --------------
     */

    /**
     * 线程池名称配置字段
     * 作用：唯一标识一个动态线程池实例，是线程池的核心标识，用于配置读取、监控、动态更新的核心关键字
     */
    public static final String THREAD_POOL_NAME = "threadPoolName";

    /**
     * 线程池别名配置字段
     * 作用：线程池的友好名称，用于监控面板、告警信息中展示，提高可读性（可与线程池名称一致，也可自定义）
     */
    public static final String THREAD_POOL_ALIAS_NAME = "threadPoolAliasName";

    /**
     * 是否允许核心线程超时配置字段
     * 作用：控制核心线程是否允许超时销毁（默认 false，核心线程一直存活；true 时，核心线程空闲超时后会被销毁）
     * 对应 ThreadPoolExecutor 的 allowCoreThreadTimeOut 属性
     */
    public static final String ALLOW_CORE_THREAD_TIMEOUT = "allowCoreThreadTimeOut";

    /**
     * 告警项配置字段
     * 作用：配置当前线程池需要开启的告警类型（如活跃度告警、队列容量告警、拒绝策略告警等）
     * 对应 NotifyItemEnum 枚举中的各项值
     */
    public static final String NOTIFY_ITEMS = "notifyItems";

    /**
     * 告警平台ID配置字段
     * 作用：配置当前线程池的告警信息需要推送至哪些告警平台（如钉钉、企业微信、飞书等），多个平台用逗号分隔
     */
    public static final String PLATFORM_IDS = "platformIds";

    /**
     * 告警功能总开关配置字段
     * 作用：控制当前线程池是否启用告警功能（true 启用，false 关闭所有告警）
     */
    public static final String NOTIFY_ENABLED = "notifyEnabled";

    /**
     * 线程池关闭时是否等待任务完成配置字段
     * 作用：线程池关闭（shutdown）时，是否等待队列中所有待执行任务完成后再关闭
     * 对应 ThreadPoolExecutor 的 waitForTasksToCompleteOnShutdown 属性
     */
    public static final String WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN = "waitForTasksToCompleteOnShutdown";

    /**
     * 线程池关闭时的等待超时时间配置字段
     * 作用：配合 waitForTasksToCompleteOnShutdown 使用，指定线程池关闭时的最大等待时间（单位：秒）
     * 超时后，无论任务是否完成，线程池都会强制关闭
     */
    public static final String AWAIT_TERMINATION_SECONDS = "awaitTerminationSeconds";

    /**
     * 是否预启动所有核心线程配置字段
     * 作用：线程池初始化时，是否提前创建并启动所有核心线程（默认 false，核心线程在任务到达时才创建）
     * 对应 ThreadPoolExecutor 的 prestartAllCoreThreads() 方法
     */
    public static final String PRE_START_ALL_CORE_THREADS = "preStartAllCoreThreads";

    /**
     * 拒绝策略增强开关配置字段
     * 作用：控制是否启用 DynamicTP 自定义的拒绝策略增强功能（如拒绝时打印任务详情、推送告警等）
     */
    public static final String REJECT_ENHANCED = "rejectEnhanced";

    /**
     * 拒绝策略类型配置字段
     * 作用：配置当前线程池的拒绝策略类型（如 AbortPolicy、CallerRunsPolicy、自定义拒绝策略等）
     */
    public static final String REJECT_HANDLER_TYPE = "rejectHandlerType";

    /**
     * 任务运行超时时间配置字段
     * 作用：配置线程池任务的最大运行超时时间，超过该时间未完成的任务，会触发超时告警或中断处理
     */
    public static final String RUN_TIMEOUT = "runTimeout";

    /**
     * 任务运行超时时是否尝试中断配置字段
     * 作用：当任务运行超时后，是否尝试调用 Thread.interrupt() 中断任务执行（true 尝试中断，false 仅告警不中断）
     */
    public static final String TRY_INTERRUPT_WHEN_TIMEOUT = "tryInterrupt";

    /**
     * 任务队列等待超时时间配置字段
     * 作用：配置任务在队列中等待执行的最大超时时间，超过该时间仍未被执行的任务，会触发队列超时告警
     */
    public static final String QUEUE_TIMEOUT = "queueTimeout";

    /**
     * 任务包装器配置字段
     * 作用：配置当前线程池需要启用的任务包装器（如链路追踪包装器、日志包装器、超时控制包装器等）
     */
    public static final String TASK_WRAPPERS = "taskWrappers";

    /**
     * 插件名称配置字段
     * 作用：配置当前线程池需要启用的 DynamicTP 插件（如监控插件、告警插件、动态配置插件等）
     */
    public static final String PLUGIN_NAMES = "pluginNames";

    /**
     * 感知器名称配置字段
     * 作用：配置当前线程池需要启用的感知器（如线程池状态感知器、配置变更感知器等），用于感知线程池的运行状态变化
     */
    public static final String AWARE_NAMES = "awareNames";

    /**
     * -------------- 以下为 通用符号常量 --------------
     */

    /**
     * 点号（.）符号常量
     * 作用：用于拼接配置前缀和配置字段（如 dynamictp.enabled），避免硬编码字符串拼接
     */
    public static final String DOT = ".";

    /**
     * 数组左括号（[）符号常量
     * 作用：用于拼接数组类型的配置前缀（如 dynamictp.executors[0]），对应配置文件中的数组下标
     */
    public static final String ARR_LEFT_BRACKET = "[";

    /**
     * 数组右括号（]）符号常量
     * 作用：用于闭合数组类型的配置（如 dynamictp.executors[0]），完成数组下标的标识
     */
    public static final String ARR_RIGHT_BRACKET = "]";

    /**
     * 定时任务默认告警项列表
     * 作用：指定定时任务类型的线程池默认启用的告警项（活跃度告警、容量告警）
     * 说明：使用 ImmutableList 构建不可变列表，避免外部修改列表内容，保证常量的安全性
     */
    public static final List<NotifyItemEnum> SCHEDULE_NOTIFY_ITEMS = ImmutableList.of(NotifyItemEnum.LIVENESS,
            NotifyItemEnum.CAPACITY);

    /**
     * -------------- 以下为 单位相关常量 --------------
     */

    /**
     * 1MB 对应的字节数（1024*1024）
     * 作用：用于计算内存相关的配置（如队列内存阈值、任务内存大小等），避免硬编码数字
     */
    public static final Integer M_1 = 1024 * 1024;

    /**
     * -------------- 以下为 操作系统相关常量 --------------
     */

    /**
     * 操作系统名称环境变量键
     * 作用：用于从系统环境变量中获取操作系统名称（System.getProperty("os.name")）
     */
    public static final String OS_NAME_KEY = "os.name";

    /**
     * Linux 操作系统名称前缀
     * 作用：判断当前操作系统是否为 Linux 系统（通过 os.name 包含 linux 关键字判断，忽略大小写）
     */
    public static final String OS_LINUX_PREFIX = "linux";

    /**
     * Windows 操作系统名称前缀
     * 作用：判断当前操作系统是否为 Windows 系统（通过 os.name 包含 win 关键字判断，忽略大小写）
     */
    public static final String OS_WIN_PREFIX = "win";

    /**
     * -------------- 以下为 功能开关相关常量 --------------
     */

    /**
     * 动态线程池任务执行增强开关
     * 配置格式：dtp.execute.enhanced=true/false
     * 作用：控制是否启用 DynamicTP 对任务执行过程的增强功能（如超时控制、异常捕获、日志增强等）
     */
    public static final String DTP_EXECUTE_ENHANCED = "dtp.execute.enhanced";

    /**
     * 字符串类型的 true 值
     * 作用：用于配置项的布尔值判断（当配置项值为 "true" 时，视为启用对应功能），避免硬编码字符串
     */
    public static final String TRUE_STR = "true";

    /**
     * 字符串类型的 false 值
     * 作用：用于配置项的布尔值判断（当配置项值为 "false" 时，视为关闭对应功能），避免硬编码字符串
     */
    public static final String FALSE_STR = "false";
}
