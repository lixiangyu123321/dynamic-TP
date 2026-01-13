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

package org.dromara.dynamictp.starter.common;

import org.dromara.dynamictp.spring.DtpBaseBeanConfiguration;
import org.dromara.dynamictp.starter.common.monitor.DtpEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DtpBootBeanConfiguration related
 * XXX 在满足特定条件的前提下，自动向 Spring 容器中注册 DtpEndpoint 这个动态线程池监控端点 Bean
 * 控制配置类的执行时机：必须在DtpBaseBeanConfiguration配置类之后执行
 * 配置类生效的前提：Spring容器中必须存在DtpBaseBeanConfiguration这个Bean
 * @author dragon-zhang
 * @since 1.1.4
 */
@Configuration
@AutoConfigureAfter({DtpBaseBeanConfiguration.class})
@ConditionalOnBean({DtpBaseBeanConfiguration.class})
public class DtpBootBeanConfiguration {

    /**
     * 声明要注册的Bean：DtpEndpoint（动态线程池监控端点）
     * Bean生效条件1：当前环境支持Actuator端点（即Actuator依赖存在且端点功能可用）
     * Bean生效条件2：Spring容器中还没有DtpEndpoint类型的Bean（避免重复注册）
     * @return
     */
    @Bean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnMissingBean
    public DtpEndpoint dtpEndpoint() {
        // 创建并返回DtpEndpoint实例
        return new DtpEndpoint();
    }

}
