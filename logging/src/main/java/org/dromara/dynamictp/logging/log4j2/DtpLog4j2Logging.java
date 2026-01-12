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

package org.dromara.dynamictp.logging.log4j2;

import org.dromara.dynamictp.logging.AbstractDtpLogging;
import org.dromara.dynamictp.logging.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.net.URL;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * DtpLog4j2Logging related
 * XXX 更新log4j2相关的日志配置
 * 加载 dynamic-tp 内置的 Log4j2 配置文件，避免框架日志使用业务的日志配置；
 * 为框架日志（如监控日志、告警日志）创建独立的 Logger（前缀 DTP），保证日志隔离；
 * 初始化监控日志的 Logger 实例，供框架内部打印监控数据（如线程池指标）。
 * @author yanhom
 * @since 1.0.5
 */
@Slf4j
public class DtpLog4j2Logging extends AbstractDtpLogging {

    /**
     * XXX Log4j2 配置文件路径（classpath 下的 dtp-log4j2.xml）
     */
    private static final String LOG4J2_LOCATION = "classpath:dtp-log4j2.xml";

    /**
     * 框架日志器名称前缀
     */
    private static final String LOGGER_NAME_PREFIX = "DTP";

    /**
     * XXX 将框架内置的 Log4j2 配置（dtp-log4j2.xml）合并到项目全局的 Log4j2 配置中
     */
    @Override
    public void loadConfiguration() {

        // XXX 1. 管理 Log4j2 全局配置（唯一入口）；
        // XXX 2. 存储所有 Logger/LoggerConfig/Appender；
        // XXX 3. 调用 updateLoggers() 让配置生效

        // XXX LogManager
        // XXX 1. 获取 LoggerContext（getContext(false) 取全局上下文）；
        // XXX 2. 创建 Logger 实例
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);

        // XXX 1. 存储所有 LoggerConfig、Appender、Layout 等配置；
        // XXX 2. 每个 LoggerContext 对应一个 Configuration；
        // XXX 3. 必须调用 start() 激活配置
        Configuration configuration = loadConfiguration(loggerContext, LOG4J2_LOCATION);
        // 加载失败则直接返回，不影响业务日志
        if (configuration == null) {
            return;
        }

        // 步骤3：启动配置（Log4j2 要求 Configuration 必须 start 后才能使用）
        configuration.start();

        // 步骤4：获取配置中的所有 Appender（日志输出目的地，如文件、控制台、ELK）
        Map<String, Appender> appenderMap = configuration.getAppenders();
        // 获取 Log4j2 全局上下文的配置（业务日志的配置）
        Configuration contextConfiguration = loggerContext.getConfiguration();

        // XXX 1. 定义日志输出目的地（文件 / 控制台 / ELK）；
        // XXX 2. 每个 Appender 绑定 Layout（格式化）、Filter（过滤）
        for (Appender appender : appenderMap.values()) {
            contextConfiguration.addAppender(appender);
        }

        // XXX 1. 定义某个 Logger（如 DTP.MONITOR.LOG）的级别（INFO/ERROR）、关联的 Appender；
        // XXX 2. 避免重复定义 Logger，统一管理规则
        Map<String, LoggerConfig> loggers = configuration.getLoggers();
        // 遍历日志器配置，只合并以 DTP 为前缀的日志器（避免覆盖业务日志器）
        loggers.forEach((k, v) -> {
            if (k.startsWith(LOGGER_NAME_PREFIX)) {
                contextConfiguration.addLogger(k, v);
            }
        });

        // 步骤7：更新 Log4j2 全局上下文的日志配置，使合并后的配置生效
        loggerContext.updateLoggers();


    }

    /**
     * 加载对应配置文件为Configuration
     * @param loggerContext 日志log4j2全局配置
     * @param location 配置文件地址
     * @return
     */
    private Configuration loadConfiguration(LoggerContext loggerContext, String location) {
        try {
            // XXX 获得资源URL
            URL url = getResourceUrl(location);
            // XXX 配置源包含配置的来源信息，也可以通过一定方法获得配置信息
            // XXX 这里相当于再加一层，因为对于不同来源的配置文件处理不同
            // XXX classpath 资源：输入流是 url.openStream()，但需要知道资源的 “位置标识”（URL）用于日志 / 异常提示；
            // XXX 远程 URL：输入流可能是网络流，需要处理超时 / 重试；
            // XXX 本地文件：输入流是文件流，还需要知道文件路径用于后续的 “配置自动刷新”。
            // XXX 打包成一个ConfigurationSource，用于统一配置的获取
            ConfigurationSource source = new ConfigurationSource(url.openStream(), url);
            // XXX 配置工厂肯定是获得配置的，拿到配置源，获得配置
            return ConfigurationFactory.getInstance().getConfiguration(loggerContext, source);
        } catch (Exception e) {
            log.error("Cannot initialize dtp log4j2 logging.");
            return null;
        }
    }

    /**
     * 获得指定的日志器
     * XXX 要求配置中必须配置MONITOR_LOG_NAME名称的日志器
     */
    @Override
    public void initMonitorLogger() {
        LogHelper.init(getLogger(MONITOR_LOG_NAME));
    }
}
