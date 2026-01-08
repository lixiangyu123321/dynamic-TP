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

/**
 * 定时任务句柄接口：作为「定时器（Timer）」和「定时任务（TimerTask）」之间的关联载体，
 * 提供任务状态查询、任务取消等核心能力，是调用方管理单个定时任务的核心入口。
 */
public interface Timeout {

    /**
     * 获取创建当前句柄的定时器实例。
     * （核心作用：通过句柄反向关联到所属定时器，便于调用方统一管理定时器生命周期）
     *
     * @return 创建此句柄的{@link Timer}实例，非null
     */
    Timer timer();

    /**
     * 获取与当前句柄关联的定时任务。
     * （核心作用：通过句柄定位到具体的任务实例，便于查看/操作任务本身）
     *
     * @return 与此句柄绑定的{@link TimerTask}实例，非null
     */
    TimerTask task();

    /**
     * 判断关联的定时任务是否已「过期执行」（即任务已到达执行时间并完成/开始执行）。
     * 语义说明：
     * - true：任务已到延迟时间，且已执行（或正在执行）；
     * - false：任务仍在等待执行（未到延迟时间）、已被取消，或定时器已停止。
     *
     * @return 仅当关联的{@link TimerTask}已过期执行时返回true，否则返回false
     */
    boolean isExpired();

    /**
     * 判断关联的定时任务是否已被取消。
     * 语义说明：
     * - true：任务已通过{@link #cancel()}或定时器{@link Timer#stop()}取消；
     * - false：任务未被取消（可能等待执行/已执行完成）。
     *
     * @return 仅当关联的{@link TimerTask}已被取消时返回true，否则返回false
     */
    boolean isCancelled();

    /**
     * 尝试取消与当前句柄关联的定时任务。
     * 幂等性说明：
     * - 若任务已执行完成、或已被取消，调用此方法无任何副作用（不会抛异常）；
     * - 若任务仍在等待执行，调用后任务会被标记为取消，且不会再被执行。
     *
     * @return 取消操作成功完成返回true；任务已执行/已取消导致取消失败，返回false
     */
    boolean cancel();
}
