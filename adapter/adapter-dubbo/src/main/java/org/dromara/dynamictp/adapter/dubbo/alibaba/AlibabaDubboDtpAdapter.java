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

package org.dromara.dynamictp.adapter.dubbo.alibaba;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.adapter.common.AbstractDtpAdapter;
import org.dromara.dynamictp.common.event.CustomContextRefreshedEvent;
import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.jvmti.JVMTI;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.dubbo.common.Constants.EXECUTOR_SERVICE_COMPONENT_KEY;

/**
 * AlibabaDubboDtpAdapter related
 * XXX InitializingBean属性注入后的初始化
 * @author yanhom
 * @since 1.0.6
 */
@SuppressWarnings("all")
@Slf4j
public class AlibabaDubboDtpAdapter extends AbstractDtpAdapter implements InitializingBean {

    /**
     * 线程池名称
     */
    private static final String TP_PREFIX = "dubboTp";

    /**
     * 相关线程池字段
     */
    private static final String EXECUTOR_FIELD = "executor";

    /**
     * 初始化完成标识
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * XXX 父类 AbstractDtpAdapter 原本会在 ContextRefreshedEvent（Spring 容器刷新完成）时触发初始化
     * XXX 但阿里 Dubbo 版本的线程池初始化时机晚于 Spring 容器刷新，此时获取不到线程池，因此禁用该方法，改在 afterPropertiesSet 中处理。
     * @param event
     */
    @Subscribe
    @Override
    public synchronized void onContextRefreshedEvent(CustomContextRefreshedEvent event) {
        // do nothing, initialize in afterPropertiesSet
    }

    /**
     * XXX 解决「Dubbo 线程池初始化时机晚」的痛点
     * TODO Spring容器刷新和初始化哪个晚
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        // 从ApplicationReadyEvent改为ContextRefreshedEvent后，
        // 启动时无法dubbo获取线程池，这里直接每隔1s轮循，直至成功初始化线程池
        // XXX 阿里 Dubbo 启动时，会先初始化 Spring 容器，再初始化 Dubbo 服务暴露和线程池（WrappedChannelHandler 中的 executor）。如果在 Spring 容器刷新时直接获取 Dubbo 线程池，会拿到 null；
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (!registered.get()) {
                try {
                    Thread.sleep(1000);
                    DtpProperties dtpProperties = ContextManagerHelper.getBean(DtpProperties.class);
                    initialize();
                    afterInitialize();
                    refresh(dtpProperties);
                    log.info("DynamicTp adapter, {} init end, executors {}", getTpPrefix(), executors.keySet());
                } catch (Throwable e) { }
            }
        });
        executor.shutdown();
    }

    @Override
    public void refresh(DtpProperties dtpProperties) {
        refresh(dtpProperties.getDubboTp(), dtpProperties.getPlatforms());
    }

    @Override
    protected void initialize() {
        super.initialize();
        // XXX JVMTI 是 JVM 工具接口，这里用于获取 JVM 中所有 WrappedChannelHandler 实例（Dubbo 的网络处理器，每个端口对应一个实例），相比普通反射更精准；
        val handlers = JVMTI.getInstances(WrappedChannelHandler.class);
        if (CollectionUtils.isNotEmpty(handlers) && registered.compareAndSet(false, true)) {
            DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
            handlers.forEach(handler -> {
                val executor = handler.getExecutor();
                if (executor instanceof ThreadPoolExecutor) {
                    String port = String.valueOf(handler.getUrl().getPort());
                    String tpName = genTpName(port);
                    enhanceOriginExecutor(tpName, (ThreadPoolExecutor) executor, EXECUTOR_FIELD, handler);
                    dataStore.put(EXECUTOR_SERVICE_COMPONENT_KEY, port, handler.getExecutor());
                }
            });
        }
    }

    @Override
    protected String getTpPrefix() {
        return TP_PREFIX;
    }

    private String genTpName(String port) {
        return TP_PREFIX + "#" + port;
    }
}
