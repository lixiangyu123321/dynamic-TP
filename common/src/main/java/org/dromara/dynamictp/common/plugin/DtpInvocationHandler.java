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

import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Dtp（动态线程池）代理调用处理器
 * XXX 将方法调用转发给拦截器的intecepter，并且将源方法封装
 * 核心作用：实现 JDK InvocationHandler 接口，封装代理对象的方法调用逻辑，
 * 仅对 signatureMap 中指定的目标方法执行拦截器逻辑，非目标方法直接执行原逻辑。
 * @author yanhom
 * @since 1.2.1
 */
public class DtpInvocationHandler implements InvocationHandler {

    /** 原始目标对象：被代理的真实对象（最终执行方法的本体） */
    private final Object target;

    /** 拦截器实例：包含自定义的方法拦截逻辑（如前置处理、后置增强等） */
    private final DtpInterceptor interceptor;

    /** 方法签名映射：key=目标类，value=该类下需要被拦截的方法集合，用于快速匹配是否需要拦截当前调用 */
    private final Map<Class<?>, Set<Method>> signatureMap;

    /**
     * 构造方法：初始化代理调用处理器的核心依赖
     * @param target 原始目标对象（被代理的真实对象）
     * @param interceptor 自定义拦截器（提供具体的拦截逻辑）
     * @param signatureMap 方法签名映射（标记需要拦截的方法）
     */
    public DtpInvocationHandler(Object target, DtpInterceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        // 赋值原始目标对象
        this.target = target;
        // 赋值拦截器实例
        this.interceptor = interceptor;
        // 赋值方法签名映射
        this.signatureMap = signatureMap;
    }

    /**
     * 核心方法：代理对象的所有方法调用都会触发此方法（JDK 动态代理的核心入口）
     * 逻辑：判断当前调用的方法是否在需要拦截的范围内，是则执行拦截器逻辑，否则直接执行原始方法
     * XXX 代理类的所有方法调用会被转发到这里，这是在增强的时候实现的
     * @param proxy 代理对象本身（注意：通常不使用此参数，避免调用时触发递归）
     * @param method 被调用的方法实例（包含方法名、参数、返回值等信息）
     * @param args 方法调用时传入的参数数组（无参数时为 null；基本类型会被包装为包装类）
     * @return 方法执行结果（拦截器处理后的结果 或 原始方法执行结果）
     * @throws Throwable 允许抛出任意异常，由上层调用方处理
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 从签名映射中获取当前方法所属类需要拦截的方法集合
        //    method.getDeclaringClass()：获取方法声明的类（而非代理类），保证精准匹配
        Set<Method> methods = signatureMap.get(method.getDeclaringClass());

        // 2. 校验：需要拦截的方法集合非空 且 当前调用的方法在集合中 → 执行拦截逻辑
        //
        if (CollectionUtils.isNotEmpty(methods) && methods.contains(method)) {
            // XXX 这里再调用拦截其中定义的增强方法
            return interceptor.intercept(new DtpInvocation(target, method, args));
        }
        // 5. 非拦截方法：直接通过反射调用原始目标对象的方法，返回原生结果
        return method.invoke(target, args);
    }
}