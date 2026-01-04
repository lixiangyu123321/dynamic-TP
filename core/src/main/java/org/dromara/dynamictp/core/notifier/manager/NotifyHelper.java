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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.entity.DtpExecutorProps;
import org.dromara.dynamictp.common.entity.NotifyItem;
import org.dromara.dynamictp.common.entity.NotifyPlatform;
import org.dromara.dynamictp.common.entity.TpExecutorProps;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.common.util.StreamUtil;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.support.ExecutorWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.dromara.dynamictp.common.em.NotifyItemEnum.CAPACITY;
import static org.dromara.dynamictp.common.em.NotifyItemEnum.LIVENESS;
import static org.dromara.dynamictp.common.em.NotifyItemEnum.QUEUE_TIMEOUT;
import static org.dromara.dynamictp.common.em.NotifyItemEnum.REJECT;
import static org.dromara.dynamictp.common.em.NotifyItemEnum.RUN_TIMEOUT;
import static org.dromara.dynamictp.common.entity.NotifyItem.mergeAllNotifyItems;

/**
 * NotifyHelper related
 * 通知助手类，提供通知相关的辅助方法
 * @author yanhom
 * @since 1.0.0
 */
@Slf4j
public class NotifyHelper {

    /**
     * 定义公共告警字段集合：所有告警类型都需要携带的通用字段
     */
    private static final List<String> COMMON_ALARM_KEYS = Lists.newArrayList("alarmType", "alarmValue");

    /**
     * 定义「活跃度告警」专属字段集合：仅LIVENESS类型告警需要展示的核心指标
     */
    private static final Set<String> LIVENESS_ALARM_KEYS = Sets.newHashSet(
            "corePoolSize", "maximumPoolSize", "poolSize", "activeCount");

    /**
     * 定义「队列容量利用率告警」专属字段集合：仅CAPACITY类型告警需要展示的核心指标
     */
    private static final Set<String> CAPACITY_ALARM_KEYS = Sets.newHashSet(
            "queueType", "queueCapacity", "queueSize", "queueRemaining");

    /**
     * 定义「任务拒绝告警」专属字段集合：仅REJECT类型告警需要展示的核心指标
     */
    private static final Set<String> REJECT_ALARM_KEYS = Sets.newHashSet("rejectType", "rejectCount");

    /**
     * 定义「任务运行超时告警」专属字段集合：仅RUN_TIMEOUT类型告警需要展示的核心指标
     */
    private static final Set<String> RUN_TIMEOUT_ALARM_KEYS = Sets.newHashSet("runTimeoutCount");

    /**
     * 定义「任务排队超时告警」专属字段集合：仅QUEUE_TIMEOUT类型告警需要展示的核心指标
     */
    private static final Set<String> QUEUE_TIMEOUT_ALARM_KEYS = Sets.newHashSet("queueTimeoutCount");

    /**
     * 定义全量告警字段集合：存放所有告警字段（公共字段+所有类型专属字段）
     */
    private static final Set<String> ALL_ALARM_KEYS;

    /**
     * 定义「告警类型-专属字段」映射Map：快速通过告警类型获取对应的专属字段
     */
    private static final Map<String, Set<String>> ALARM_KEYS = Maps.newHashMap();

    static {
        ALARM_KEYS.put(LIVENESS.name(), LIVENESS_ALARM_KEYS);
        ALARM_KEYS.put(CAPACITY.name(), CAPACITY_ALARM_KEYS);
        ALARM_KEYS.put(REJECT.name(), REJECT_ALARM_KEYS);
        ALARM_KEYS.put(RUN_TIMEOUT.name(), RUN_TIMEOUT_ALARM_KEYS);
        ALARM_KEYS.put(QUEUE_TIMEOUT.name(), QUEUE_TIMEOUT_ALARM_KEYS);

        ALL_ALARM_KEYS = ALARM_KEYS.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        ALL_ALARM_KEYS.addAll(COMMON_ALARM_KEYS);
    }

    private NotifyHelper() {
    }

    public static Set<String> getAllAlarmKeys() {
        return ALL_ALARM_KEYS;
    }

    /**
     * 基于类型获得字段
     * @param notifyItemEnum
     * @return
     */
    public static Set<String> getAlarmKeys(NotifyItemEnum notifyItemEnum) {
        val keys = ALARM_KEYS.get(notifyItemEnum.name());
        // 添加公共字段
        keys.addAll(COMMON_ALARM_KEYS);
        return keys;
    }

    public static Optional<NotifyItem> getNotifyItem(ExecutorWrapper executor, NotifyItemEnum notifyType) {
        if (CollectionUtils.isEmpty(executor.getNotifyItems())) {
            return Optional.empty();
        }
        // 找到第一个通知项
        return executor.getNotifyItems().stream()
                .filter(x -> notifyType.getValue().equalsIgnoreCase(x.getType()))
                .findFirst();
    }

    /**
     * 填充通知项的平台id
     * @param platformIds
     * @param platforms
     * @param notifyItems
     */
    public static void fillPlatforms(List<String> platformIds,
                                     List<NotifyPlatform> platforms,
                                     List<NotifyItem> notifyItems) {
        if (CollectionUtils.isEmpty(platforms) || CollectionUtils.isEmpty(notifyItems)) {
            return;
        }
        // 提取所有平台id
        List<String> globalPlatformIds = StreamUtil.fetchProperty(platforms, NotifyPlatform::getPlatformId);
        // notifyItem > executor > global
        notifyItems.forEach(n -> {
            if (CollectionUtils.isNotEmpty(n.getPlatformIds())) {
                // intersection of notifyItem and global
                // 通知项自己配置了平台id
                n.setPlatformIds((List<String>) CollectionUtils.intersection(globalPlatformIds, n.getPlatformIds()));
            } else if (CollectionUtils.isNotEmpty(platformIds)) {
                // 线程池配置了平台id
                n.setPlatformIds((List<String>) CollectionUtils.intersection(globalPlatformIds, platformIds));
            } else {
                // 二者均为设置，使用全局平台id
                n.setPlatformIds(globalPlatformIds);
            }
        });
    }

    /**
     * 基于平台id获得通知平台
     * @param platformId
     * @return
     */
    public static Optional<NotifyPlatform> getPlatform(String platformId) {
        Map<String, NotifyPlatform> platformMap = getAllPlatforms();
        return Optional.ofNullable(platformMap.get(platformId));
    }

    /**
     * 基于动态线程池配置参数获得通知平台
     * @return
     */
    public static Map<String, NotifyPlatform> getAllPlatforms() {
        val dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
        if (CollectionUtils.isEmpty(dtpProperties.getPlatforms())) {
            return Collections.emptyMap();
        }
        return StreamUtil.toMap(dtpProperties.getPlatforms(), NotifyPlatform::getPlatformId);
    }

    public static void initNotify(DtpExecutor executor) {
        val dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
        val platforms = dtpProperties.getPlatforms();
        // 没有告警平台配置，清空通知相关配置
        if (CollectionUtils.isEmpty(platforms)) {
            executor.setNotifyItems(Lists.newArrayList());
            executor.setPlatformIds(Lists.newArrayList());
            log.warn("DynamicTp notify, no notify platforms configured for [{}]", executor.getThreadPoolName());
            return;
        }
        // 线程池中无通知项配置
        if (CollectionUtils.isEmpty(executor.getNotifyItems())) {
            log.warn("DynamicTp notify, no notify items configured for [{}]", executor.getThreadPoolName());
            return;
        }
        // 填充告警平台id信息
        fillPlatforms(executor.getPlatformIds(), platforms, executor.getNotifyItems());
        // 初始化告警计数器和限流器
        AlarmManager.initAlarm(executor.getThreadPoolName(), executor.getNotifyItems());
    }

    public static void updateNotifyInfo(ExecutorWrapper executorWrapper,
                                        TpExecutorProps props,
                                        List<NotifyPlatform> platforms) {
        // 第一步：合并新的通知项配置（保证配置的完整性）
        val allNotifyItems = mergeAllNotifyItems(props.getNotifyItems());

        // 第二步：刷新通知配置（填充有效平台ID、初始化变更的告警组件）
        refreshNotify(executorWrapper.getThreadPoolName(),
                props.getPlatformIds(),
                platforms,
                executorWrapper.getNotifyItems(),
                allNotifyItems);

        // 第三步：更新线程池包装类的最新配置（覆盖旧配置）
        executorWrapper.setNotifyItems(allNotifyItems);
        executorWrapper.setPlatformIds(props.getPlatformIds());
        executorWrapper.setNotifyEnabled(props.isNotifyEnabled());
    }

    /**
     * 动态更新动态线程池的告警通知配置（支持运行时更新，无需重启应用）
     * @param executor  动态线程池对象
     * @param props  新的线程池配置属性
     * @param platforms  全局告警平台列表
     */
    public static void updateNotifyInfo(DtpExecutor executor,
                                        TpExecutorProps props,
                                        List<NotifyPlatform> platforms) {
        // 第一步：合并新的通知项配置（保证配置的完整性）
        val allNotifyItems = mergeAllNotifyItems(props.getNotifyItems());

        // 第二步：刷新通知配置（填充有效平台ID、初始化变更的告警组件）
        refreshNotify(executor.getThreadPoolName(),
                props.getPlatformIds(),
                platforms,
                executor.getNotifyItems(),
                allNotifyItems);

        // 第三步：更新动态线程池的最新配置（覆盖旧配置）
        // 更新通知项列表
        executor.setNotifyItems(allNotifyItems);
        // 更新平台ID列表
        executor.setPlatformIds(props.getPlatformIds());
        // 更新告警启用状态
        executor.setNotifyEnabled(props.isNotifyEnabled());
    }

    /**
     * 私有辅助方法：刷新通知配置，处理新旧配置差异，初始化相关告警组件
     * @param poolName  线程池名称
     * @param platformIds  新的线程池平台ID列表
     * @param platforms  全局告警平台列表
     * @param oldNotifyItems  旧的通知项配置列表
     * @param newNotifyItems  新的通知项配置列表
     */
    private static void refreshNotify(String poolName,
                                      List<String> platformIds,
                                      List<NotifyPlatform> platforms,
                                      List<NotifyItem> oldNotifyItems,
                                      List<NotifyItem> newNotifyItems) {
        // 第一步：为新通知项填充有效的告警平台ID列表（保证平台ID有效）
        fillPlatforms(platformIds, platforms, newNotifyItems);

        // 第二步：将旧通知项转换为「通知项类型 -> 通知项」的Map，便于快速对比差异
        Map<String, NotifyItem> oldNotifyItemMap = StreamUtil.toMap(oldNotifyItems, NotifyItem::getType);

        // 第三步：遍历新通知项，处理新旧配置差异
        newNotifyItems.forEach(x -> {
            // 从旧配置中获取同类型的通知项
            NotifyItem oldNotifyItem = oldNotifyItemMap.get(x.getType());

            // 场景1：新通知项（旧配置中无对应类型），直接初始化告警计数器和限流器
            if (Objects.isNull(oldNotifyItem)) {
                AlarmManager.initAlarm(poolName, x);
                return;
            }

            // 场景2：已有通知项，告警周期变更，重新初始化告警计数器
            if (oldNotifyItem.getPeriod() != x.getPeriod()) {
                AlarmManager.initAlarmCounter(poolName, x);
            }

            // 场景3：已有通知项，静默周期变更，重新初始化告警限流器
            if (oldNotifyItem.getSilencePeriod() != x.getSilencePeriod()) {
                AlarmManager.initAlarmLimiter(poolName, x);
            }
        });
    }
}
