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

package org.dromara.dynamictp.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import org.dromara.dynamictp.logging.AbstractDtpLogging;
import org.dromara.dynamictp.logging.LogHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * DtpLogbackLogging related
 * XXX 更新logback日志相关的配置
 * XXX Logger
 * 1. 业务代码中 log.info() 实际调用的就是这个类的方法；
 * 2. 每个 Logger 关联到 LoggerContext，使用上下文的配置规则；
 * 3. 按名称（如 DTP.MONITOR.LOG）区分不同日志器
 * @author yanhom
 * @since 1.0.5
 */
@Slf4j
public class DtpLogbackLogging extends AbstractDtpLogging {

    private static final String LOGBACK_LOCATION = "classpath:dtp-logback.xml";

    private LoggerContext loggerContext;

    @Override
    public void loadConfiguration() {
        try {
            // XXX logback全局配置
            /**
             * 1. Logback 全局上下文（单例 / 独立上下文），管理所有 Logger、Appender、配置；
             * 2. 每个 LoggerContext 对应一套独立的日志配置；
             * 3. 是 Logback 日志系统的 “入口中枢”
             */
            loggerContext = new LoggerContext();
            /**
             * 1. 负责加载 Logback 配置文件（XML/ Groovy）；
             * 2. 将配置解析后注入到 LoggerContext 中；
             * 3. 支持从 URL/File/Classpath 加载配置
             */
            new ContextInitializer(loggerContext).configureByResource(getResourceUrl(LOGBACK_LOCATION));
        } catch (Exception e) {
            log.error("Cannot initialize dtp logback logging.");
        }
    }

    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    /**
     * 获得指定的日志器
     */
    @Override
    public void initMonitorLogger() {
        LogHelper.init(getLoggerContext().getLogger(MONITOR_LOG_NAME));
    }
}
