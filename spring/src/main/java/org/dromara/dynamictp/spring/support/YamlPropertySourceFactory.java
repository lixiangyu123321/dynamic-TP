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

package org.dromara.dynamictp.spring.support;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Properties;

/**
 * YamlPropertySourceFactory related
 * XXX 核心目的是标准化、可扩展地从各类资源（如.properties、.yml 文件）中加载配置属性并封装为PropertySource，是 Spring 外部化配置的核心扩展点。
 * XXX 通过getProperties方法直接获得相应的property
 * @author yanhom
 * @since 1.1.0
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource) {
        // 创建yaml工厂Bean
        // XXX 解析yaml文件，直接获得Properties
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        // 设置资源
        factory.setResources(encodedResource.getResource());
        Properties properties = factory.getObject();
        if (Objects.isNull(properties)
                || StringUtils.isBlank(encodedResource.getResource().getFilename())) {
            return null;
        }
        // XXX 本质是为了适配 Spring 的统一配置体系—— 让 YAML 配置以 Spring 标准的PropertySource格式被识别和使用
        return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
    }
}
