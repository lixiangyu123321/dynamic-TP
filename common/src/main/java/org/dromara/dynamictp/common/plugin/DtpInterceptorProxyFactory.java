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
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatchers;
import org.dromara.dynamictp.common.util.UUIDUtil;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 生成代理类增强方法，并处理DtpInterceptor的注解从而获得增强的方法
 * @author windsearcher.lq
 * @since 1.1.4
 */
public class DtpInterceptorProxyFactory {

    private DtpInterceptorProxyFactory() { }

    /**
     * 重载方法：为目标对象创建动态代理（增强）对象
     * 简化调用，默认不传参数类型和参数，适配无参构造的场景
     * @param target 待增强的目标对象
     * @param interceptor 方法拦截器，包含自定义的拦截逻辑
     * @return 增强后的代理对象（若无需增强则返回原对象）
     */
    public static Object enhance(Object target, DtpInterceptor interceptor) {
        // 调用重载的核心增强方法，参数类型和参数传null，适配无参构造场景
        return enhance(target, null, null, interceptor);
    }

    /**
     * 核心增强方法：基于 ByteBuddy 为目标对象创建动态子类代理
     * 仅对拦截器指定的目标方法进行增强，非目标方法直接执行原逻辑
     * @param target 待增强的目标对象
     * @param argumentTypes 代理类构造方法的参数类型数组（用于有参构造创建实例）
     * @param arguments 代理类构造方法的参数数组（用于有参构造创建实例）
     * @param interceptor 方法拦截器，提供自定义的拦截逻辑
     * @return 增强后的代理对象（若无需增强则返回原对象）
     */
    public static Object enhance(Object target, Class<?>[] argumentTypes, Object[] arguments, DtpInterceptor interceptor) {
        // 1. 获取拦截器中标记的需要拦截的方法签名映射（类 -> 该类下需要拦截的方法集合）
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);

        // 2. 校验：若目标对象的类不在签名映射中，说明无需增强，直接返回原对象（避免无意义的代理）
        if (!signatureMap.containsKey(target.getClass())) {
            // 无需增强，返回原始目标对象
            return target;
        }

        try {
            // 3. 使用 ByteBuddy 构建目标类的动态子类（核心：动态生成代理类）
            Class<?> proxyClass = new ByteBuddy()
                    // 创建目标类的子类（ByteBuddy 基于继承实现代理，区别于JDK动态代理的接口实现）
                    .subclass(target.getClass())
                    // 自定义代理类的名称：原类名 + $ByteBuddy$ + 5位UUID（避免类名冲突）
                    .name(String.format("%s$ByteBuddy$%s", target.getClass().getName(), UUIDUtil.genUuid(5)))
                    // 匹配目标类的所有方法（后续会在InvocationHandler中过滤仅处理需要拦截的方法）
                    .method(ElementMatchers.any())
                    // 拦截匹配到的方法：将方法调用委托给自定义的InvocationHandler处理
                    // InvocationHandlerAdapter：ByteBuddy 与 JDK InvocationHandler 的适配器
                    // XXX 这里的作用就是将方法转发
                    .intercept(InvocationHandlerAdapter.of(new DtpInvocationHandler(target, interceptor, signatureMap)))
                    // 保留方法的属性（包括接收者信息），保证代理方法与原方法属性一致
                    .attribute(MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER)
                    // 继承目标类的类级别注解，保证注解语义不丢失
                    .annotateType(target.getClass().getAnnotations())
                    // 生成代理类的字节码
                    .make()
                    // 加载生成的代理类：使用当前类的类加载器，采用INJECTION策略（将类注入到当前类加载器）
                    .load(DtpInterceptorProxyFactory.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    // 获取加载后的代理类Class对象
                    .getLoaded();

            // 4. 根据参数是否为空，选择构造方法创建代理实例
            if (Objects.isNull(argumentTypes) || Objects.isNull(arguments)) {
                // 无参构造：参数类型/参数为空时，调用代理类的无参构造创建实例
                return proxyClass.getDeclaredConstructor().newInstance();
            }
            // 有参构造：使用指定的参数类型和参数，调用代理类的有参构造创建实例
            return proxyClass.getDeclaredConstructor(argumentTypes).newInstance(arguments);

        } catch (Exception e) {
            // 5. 捕获所有异常（反射/字节码生成/类加载等），封装为自定义异常抛出
            throw new PluginException("Failed to create proxy instance", e);
        }
    }

    /**
     * 等于说将注解中的方法签名基于目标类聚合
     * @param interceptor
     * @return
     */
    private static Map<Class<?>, Set<Method>> getSignatureMap(DtpInterceptor interceptor) {
        // 获得拦截器的注解DtpIntercepts （拦截器名 + 方法签名数组）
        DtpIntercepts interceptsAnno = interceptor.getClass().getAnnotation(DtpIntercepts.class);
        if (interceptsAnno == null) {
            throw new PluginException("No @DtpIntercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        // 获得方法签名数组
        DtpSignature[] signatures = interceptsAnno.signatures();
        Map<Class<?>, Set<Method>> signatureMap = Maps.newHashMap();
        for (DtpSignature signature : signatures) {
            // 目标类的方法集合
            Set<Method> methods = signatureMap.computeIfAbsent(signature.clazz(), k -> new HashSet<>());
            try {
                // 基于方法名和参数类型数组获得方法类
                Method method = signature.clazz().getMethod(signature.method(), signature.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + signature.clazz() + " named " + signature.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }
}
