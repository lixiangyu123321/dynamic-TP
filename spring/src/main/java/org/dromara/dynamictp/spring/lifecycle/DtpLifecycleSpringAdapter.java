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

package org.dromara.dynamictp.spring.lifecycle;

import org.dromara.dynamictp.core.lifecycle.LifeCycleManagement;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapts LifeCycleManagement to Spring's SmartLifecycle interface.
 * XXX SmartLifecycle 在 原有Lifecycle的基础上做了扩展，stop方法在stop之后会调用回调函数
 * XXX Phase 此接口为标识接口， 同于标识生命周期的接口，需自定义实现
 * XXX 总之这里实现的是spring的接口
 * @author vzer200
 * @since 1.2.0
 */
public class DtpLifecycleSpringAdapter implements SmartLifecycle {

    /**
     * XXX 生命周期管理器
     * XXX 适配（委托）模式，将stop/start委托给生命周期管理器执行
     */
    private final LifeCycleManagement lifeCycleManagement;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DtpLifecycleSpringAdapter(LifeCycleManagement lifeCycleManagement) {
        this.lifeCycleManagement = lifeCycleManagement;
    }

    @Override
    public void start() {
        if (this.running.compareAndSet(false, true)) {
            lifeCycleManagement.start();
        }
    }

    @Override
    public void stop() {
        if (this.running.compareAndSet(true, false)) {
            lifeCycleManagement.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public void stop(Runnable callback) {
        if (this.running.compareAndSet(true, false)) {
            lifeCycleManagement.stop(callback);
        }
    }

    /**
     * Compatible with lower versions of spring.
     *
     * @return isAutoStartup
     */
    @Override
    public boolean isAutoStartup() {
        return lifeCycleManagement.isAutoStartup();
    }

    /**
     * Compatible with lower versions of spring.
     *
     * @return phase
     */
    @Override
    public int getPhase() {
        return lifeCycleManagement.getPhase();
    }
}
