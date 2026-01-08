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

package org.dromara.dynamictp.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.entity.ServiceInstance;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.common.properties.DtpProperties;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_ENV_KEY;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_NAME_KEY;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.APP_PORT_KEY;

/**
 * 通用工具类（CommonUtil）
 * 核心功能：获取当前服务实例的关键信息（IP、端口、应用名、环境），并封装为ServiceInstance对象
 * XXX 说白了就是为了获得IP + port + appName + env
 * @author yanhom
 */
@Slf4j
public final class CommonUtil {  // final修饰类，禁止被继承（工具类最佳实践）

    private CommonUtil() {
    }

    /**
     * 静态常量：存储当前服务实例的核心信息（IP、端口、应用名、环境），全局唯一
     */
    private static final ServiceInstance SERVICE_INSTANCE;

    static {
        String address = null;
        try {
            // 获取本机精确的内网IP地址（非回环地址、非点对点地址）
            address = getLocalHostExactAddress().getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            // 捕获IP获取失败的异常，打印错误日志（不中断程序，address保持null）
            log.error("get localhost address error.", e);
        }

        // 步骤1：获取当前服务运行环境（如dev/test/prod）
        String env = DtpProperties.getInstance().getEnv();
        // 如果配置中没有，从上下文环境变量中获取
        if (StringUtils.isBlank(env)) {
            env = ContextManagerHelper.getEnvironmentProperty(APP_ENV_KEY);
        }

        // 步骤2：获取应用名称（从上下文环境变量中获取，如spring.application.name）
        String appName = ContextManagerHelper.getEnvironmentProperty(APP_NAME_KEY);

        // 步骤3：获取应用端口（从上下文环境变量中获取，如server.port）
        String portStr = ContextManagerHelper.getEnvironmentProperty(APP_PORT_KEY);
        // 端口转换：有值则转为int，无值则默认0（0表示端口未配置/未知）
        int port = StringUtils.isNotBlank(portStr) ? Integer.parseInt(portStr) : 0;

        // 初始化服务实例对象（封装IP、端口、应用名、环境）
        SERVICE_INSTANCE = new ServiceInstance(address, port, appName, env);
    }

    /**
     * 对外提供的静态方法：获取当前服务实例的信息（单例）
     * @return 封装好的ServiceInstance对象
     */
    public static ServiceInstance getInstance() {
        return SERVICE_INSTANCE;
    }

    /**
     * 私有工具方法：获取本机精确的内网IP地址（核心逻辑）
     * 优先级：非回环地址 > 非点对点的本地站点地址 > 候选地址 > 默认本地地址
     * @return 本机有效内网IP
     * @throws SocketException 网络接口获取失败异常
     * @throws UnknownHostException 主机地址解析失败异常
     */
    private static InetAddress getLocalHostExactAddress() throws SocketException, UnknownHostException {
        InetAddress candidateAddress = null;

        // 遍历本机所有网络接口（如eth0、lo、wlan0等）
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            // 遍历当前网络接口下的所有IP地址
            for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                InetAddress inetAddress = inetAddresses.nextElement();

                // 过滤条件1：不是回环地址（排除127.0.0.1） + 是本地站点地址（内网IP，如192.168/10./172.16段）
                if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                    // 过滤条件2：不是点对点地址（如VPN的ppp接口），直接返回该IP（最优解）
                    if (!networkInterface.isPointToPoint()) {
                        return inetAddress;
                    } else {
                        // 是点对点地址，暂存为候选（后续无更优IP时使用）
                        candidateAddress = inetAddress;
                    }
                }
            }
        }

        // 所有网络接口遍历完毕：有候选地址则返回，无则返回默认本地地址（可能是127.0.0.1）
        return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
    }
}