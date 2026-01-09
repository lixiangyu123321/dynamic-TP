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

/**
 * 拦截器可扩展，包含了需要扩展的业务逻辑
 * @author windsearcher.lq
 * @since 1.1.4
 */
/**
 * XXX 所以这个方法其实是实现代理对象的生成和代理方法的加强的
 * 动态线程池（Dtp, Dynamic Thread Pool）拦截器接口
 * 作用：定义方法拦截的规范，用于对目标对象的方法执行进行增强（如前置/后置处理、异常处理、耗时统计等）
 * 设计：基于动态代理模式，通过默认方法提供便捷的代理对象创建能力
 */
public interface DtpInterceptor {

    /**
     * 核心拦截方法：对目标方法的执行过程进行拦截处理
     * 实现类需重写此方法，自定义拦截逻辑（如前置校验、后置处理、异常捕获、耗时统计等）
     * XXX 拦截器在通过DtpInvocation封装的方法进行源类的方法调用
     * @param invocation 方法调用上下文对象，包含目标对象、方法、参数、执行逻辑等核心信息
     * @return 目标方法执行后的返回值（可被拦截器修改）
     * @throws Throwable 允许抛出任意异常，由上层调用方处理
     */
    Object intercept(DtpInvocation invocation) throws Throwable;

    /**
     * 默认方法：为目标对象创建代理（增强）对象
     * 无需实现类重写，直接使用工厂类创建代理，简化增强对象的创建流程
     * XXX 基于代理工厂实现目标类的增强
     * @param target 待增强的目标对象（被拦截的原始对象）
     * @return 增强后的代理对象（调用代理对象的方法时会触发intercept拦截逻辑）
     */
    default Object plugin(Object target) {
        // 委托给代理工厂类，基于当前拦截器实例创建目标对象的代理
        return DtpInterceptorProxyFactory.enhance(target, this);
    }

    /**
     * 重载的默认方法：为目标对象创建代理（增强）对象（支持指定方法参数类型和参数）
     * 适用于需要精准匹配特定方法（通过参数类型/参数）进行增强的场景
     * XXX 对于代理类的构造使用对应的有参构造方法
     * @param target 待增强的目标对象（被拦截的原始对象）
     * @param argumentTypes 目标方法的参数类型数组（用于精准定位需要增强的方法）
     * @param arguments 目标方法的参数数组（用于方法匹配或参数传递）
     * @return 增强后的代理对象（仅对匹配参数的方法生效拦截逻辑）
     */
    default Object plugin(Object target, Class<?>[] argumentTypes, Object[] arguments) {
        // 委托给代理工厂类，基于参数类型+参数精准创建目标对象的代理
        // XXX 创建代理类，并基于指定参数构造代理类
        return DtpInterceptorProxyFactory.enhance(target, argumentTypes, arguments, this);
    }
}
