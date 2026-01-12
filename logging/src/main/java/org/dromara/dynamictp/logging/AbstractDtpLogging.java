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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.common.properties.DtpProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * AbstractDtpLogging related
 * XXX 加载配置以及配置URL，初始化日志器
 * XXX 为框架的日志功能提供「通用基础能力」（日志路径初始化、资源路径解析），并定义「日志配置加载、监控日志初始化」的标准化接口
 * @author yanhom
 * @since 1.0.5
 */
@Slf4j
public abstract class AbstractDtpLogging {

    protected static final String MONITOR_LOG_NAME = "DTP.MONITOR.LOG";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String LOGGING_PATH = "LOG.PATH";

    /**
     * 将日志地址存入System中
     */
    static {
        try {
            DtpProperties dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
            String logPath = dtpProperties.getLogPath();
            if (StringUtils.isBlank(logPath)) {
                String userHome = System.getProperty("user.home");
                System.setProperty(LOGGING_PATH, userHome + File.separator + "logs");
            } else {
                System.setProperty(LOGGING_PATH, logPath);
            }
        } catch (Exception e) {
            log.error("DynamicTp logging env init failed, if collectType is not logging, this error can be ignored.", e);
        }
    }

    /**
     * 通用资源路径解析方法：将不同格式的资源路径解析为URL对象
     * @param resource 资源路径字符串，支持三种格式：
     *                 1. classpath:前缀（如classpath:dtp-log4j2.xml）
     *                 2. 标准URL（如http://xxx/config.xml、file:///usr/local/config.xml）
     *                 3. 本地文件路径（如/usr/local/config.xml、D:/config.xml）
     * @return 解析后的URL对象，可用于读取资源内容
     * @throws IOException 资源不存在或解析失败时抛出（如classpath资源找不到、文件路径无效）
     */
    public URL getResourceUrl(String resource) throws IOException {
        // 第一步：处理classpath前缀的资源（项目内置资源，优先级最高）
        if (resource.startsWith(CLASSPATH_PREFIX)) {
            // 1. 截取classpath:前缀后的真实路径（比如classpath:dtp.xml → dtp.xml）
            String path = resource.substring(CLASSPATH_PREFIX.length());

            // 2. 获取当前类的类加载器（优先用当前类加载器，兼容不同环境的类加载机制）
            ClassLoader classLoader = DtpLoggingInitializer.class.getClassLoader();

            // 3. 通过类加载器加载classpath下的资源：
            //    - 若当前类加载器非空，用当前类加载器（推荐，适配web容器/模块化环境）
            //    - 若为空，降级用系统类加载器（兜底方案，适配简单环境）
            URL url = (classLoader != null ? classLoader.getResource(path) : ClassLoader.getSystemResource(path));

            // 4. 校验资源是否存在：classpath资源不存在则抛明确的文件未找到异常
            if (url == null) {
                throw new FileNotFoundException("Cannot find file: +" + resource);
            }

            // 5. 返回classpath资源的URL（如jar:file:/xxx/xxx.jar!/dtp.xml）
            return url;
        }

        // 第二步：处理非classpath前缀的资源（URL/本地文件）
        try {
            // 1. 尝试直接解析为标准URL（适配http/https/file等协议的URL字符串）
            return new URL(resource);
        } catch (MalformedURLException ex) {
            // 2. 若不是合法URL（比如是本地文件路径），则按本地文件处理：
            //    - 先转为File对象，再通过URI转为URL（兼容不同操作系统的文件路径格式）
            //    - 比如D:/config.xml → file:/D:/config.xml；/usr/local/config.xml → file:/usr/local/config.xml
            return new File(resource).toURI().toURL();
        }
    }

    /**
     * Load configuration.
     */
    public abstract void loadConfiguration();

    /**
     * Init monitor logger.
     */
    public abstract void initMonitorLogger();
}
