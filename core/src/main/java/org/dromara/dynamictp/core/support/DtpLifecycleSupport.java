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

package org.dromara.dynamictp.core.support;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * DtpLifecycleSupport which mainly implements ThreadPoolExecutor's lifecycle management.
 * 线程池生命周期工具类 XXX 用于管理动态线程池，或者说线程池包装器的启动/暂停的工具类
 * 初始化管理: 提供线程池初始化的统一入口
 * 优雅关闭: 实现线程池的优雅关闭逻辑
 * 异步关闭: 支持异步关闭线程池
 * @author yanhom
 * @since 1.0.3
 **/
@Slf4j
public class DtpLifecycleSupport {

    private DtpLifecycleSupport() { }

    /**
     * Initialize, do sth.
     *
     * @param executorWrapper executor wrapper
     */
    public static void initialize(ExecutorWrapper executorWrapper) {
        executorWrapper.initialize();
    }

    /**
     * Calls {@code internalShutdown} when the BeanFactory destroys
     * the task executor instance.
     * @param executorWrapper executor wrapper
     */
    public static void destroy(ExecutorWrapper executorWrapper) {
        if (executorWrapper.isExecutorService()) {
            ExecutorService executorService = (ExecutorService) executorWrapper.getExecutor().getOriginal();
            internalShutdown(executorService,
                    executorWrapper.getThreadPoolName(),
                    executorWrapper.isWaitForTasksToCompleteOnShutdown(),
                    executorWrapper.getAwaitTerminationSeconds());
        }
    }

    /**
     * 使用一个新线程去关闭线程池并阻塞等待线程池终止，不会阻塞主线程
     * @param executor
     * @param threadPoolName
     * @param timeout
     */
    public static void shutdownGracefulAsync(ExecutorService executor,
                                             String threadPoolName,
                                             int timeout) {
        ExecutorService tmpExecutor = Executors.newSingleThreadExecutor();
        tmpExecutor.execute(() -> internalShutdown(executor, threadPoolName,
                true, timeout));
        tmpExecutor.shutdown();
    }

    /**
     * Perform a shutdown on the underlying ExecutorService.
     * @param executor the executor to shut down (maybe {@code null})
     * @param threadPoolName the name of the thread pool (for logging purposes)
     * @param waitForTasksToCompleteOnShutdown whether to wait for tasks to complete on shutdown
     * @param awaitTerminationSeconds the maximum number of seconds to wait
     *
     * @see ExecutorService#shutdown()
     * @see ExecutorService#shutdownNow()
     */
    public static void internalShutdown(ExecutorService executor,
                                        String threadPoolName,
                                        boolean waitForTasksToCompleteOnShutdown,
                                        int awaitTerminationSeconds) {
        if (Objects.isNull(executor)) {
            return;
        }
        log.info("Shutting down ExecutorService, threadPoolName: {}", threadPoolName);
        // 是否要等未完成任务完成
        if (waitForTasksToCompleteOnShutdown) {
            /**
             * shutdown异步返回，会执行所有已提交的任务，拒绝接受新的任务
             */
            executor.shutdown();
        } else {
            // 依次取消未完成任务
            for (Runnable remainingTask : executor.shutdownNow()) {
                cancelRemainingTask(remainingTask);
            }
        }
        awaitTerminationIfNecessary(executor, threadPoolName, awaitTerminationSeconds);
    }

    /**
     * Cancel the given remaining task which never commended execution,
     * as returned from {@link ExecutorService#shutdownNow()}.
     * @param task the task to cancel (typically a {@link RunnableFuture})
     * @see RunnableFuture#cancel(boolean)
     */
    protected static void cancelRemainingTask(Runnable task) {
        if (task instanceof Future) {
            ((Future<?>) task).cancel(true);
        }
    }

    /**
     * Wait for the executor to terminate, according to the value of the awaitTerminationSeconds property.
     * 这个方法会阻塞等待线程池终止，当然是带超时时间的等待
     * @param executor executor
     */
    private static void awaitTerminationIfNecessary(ExecutorService executor,
                                                    String threadPoolName,
                                                    int awaitTerminationSeconds) {
        if (awaitTerminationSeconds <= 0) {
            return;
        }
        try {
            if (!executor.awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) {
                log.warn("Timed out while waiting for executor {} to terminate", threadPoolName);
            }
        } catch (InterruptedException ex) {
            log.warn("Interrupted while waiting for executor {} to terminate", threadPoolName);
            Thread.currentThread().interrupt();
        }
    }
}
