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

import com.google.common.collect.Lists;

import org.dromara.dynamictp.common.timer.HashedWheelTimer;
import org.dromara.dynamictp.core.executor.NamedThreadFactory;
import org.dromara.dynamictp.spring.DtpPostProcessor;
import org.dromara.dynamictp.spring.holder.SpringContextHolder;
import org.dromara.dynamictp.spring.util.BeanRegistrationUtil;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.concurrent.TimeUnit;

/**
 * DtpBaseBeanDefinitionRegistrar related
 * 【类注释】DTP（动态线程池）基础Bean定义注册器
 * 作用：通过ImportBeanDefinitionRegistrar接口手动向Spring容器注册核心Bean，
 *      确保DTP功能依赖的关键组件优先被初始化
 * XXX ImportBeanDefinitionRegistrar核心作用是在注解配置解析阶段手动注册 BeanDefinition
 * @author yanhom
 * @since 1.0.4
 **/
public class DtpBaseBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * 【常量】Spring上下文持有器的Bean名称（自定义命名，便于后续引用）
     */
    private static final String APPLICATION_CONTEXT_HOLDER = "dtpApplicationContextHolder";

    /**
     * 【常量】时间轮定时器的Bean名称（HashedWheelTimer是Netty的高效定时任务组件）
     */
    private static final String HASHED_WHEEL_TIMER = "dtpHashedWheelTimer";

    /**
     * 【常量】DTP后置处理器的Bean名称（用于处理DTP线程池的自定义逻辑）
     */
    private static final String DTP_POST_PROCESSOR = "dtpPostProcessor";

    /**
     * 【核心方法】实现ImportBeanDefinitionRegistrar接口的注册方法
     * 作用：在Spring启动时，手动向BeanDefinitionRegistry注册指定的Bean定义
     * @param importingClassMetadata 导入当前Registrar的类的注解元数据（如注解属性等）
     * @param registry Spring的BeanDefinition注册器（用于注册/获取Bean定义）
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 1. 第一步：注册时间轮定时器Bean（DTP线程池超时任务依赖此组件）
        registerHashedWheelTimer(registry);

        // 2. 第二步：注册Spring上下文持有器Bean（用于手动获取Spring容器中的Bean）
        //    registerIfAbsent：确保Bean不存在时才注册，避免重复注册
        BeanRegistrationUtil.registerIfAbsent(registry, APPLICATION_CONTEXT_HOLDER, SpringContextHolder.class);

        // 【注释说明】ApplicationContextHolder和HashedWheelTimer是DtpExecutor执行方法的必需组件，
        // 因此必须优先注册，保证DtpPostProcessor初始化时能依赖这两个Bean
        // 3. 第三步：注册DTP后置处理器Bean
        //    参数说明：
        //    - registry：Bean注册器
        //    - DTP_POST_PROCESSOR：Bean名称
        //    - DtpPostProcessor.class：要注册的Bean类型
        //    - null：构造函数参数（此处无）
        //    - Lists.newArrayList(...)：指定依赖的Bean名称，确保这些Bean先初始化
        BeanRegistrationUtil.registerIfAbsent(registry, DTP_POST_PROCESSOR, DtpPostProcessor.class,
                null, Lists.newArrayList(APPLICATION_CONTEXT_HOLDER, HASHED_WHEEL_TIMER));
    }

    /**
     * 【私有方法】注册HashedWheelTimer（时间轮定时器）Bean
     * 作用：创建自定义配置的HashedWheelTimer Bean，用于处理DTP线程池的超时任务
     * @param registry Spring的BeanDefinition注册器
     */
    private void registerHashedWheelTimer(BeanDefinitionRegistry registry) {
        // 1. 构造HashedWheelTimer的构造函数参数数组
        //    HashedWheelTimer的构造参数说明：
        //    - NamedThreadFactory：线程工厂（指定线程名前缀"dtp-runnable-timeout"，daemon=true表示守护线程）
        //    - 10：时间轮的每一格时间间隔（单位由下一个参数指定）
        //    - TimeUnit.MILLISECONDS：时间单位（毫秒），即每格10ms
        Object[] constructorArgs = new Object[] {
                new NamedThreadFactory("dtp-runnable-timeout", true),
                10,
                TimeUnit.MILLISECONDS
        };

        // 2. 注册HashedWheelTimer Bean（不存在时才注册）
        //    参数：注册器、Bean名称、Bean类型、构造函数参数
        BeanRegistrationUtil.registerIfAbsent(registry, HASHED_WHEEL_TIMER, HashedWheelTimer.class, constructorArgs);
    }
}