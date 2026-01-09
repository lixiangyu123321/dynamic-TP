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

package org.dromara.dynamictp.core.support.task.wrapper;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.util.ExtensionServiceLoader;
import org.dromara.dynamictp.common.util.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * TaskWrapperHolder related
 * XXX 所有的任务包装器，全局的任务包装器
 * @author yanhom
 * @since 1.0.4
 **/
public class TaskWrappers {


    /**
     * 缓存任务包装器
     */
    private static final List<TaskWrapper> TASK_WRAPPERS = Lists.newArrayList();

    /**
     * SPI发现任务包装器
     */
    private TaskWrappers() {
        List<TaskWrapper> loadedWrappers = ExtensionServiceLoader.get(TaskWrapper.class);
        if (CollectionUtils.isNotEmpty(loadedWrappers)) {
            TASK_WRAPPERS.addAll(loadedWrappers);
        }

        // 默认的TTL任务包装器，和MDC任务包装器，包装可过期的任务和含MDC上下文的任务
        TASK_WRAPPERS.add(new TtlTaskWrapper());
        TASK_WRAPPERS.add(new MdcTaskWrapper());
    }

    public List<TaskWrapper> getByNames(Set<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return Collections.emptyList();
        }

        return TASK_WRAPPERS.stream().filter(t -> StringUtil.containsIgnoreCase(t.name(), names)).collect(toList());
    }

    /**
     * 手动注册任务封装器
     * @param taskWrapper
     */
    public static void register(TaskWrapper taskWrapper) {
        Set<String> names = TASK_WRAPPERS.stream().map(TaskWrapper::name).collect(Collectors.toSet());
        if (names.contains(taskWrapper.name())) {
            return;
        }
        TASK_WRAPPERS.add(taskWrapper);
    }

    public static TaskWrappers getInstance() {
        return TaskWrappersHolder.INSTANCE;
    }

    /**
     * 静态内部类实现的单例模式，懒汉式的加载，外部类的加载不会触发内部静态类的加载
     */
    private static class TaskWrappersHolder {
        private static final TaskWrappers INSTANCE = new TaskWrappers();
    }
}
