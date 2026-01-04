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

package org.dromara.dynamictp.core.notifier.chain.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.entity.AlarmInfo;
import org.dromara.dynamictp.common.entity.NotifyItem;
import org.dromara.dynamictp.common.pattern.filter.Invoker;
import org.dromara.dynamictp.core.notifier.alarm.AlarmCounter;
import org.dromara.dynamictp.core.notifier.context.AlarmCtx;
import org.dromara.dynamictp.core.notifier.context.BaseNotifyCtx;
import org.dromara.dynamictp.core.support.ExecutorWrapper;

import java.util.Objects;

/**
 * BaseAlarmFilter related
 * 告警过滤
 * @author yanhom
 * @since 1.0.8
 **/
@Slf4j
public class BaseAlarmFilter implements NotifyFilter {

    /**
     * 基于内容过滤
     * Invoker是对于过滤后内容的调用
     * @param context context
     * @param nextInvoker next invoker
     */
    @Override
    public void doFilter(BaseNotifyCtx context, Invoker<BaseNotifyCtx> nextInvoker) {
        ExecutorWrapper executorWrapper = context.getExecutorWrapper();
        NotifyItem notifyItem = context.getNotifyItem();
        if (Objects.isNull(notifyItem) || !satisfyBaseCondition(notifyItem, executorWrapper)) {
            return;
        }

        // 告警次数递增
        String threadPoolName = executorWrapper.getThreadPoolName();
        AlarmCounter.incAlarmCount(threadPoolName, notifyItem.getType());
        AlarmInfo alarmInfo = AlarmCounter.getAlarmInfo(threadPoolName, notifyItem.getType());
        if (Objects.isNull(alarmInfo)) {
            return;
        }

        // 判断是否达到与之
        if (alarmInfo.getCount() < notifyItem.getCount()) {
            if (log.isDebugEnabled()) {
                log.debug("DynamicTp notify, alarm count not reached, current count: {}, threshold: {}, threadPoolName: {}, notifyItem: {}",
                        alarmInfo.getCount(), notifyItem.getCount(), threadPoolName, notifyItem);
            }
            return;
        }
        // 达到阈值，发送告警
        ((AlarmCtx) context).setAlarmInfo(alarmInfo);
        nextInvoker.invoke(context);
    }

    /**
     * 基于多个内容判断是否满足通知条件
     * 执行器通知已启用
     * 通知项已启用
     * 通知项配置了平台 ID
     * @param notifyItem
     * @param executorWrapper
     * @return
     */
    private boolean satisfyBaseCondition(NotifyItem notifyItem, ExecutorWrapper executorWrapper) {
        return executorWrapper.isNotifyEnabled()
                && notifyItem.isEnabled()
                && CollectionUtils.isNotEmpty(notifyItem.getPlatformIds());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
