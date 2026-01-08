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

package org.dromara.dynamictp.common.timer;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务核心接口：封装需要在指定延迟后执行的业务逻辑，是定时器框架的「任务载体」。
 * 与 Timer（定时器）、Timeout（任务句柄）协同工作：
 * - Timer 负责调度 TimerTask；
 * - Timeout 作为任务执行时的上下文句柄；
 * - TimerTask 封装具体要执行的业务逻辑。
 */
public interface TimerTask {

    /**
     * 定时任务的核心执行方法：在 {@link Timer#newTimeout(TimerTask, long, TimeUnit)}
     * 指定的延迟时间到达后，由定时器框架触发执行。
     * （注：方法执行时机由定时器控制，调用方无需手动调用）
     *
     * @param timeout 与当前任务关联的句柄（Timeout），包含以下核心作用：
     *                1. 任务内部可通过 timeout 查询自身状态（如 isExpired()/isCancelled()）；
     *                2. 任务内部可通过 timeout 反向获取所属定时器（timeout.timer()）；
     *                3. 任务内部可通过 timeout 主动取消自身（timeout.cancel()）；
     * @throws Exception 任务执行过程中抛出的任意异常（受检异常），由定时器框架处理：
     *                   - 通常定时器会捕获异常并记录日志，不会影响其他任务执行；
     *                   - 调用方需根据业务场景声明/处理具体异常（如IO异常、业务异常等）。
     */
    void run(Timeout timeout) throws Exception;
}
