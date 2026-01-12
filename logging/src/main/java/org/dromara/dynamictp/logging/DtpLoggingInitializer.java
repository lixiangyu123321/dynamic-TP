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

package org.dromara.dynamictp.logging;

import org.dromara.dynamictp.logging.log4j2.DtpLog4j2Logging;
import org.dromara.dynamictp.logging.logback.DtpLogbackLogging;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * DtpLogging related
 * XXX 全局的日志配置初始化器
 * XXX 主要方法是初始化日志门面实现，以及提供手动加载日志配置的能力
 * @author yanhom
 * @since 1.0.5
 **/
@Slf4j
public class DtpLoggingInitializer {

    /**
     * 框架日志能力的通用功能
     */
    private static AbstractDtpLogging dtpLogging;

    /**
     * XXX 在框架启动时，自动检测项目中引入的日志框架（优先 Logback，其次 Log4j2），并初始化对应的日志实现类
     */
    static  {
        try {
            // XXX 默认logback的日志
            Class.forName("ch.qos.logback.classic.Logger");
            dtpLogging = new DtpLogbackLogging();
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.apache.logging.log4j.LogManager");
                dtpLogging = new DtpLog4j2Logging();
            } catch (ClassNotFoundException classNotFoundException) {
                log.error("DynamicTp initialize logging failed, please check whether logback or log4j related dependencies exist.");
            }
        }
    }

    /**
     * 静态内部类实现单例，而且是一种懒汉式的单例
     */
    private static class LoggingInstance {
        private static final DtpLoggingInitializer INSTANCE = new DtpLoggingInitializer();
    }

    public static DtpLoggingInitializer getInstance() {
        return LoggingInstance.INSTANCE;
    }

    /**
     * XXX 加载配置
     */
    public void loadConfiguration() {
        if (Objects.isNull(dtpLogging)) {
            return;
        }
        dtpLogging.loadConfiguration();
        dtpLogging.initMonitorLogger();
    }
}
