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

package org.dromara.dynamictp.core.timer;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dromara.dynamictp.common.timer.Timeout;
import org.dromara.dynamictp.common.timer.TimerTask;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.task.runnable.DtpRunnable;

import java.util.Objects;

/**
 * AbstractTimeoutTimerTask related
 * 抽象超时定时任务基类
 * @author yanhom
 * @since 1.1.4
 **/
public abstract class AbstractTimeoutTimerTask implements TimerTask {

    protected final ExecutorWrapper executorWrapper;

    protected final Runnable runnable;

    protected AbstractTimeoutTimerTask(ExecutorWrapper executorWrapper, Runnable runnable) {
        this.executorWrapper = executorWrapper;
        this.runnable = runnable;
    }

    /**
     * 重写TimerTask中run方法
     * @param timeout a handle which is associated with this task
     * @throws Exception
     */
    @Override
    public void run(Timeout timeout) throws Exception {
        // 线程池状态信息提供者
        val statProvider = executorWrapper.getThreadPoolStatProvider();
        if (Objects.isNull(statProvider)) {
            return;
        }
        doRun();
    }

    /**
     * 获得任务名称以及分布式链路ID
     * @return
     */
    protected Pair<String, String> getTaskNameAndTraceId() {
        String taskName = StringUtils.EMPTY;
        String traceId = StringUtils.EMPTY;
        if (runnable instanceof DtpRunnable) {
            DtpRunnable dtpRunnable = (DtpRunnable) runnable;
            taskName = dtpRunnable.getTaskName();
            traceId = dtpRunnable.getTraceId();
        }
        return Pair.of(taskName, traceId);
    }

    /**
     * Do run.
     */
    protected abstract void doRun();
}
