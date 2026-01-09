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

package org.dromara.dynamictp.spring.annotation;

import org.apache.commons.lang3.BooleanUtils;
import org.dromara.dynamictp.spring.DtpBaseBeanConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.DTP_ENABLED_PROP;

/**
 * DtpConfigurationSelector related
 * XXX 执行延迟导入，即所有@Import注解配置导入后再导入
 * @author KamTo Hung
 * @since 1.1.1
 */
public class DtpConfigurationSelector implements DeferredImportSelector, Ordered, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    // XXX 延迟导入的配置类
    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        if (!BooleanUtils.toBoolean(environment.getProperty(DTP_ENABLED_PROP, BooleanUtils.TRUE))) {
            return new String[]{};
        }
        // XXX Spring 通过DeferredImportSelector的selectImports()拿到这三个类的全限定名后，会解析这些类的逻辑并执行—— 不同类型的类，“导入” 后的行为完全不同：
        // XXX 如果是@Configuration配置类（如DtpBaseBeanConfiguration）：解析类中的@Bean方法，注册对应的 Bean；
        // XXX 如果是ImportBeanDefinitionRegistrar实现类（如DtpBaseBeanDefinitionRegistrar、DtpBeanDefinitionRegistrar）：执行其registerBeanDefinitions()方法，手动注册 BeanDefinition；

        return new String[] {
                DtpBaseBeanDefinitionRegistrar.class.getName(),
                DtpBeanDefinitionRegistrar.class.getName(),
                DtpBaseBeanConfiguration.class.getName()
        };
    }

    /**
     * 最高优先级
     * @return
     */
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

}
