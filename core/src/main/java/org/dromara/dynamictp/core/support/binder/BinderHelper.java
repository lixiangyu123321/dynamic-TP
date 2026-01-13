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

package org.dromara.dynamictp.core.support.binder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.pattern.singleton.Singleton;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.common.util.ExtensionServiceLoader;

import java.util.Map;
import java.util.Objects;

/**
 * BinderHelper related
 * XXX 将Map类型/environment中信息绑定到dtpProperties中
 * XXX 绑定器延迟加载，只有当第一次getBinder发现没有的时候，才从全局单例池中加载
 * @author dragon-zhang
 * @since 1.1.4
 */
@Slf4j
public class BinderHelper {

    private BinderHelper() { }

    /**
     * 看看有没有加载，没有加载通过SPI机制加载
     * ExtensionService相当于一层缓存，顶层还是SPI机制（ServiceLoader）
     * @return
     */
    private static PropertiesBinder getBinder() {
        // 从单例池中获得PropertiesBinder对应的单例对象
        PropertiesBinder binder = Singleton.INST.get(PropertiesBinder.class);
        if (Objects.nonNull(binder)) {
            return binder;
        }
        // XXX 基于SPI机制加载
        final PropertiesBinder loadedFirstBinder = ExtensionServiceLoader.getFirst(PropertiesBinder.class);
        if (Objects.isNull(loadedFirstBinder)) {
            log.error("DynamicTp refresh, no SPI for org.dromara.dynamictp.core.support.binder.PropertiesBinder.");
            return null;
        }
        Singleton.INST.single(PropertiesBinder.class, loadedFirstBinder);
        return loadedFirstBinder;
    }

    public static void bindDtpProperties(Map<?, Object> properties, DtpProperties dtpProperties) {
        final PropertiesBinder binder = getBinder();
        if (Objects.isNull(binder)) {
            return;
        }
        // binder用于将Map<Object, Object> 绑定到dtpProperties上
        // 并且提供属性绑定前后的钩子方法
        binder.bindDtpProperties(properties, dtpProperties);
    }

    public static void bindDtpProperties(Object environment, DtpProperties dtpProperties) {
        final PropertiesBinder binder = getBinder();
        if (Objects.isNull(binder)) {
            return;
        }
        binder.bindDtpProperties(environment, dtpProperties);
    }
}
