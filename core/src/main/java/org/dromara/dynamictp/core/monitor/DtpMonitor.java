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

package org.dromara.dynamictp.core.monitor;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.entity.ThreadPoolStats;
import org.dromara.dynamictp.common.event.AlarmCheckEvent;
import org.dromara.dynamictp.common.event.CollectEvent;
import org.dromara.dynamictp.common.event.CustomContextRefreshedEvent;
import org.dromara.dynamictp.common.manager.EventBusManager;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.converter.ExecutorConverter;
import org.dromara.dynamictp.core.handler.CollectorHandler;
import org.dromara.dynamictp.core.notifier.manager.AlarmManager;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.ThreadPoolCreator;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.SCHEDULE_NOTIFY_ITEMS;

/**
 * DtpMonitor related
 * DtpMonitor 是监控管理器，负责定时收集线程池指标和检查告警。它使用定时任务定期执行监控逻辑。
 * 通过Collector收集指标信息 + 基于告警管理器告警信息
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class DtpMonitor {

    /**
     * 定时任务执行器
     * 提供延迟任务以及周期任务提交
     */
    private static ScheduledExecutorService monitorExecutor;

    /**
     * 动态线程池属性信息
     */
    private final DtpProperties dtpProperties;

    /**
     * 获得定时任务的结果或者延迟任务的结果，以及任务的状态
     */
    private ScheduledFuture<?> monitorFuture;

    /**
     * 监控间隔
     */
    private int monitorInterval;

    public DtpMonitor(DtpProperties dtpProperties) {
        this.dtpProperties = dtpProperties;
        EventBusManager.register(this);
    }

    /**
     * Subscribe 订阅模式
     * @param event
     */
    @Subscribe
    public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        // if monitorInterval is same as before, do nothing.
        // 如果监控间隔相同，不做刷新处理
        if (monitorInterval == dtpProperties.getMonitorInterval()) {
            return;
        }
        // cancel old monitor task.
        if (monitorFuture != null) {
            monitorFuture.cancel(true);
        }
        // Compatible with the springboot devtool restart scenario.
        if (monitorExecutor == null || monitorExecutor.isShutdown() || monitorExecutor.isTerminated()) {
            monitorExecutor = ThreadPoolCreator.newScheduledThreadPool("dtp-monitor", 1);
        }
        monitorInterval = dtpProperties.getMonitorInterval();
        monitorFuture = monitorExecutor.scheduleWithFixedDelay(this::run, 0, monitorInterval, TimeUnit.SECONDS);
    }

    private void run() {
        Set<String> executorNames = DtpRegistry.getAllExecutorNames();
        try {
            checkAlarm(executorNames);
            collectMetrics(executorNames);
        } catch (Exception e) {
            log.error("DynamicTp monitor, run error", e);
        }
    }

    private void checkAlarm(Set<String> executorNames) {
        executorNames.forEach(name -> {
            ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper(name);
            AlarmManager.checkAndTryAlarmAsync(wrapper, SCHEDULE_NOTIFY_ITEMS);
        });
        publishAlarmCheckEvent();
    }

    private void collectMetrics(Set<String> executorNames) {
        if (!dtpProperties.isEnabledCollect()) {
            return;
        }
        executorNames.forEach(x -> {
            ExecutorWrapper wrapper = DtpRegistry.getExecutorWrapper(x);
            // toMetrics 获得线程池指标信息
            doCollect(ExecutorConverter.toMetrics(wrapper));
        });
        publishCollectEvent();
    }

    private void doCollect(ThreadPoolStats threadPoolStats) {
        try {
            CollectorHandler.getInstance().collect(threadPoolStats, dtpProperties.getCollectorTypes());
        } catch (Exception e) {
            log.error("DynamicTp monitor, metrics collect error.", e);
        }
    }

    private void publishCollectEvent() {
        CollectEvent event = new CollectEvent(this, dtpProperties);
        EventBusManager.post(event);
    }

    private void publishAlarmCheckEvent() {
        AlarmCheckEvent event = new AlarmCheckEvent(this, dtpProperties);
        EventBusManager.post(event);
    }

    public static void destroy() {
        monitorExecutor.shutdownNow();
    }
}
