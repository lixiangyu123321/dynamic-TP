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
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.function.ToLongFunction;

/**
 * MemoryMetricsCaptor related
 *
 * @author yanhom
 * @since 1.1.6
 **/
@Slf4j
public class MemoryMetricsCaptor implements Runnable {

    /**
     * 最大内存
     */
    private double max = -1;

    /**
     * 已使用内存
     */
    private double used = -1;

    /**
     * 获得老年代内存使用率
     * @return
     */
    public double getLongLivedMemoryUsage() {
        if (max == -1 || used == -1) {
            return -1;
        }
        return used / max;
    }

    /**
     * 获取所有内存池
     * 查找老年代内存池
     * 获取已使用内存和最大内存
     * 计算使用率
     */
    @Override
    public void run() {
        try {
            val memoryPoolBeans = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);
            if (CollectionUtils.isEmpty(memoryPoolBeans)) {
                return;
            }
            for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
                String name = memoryPoolBean.getName();
                boolean isLongLivedPool = isLongLivedPool(name);
                if (isLongLivedPool) {
                    used = getUsageValue(memoryPoolBean, MemoryUsage::getUsed);
                    max = getUsageValue(memoryPoolBean, MemoryUsage::getMax);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("MemoryMetricsCaptor run failed.", e);
        }
    }

    /**】
     *
     * @param memoryPoolMXBean 内存池信息获取类
     * @param getter 函数式接口， 接受类型为T的值，返回Long类型的结果
     * @return
     */
    private double getUsageValue(MemoryPoolMXBean memoryPoolMXBean, ToLongFunction<MemoryUsage> getter) {
        MemoryUsage usage = getUsage(memoryPoolMXBean);
        if (usage == null) {
            return -1;
        }
        return getter.applyAsLong(usage);
    }

    private MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
        try {
            return memoryPoolMXBean.getUsage();
        } catch (InternalError e) {
            return null;
        }
    }

    /**
     * 判断是否是老年代
     * *Old Gen: HotSpot 老年代
     * *Tenured Gen: HotSpot 老年代（旧名称）
     * ZHeap: ZGC 堆
     * Shenandoah: Shenandoah GC 堆
     * *balanced-old: G1 老年代
     * *tenured*: 包含 "tenured" 的名称
     * JavaHeap: Java 堆
     * @param name
     * @return
     */
    private boolean isLongLivedPool(String name) {
        return StringUtils.isNotBlank(name) && (name.endsWith("Old Gen") ||
                name.endsWith("Tenured Gen") ||
                "ZHeap".equals(name) ||
                "Shenandoah".equals(name) ||
                name.endsWith("balanced-old") ||
                name.contains("tenured") ||
                "JavaHeap".equals(name)
        );
    }
}
