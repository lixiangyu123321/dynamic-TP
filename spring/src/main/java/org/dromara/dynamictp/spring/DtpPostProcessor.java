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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.plugin.DtpInterceptorRegistry;
import org.dromara.dynamictp.common.util.ConstructorUtil;
import org.dromara.dynamictp.common.util.ReflectionUtil;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.executor.eager.EagerDtpExecutor;
import org.dromara.dynamictp.core.executor.eager.TaskQueue;
import org.dromara.dynamictp.core.support.DynamicTp;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.proxy.ScheduledThreadPoolExecutorProxy;
import org.dromara.dynamictp.core.support.proxy.ThreadPoolExecutorProxy;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrappers;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.dromara.dynamictp.core.support.DtpLifecycleSupport.shutdownGracefulAsync;

/**
 * BeanPostProcessor that handles all related beans managed by Spring.
 * XXX
 *
 * XXX BeanPostProcessor专门用于增强Bean
 * XXX BeanFactoryAware 用于获得BeanFactory
 * XXX PriorityOrdered 优先级排序接口，表示本处理器优先执行
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
@SuppressWarnings("all")
public class DtpPostProcessor implements BeanPostProcessor, BeanFactoryAware, PriorityOrdered {

    /**
     * 注册源
     */
    private static final String REGISTER_SOURCE = "beanPostProcessor";

    /**
     * XXX 注入BeanFactory专门用于获得Bean
     */
    private DefaultListableBeanFactory beanFactory;

    /**
     * Compatible with lower versions of Spring.
     * XXX 初始化之前不做处理
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return the bean instance to use
     * @throws BeansException in case of errors
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 专门增强线程池
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (!(bean instanceof ThreadPoolExecutor) && !(bean instanceof ThreadPoolTaskExecutor)) {
            return bean;
        }
        if (bean instanceof DtpExecutor) {
            return registerAndReturnDtp(bean);
        }
        // register juc ThreadPoolExecutor or ThreadPoolTaskExecutor
        return registerAndReturnCommon(bean, beanName);
    }

    private Object registerAndReturnDtp(Object bean) {
        DtpExecutor dtpExecutor = (DtpExecutor) bean;
        // XXX 获得线程池的参数对象
        Object[] args = ConstructorUtil.buildTpExecutorConstructorArgs(dtpExecutor);
        // XXX 获得线程池的参数类型
        Class<?>[] argTypes = ConstructorUtil.buildTpExecutorConstructorArgTypes();

        // XXX 这里看似是叫PluginNames，其实是Interceptor，其实是增强线程池功能的，或者说对线程池的方法调用进行转发实现增强的
        Set<String> pluginNames = dtpExecutor.getPluginNames();

        // XXX 基于获得的增强名
        val enhancedBean = (DtpExecutor) DtpInterceptorRegistry.plugin(bean, pluginNames, argTypes, args);
        if (enhancedBean instanceof EagerDtpExecutor) {
            // XXX 指向代理对象
            ((TaskQueue) enhancedBean.getQueue()).setExecutor((EagerDtpExecutor) enhancedBean);
        }
        // XXX 注册到全局线程池中
        DtpRegistry.registerExecutor(ExecutorWrapper.of(enhancedBean), REGISTER_SOURCE);
        return enhancedBean;
    }

    /**
     * 核心原因：Spring 对 @Bean 方法的元数据存储逻辑
     * 当你在 @Configuration 类中定义 @Bean 方法时，Spring 并不会直接把方法返回的对象作为 “Bean 定义”，而是会：
     * 解析 @Bean 方法：Spring 扫描到 @Bean 方法时，会为这个方法创建一个 BeanDefinition；
     * 绑定方法元数据：为了记住 “这个 Bean 是由哪个方法创建的”，以及 “方法上有哪些注解”，Spring 会把该方法的元数据（MethodMetadata）绑定到 BeanDefinition 的 source 属性上；
     * 特殊的 BeanDefinition 类型：对于注解驱动的 @Bean 方法，Spring 会使用 AnnotatedBeanDefinition 的实现类（如 ConfigurationClassBeanDefinition），专门存储这类带注解的方法元数据。
     * XXX 整半天就是为了获得一个线程池的名称哪呐，基于类和方法上的注解
     * 注册普通线程池（非DtpExecutor类型）并返回处理后的bean
     * @param bean 待处理的线程池bean实例（ThreadPoolExecutor/ThreadPoolTaskExecutor）
     * @param beanName bean在Spring容器中的名称
     * @return 处理后的bean（原bean或代理bean）
     */
    private Object registerAndReturnCommon(Object bean, String beanName) {
        // 存储@DynamicTp注解的value值（即自定义的线程池名称）
        String dtpAnnoValue;
        try {
            // 第一步：尝试从bean级别获取@DynamicTp注解（比如类上的注解）
            DynamicTp dynamicTp = beanFactory.findAnnotationOnBean(beanName, DynamicTp.class);
            if (Objects.nonNull(dynamicTp)) {
                // 如果找到bean级别注解，直接获取注解的value值
                dtpAnnoValue = dynamicTp.value();
            } else {
                // 第二步：bean级别没找到注解，尝试从@Bean方法级别获取
                // 获取当前bean的BeanDefinition（bean的元数据定义）
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                // 判断该BeanDefinition是否包含注解元数据（非注解驱动的bean直接返回原bean）
                if (!(beanDefinition instanceof AnnotatedBeanDefinition)) {
                    return bean;
                }
                // 强转为带注解元数据的BeanDefinition
                AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
                // 获取创建该bean的@Bean方法的元数据（MethodMetadata封装了方法的注解信息）
                MethodMetadata methodMetadata = (MethodMetadata) annotatedBeanDefinition.getSource();
                // 如果方法元数据为空，或方法上没有标注@DynamicTp注解，直接返回原bean
                if (Objects.isNull(methodMetadata) || !methodMetadata.isAnnotated(DynamicTp.class.getName())) {
                    return bean;
                }
                // 解析@DynamicTp注解的属性：获取注解的value值
                dtpAnnoValue = Optional.ofNullable(methodMetadata.getAnnotationAttributes(DynamicTp.class.getName()))
                        // 如果注解属性为空，返回空Map
                        .orElse(Collections.emptyMap())
                        // 从注解属性中获取value值，默认空字符串
                        .getOrDefault("value", "")
                        // 转为字符串
                        .toString();
            }
        } catch (NoSuchBeanDefinitionException e) {
            // 捕获找不到bean定义的异常，打印警告日志后返回原bean
            log.warn("There is no bean with the given name {}", beanName, e);
            return bean;
        }
        // 确定最终的线程池名称：优先使用@DynamicTp的value值，为空则使用beanName
        String poolName = StringUtils.isNotBlank(dtpAnnoValue) ? dtpAnnoValue : beanName;
        // 执行真正的注册逻辑，并返回处理后的bean
        return doRegisterAndReturnCommon(bean, poolName);
    }

    /**
     * 创建相关代理
     * TODO 关于代理哪里还没看
     * @param bean
     * @param poolName
     * @return
     */
    private Object doRegisterAndReturnCommon(Object bean, String poolName) {
        if (bean instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor poolTaskExecutor = (ThreadPoolTaskExecutor) bean;
            val proxy = newProxy(poolName, poolTaskExecutor.getThreadPoolExecutor());
            try {
                ReflectionUtil.setFieldValue("threadPoolExecutor", bean, proxy);
                tryWrapTaskDecorator(poolName, poolTaskExecutor, proxy);
            } catch (IllegalAccessException ignored) { }
            DtpRegistry.registerExecutor(new ExecutorWrapper(poolName, proxy), REGISTER_SOURCE);
            return bean;
        }
        Executor proxy;
        if (bean instanceof ScheduledThreadPoolExecutor) {
            proxy = newScheduledTpProxy(poolName, (ScheduledThreadPoolExecutor) bean);
        } else {
            proxy = newProxy(poolName, (ThreadPoolExecutor) bean);
        }
        DtpRegistry.registerExecutor(new ExecutorWrapper(poolName, proxy), REGISTER_SOURCE);
        return proxy;
    }

    /**
     * BeanFactoryAware接口
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    /**
     * 最高优先级
     * @return
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ThreadPoolExecutorProxy newProxy(String name, ThreadPoolExecutor originExecutor) {
        val proxy = new ThreadPoolExecutorProxy(originExecutor);
        shutdownGracefulAsync(originExecutor, name, 0);
        return proxy;
    }

    private ScheduledThreadPoolExecutorProxy newScheduledTpProxy(String name, ScheduledThreadPoolExecutor originExecutor) {
        val proxy = new ScheduledThreadPoolExecutorProxy(originExecutor);
        shutdownGracefulAsync(originExecutor, name, 0);
        return proxy;
    }

    private void tryWrapTaskDecorator(String poolName, ThreadPoolTaskExecutor poolTaskExecutor, ThreadPoolExecutorProxy proxy) throws IllegalAccessException {
        Object taskDecorator = ReflectionUtil.getFieldValue("taskDecorator", poolTaskExecutor);
        if (Objects.isNull(taskDecorator)) {
            return;
        }
        TaskWrapper taskWrapper = (taskDecorator instanceof TaskWrapper) ? (TaskWrapper) taskDecorator : new TaskWrapper() {
            @Override
            public String name() {
                return poolName + "#taskDecorator";
            }

            @Override
            public Runnable wrap(Runnable runnable) {
                return ((TaskDecorator) taskDecorator).decorate(runnable);
            }
        };
        ReflectionUtil.setFieldValue("taskWrappers", proxy, Lists.newArrayList(taskWrapper));
        TaskWrappers.getInstance().register(taskWrapper);
    }
}
