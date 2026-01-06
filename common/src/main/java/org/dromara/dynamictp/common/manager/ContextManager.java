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

package org.dromara.dynamictp.common.manager;

import java.util.Map;


/**
 * 上下文管理器核心接口
 * 作用：统一封装 Bean 获取、环境变量读取等上下文相关操作，适配 Spring/其他容器的上下文能力
 */
public interface ContextManager {

    /**
     * 根据 Bean 的类型获取对应的 Bean 实例
     *
     * @param <T>   泛型：Bean 的类型（自动推导）
     * @param clazz Bean 的类型类对象（如 UserService.class）
     * @return 匹配类型的 Bean 实例，未找到则返回 null
     */
    <T> T getBean(Class<T> clazz);

    /**
     * 根据 Bean 的名称和类型获取对应的 Bean 实例
     * （解决同类型多个 Bean 的场景，通过名称精准匹配）
     *
     * @param <T>   泛型：Bean 的类型（自动推导）
     * @param name  Bean 的名称（如 "userServiceImpl"）
     * @param clazz Bean 的类型类对象
     * @return 匹配名称和类型的 Bean 实例，未找到则返回 null
     */
    <T> T getBean(String name, Class<T> clazz);

    /**
     * 获取指定类型的所有 Bean 实例
     *
     * @param <T>   泛型：Bean 的类型（自动推导）
     * @param clazz Bean 的类型类对象
     * @return Key 为 Bean 名称、Value 为 Bean 实例的 Map 集合
     */
    <T> Map<String, T> getBeansOfType(Class<T> clazz);

    /**
     * 获取当前上下文的环境对象
     * （如 Spring 的 Environment 对象、自定义环境配置对象）
     *
     * @return 环境对象（具体类型由实现类决定）
     */
    Object getEnvironment();

    /**
     * 根据属性键获取环境配置值
     *
     * @param key 环境属性的键（如 "spring.profiles.active"）
     * @return 匹配键的属性值，未找到则返回 null
     */
    String getEnvironmentProperty(String key);

    /**
     * 在指定的环境对象中，根据属性键获取环境配置值
     * （适配多环境场景，可指定非当前上下文的环境对象读取配置）
     *
     * @param key         环境属性的键
     * @param environment 指定的环境对象（如自定义的 Environment 实例）
     * @return 匹配键的属性值，未找到则返回 null
     */
    String getEnvironmentProperty(String key, Object environment);

    /**
     * 根据属性键获取环境配置值，若未找到则返回默认值
     * （避免空值，简化业务代码的空值判断）
     *
     * @param key          环境属性的键
     * @param defaultValue 未找到属性时返回的默认值
     * @return 匹配键的属性值，未找到则返回 defaultValue
     */
    String getEnvironmentProperty(String key, String defaultValue);
}
