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

package org.dromara.dynamictp.common.queue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存限制计算器
 * 用于实时监控并获取JVM当前可用的最大内存值
 * 核心功能：通过定时任务周期性刷新可用内存值，对外提供快速读取接口
 */
public class MemoryLimitCalculator {

    /**
     * 存储当前JVM可用的最大内存值（单位：字节）
     * volatile关键字：
     * 1. 保证可见性：一个线程修改后，其他线程能立即看到最新值
     * 2. 禁止指令重排序：确保变量赋值操作不会被优化重排
     * static：属于类级别，全局共享
     */
    private static volatile long maxAvailable;

    /**
     * 定时任务执行器（单线程）
     * 用于周期性执行内存刷新任务
     * newSingleThreadScheduledExecutor：创建单线程的定时任务线程池，保证任务串行执行
     * static final：类级别常量，避免重复创建，保证线程池唯一
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    /**
     * 静态代码块：类加载时立即执行，且仅执行一次
     * 用于初始化定时任务和初始内存值
     */
    static {
        // 类加载时立即刷新内存值，防止maxAvailable初始值为0导致业务异常
        refresh();
        // 启动周期性任务：每隔50毫秒刷新一次内存值
        // 参数说明：
        // 1. MemoryLimitCalculator::refresh：要执行的任务（方法引用）
        // 2. 50：首次执行延迟时间（毫秒）
        // 3. 50：后续执行的间隔时间（毫秒）
        // 4. TimeUnit.MILLISECONDS：时间单位
        // XXX 等上一次任务完成解释开始计时
        SCHEDULER.scheduleWithFixedDelay(MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);

        // 注册JVM关闭钩子：JVM退出时优雅关闭定时任务线程池
        // 避免线程池残留导致JVM无法正常退出
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    /**
     * 私有刷新方法：更新maxAvailable为当前JVM的空闲内存值
     * private：仅类内部调用，封装实现细节
     * static：类方法，可通过方法引用被定时任务调用
     */
    private static void refresh() {
        // Runtime.getRuntime()：获取JVM运行时实例
        // freeMemory()：返回JVM当前的空闲内存量（字节）
        maxAvailable = Runtime.getRuntime().freeMemory();
    }

    /**
     * 对外提供的获取可用内存的接口
     * 实时返回最新的空闲内存值，无性能损耗（仅读取volatile变量）
     *
     * @return 当前JVM可用的最大内存值（字节）
     */
    public static long maxAvailable() {
        return maxAvailable;
    }
}
