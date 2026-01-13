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

package org.dromara.dynamictp.starter.nacos.refresher;

import com.alibaba.nacos.spring.context.event.config.NacosConfigEvent;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.spring.AbstractSpringRefresher;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

/**
 * NacosRefresher related
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class NacosRefresher extends AbstractSpringRefresher implements SmartApplicationListener {

    /**
     * 要更新的数据
     * @param dtpProperties 需要更新的全局配置数据，XXX 一般是唯一的
     */
    public NacosRefresher(DtpProperties dtpProperties) {
        super(dtpProperties);
    }

    /**
     * 支持监听的事件
     * @param eventType XXX 事件类型
     * @return
     */
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return NacosConfigEvent.class.isAssignableFrom(eventType);
    }

    /**
     * TODO 监听Nacos事件，基于environment进行更新，那监听nacos配置刷新并将更新后配置刷新到environment中的操作在哪里
     * Spring的相关事件监听
     * @param event Spring事件
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof NacosConfigEvent) {
            refresh(environment);
        }
    }
}
