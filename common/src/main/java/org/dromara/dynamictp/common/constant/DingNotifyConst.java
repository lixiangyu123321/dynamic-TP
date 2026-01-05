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

/**
 * DingNotifyConst related
 *
 * @author yanhom
 * @since 1.0.0
 **/
/**
 * 钉钉通知常量类（存储动态线程池钉钉告警/变更通知的所有固定常量）
 * 采用 final 修饰类，禁止被继承（常量类无需扩展，避免子类修改或破坏常量语义）
 */
public final class DingNotifyConst {

    /**
     * 私有构造方法，禁止外部实例化
     * 原因：该类是纯常量工具类，所有属性都是 public static final，无需创建实例即可访问
     * 私有化构造可避免外部通过 new DingNotifyConst() 无意义实例化，同时防止类被继承（子类无法调用父类私有构造）
     */
    private DingNotifyConst() { }

    /**
     * 钉钉自定义机器人的消息发送接口地址（POST 请求）
     * 所有钉钉机器人的消息推送都需要调用该接口，后续拼接 access_token 等参数进行请求
     */
    public static final String DING_WEBHOOK = "https://oapi.dingtalk.com/robot/send";

    /**
     * 钉钉接口请求参数：访问令牌参数名
     * 用于拼接在 DING_WEBHOOK 后，格式为 ?access_token=xxx，标识具体的钉钉机器人（每个机器人有唯一 access_token）
     */
    public static final String ACCESS_TOKEN_PARAM = "access_token";

    /**
     * 钉钉接口请求参数：时间戳参数名
     * 用于钉钉机器人的签名验证（防止请求被篡改），需与 sign 参数配合使用，格式为 timestamp=xxx
     */
    public static final String TIMESTAMP_PARAM = "timestamp";

    /**
     * 钉钉接口请求参数：签名参数名
     * 用于钉钉机器人的签名验证，由 timestamp + 机器人密钥加密生成，格式为 sign=xxx
     */
    public static final String SIGN_PARAM = "sign";

    /**
     * 钉钉消息字体颜色：警告色（橙黄色）
     * 用于高亮显示告警相关的核心标题，提升视觉辨识度，方便接收者快速注意到告警信息
     */
    public static final String WARNING_COLOR = "#EA9F00";

    /**
     * 钉钉消息字体颜色：正文内容色（深棕色）
     * 用于显示通知/告警的常规正文内容，字体颜色柔和，保证可读性，同时与警告色形成区分
     */
    public static final String CONTENT_COLOR = "#664B4B";

    /**
     * 钉钉通知的统一标题
     * 所有动态线程池相关的钉钉消息（告警/变更通知）的通用标题，统一消息标识
     */
    public static final String DING_NOTICE_TITLE = "动态线程池通知";

    /**
     * 钉钉告警消息模板（采用钉钉支持的 Markdown 格式）
     * 用于构建动态线程池运行异常时的告警消息，包含完整的线程池运行状态信息
     * 格式说明：
     * 1. <font color=xxx>：钉钉 Markdown 支持的字体颜色设置，部分占位符（如 alarmType）为后续动态替换颜色预留
     * 2. \n\n：钉钉 Markdown 中的换行分隔，保证消息排版整洁，避免内容拥挤
     * 3. %s / %d：字符串/数字类型占位符，后续通过 String.format() 动态填充具体数据
     * 4. 消息内容包含：服务信息、线程池核心参数、运行状态、告警详情、统计信息等，方便运维排查问题
     */
    public static final String DING_ALARM_TEMPLATE =
            "<font color=#EA9F00>【报警】 </font> 动态线程池运行告警 \n\n" +  // 告警标题，橙黄色高亮
                    "<font color=#664B4B size=2>服务名称：%s</font> \n\n " +          // 占位符：微服务名称（如 user-service）
                    "<font color=#664B4B size=2>实例信息：%s</font> \n\n " +          // 占位符：服务实例地址（如 192.168.1.100:8080）
                    "<font color=#664B4B size=2>环境：%s</font> \n\n " +              // 占位符：部署环境（如 prod/test/dev）
                    "<font color=#664B4B size=2>线程池名称：%s</font> \n\n " +        // 占位符：异常线程池的名称
                    "<font color=alarmType size=2>报警项：%s</font> \n\n " +          // 占位符：具体告警项（如 活跃线程数超限/队列满）
                    "<font color=alarmValue size=2>报警阈值 / 当前值：%s</font> \n\n " +  // 占位符：告警阈值与当前实际值（如 80% / 95%）
                    "<font color=corePoolSize size=2>核心线程数：%d</font> \n\n " +    // 占位符：线程池核心线程数（数字类型）
                    "<font color=maximumPoolSize size=2>最大线程数：%d</font> \n\n " +// 占位符：线程池最大线程数（数字类型）
                    "<font color=poolSize size=2>当前线程数：%d</font> \n\n " +        // 占位符：线程池当前已创建线程数（数字类型）
                    "<font color=activeCount size=2>活跃线程数：%d</font> \n\n " +    // 占位符：当前正在执行任务的线程数（数字类型）
                    "<font color=#664B4B size=2>历史最大线程数：%d</font> \n\n " +    // 占位符：线程池运行以来的历史最大线程数（数字类型）
                    "<font color=#664B4B size=2>任务总数：%d</font> \n\n " +          // 占位符：线程池接收的总任务数（数字类型）
                    "<font color=#664B4B size=2>执行完成任务数：%d</font> \n\n " +    // 占位符：已正常执行完成的任务数（数字类型）
                    "<font color=#664B4B size=2>等待执行任务数：%d</font> \n\n " +    // 占位符：队列中等待执行的任务数（数字类型）
                    "<font color=queueType size=2>队列类型：%s</font> \n\n " +        // 占位符：线程池任务队列类型（如 ArrayBlockingQueue）
                    "<font color=queueCapacity size=2>队列容量：%d</font> \n\n " +    // 占位符：任务队列的总容量（数字类型）
                    "<font color=queueSize size=2>队列任务数量：%d</font> \n\n " +    // 占位符：当前队列中的任务数（数字类型）
                    "<font color=queueRemaining size=2>队列剩余容量：%d</font> \n\n " +  // 占位符：队列剩余可用容量（数字类型）
                    "<font color=rejectType size=2>拒绝策略：%s</font> \n\n" +        // 占位符：线程池的任务拒绝策略（如 AbortPolicy）
                    "<font color=rejectCount size=2>总拒绝任务数量：%s</font> \n\n " +// 占位符：线程池运行以来拒绝的总任务数
                    "<font color=runTimeoutCount size=2>总执行超时任务数量：%s</font> \n\n " +// 占位符：任务执行超时的总数量
                    "<font color=queueTimeoutCount size=2>总等待超时任务数量：%s</font> \n\n " +// 占位符：任务队列等待超时的总数量
                    "<font color=#664B4B size=2>上次报警时间：%s</font> \n\n" +       // 占位符：该告警项的上一次告警时间（用于去重/静默）
                    "<font color=#664B4B size=2>报警时间：%s</font> \n\n" +           // 占位符：本次告警的具体时间
                    "<font color=#664B4B size=2>接收人：@%s</font> \n\n" +           // 占位符：告警消息的被@人（如 运维组/张三）
                    "<font color=#22B838 size=2>统计窗口：%ss</font> \n\n" +         // 占位符：数据统计窗口时长（绿色字体，如 60s）
                    "<font color=#22B838 size=2>静默时长：%ss</font> \n\n" +         // 占位符：告警静默时长（绿色字体，避免重复告警，如 300s）
                    "<font color=#664B4B size=2>trace 信息：%s</font> \n\n" +         // 占位符：链路追踪信息（如 traceId，方便排查全链路问题）
                    "<font color=#664B4B size=2>扩展信息：%s</font> \n\n";            // 占位符：预留扩展信息，方便后续补充额外内容

    /**
     * 钉钉参数变更通知模板（采用钉钉支持的 Markdown 格式）
     * 用于构建动态线程池参数修改后的通知消息，记录参数变更前后的对比信息
     * 格式说明：
     * 1. 整体排版与告警模板一致，保证消息风格统一，提升可读性
     * 2. %s 占位符：除常规信息外，重点包含参数「旧值 => 新值」的对比，清晰展示变更内容
     * 3. 绿色标题【通知】：与告警的橙黄色区分，表明是正常变更通知，非异常告警
     */
    public static final String DING_CHANGE_NOTICE_TEMPLATE =
            "<font color=#5AB030>【通知】</font> 动态线程池参数变更 \n\n " +  // 变更通知标题，绿色高亮，与告警区分
                    "<font color=#664B4B size=2>服务名称：%s</font> \n\n " +          // 占位符：微服务名称
                    "<font color=#664B4B size=2>实例信息：%s</font> \n\n " +          // 占位符：服务实例地址
                    "<font color=#664B4B size=2>环境：%s</font> \n\n " +              // 占位符：部署环境
                    "<font color=#664B4B size=2>线程池名称：%s</font> \n\n " +        // 占位符：被修改的线程池名称
                    "<font color=corePoolSize size=2>核心线程数：%s => %s</font> \n\n " +  // 占位符：核心线程数「旧值 => 新值」
                    "<font color=maxPoolSize size=2>最大线程数：%s => %s</font> \n\n " +    // 占位符：最大线程数「旧值 => 新值」
                    "<font color=allowCoreThreadTimeOut size=2>允许核心线程超时：%s => %s</font> \n\n " +// 占位符：核心线程超时配置「旧值 => 新值」
                    "<font color=keepAliveTime size=2>线程存活时间：%ss => %ss</font> \n\n " +// 占位符：线程存活时间「旧值 => 新值」
                    "<font color=#664B4B size=2>队列类型：%s</font> \n\n " +        // 占位符：任务队列类型（通常不轻易变更）
                    "<font color=queueCapacity size=2>队列容量：%s => %s</font> \n\n " +// 占位符：队列容量「旧值 => 新值」
                    "<font color=rejectType size=2>拒绝策略：%s => %s</font> \n\n " +// 占位符：拒绝策略「旧值 => 新值」
                    "<font color=#664B4B size=2>接收人：@%s</font> \n\n" +           // 占位符：通知被@人
                    "<font color=#664B4B size=2>通知时间：%s</font> \n\n";           // 占位符：参数变更的具体时间
}
