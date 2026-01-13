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

package org.dromara.dynamictp.starter.common.binder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.common.util.DtpPropertiesBinderUtil;
import org.dromara.dynamictp.core.support.binder.PropertiesBinder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.MAIN_PROPERTIES_PREFIX;

/**
 * SpringBootPropertiesBinder related
 * XXX 基于SpringBoot实现属性绑定的功能
 * @author yanhom
 * @since 1.0.3
 **/
@Slf4j
public class SpringBootPropertiesBinder implements PropertiesBinder {

    @Override
    public void bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties) {
        // XXX 钩子方法依旧没有实现
        // XXX 钩子方法依旧为空
        beforeBind(properties, dtpProperties);
        try {
            // XXX 判断版本
            Class.forName("org.springframework.boot.context.properties.bind.Binder");
            doBindIn2X(properties, dtpProperties);
        } catch (ClassNotFoundException e) {
            doBindIn1X(properties, dtpProperties);
        }
        afterBind(properties, dtpProperties);
    }

    @Override
    public void bindDtpProperties(Object environment, DtpProperties dtpProperties) {
        if (!(environment instanceof Environment)) {
            throw new IllegalArgumentException("Invalid environment type, expected org.springframework.core.env.Environment");
        }
        Environment env = (Environment) environment;
        beforeBind(env, dtpProperties);
        try {
            Class.forName("org.springframework.boot.context.properties.bind.Binder");
            doBindIn2X(env, dtpProperties);
        } catch (ClassNotFoundException e) {
            doBindIn1X(env, dtpProperties);
        }
        afterBind(environment, dtpProperties);
    }

    @Override
    public void afterBind(Object source, DtpProperties dtpProperties) {
        DtpPropertiesBinderUtil.tryResetWithGlobalConfig(source, dtpProperties);
    }

    /**
     * 基于SpringBoot提供属性绑定机制实现属性绑定
     * @param properties Map
     * @param dtpProperties 实体类
     */
    private void doBindIn2X(Map<?, Object> properties, DtpProperties dtpProperties) {
        // 1. 将原始Map包装成Spring能识别的配置属性源
        ConfigurationPropertySource sources = new MapConfigurationPropertySource(properties);
        // 2. 创建配置绑定器（核心工具类），用于后续属性绑定
        // XXX Binder 是 Spring Core 中负责属性绑定的核心类，相当于 “属性赋值工具”，
        // XXX 它能从 ConfigurationPropertySource 中读取配置，并映射到 Java 对象的字段上。
        Binder binder = new Binder(sources);
        // 3. 获取DtpProperties类的类型元信息（包含泛型、字段等）
        // XXX ResolvableType 是 Spring 提供的类型解析工具，能处理泛型、继承等复杂类型场景，这里用来明确绑定的目标类型是 DtpProperties。
        ResolvableType type = ResolvableType.forClass(DtpProperties.class);
        // 4. 包装待绑定的目标对象：指定绑定类型为DtpProperties，且使用已存在的dtpProperties实例（而非新建）
        Bindable<?> target = Bindable.of(type).withExistingValue(dtpProperties);
        // 5. 核心操作：将配置源中以MAIN_PROPERTIES_PREFIX为前缀的属性，绑定到dtpProperties对象上
        binder.bind(MAIN_PROPERTIES_PREFIX, target);
    }

    private void doBindIn2X(Environment environment, DtpProperties dtpProperties) {
        // XXX Binder.get(environment) 是 Spring 提供的 “快捷工厂方法”
        // XXX Spring与environment是一家，所以提供这样的快捷方式
        Binder binder = Binder.get(environment);
        ResolvableType type = ResolvableType.forClass(DtpProperties.class);
        Bindable<?> target = Bindable.of(type).withExistingValue(dtpProperties);
        binder.bind(MAIN_PROPERTIES_PREFIX, target);
    }

    private void doBindIn1X(Environment environment, DtpProperties dtpProperties) {
        try {
            // new RelaxedPropertyResolver(environment)
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            Object resolver = resolverConstructor.newInstance(environment);

            // resolver.getSubProperties(MAIN_PROPERTIES_PREFIX)
            // return a map of all underlying properties that start with the specified key.
            // NOTE: this method can only be used if the underlying resolver is a ConfigurableEnvironment.
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            Map<?, ?> properties = (Map<?, ?>) getSubPropertiesMethod.invoke(resolver, StringUtils.EMPTY);

            doBindIn1X(properties, dtpProperties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SpringBoot1.X的方式
     * @param properties Map
     * @param dtpProperties 实体类
     */
    private void doBindIn1X(Map<?, ?> properties, DtpProperties dtpProperties) {
        try {
            // new RelaxedDataBinder(dtpProperties, MAIN_PROPERTIES_PREFIX)
            Class<?> binderClass = Class.forName("org.springframework.boot.bind.RelaxedDataBinder");
            Constructor<?> binderConstructor = binderClass.getDeclaredConstructor(Object.class, String.class);
            Object binder = binderConstructor.newInstance(dtpProperties, MAIN_PROPERTIES_PREFIX);

            // binder.bind(new MutablePropertyValues(properties))
            Method bindMethod = binderClass.getMethod("bind", PropertyValues.class);
            bindMethod.invoke(binder, new MutablePropertyValues(properties));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
