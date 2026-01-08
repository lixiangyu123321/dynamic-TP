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

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 定时器核心接口：定义定时任务的调度、停止、状态查询能力
 * 核心功能：支持一次性定时任务的调度，以及定时器的生命周期管理
 */
public interface Timer {

    /**
     * 调度一个一次性执行的定时任务，在指定延迟时间后执行。
     * （注：接口语义为“一次性执行”，区别于周期性执行的定时任务）
     *
     * @param task  要执行的定时任务（TimerTask接口实现类，封装具体业务逻辑）
     * @param delay 从当前时间开始，延迟执行的时长
     * @param unit  delay参数的时间单位（如TimeUnit.SECONDS、TimeUnit.MILLISECONDS）
     * @return 与该定时任务关联的句柄（Timeout），可通过该句柄查询/取消任务状态
     * @throws IllegalStateException 若该定时器已调用{@link #stop()}方法停止，抛出此异常（拒绝调度新任务）
     * @throws RejectedExecutionException 若待处理的定时任务数量过多，创建新的超时任务可能导致系统不稳定时抛出
     *                                    （如任务队列满、内存不足等场景，保护系统稳定性）
     */
    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

    /**
     * 释放当前Timer持有的所有资源，并取消所有已调度但尚未执行的定时任务。
     * （注：执行中的任务不受影响，仅取消待执行任务；释放资源通常包括线程池、队列等）
     *
     * @return 由该方法取消的所有任务对应的Timeout句柄集合
     *         （可用于确认哪些任务被取消，便于业务侧做后续清理）
     */
    Set<Timeout> stop();

    /**
     * 查询当前定时器是否已停止。
     * （补充：停止状态的定时器无法再调度新任务，调用newTimeout会抛IllegalStateException）
     *
     * @return true - 定时器已停止；false - 定时器仍在运行，可正常调度任务
     */
    boolean isStop();
}
