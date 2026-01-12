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

package org.dromara.dynamictp.core.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DynamicTp annotation, mainly used to manage juc ThreadPoolExecutor by this framework.
 * 标识动态线程池的注解
 * XXX 标记 Spring 容器中创建线程池的 @Bean 方法
 * XXX 此处注解只用于非DtpExecutor，或者说只用于ThreadPoolExecutor/ThreadPoolTaskExecutor
 * XXX 用于为上述两种官方线程池创建代理
 * @author yanhom
 * @since 1.0.3
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicTp {

    /**
     * Thread pool name, has higher priority than @Bean annotated method name.
     *
     * @return name
     */
    String value() default "";
}
