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

package org.dromara.dynamictp.spring;

import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.lifecycle.DtpLifecycle;
import org.dromara.dynamictp.core.lifecycle.LifeCycleManagement;
import org.dromara.dynamictp.core.monitor.DtpMonitor;
import org.dromara.dynamictp.core.support.DtpBannerPrinter;
import org.dromara.dynamictp.spring.lifecycle.DtpLifecycleSpringAdapter;
import org.dromara.dynamictp.spring.listener.DtpApplicationListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * DtpBaseBeanConfiguration related
 * XXX 基于Spring配置一些配置类
 * XXX 这里没有配置DtpInitializerExecutor，所以初始化器需要手动配置，像DtpApplicationContextInitializer
 * @author yanhom
 * @since 1.0.0
 **/
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DtpBaseBeanConfiguration {

    /**
     * XXX 全局配置类
     * @return 全局配置类
     */
    @Bean
    public DtpProperties dtpProperties() {
        return DtpProperties.getInstance();
    }

    /**
     * XXX 全局生命周期管理，统一管理所有线程池的生命周期
     * @return 全局生命周期管理器
     */
    @Bean
    public DtpLifecycle dtpLifecycle() {
        return new DtpLifecycle();
    }

    /**
     * XXX 用于管理全局的动态线程池
     * @param dtpProperties 注入全局配置
     * @return 动态线程池的管理器
     */
    @Bean
    public DtpRegistry dtpRegistry(DtpProperties dtpProperties) {
        return new DtpRegistry(dtpProperties);
    }

    /**
     * XXX 指标监控
     * @param dtpProperties 注入全局配置
     * @return 指标监控器
     */
    @Bean
    public DtpMonitor dtpMonitor(DtpProperties dtpProperties) {
        return new DtpMonitor(dtpProperties);
    }

    /**
     * 打印dynamic-tp的banner
     * @return 打印器
     */
    @Bean
    public DtpBannerPrinter dtpBannerPrinter() {
        return new DtpBannerPrinter();
    }

    /**
     * XXX Spring相关的生命周期管理器
     * XXX 将已有的DTP的生命周期管理适配Spring的生命周期管理
     * @param lifeCycleManagement 相关的生命周期管理器，XXX 委托给该参数进行生命周期管理
     * @return
     */
    @Bean
    public DtpLifecycleSpringAdapter dtpLifecycleSpringAdapter(LifeCycleManagement lifeCycleManagement) {
        return new DtpLifecycleSpringAdapter(lifeCycleManagement);
    }

    /**
     * XXX 基于Spring事件监听的事件监听器
     * XXX 底层的事件处理是基于EventBusManager来的
     * @return 事件监听器
     */
    @Bean
    public DtpApplicationListener dtpApplicationListener() {
        return new DtpApplicationListener();
    }
}
