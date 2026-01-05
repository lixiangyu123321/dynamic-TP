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

package org.dromara.dynamictp.core.system;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.util.MethodUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * OperatingSystemBeanManager related.
 * 操作系统Bean管理器，提供系统信息的访问。它封装了不同 JVM 实现（HotSpot、J9）的差异，提供统一的系统信息访问接口。
 * 兼容不同 JVM 实现（HotSpot/J9）的操作系统信息获取工具类
 * @author yanhom
 * @since 1.1.5
 */
@Slf4j
public class OperatingSystemBeanManager {

    /**
     * com.ibm for J9
     * com.sun for HotSpot
     * 操作系统管理类
     */
    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
            "com.sun.management.OperatingSystemMXBean", "com.ibm.lang.management.OperatingSystemMXBean");

    /**
     * JVM提供的操作系统MXBean实例
     */
    private static final OperatingSystemMXBean OPERATING_SYSTEM_BEAN;

    private static final Class<?> OPERATING_SYSTEM_BEAN_CLASS;

    private static final Method SYSTEM_CPU_USAGE_METHOD;

    private static final Method PROCESS_CPU_TIME_METHOD;

    /**
     * 空闲为物理内存
     */
    private static final Method FREE_PHYSICAL_MEM_METHOD;

    private static final Method TOTAL_PHYSICAL_MEM_METHOD;

    static {
        OPERATING_SYSTEM_BEAN = ManagementFactory.getOperatingSystemMXBean();
        OPERATING_SYSTEM_BEAN_CLASS = loadOne(OPERATING_SYSTEM_BEAN_CLASS_NAMES);
        SYSTEM_CPU_USAGE_METHOD = deduceMethod("getSystemCpuLoad");
        PROCESS_CPU_TIME_METHOD = deduceMethod("getProcessCpuTime");

        Method totalPhysicalMem = deduceMethod("getTotalPhysicalMemorySize");
        // getTotalPhysicalMemory for ibm jdk 7.
        TOTAL_PHYSICAL_MEM_METHOD = totalPhysicalMem != null ? totalPhysicalMem :
                deduceMethod("getTotalPhysicalMemory");

        FREE_PHYSICAL_MEM_METHOD = deduceMethod("getFreePhysicalMemorySize");
    }

    private OperatingSystemBeanManager() { }

    /**
     * 下面4个方法在原有反射的基础上进行了一定的异常处理，本质还是反射的方法调用
     * @return
     */
    public static OperatingSystemMXBean getOperatingSystemBean() {
        return OPERATING_SYSTEM_BEAN;
    }

    public static double getSystemCpuUsage() {
        return MethodUtil.invokeAndReturnDouble(SYSTEM_CPU_USAGE_METHOD, OPERATING_SYSTEM_BEAN);
    }

    public static long getProcessCpuTime() {
        return MethodUtil.invokeAndReturnLong(PROCESS_CPU_TIME_METHOD, OPERATING_SYSTEM_BEAN);
    }

    public static long getTotalPhysicalMem() {
        return MethodUtil.invokeAndReturnLong(TOTAL_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
    }

    public static long getFreePhysicalMem() {
        return MethodUtil.invokeAndReturnLong(FREE_PHYSICAL_MEM_METHOD, OPERATING_SYSTEM_BEAN);
    }

    /**
     * 尝试加载非标准操作系统管理Bean
     * @param classNames
     * @return
     */
    private static Class<?> loadOne(List<String> classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load operating system bean class.", e);
            }
        }
        return null;
    }

    /**
     * 类型转换校验 + 获得方法
     * @param name
     * @return
     */
    private static Method deduceMethod(String name) {
        if (Objects.isNull(OPERATING_SYSTEM_BEAN_CLASS)) {
            return null;
        }
        try {
            // 类型转换校验，确保当前获取的 MXBean 实例是已加载的扩展类的实例，避免后续反射调用出现类型不匹配问题。
            OPERATING_SYSTEM_BEAN_CLASS.cast(OPERATING_SYSTEM_BEAN);
            return OPERATING_SYSTEM_BEAN_CLASS.getDeclaredMethod(name);
        } catch (Exception e) {
            return null;
        }
    }
}

