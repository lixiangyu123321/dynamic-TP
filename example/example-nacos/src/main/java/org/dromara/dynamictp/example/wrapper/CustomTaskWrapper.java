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

package org.dromara.dynamictp.example.wrapper;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;

/**
 * CustomTaskWrapper related
 * 任务包装器，用于增强任务
 * XXX 在DtpPostProcessor中初次调用TaskWrappers.getInstance().register(taskWrapper);
 * XXX 由于相关的线程池Bean中有对应类型的线程池，所以会调用TaskWrappers.getInstance().register(taskWrapper);
 * XXX 从而会调用SPI机制发现扩展的增强类，加入到全局增强中TaskWrappers，真正增强发生在提交任务时DtpExecutor的execute时
 * @Override
 *     public void execute(Runnable command) {
 *         command = getEnhancedTask(command);
 *         AwareManager.execute(this, command);
 *         super.execute(command);
 *     }
 * @author yanhom
 * @since 1.1.0
 */
@Slf4j
public class CustomTaskWrapper implements TaskWrapper {

    @Override
    public String name() {
        return "custom";
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return new MyRunnable(runnable);
    }

    /**
     * 任务包装任务
     */
    public static class MyRunnable implements Runnable {

        private final Runnable runnable;

        public MyRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            log.info("before run");
            runnable.run();
            log.info("after run");
        }
    }
}
