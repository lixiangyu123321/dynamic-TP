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

package org.dromara.dynamictp.spring.initializer;

import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.support.init.DtpInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_ENV_KEY;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_NAME_KEY;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_PORT_KEY;

/**
 * SpringDtpInitializer related
 * XXX 框架初始化扩展点的实现
 *
 * @author yanhom
 * @since 1.2.0
 */
public class SpringDtpInitializer implements DtpInitializer {

    /**
     * spring相关配置的键名
     */
    private static final String SPRING_APP_NAME_KEY = "spring.application.name";

    /**
     * spring服务的端口
     */
    private static final String SERVER_PORT = "server.port";

    /**
     * spring激活环境
     */
    private static final String ACTIVE_PROFILES = "spring.profiles.active";

    @Override
    public String getName() {
        return "SpringDtpInitializer";
    }

    /**
     * 将配置文件application.yml中的配置信息存到System中
     * @param args args
     */
    @Override
    public void init(Object... args) {
        /**
         * XXX ConfigurableApplicationContext 在 ApplicationContext的基础上进行的一些扩展，支持了对容器生命周期的一些管控
         * refresh()	刷新容器：重新加载所有 Bean 定义、初始化所有 Bean、触发上下文刷新事件	热部署（修改配置后刷新容器）、框架启动时初始化容器
         * close()	关闭容器：销毁所有单例 Bean、释放资源、触发上下文关闭事件	应用优雅停机、测试用例结束后清理容器
         * registerShutdownHook()	注册 JVM 关闭钩子：JVM 退出时自动调用close()，保证容器优雅关闭	应用正常退出时销毁 Bean、释放连接池 / 线程池
         * isActive()	判断容器是否处于活跃状态（已刷新且未关闭）	框架判断容器是否可用的依据
         */
        ConfigurableApplicationContext c = (ConfigurableApplicationContext) args[0];
        String appName = c.getEnvironment().getProperty(SPRING_APP_NAME_KEY, "application");
        String appPort = c.getEnvironment().getProperty(SERVER_PORT, "0");
        String appEnv = c.getEnvironment().getProperty(ACTIVE_PROFILES);
        if (StringUtils.isBlank(appEnv)) {
            // 未配置，选择默认配置
            String[] profiles = c.getEnvironment().getActiveProfiles();
            if (profiles.length < 1) {
                profiles = c.getEnvironment().getDefaultProfiles();
            }
            if (profiles.length >= 1) {
                appEnv = profiles[0];
            }
        }
        if (StringUtils.isBlank(appEnv)) {
            appEnv = "unknown";
        }
        System.setProperty(APP_NAME_KEY, appName);
        System.setProperty(APP_PORT_KEY, appPort);
        System.setProperty(APP_ENV_KEY, appEnv);
    }
}
