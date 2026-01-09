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

package org.dromara.dynamictp.common.entity;

import lombok.Data;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.dromara.dynamictp.common.util.DefaultValueUtil.setIfZero;

/**
 * NotifyItem related
 * 通知项 XXX 需要触发通知的告警类型
 * @author yanhom
 * @since 1.0.0
 **/
@Data
public class NotifyItem {

    /**
     * 该告警项是否启用
     * 默认值：true（启用）
     */
    private boolean enabled = true;

    /**
     * 告警类型标识
     * 对应NotifyItemEnum枚举的字符串值（如REJECT、RUN_TIMEOUT等）
     */
    private String type;

    /**
     * 告警触发阈值（百分比/数量）
     * 不同告警类型含义不同，如活跃度/容量告警为百分比，超时/拒绝告警无实际意义
     */
    private int threshold;

    /**
     * 告警触发次数阈值
     * 统计周期内累计达到该次数则触发告警
     */
    private int count;

    /**
     * 告警统计周期（单位：秒）
     * 默认值：120秒（2分钟）
     */
    private int period = 120;

    /**
     * 告警静默期（单位：秒）
     * 触发告警后，静默期内不再重复推送同类型告警，默认值：120秒
     */
    private int silencePeriod = 120;

    /**
     * 集群告警限流次数
     * 分布式集群中，同类型告警最多由N个节点推送，默认值：1
     */
    private int clusterLimit = 1;

    /**
     * 告警接收人
     * 格式：手机号/邮箱，多个用逗号分隔
     */
    private String receivers;

    /**
     * 告警推送平台ID列表
     * 如钉钉、企业微信等平台的唯一标识
     */
    private List<String> platformIds;

    /**
     * 合并用户配置的告警项与默认告警项
     * 逻辑：用户配置的保留，未配置的补充默认值，保证告警项完整性
     * @param source 用户自定义配置的告警项列表
     * @return 合并后的完整告警项列表
     */
    public static List<NotifyItem> mergeAllNotifyItems(List<NotifyItem> source) {
        // 1. 如果用户未配置任何告警项，返回全量默认告警项
        if (CollectionUtils.isEmpty(source)) {
            return getAllNotifyItems();
        }
        // 2. 提取用户配置的所有告警类型（用于过滤默认项）
        val configuredTypes = source.stream().map(NotifyItem::getType).collect(toList());
        // 3. 筛选默认告警项中用户未配置的类型（补充默认配置）
        val defaultItems = getAllNotifyItems().stream()
                .filter(t -> !StringUtil.containsIgnoreCase(t.getType(), configuredTypes))
                .collect(Collectors.toList());
        // 4. 初始化合并后的列表（初始容量6，对应默认6种告警项）
        List<NotifyItem> notifyItems = new ArrayList<>(6);
        // 5. 先添加用户未配置的默认告警项
        notifyItems.addAll(defaultItems);
        // 6. 为用户配置的告警项填充默认值（避免配置缺失）
        populateDefaultValues(source);
        // 7. 再添加用户自定义配置的告警项
        notifyItems.addAll(source);
        // 8. 返回合并后的完整列表
        return notifyItems;
    }

    /**
     * 获取全量默认告警项列表
     * 包含：活跃度、配置变更、容量、拒绝、运行超时、队列超时6种告警
     * @return 全量默认告警项列表
     */
    public static List<NotifyItem> getAllNotifyItems() {
        // 初始化「任务拒绝」告警项
        NotifyItem rejectNotify = new NotifyItem();
        // 设置告警类型为任务拒绝（对应NotifyItemEnum.REJECT的值）
        rejectNotify.setType(NotifyItemEnum.REJECT.getValue());

        // 初始化「任务运行超时」告警项
        NotifyItem runTimeoutNotify = new NotifyItem();
        // 设置告警类型为运行超时（对应NotifyItemEnum.RUN_TIMEOUT的值）
        runTimeoutNotify.setType(NotifyItemEnum.RUN_TIMEOUT.getValue());

        // 初始化「任务队列超时」告警项
        NotifyItem queueTimeoutNotify = new NotifyItem();
        // 设置告警类型为队列超时（对应NotifyItemEnum.QUEUE_TIMEOUT的值）
        queueTimeoutNotify.setType(NotifyItemEnum.QUEUE_TIMEOUT.getValue());

        // 初始化全量告警项列表（初始容量6）
        List<NotifyItem> notifyItems = new ArrayList<>(6);
        // 添加基础告警项（活跃度、配置变更、容量）
        notifyItems.addAll(getSimpleNotifyItems());
        // 添加扩展告警项（拒绝、运行超时、队列超时）
        notifyItems.add(rejectNotify);
        notifyItems.add(runTimeoutNotify);
        notifyItems.add(queueTimeoutNotify);

        // 为所有默认告警项填充类型对应的默认值
        populateDefaultValues(notifyItems);
        // 返回全量默认告警项
        return notifyItems;
    }

    /**
     * 获取基础默认告警项列表（简单告警类型）
     * 包含：活跃度、配置变更、容量3种核心告警
     * @return 基础默认告警项列表
     */
    public static List<NotifyItem> getSimpleNotifyItems() {
        // 初始化「配置变更」告警项
        NotifyItem changeNotify = new NotifyItem();
        // 设置告警类型为配置变更（对应NotifyItemEnum.CHANGE的值）
        changeNotify.setType(NotifyItemEnum.CHANGE.getValue());
        // 配置变更告警静默期特殊设置：1秒（高频变更仅短时间静默）
        changeNotify.setSilencePeriod(1);

        // 初始化「线程池活跃度」告警项
        NotifyItem livenessNotify = new NotifyItem();
        // 设置告警类型为活跃度（对应NotifyItemEnum.LIVENESS的值）
        livenessNotify.setType(NotifyItemEnum.LIVENESS.getValue());

        // 初始化「队列容量」告警项
        NotifyItem capacityNotify = new NotifyItem();
        // 设置告警类型为容量（对应NotifyItemEnum.CAPACITY的值）
        capacityNotify.setType(NotifyItemEnum.CAPACITY.getValue());

        // 初始化基础告警项列表（初始容量3）
        List<NotifyItem> notifyItems = new ArrayList<>(3);
        // 添加基础告警项
        notifyItems.add(livenessNotify);
        notifyItems.add(changeNotify);
        notifyItems.add(capacityNotify);

        // 返回基础告警项列表
        return notifyItems;
    }

    /**
     * 为告警项填充类型对应的默认值
     * 不同告警类型设置差异化的count/threshold默认值
     * @param source 需要填充默认值的告警项列表
     */
    private static void populateDefaultValues(List<NotifyItem> source) {
        // 空列表直接返回，避免空指针
        if (CollectionUtils.isEmpty(source)) {
            return;
        }
        // 遍历每个告警项，按类型填充默认值
        for (NotifyItem item : source) {
            // 根据告警类型字符串获取对应的枚举实例
            NotifyItemEnum itemEnum = NotifyItemEnum.of(item.getType());
            // 按枚举类型分支处理（空值会抛异常，保证类型合法性）
            switch (Objects.requireNonNull(itemEnum)) {
                case REJECT:
                    // 任务拒绝告警：count默认值1（未配置时填充）
                    setIfZero(item::getCount, item::setCount, 1);
                    break;
                case RUN_TIMEOUT:
                case QUEUE_TIMEOUT:
                    // 超时类告警：count默认值10（未配置时填充）
                    setIfZero(item::getCount, item::setCount, 10);
                    break;
                case LIVENESS:
                case CAPACITY:
                    // 活跃度/容量告警：threshold默认70，count默认1
                    setIfZero(item::getThreshold, item::setThreshold, 70);
                    setIfZero(item::getCount, item::setCount, 1);
                    break;
                default:
                    // 其他类型（如配置变更）无需填充默认值
                    break;
            }
        }
    }
}