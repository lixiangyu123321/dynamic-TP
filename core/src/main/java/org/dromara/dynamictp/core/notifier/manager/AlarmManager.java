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

package org.dromara.dynamictp.core.notifier.manager;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.em.RejectedTypeEnum;
import org.dromara.dynamictp.common.entity.NotifyItem;
import org.dromara.dynamictp.common.pattern.filter.InvokerChain;
import org.dromara.dynamictp.core.notifier.alarm.AlarmCounter;
import org.dromara.dynamictp.core.notifier.alarm.AlarmLimiter;
import org.dromara.dynamictp.core.notifier.context.AlarmCtx;
import org.dromara.dynamictp.core.notifier.context.BaseNotifyCtx;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.ThreadPoolBuilder;
import org.dromara.dynamictp.core.support.task.runnable.DtpRunnable;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrappers;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.TRACE_ID;
import static org.dromara.dynamictp.common.em.QueueTypeEnum.LINKED_BLOCKING_QUEUE;

/**
 * AlarmManager related
 * 告警管理器，基于责任链模式处理告警，支持告警限流和计数
 * @author yanhom
 * @since 1.0.0
 */
@Slf4j
public class AlarmManager {

    private static final ExecutorService ALARM_EXECUTOR = ThreadPoolBuilder.newBuilder()
            .threadFactory("dtp-alarm")
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(LINKED_BLOCKING_QUEUE.getName(), 2000)
            .rejectedExecutionHandler(RejectedTypeEnum.DISCARD_OLDEST_POLICY.getName())
            .rejectEnhanced(false)
            .taskWrappers(TaskWrappers.getInstance().getByNames(Sets.newHashSet("mdc")))
            .buildDynamic();

    /**
     * 责任链，过滤 + 最终执行Invoker
     */
    private static final InvokerChain<BaseNotifyCtx> ALARM_INVOKER_CHAIN;

    static {
        ALARM_INVOKER_CHAIN = NotifyFilterBuilder.getAlarmInvokerChain();
    }

    private AlarmManager() { }

    public static void initAlarm(String poolName, List<NotifyItem> notifyItems) {
        notifyItems.forEach(x -> initAlarm(poolName, x));
    }

    /**
     * 进行告警计数和限流
     * @param poolName
     * @param notifyItem
     */
    public static void initAlarm(String poolName, NotifyItem notifyItem) {
        AlarmLimiter.initAlarmLimiter(poolName, notifyItem);
        AlarmCounter.initAlarmCounter(poolName, notifyItem);
    }

    public static void initAlarmLimiter(String poolName, NotifyItem notifyItem) {
        AlarmLimiter.initAlarmLimiter(poolName, notifyItem);
    }

    public static void initAlarmCounter(String poolName, NotifyItem notifyItem) {
        AlarmCounter.initAlarmCounter(poolName, notifyItem);
    }

    /**
     * 尝试异步发送告警
     * @param executorWrapper
     * @param notifyType
     * @param runnable
     */
    public static void tryAlarmAsync(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType, Runnable runnable) {
        preAlarm(runnable);
        try {
            ALARM_EXECUTOR.execute(() -> doTryAlarm(executorWrapper, notifyType));
        } finally {
            postAlarm(runnable);
        }
    }

    /**
     * checkAndTryAlarmAsync 传入的是 “待检查的告警类型列表”，而非 “已满足条件的告警信息”
     * @param executorWrapper
     * @param notifyTypes
     */
    public static void checkAndTryAlarmAsync(ExecutorWrapper executorWrapper, List<NotifyItemEnum> notifyTypes) {
        ALARM_EXECUTOR.execute(() -> notifyTypes.forEach(x -> doCheckAndTryAlarm(executorWrapper, x)));
    }

    public static void doCheckAndTryAlarm(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType) {
        NotifyHelper.getNotifyItem(executorWrapper, notifyType).ifPresent(notifyItem -> {
            if (hasReachedThreshold(executorWrapper, notifyType, notifyItem)) {
                ALARM_INVOKER_CHAIN.proceed(new AlarmCtx(executorWrapper, notifyItem));
            }
        });
    }

    public static void tryAlarmAsync(ExecutorWrapper executorWrapper, List<NotifyItemEnum> notifyTypes) {
        ALARM_EXECUTOR.execute(() -> notifyTypes.forEach(x -> doTryAlarm(executorWrapper, x)));
    }

    /**
     * 基于NotifyHelper获得通知项，保证为通知上下文，交给责任链处理
     * @param executorWrapper
     * @param notifyType
     */
    public static void doTryAlarm(ExecutorWrapper executorWrapper, NotifyItemEnum notifyType) {
        NotifyHelper.getNotifyItem(executorWrapper, notifyType).ifPresent(notifyItem -> {
            val alarmCtx = new AlarmCtx(executorWrapper, notifyItem);
            ALARM_INVOKER_CHAIN.proceed(alarmCtx);
        });
    }

    /**
     * 由于异步发送告警信息，需要处理MDC分布式链路ID
     * @param runnable
     */
    private static void preAlarm(Runnable runnable) {
        if (runnable instanceof DtpRunnable) {
            MDC.put(TRACE_ID, ((DtpRunnable) runnable).getTraceId());
        }
    }

    /**
     * 线程池线程长期存在，避免分布式链路ID互相影响
     * @param runnable
     */
    private static void postAlarm(Runnable runnable) {
        if (runnable instanceof DtpRunnable) {
            MDC.remove(TRACE_ID);
        }
    }

    /**
     * Check if the threshold is reached, for capacity and liveness we need to check.
     * for reject, run timeout and queue timeout we don't need to check here, because it has been checked before.
     * 它就是一个 “阈值校验开关”，只有返回 true，才会进入后续的责任链告警流程；返回 false 则直接跳过该监控项的告警。
     * @param executor    the executor
     * @param notifyType  the notify type
     * @param notifyItem  the notify item
     * @return true if the threshold is reached, false otherwise
     */
    private static boolean hasReachedThreshold(ExecutorWrapper executor, NotifyItemEnum notifyType, NotifyItem notifyItem) {
        switch (notifyType) {
            case CAPACITY:
                return checkCapacity(executor, notifyItem);
            case LIVENESS:
                return checkLiveness(executor, notifyItem);
            case REJECT:
            case RUN_TIMEOUT:
            case QUEUE_TIMEOUT:
                return true;
            default:
                log.error("Unsupported alarm type [{}]", notifyType);
                return false;
        }
    }

    private static boolean checkLiveness(ExecutorWrapper executorWrapper, NotifyItem notifyItem) {
        val executor = executorWrapper.getExecutor();
        int maximumPoolSize = executor.getMaximumPoolSize();
        double div = NumberUtil.div(executor.getActiveCount(), maximumPoolSize, 2) * 100;
        if (div >= notifyItem.getThreshold()) {
            log.warn("DynamicTp monitor, current liveness [{}] >= threshold [{}], threadPoolName: {}",
                    div, notifyItem.getThreshold(), executorWrapper.getThreadPoolName());
            return true;
        }
        return false;
    }

    private static boolean checkCapacity(ExecutorWrapper executorWrapper, NotifyItem notifyItem) {
        val executor = executorWrapper.getExecutor();
        if (executor.getQueueSize() <= 0) {
            return false;
        }
        double div = NumberUtil.div(executor.getQueueSize(), executor.getQueueCapacity(), 2) * 100;
        if (div >= notifyItem.getThreshold()) {
            log.warn("DynamicTp monitor, current queue utilization [{}] >= threshold [{}], threadPoolName: {}",
                    div, notifyItem.getThreshold(), executorWrapper.getThreadPoolName());
            return true;
        }
        return false;
    }

    public static void destroy() {
        ALARM_EXECUTOR.shutdownNow();
    }
}
