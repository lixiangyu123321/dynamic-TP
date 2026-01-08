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

import cn.hutool.core.util.ReflectUtil;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.entity.DtpExecutorProps;
import org.dromara.dynamictp.common.entity.TpExecutorProps;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.common.properties.DtpProperties;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.AWARE_NAMES;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.EXECUTORS_CONFIG_PREFIX;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.GLOBAL_CONFIG_PREFIX;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.MAIN_PROPERTIES_PREFIX;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.NOTIFY_ITEMS;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.PLATFORM_IDS;
import static org.dromara.dynamictp.common.constant.DynamicTpConst.PLUGIN_NAMES;

/**
 * XXX 为所有的线程池设置兜底配置val globalExecutorProps = dtpProperties.getGlobalExecutorProps();
 * 动态线程池（DTP）配置绑定工具类
 * 核心功能：将全局配置（globalExecutorProps）兜底赋值给未显式配置的线程池属性，实现配置的“全局默认+局部覆盖”
 *
 * @author yanhom
 * @since 1.1.9
 */
@SuppressWarnings("unchecked")
public final class DtpPropertiesBinderUtil {


    private DtpPropertiesBinderUtil() {
    }

    /**
     * 核心入口方法：将全局环境变量/配置兜底赋值给DTP配置对象
     * 逻辑：如果线程池属性未显式配置，则使用全局配置（globalExecutorProps）填充
     * @param source 环境变量源（可能是Map/上下文环境）
     * @param dtpProperties 待赋值的DTP核心配置对象
     */
    public static void tryResetWithGlobalConfig(Object source, DtpProperties dtpProperties) {
        // 前置校验：如果全局配置为空，直接返回（无兜底值可赋值）
        if (Objects.isNull(dtpProperties.getGlobalExecutorProps())) {
            return;
        }
        // 1. 处理自定义线程池配置（executors列表）
        if (CollectionUtils.isNotEmpty(dtpProperties.getExecutors())) {
            tryResetCusExecutors(dtpProperties, source);
        }
        // 2. 处理适配器线程池配置（如TpExecutorProps类型的字段/列表）
        tryResetAdapterExecutors(dtpProperties, source);
    }

    /**
     * 处理自定义线程池配置（dtpProperties.executors列表）的全局兜底赋值
     * @param dtpProperties DTP核心配置对象
     * @param source 环境变量源
     */
    private static void tryResetCusExecutors(DtpProperties dtpProperties, Object source) {
        // 获取DtpExecutorProps类的所有字段（包括私有字段）
        val dtpPropsFields = ReflectionUtil.getAllFields(DtpExecutorProps.class);
        // 获取全局线程池配置（兜底值来源）
        val globalExecutorProps = dtpProperties.getGlobalExecutorProps();
        // XXX 数组存储索引（lambda中无法修改基本类型变量，用数组替代）
        // XXX 处理lambda中无法修改基本类型变量的问题
        int[] idx = {0};

        // 遍历每个自定义线程池配置
        dtpProperties.getExecutors().forEach(executor -> {
            // 第一步：处理基础字段（如核心线程数、最大线程数等）的兜底赋值
            dtpPropsFields.forEach(field -> {
                // 拼接配置key：executors[0].corePoolSize（示例）
                String propKey = EXECUTORS_CONFIG_PREFIX + idx[0] + "]." + field.getName();
                setBasicField(source, field, executor, propKey);
            });
            // 第二步：处理集合字段（如taskWrapperNames、platformIds等）的兜底赋值
            // executorFieldNamePrefix = “dynamictp.executors[i]”
            String executorFieldNamePrefix = EXECUTORS_CONFIG_PREFIX + idx[0] + "]";
            setCollectionField(source, globalExecutorProps, executor, executorFieldNamePrefix);
            // 索引自增（处理下一个线程池）
            idx[0]++;
        });
    }

    /**
     * 处理适配器线程池配置的全局兜底赋值（覆盖单个TpExecutorProps字段/TPExecutorProps列表字段）
     * @param dtpProperties DTP核心配置对象
     * @param source 环境变量源
     */
    private static void tryResetAdapterExecutors(DtpProperties dtpProperties, Object source) {
        // XXX DtpProperties 汇总所有线程池的配置 + 兜底配置信息
        // XXX TpProperties 通用线程池配置
        // XXX DtpProperties 动态线程池配置
        // 获取DtpProperties类的所有字段（待遍历的适配器字段）
        val dtpPropertiesFields = ReflectionUtil.getAllFields(DtpProperties.class);
        // 获取TpExecutorProps类的所有字段（基础字段）
        val tpExecutorPropFields = ReflectionUtil.getAllFields(TpExecutorProps.class);
        // 获取全局线程池配置（兜底值来源）  DtpExecutorProps extends TpExecutorProps
        val globalExecutorProps = dtpProperties.getGlobalExecutorProps();

        // 遍历DtpProperties的每个字段（寻找TpExecutorProps类型的配置）
        dtpPropertiesFields.forEach(dtpPropertiesField -> {
            // 通过反射获取字段值（当前适配器配置对象）
            // XXX 获得全局配置中当前需要配置的线程池
            val candidateExecutor = ReflectUtil.getFieldValue(dtpProperties, dtpPropertiesField);
            // 字段值为空，跳过
            if (Objects.isNull(candidateExecutor)) {
                return;
            }
            // 获取字段名（用于拼接配置key）
            String candidateExecutorFieldName = dtpPropertiesField.getName();

            // 场景1：字段类型是TpExecutorProps（单个线程池配置）
            // XXX
            if (dtpPropertiesField.getType().isAssignableFrom(TpExecutorProps.class)) {
                // 处理基础字段兜底赋值
                tpExecutorPropFields.forEach(field -> setBasicField(source, field, candidateExecutorFieldName, candidateExecutor));
                // 拼接集合字段的配置前缀：mainProperties.executorName（示例）
                String executorFieldNamePrefix = MAIN_PROPERTIES_PREFIX + "." + dtpPropertiesField.getName();
                // 处理集合字段兜底赋值
                setCollectionField(source, globalExecutorProps, candidateExecutor, executorFieldNamePrefix);
            }
            // 场景2：字段是泛型类型（如List<TpExecutorProps>）
            // TODO 这里就看不懂了
            else if (dtpPropertiesField.getGenericType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) dtpPropertiesField.getGenericType();
                // 获取泛型实际参数类型（如List<TpExecutorProps>中的TpExecutorProps）
                Type[] argTypes = paramType.getActualTypeArguments();
                // 校验：泛型参数只有1个，且是TpExecutorProps类型
                if (argTypes.length == 1 && argTypes[0].equals(TpExecutorProps.class)) {
                    // 强转为TpExecutorProps列表
                    // XXX 如果是线程池数组就强转
                    List<TpExecutorProps> executors = (List<TpExecutorProps>) candidateExecutor;
                    // 列表为空，跳过
                    if (CollectionUtils.isEmpty(executors)) {
                        return;
                    }
                    // 索引数组（lambda中使用）
                    int[] idx = {0};
                    // 遍历每个TpExecutorProps对象
                    executors.forEach(executor -> {
                        // 处理基础字段兜底赋值（带索引）
                        tpExecutorPropFields.forEach(field -> setBasicField(source, field, candidateExecutorFieldName, executor, idx));
                        // 拼接集合字段的配置前缀：mainProperties.executorList[0]（示例）
                        String executorFieldNamePrefix = MAIN_PROPERTIES_PREFIX + "." + candidateExecutorFieldName + "[" + idx[0] + "]";
                        // 处理集合字段兜底赋值
                        setCollectionField(source, globalExecutorProps, executor, executorFieldNamePrefix);
                        idx[0]++;
                    });
                }
            }
        });
    }

    /**
     * 从环境变量源中获取指定key的配置值
     * @param key 配置key（如global.corePoolSize）
     * @param environment 环境变量源（Map/上下文环境）
     * @return 配置值（null表示未配置）
     */
    private static Object getProperty(String key, Object environment) {
        // 场景1：环境源是Map（如测试场景/本地配置）
        if (environment instanceof Map) {
            Map<?, Object> properties = (Map<?, Object>) environment;
            return properties.get(key);
        }
        // 场景2：环境源是上下文（如Spring环境），通过工具类获取
        else {
            return ContextManagerHelper.getEnvironmentProperty(key, environment);
        }
    }

    /**
     * 重载方法：处理带索引的基础字段兜底赋值（用于列表场景）
     * @param source 环境变量源
     * @param field 待赋值的字段（如corePoolSize）
     * @param executorFieldName 线程池字段名（如executorList）
     * @param executor 待赋值的线程池配置对象
     * @param idx 索引数组（如[0]）
     */
    private static void setBasicField(Object source, Field field, String executorFieldName, Object executor, int[] idx) {
        // 拼接配置key：mainProperties.executorList[0].corePoolSize（示例）
        String propKey = MAIN_PROPERTIES_PREFIX + "." + executorFieldName + "[" + idx[0] + "]." + field.getName();
        setBasicField(source, field, executor, propKey);
    }

    /**
     * 重载方法：处理单个线程池配置的基础字段兜底赋值
     * @param source 环境变量源
     * @param field 待赋值的字段
     * @param executorFieldName 线程池字段名
     * @param executor 待赋值的线程池配置对象
     */
    private static void setBasicField(Object source, Field field, String executorFieldName, Object executor) {
        // 拼接配置key：mainProperties.executorName.corePoolSize（示例）
        String propKey = MAIN_PROPERTIES_PREFIX + "." + executorFieldName + "." + field.getName();
        setBasicField(source, field, executor, propKey);
    }

    /**
     * 核心方法：基础字段的全局兜底赋值逻辑
     * 逻辑：如果线程池字段未显式配置 → 使用全局配置赋值
     * @param source 环境变量源
     * @param field 待赋值的字段（如corePoolSize）
     * @param executor 待赋值的线程池配置对象
     * @param propKey 字段对应的配置key（如executors[0].corePoolSize）
     */
    private static void setBasicField(Object source, Field field, Object executor, String propKey) {
        // 步骤1：获取字段的显式配置值（如executors[0].corePoolSize）
        Object propVal = getProperty(propKey, source);
        // 如果显式配置了值，直接返回（局部配置覆盖全局）
        if (Objects.nonNull(propVal)) {
            return;
        }
        // 步骤2：获取该字段的全局配置值（如global.corePoolSize）
        Object globalFieldVal = getProperty(GLOBAL_CONFIG_PREFIX + field.getName(), source);
        // 全局配置也为空，返回（无值可赋值）
        if (Objects.isNull(globalFieldVal)) {
            return;
        }
        // 步骤3：通过反射将全局配置值赋值给线程池配置对象的字段
        ReflectUtil.setFieldValue(executor, field.getName(), globalFieldVal);
    }

    /**
     * XXX 如果环境未配置，通过全局配置兜底
     * 集合字段的全局兜底赋值逻辑（如taskWrapperNames、platformIds等）
     * 逻辑：如果集合字段未显式配置 → 使用全局配置的集合赋值
     * @param source 环境变量源
     * @param globalExecutorProps 全局配置对象
     * @param executor 待赋值的线程池配置对象
     * @param prefix 配置前缀（如executors[0]）
     */
    private static void setCollectionField(Object source, DtpExecutorProps globalExecutorProps, Object executor, String prefix) {
        // 1. taskWrapperNames：任务包装器名称列表
        if (isNotContains(prefix + ".taskWrapperNames[0]", source) &&
                CollectionUtils.isNotEmpty(globalExecutorProps.getTaskWrapperNames())) {
            ReflectUtil.setFieldValue(executor, "taskWrapperNames", globalExecutorProps.getTaskWrapperNames());
        }
        // 2. platformIds：平台ID列表
        if (isNotContains(prefix + ".platformIds[0]", source) &&
                CollectionUtils.isNotEmpty(globalExecutorProps.getPlatformIds())) {
            ReflectUtil.setFieldValue(executor, PLATFORM_IDS, globalExecutorProps.getPlatformIds());
        }
        // 3. notifyItems：通知项列表（如告警阈值）
        if (isNotContains(prefix + ".notifyItems[0].type", source) &&
                CollectionUtils.isNotEmpty(globalExecutorProps.getNotifyItems())) {
            ReflectUtil.setFieldValue(executor, NOTIFY_ITEMS, globalExecutorProps.getNotifyItems());
        }
        // 4. awareNames：感知器名称列表
        if (isNotContains(prefix + ".awareNames[0]", source) &&
                CollectionUtils.isNotEmpty(globalExecutorProps.getAwareNames())) {
            ReflectUtil.setFieldValue(executor, AWARE_NAMES, globalExecutorProps.getAwareNames());
        }
        // 5. pluginNames：插件名称列表（捕获异常，避免单个字段赋值失败影响整体）
        try {
            if (isNotContains(prefix + ".pluginNames[0]", source) &&
                    CollectionUtils.isNotEmpty(globalExecutorProps.getPluginNames())) {
                ReflectUtil.setFieldValue(executor, PLUGIN_NAMES, globalExecutorProps.getPluginNames());
            }
        } catch (Exception e) {
            // 忽略异常（插件字段非核心，赋值失败不影响主线程池配置）
        }
    }

    /**
     * 反向判断：环境源中是否不包含指定配置key
     * @param key 配置key（如executors[0].taskWrapperNames[0]）
     * @param environment 环境变量源
     * @return true=不包含，false=包含
     */
    private static boolean isNotContains(String key, Object environment) {
        return !contains(key, environment);
    }

    /**
     * 判断环境源中是否包含指定配置key（用于集合字段的显式配置校验）
     * @param key 配置key（如executors[0].taskWrapperNames[0]）
     * @param environment 环境变量源
     * @return true=包含（有显式配置），false=不包含（需兜底）
     */
    private static boolean contains(String key, Object environment) {
        // 场景1：环境源是Map，检查key是否存在
        if (environment instanceof Map) {
            Map<?, Object> properties = (Map<?, Object>) environment;
            return properties.containsKey(key);
        }
        // 场景2：环境源是上下文，检查配置值是否非空
        else {
            return StringUtils.isNotBlank(ContextManagerHelper.getEnvironmentProperty(key, environment));
        }
    }
}
