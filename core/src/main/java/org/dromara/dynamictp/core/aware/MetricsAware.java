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

package org.dromara.dynamictp.core.aware;

import org.dromara.dynamictp.common.entity.ThreadPoolStats;

import java.util.Collections;
import java.util.List;

/**
 * MetricsAware related
 * 1-2 18:34 这里弱化了感知属性，只是提供了指标的查询的功能
 * XXX 这里的感知器是用来被感知的，当一个动态线程池实现了该类，就可以被其他统计程序感知到了
 * XXX val handlerMap = ContextManagerHelper.getBeansOfType(MetricsAware.class);
 * XXX 从而可以对外提供统计信息的能力
 * @author yanhom
 * @since 1.0.9
 */
public interface MetricsAware extends DtpAware {

    /**
     * Get thread pool stats.
     *
     * @return the thread pool stats
     */
    default ThreadPoolStats getPoolStats() {
        return null;
    }

    /**
     * Get multi thread pool stats.
     * XXX 获得多个统计数据
     * @return thead pools stats
     */
    default List<ThreadPoolStats> getMultiPoolStats() {
        return Collections.emptyList();
    }
}
