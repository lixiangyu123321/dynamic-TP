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

package org.dromara.dynamictp.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Version related
 *
 * @author kamtohung
 */
@Slf4j
public final class VersionUtil {  // final修饰类，禁止被继承（工具类最佳实践）


    private static String version;

    // 静态代码块：类加载时执行，初始化version变量（只执行一次，保证版本号只读取一次）
    static {
        try {
            // 调用version()方法读取版本号，赋值给静态变量
            version = version();
        } catch (Exception e) {
            // 捕获所有异常（如反射/IO异常），打印警告日志（不中断类加载）
            log.warn("no version number found");
        }
    }

    private VersionUtil() { }

    /**
     * 核心方法：读取当前类所在Jar包的版本号（优先从MANIFEST.MF文件读取）
     * @return 版本号字符串（如1.0.0），读取失败则返回"unknown"
     */
    public static String version() {
        // 第一步：优先从MANIFEST.MF文件中读取版本信息
        // 获取当前类对应的Package对象（Package封装了类所在Jar包的MANIFEST.MF元信息）
        Package pkg = VersionUtil.class.getPackage();

        String version;
        // 判空：如果Package对象不为null（说明类在Jar包中，且MANIFEST.MF存在）
        if (pkg != null) {
            // 1. 优先读取Implementation-Version（实现版本，通常是Jar包的实际版本号）
            // 对应MANIFEST.MF中的 "Implementation-Version: 1.0.0" 配置
            version = pkg.getImplementationVersion();
            if (StringUtils.isNotEmpty(version)) {
                return version;
            }

            // 2. 若实现版本为空，读取Specification-Version（规范版本，可选）
            // 对应MANIFEST.MF中的 "Specification-Version: 1.0" 配置
            version = pkg.getSpecificationVersion();
            if (StringUtils.isNotEmpty(version)) {
                return version;
            }
        }

        // 第二步：读取失败（如类不在Jar包中、MANIFEST.MF无版本配置），返回"unknown"
        return "unknown";
    }

    /**
     * 对外提供的静态方法：获取版本号（直接返回初始化好的静态变量，无需重复读取）
     * @return 版本号（类加载时已初始化，可能是具体版本或"unknown"）
     */
    public static String getVersion() {
        return version;
    }

}
