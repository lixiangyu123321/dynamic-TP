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

package org.dromara.dynamictp.common.plugin;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.util.ExtensionServiceLoader;
import org.dromara.dynamictp.common.util.StringUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 动态线程池（Dtp）拦截器注册器
 * 核心作用：
 * 1. 统一管理所有 DtpInterceptor 拦截器（自动加载 + 手动注册）；
 * 2. 提供批量为目标对象应用拦截器的能力（链式增强）；
 * 3. 保证拦截器管理的线程安全（使用并发Map）。
 */
@Slf4j
public class DtpInterceptorRegistry {

    /**
     * 核心存储容器：维护所有拦截器（自动注册 + 手动注册）
     * 1. 键：拦截器名称（注解指定/类名）；值：拦截器实例；
     * 2. 使用ConcurrentMap保证多线程环境下的线程安全；
     * 3. Maps.newConcurrentMap()：通常是Guava/Hutool的工具方法，创建并发HashMap。
     * XXX 其实就是拦截器（增强逻辑）
     */
    private static final Map<String, DtpInterceptor> INTERCEPTORS = Maps.newConcurrentMap();

    /**
     * 静态代码块：程序启动时自动加载并注册扩展的拦截器
     * 基于SPI/扩展加载机制（ExtensionServiceLoader）加载所有DtpInterceptor实现类
     */
    static {
        // 1. 通过扩展加载器加载所有DtpInterceptor接口的实现类（SPI机制）
        List<DtpInterceptor> loadedInterceptors = ExtensionServiceLoader.get(DtpInterceptor.class);

        // 2. 校验：若加载到拦截器实例，才进行注册逻辑
        if (CollectionUtils.isNotEmpty(loadedInterceptors)) {
            // 3. 遍历每个加载到的拦截器，处理注册
            loadedInterceptors.forEach(x -> {
                // 4. 获取拦截器类上的@DtpIntercepts注解（标记拦截器的元信息，如名称）
                DtpIntercepts interceptsAnno = x.getClass().getAnnotation(DtpIntercepts.class);

                // 5. 校验：只有标注了@DtpIntercepts注解的拦截器才会被注册
                if (Objects.nonNull(interceptsAnno)) {
                    // 6. 确定拦截器名称：注解指定了name则用注解值，否则用类名（简化命名）
                    String name = StringUtils.isBlank(interceptsAnno.name()) ? x.getClass().getSimpleName() : interceptsAnno.name();
                    // 7. 将拦截器存入并发Map，完成自动注册
                    INTERCEPTORS.put(name, x);
                }
            });
        }
    }

    /**
     * 私有构造方法：禁止实例化
     * 设计意图：此类是工具类/注册器，所有方法都是静态的，无需创建实例，避免无意义的对象创建。
     */
    private DtpInterceptorRegistry() { }

    /**
     * 手动注册拦截器方法
     * 用于运行时动态添加自定义拦截器，补充自动加载的不足
     * @param name 拦截器名称（唯一标识，避免重复）
     * @param dtpInterceptor 待注册的拦截器实例
     */
    public static void register(String name, DtpInterceptor dtpInterceptor) {
        // 打印注册日志，便于排查问题（记录拦截器名称和实例）
        log.info("DynamicTp register DtpInterceptor, name: {}, interceptor: {}", name, dtpInterceptor);
        // 将拦截器存入并发Map，完成手动注册（若名称重复会覆盖原有拦截器）
        INTERCEPTORS.put(name, dtpInterceptor);
    }

    /**
     * 获取所有已注册的拦截器（只读）
     * 设计意图：返回不可修改的Map，防止外部修改核心存储容器，保证数据安全
     * @return 不可修改的拦截器Map（key：名称，value：拦截器实例）
     */
    public static Map<String, DtpInterceptor> getInterceptors() {
        return Collections.unmodifiableMap(INTERCEPTORS);
    }

    /**
     * 使用所有拦截器，对方法进行增强
     * 批量为目标对象应用所有已注册的拦截器（无参构造场景）
     * 核心：链式增强，每个拦截器依次对目标对象进行代理增强
     * @param target 待增强的目标对象
     * @return 经过所有拦截器增强后的最终代理对象
     */
    public static Object pluginAll(Object target) {
        // 调用重载方法，传入所有拦截器名称集合，适配无参构造
        return plugin(target, INTERCEPTORS.keySet());
    }

    /**
     * 批量为目标对象应用所有已注册的拦截器（有参构造场景）
     * 适配需要通过有参构造创建代理实例的场景
     * @param target 待增强的目标对象
     * @param argTypes 代理类构造方法的参数类型数组
     * @param args 代理类构造方法的参数数组
     * @return 经过所有拦截器增强后的最终代理对象
     */
    public static Object pluginAll(Object target, Class<?>[] argTypes, Object[] args) {
        // 调用重载方法，传入所有拦截器名称集合，适配有参构造
        return plugin(target, INTERCEPTORS.keySet(), argTypes, args);
    }

    /**
     * 核心增强方法：为目标对象应用指定的拦截器（无参构造）
     * 链式代理：每个拦截器依次对目标对象进行增强，最终返回多层代理后的对象
     * @param target 待增强的目标对象
     * @param interceptors 需要应用的拦截器名称集合
     * @return 经过指定拦截器增强后的代理对象
     */
    public static Object plugin(Object target, Set<String> interceptors) {
        // 1. 根据名称过滤出需要应用的拦截器实例集合
        val filterInterceptors = getInterceptors(interceptors);
        // 2. 遍历每个拦截器，依次对目标对象进行代理增强（链式处理）
        for (DtpInterceptor interceptor : filterInterceptors) {
            // 3. 每次增强后更新target为新的代理对象，实现链式增强
            target = interceptor.plugin(target);
        }
        // 4. 返回最终增强后的代理对象
        return target;
    }

    /**
     * XXX 基于拦截器（这里是增强功能）进行线程池的增强
     * 重载增强方法：为目标对象应用指定的拦截器（有参构造）
     * 适配需要通过有参构造创建代理实例的场景
     * @param target 待增强的目标对象
     * @param interceptors 需要应用的拦截器名称集合
     * @param argTypes 代理类构造方法的参数类型数组
     * @param args 代理类构造方法的参数数组
     * @return 经过指定拦截器增强后的代理对象
     */
    public static Object plugin(Object target, Set<String> interceptors, Class<?>[] argTypes, Object[] args) {
        // 1. 根据名称过滤出需要应用的拦截器实例集合
        val filterInterceptors = getInterceptors(interceptors);
        // 2. 遍历每个拦截器，依次对目标对象进行代理增强（链式处理）
        for (DtpInterceptor interceptor : filterInterceptors) {
            // 3. 调用有参构造的plugin方法，更新target为新的代理对象
            target = interceptor.plugin(target, argTypes, args);
        }
        // 4. 返回最终增强后的代理对象
        return target;
    }

    /**
     * 私有工具方法：根据拦截器名称集合过滤出对应的拦截器实例
     * 支持忽略大小写匹配，提高使用灵活性
     * @param interceptors 需要过滤的拦截器名称集合
     * @return 匹配到的拦截器实例集合
     */
    private static Collection<DtpInterceptor> getInterceptors(Set<String> interceptors) {
        // 1. 边界校验：若名称集合为空，返回所有已注册的拦截器
        if (CollectionUtils.isEmpty(interceptors)) {
            return INTERCEPTORS.values();
        }
        // 2. 流式处理：过滤出名称匹配（忽略大小写）的拦截器
        return INTERCEPTORS.entrySet()
                .stream()
                // 过滤条件：拦截器名称包含在指定集合中（忽略大小写）
                .filter(x -> StringUtil.containsIgnoreCase(x.getKey(), interceptors))
                // 提取拦截器实例
                .map(Map.Entry::getValue)
                // 收集为List返回
                .collect(Collectors.toList());
    }
}