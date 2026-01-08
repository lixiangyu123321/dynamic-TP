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

import org.dromara.dynamictp.common.constant.DynamicTpConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于「时间轮」实现的Timer，针对I/O超时调度做了近似优化（非精准定时）。
 *
 * <h3>Tick Duration（刻度时长）</h3>
 * <p>
 * 正如“近似”所述，该定时器不会精准按时执行定时任务。HashedWheelTimer会在每个刻度（tick）检查是否有超时任务，
 * 并执行所有已到执行时间的任务。
 * <p>
 * 可通过构造器指定更小/更大的刻度时长来提升/降低执行精度。多数网络应用中I/O超时无需精准，因此默认刻度为100ms，
 * 绝大多数场景无需调整。
 *
 * <h3>Ticks per Wheel（轮盘大小）</h3>
 * <p>
 * HashedWheelTimer维护一个名为“轮盘（wheel）”的数据结构，本质是TimerTask的哈希表，哈希函数为“任务截止时间”。
 * 默认轮盘刻度数（轮盘大小）为512，若需调度大量超时任务可指定更大值。
 *
 * <h3>不要创建大量实例</h3>
 * <p>
 * 每个HashedWheelTimer实例创建并启动时会新建一个线程，因此应确保整个应用仅创建少量实例并共享。
 * 常见错误：为每个连接创建新实例，导致应用无响应。
 *
 * <h3>实现细节</h3>
 * <p>
 * 基于George Varghese和Tony Lauck的论文《Hashed and Hierarchical Timing Wheels》实现，
 * 该数据结构用于高效实现定时器。
 * <p>
 * 代码拷贝自Dubbo，详见：https://github.com/apache/dubbo/blob/3.2/dubbo-common/src/main/java/org/apache/dubbo/common/timer/HashedWheelTimer.java
 */
@Slf4j
public class HashedWheelTimer implements Timer {

    /**
     * SPI名称标识（可能用于SPI扩展加载）
     * TODO 应该为hashed
     */
    public static final String NAME = "hased";

    /**
     * 全局计数器：统计当前JVM中HashedWheelTimer实例数量，防止创建过多实例
     */
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    /**
     * 标记是否已打印“实例过多”的警告，避免重复打印
     */
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();

    /**
     * 实例数量上限：超过该值会打印警告
     */
    private static final int INSTANCE_COUNT_LIMIT = 64;

    /**
     * CAS更新workerState的原子更新器：避免synchronized，提升并发性能
     */
    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    /**
     * 核心工作线程：执行时间轮的核心逻辑（轮询刻度、执行超时任务）
     */
    private final Worker worker = new Worker();

    /**
     * 工作线程实例：由ThreadFactory创建，承载Worker的run方法
     */
    private final Thread workerThread;

    /**
     * 线程工作状态
     */
    private static final int WORKER_STATE_INIT = 0;
    private static final int WORKER_STATE_STARTED = 1;
    private static final int WORKER_STATE_SHUTDOWN = 2;

    /**
     * 工作线程状态（volatile保证可见性）：
     * 0 - 初始化，1 - 已启动，2 - 已关闭
     */
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState;

    /**
     * 每个刻度的时长（纳秒）：如默认100ms → 100_000_000ns
     */
    private final long tickDuration;

    /**
     * 时间轮的核心数组：每个元素是一个HashedWheelBucket（存储该刻度的超时任务）
     */
    private final HashedWheelBucket[] wheel;

    /**
     * 轮盘掩码：用于快速计算任务所在的bucket索引（wheel.length-1，因wheel长度是2的幂）
     */
    private final int mask;

    /**
     * 启动时间初始化的闭锁：确保start()方法等待workerThread初始化startTime后再返回
     */
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    /**
     * 待处理的超时任务队列：新提交的任务先放入此队列，待下一个刻度转移到对应bucket
     */
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingQueue<>();

    /**
     * 已取消的超时任务队列：存储需要移除的任务，在每个刻度处理
     */
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new LinkedBlockingQueue<>();

    /**
     * 待执行的任务计数器：统计当前未完成的超时任务数
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);

    /**
     * 最大待处理任务数：超过该值调用newTimeout会抛RejectedExecutionException
     */
    private final long maxPendingTimeouts;

    /**
     * 定时器启动时间（纳秒）：由workerThread初始化，用于计算任务截止时间
     */
    private volatile long startTime;

    /**
     * 无参构造器：使用默认线程工厂、默认刻度时长（100ms）、默认轮盘大小（512）
     */
    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }

    /**
     * 构造器：使用默认线程工厂、默认轮盘大小（512），指定刻度时长
     *
     * @param tickDuration 刻度间隔时长
     * @param unit         tickDuration的时间单位
     * @throws NullPointerException     若unit为null
     * @throws IllegalArgumentException 若tickDuration <= 0
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }

    /**
     * 构造器：使用默认线程工厂，指定刻度时长和轮盘大小
     *
     * @param tickDuration  刻度间隔时长
     * @param unit          tickDuration的时间单位
     * @param ticksPerWheel 轮盘的刻度数（轮盘大小）
     * @throws NullPointerException     若unit为null
     * @throws IllegalArgumentException 若tickDuration或ticksPerWheel <= 0
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }

    /**
     * 构造器：指定线程工厂，使用默认刻度时长（100ms）、默认轮盘大小（512）
     *
     * @param threadFactory 创建后台工作线程的线程工厂
     * @throws NullPointerException 若threadFactory为null
     */
    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 构造器：指定线程工厂和刻度时长，使用默认轮盘大小（512）
     *
     * @param threadFactory 创建后台工作线程的线程工厂
     * @param tickDuration  刻度间隔时长
     * @param unit          tickDuration的时间单位
     * @throws NullPointerException     若threadFactory或unit为null
     * @throws IllegalArgumentException 若tickDuration <= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }

    /**
     * 构造器：指定线程工厂、刻度时长、轮盘大小
     *
     * @param threadFactory 创建后台工作线程的线程工厂
     * @param tickDuration  刻度间隔时长
     * @param unit          tickDuration的时间单位
     * @param ticksPerWheel 轮盘的刻度数（轮盘大小）
     * @throws NullPointerException     若threadFactory或unit为null
     * @throws IllegalArgumentException 若tickDuration或ticksPerWheel <= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, -1);
    }

    /**
     * 全参构造器：指定所有核心参数，包括最大待处理任务数
     *
     * @param threadFactory      创建后台工作线程的线程工厂
     * @param tickDuration       刻度间隔时长
     * @param unit               tickDuration的时间单位
     * @param ticksPerWheel      轮盘的刻度数（轮盘大小）
     * @param maxPendingTimeouts 最大待处理任务数：超过该值newTimeout抛RejectedExecutionException；<=0表示无限制
     * @throws NullPointerException     若threadFactory或unit为null
     * @throws IllegalArgumentException 若tickDuration或ticksPerWheel <= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration,
            TimeUnit unit,
            int ticksPerWheel,
            long maxPendingTimeouts) {

        // 参数校验：线程工厂非空
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        // 参数校验：时间单位非空
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        // 参数校验：刻度时长必须>0
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        // 参数校验：轮盘大小必须>0
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        // 初始化时间轮：将ticksPerWheel归一化为2的幂，保证mask=wheel.length-1的哈希效率
        // XXX 创建桶数组
        wheel = createWheel(ticksPerWheel);
        // 掩码：用于快速计算bucket索引（tick & mask）
        mask = wheel.length - 1;

        // 将刻度时长转换为纳秒（统一时间单位，避免多次转换）
        this.tickDuration = unit.toNanos(tickDuration);

        // 防止溢出：tickDuration * wheel.length 不能超过Long.MAX_VALUE
        // TODO 此为何意哪？
        if (this.tickDuration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format(
                    "tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                    tickDuration, Long.MAX_VALUE / wheel.length));
        }
        // 创建工作线程：由线程工厂生成，执行Worker的run方法
        // XXX 拿到工作的线程，绑定对应的定时任务
        workerThread = threadFactory.newThread(worker);

        // 初始化最大待处理任务数
        this.maxPendingTimeouts = maxPendingTimeouts;

        // 实例数统计：超过64个打印警告（防止创建过多线程）
        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
                WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            reportTooManyInstances();
        }
    }

    /**
     * 析构方法：GC时确保关闭定时器并减少实例计数
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            // 若未关闭，将状态置为SHUTDOWN并减少实例计数
            // 更新定时状态为shutdown
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    /**
     * 创建时间轮数组：归一化ticksPerWheel为2的幂，初始化每个bucket
     *
     * @param ticksPerWheel 期望的轮盘大小
     * @return 时间轮数组（每个元素为HashedWheelBucket）
     */
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        // 参数校验
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        // 限制最大轮盘大小为2^30（防止数组过大）
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException(
                    "ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }

        // 归一化ticksPerWheel为2的幂（如511→512，1000→1024）
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        // 初始化轮盘数组：每个元素是一个空的HashedWheelBucket
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 归一化轮盘大小为2的幂：通过位运算快速计算（如输入512→512，输入500→512）
     *
     * @param ticksPerWheel 原始轮盘大小
     * @return 归一化后的2的幂值
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = ticksPerWheel - 1;
        // 位运算：将所有低位设为1，最终+1得到2的幂
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 1;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 2;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 4;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 8;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 16;
        return normalizedTicksPerWheel + 1;
    }

    /**
     * 显式启动后台工作线程（若未启动）。即使不调用，首次调用newTimeout也会自动启动。
     *
     * @throws IllegalStateException 若定时器已停止
     */
    public void start() {
        // CAS更新工作线程状态
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                // 初始化状态→启动状态：启动工作线程
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    // XXX 启动定时线程
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                // 已启动：直接返回
                break;
            case WORKER_STATE_SHUTDOWN:
                // 已关闭：抛异常
                throw new IllegalStateException("cannot be started once stopped");
            default:
                // 非法状态：抛错误
                throw new Error("Invalid WorkerState");
        }

        // 等待workerThread初始化startTime（闭锁等待）
        while (startTime == 0) {
            try {
                // XXX 通过CountDownLatch进行同步
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // 忽略中断：startTime很快会初始化
            }
        }
    }

    /**
     * 停止定时器：释放资源，取消所有未执行的任务
     *
     * @return 被取消的未执行任务集合
     */
    @Override
    public Set<Timeout> stop() {
        // 禁止在工作线程中调用stop（避免死锁）
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(
                    HashedWheelTimer.class.getSimpleName() +
                            ".stop() cannot be called from " +
                            TimerTask.class.getSimpleName());
        }

        // CAS将状态从STARTED→SHUTDOWN
        // XXX 强制状态转换
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // 状态为INIT或SHUTDOWN：强制置为SHUTDOWN，减少实例计数
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
            // 无未处理任务：返回空集合
            return Collections.emptySet();
        }

        try {
            boolean interrupted = false;
            // 中断并等待工作线程退出
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    // 每次等待100ms，避免永久阻塞
                    // XXX join被中断会自动清除当前线程的中断状态
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }

            // 恢复当前线程的中断状态
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            // 减少实例计数
            INSTANCE_COUNTER.decrementAndGet();
        }
        // 返回未处理的任务集合
        return worker.unprocessedTimeouts();
    }

    /**
     * 判断定时器是否已停止
     *
     * @return true=已停止，false=运行中
     */
    @Override
    public boolean isStop() {
        return WORKER_STATE_SHUTDOWN == WORKER_STATE_UPDATER.get(this);
    }

    /**
     * 调度新的超时任务
     *
     * @param task  要执行的任务
     * @param delay 延迟执行时长
     * @param unit  delay的时间单位
     * @return 任务关联的Timeout句柄
     * @throws NullPointerException        若task或unit为null
     * @throws RejectedExecutionException  若待处理任务数超过最大值
     */
    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        // 参数校验：任务非空
        if (task == null) {
            throw new NullPointerException("task");
        }
        // 参数校验：时间单位非空
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        // 待处理任务数+1
        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();

        // 检查最大待处理任务数：超过则抛异常
        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }

        // 启动定时器（若未启动）
        // XXX 这里会自动启动定时任务
        // XXX 一般是懒加载更合理，只有有任务了才启动定时任务
        start();

        // 计算任务截止时间：当前纳秒时间 + 延迟时间 - 定时器启动时间
        // XXX 这里也是相对时间
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;

        // 防止溢出：若delay>0但deadline<0，设为Long.MAX_VALUE
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        // 创建超时任务句柄
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        // 添加到待处理队列：等待下一个刻度转移到对应bucket
        timeouts.add(timeout);
        return timeout;
    }

    /**
     * 获取当前待处理的任务数
     *
     * @return 待处理任务数
     */
    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    /**
     * 打印“实例过多”的警告日志
     */
    private static void reportTooManyInstances() {
        String resourceType = ClassUtils.getSimpleName((HashedWheelTimer.class));
        log.error("You are creating too many " + resourceType + " instances. " +
                resourceType + " is a shared resource that must be reused across the JVM," +
                "so that only a few instances are created.");
    }

    /**
     * XXX 这里是私有内部类唉
     * 工作线程核心类：执行时间轮的核心逻辑（轮询刻度、处理任务）
     */
    private final class Worker implements Runnable {
        // 存储停止时未处理的任务
        // XXX Timeout是定时器句柄
        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();

        // 当前刻度数：从0开始递增
        private long tick;

        @Override
        public void run() {
            // 初始化定时器启动时间（纳秒）
            startTime = System.nanoTime();
            if (startTime == 0) {
                // 避免startTime为0（0作为未初始化的标识）
                startTime = 1;
            }

            // 释放闭锁：通知start()方法已初始化startTime
            // XXX 开始定时
            startTimeInitialized.countDown();

            // 核心循环：只要状态为STARTED，持续轮询刻度
            do {
                // 等待下一个刻度到达，返回当前截止时间
                // XXX 这里到达指定刻度了
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    // 计算当前刻度对应的bucket索引（tick & mask 等价于 tick % wheel.length）
                    int idx = (int) (tick & mask);
                    // 处理已取消的任务：从bucket中移除
                    processCancelledTasks();
                    // 获取当前刻度的bucket
                    HashedWheelBucket bucket = wheel[idx];
                    // 将待处理队列中的任务转移到对应bucket
                    transferTimeoutsToBuckets();
                    // 执行当前bucket中已超时的任务
                    bucket.expireTimeouts(deadline);
                    // 刻度数+1
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

            // 停止后：收集所有未处理的任务
            // 1. 清空所有bucket中的任务
            for (HashedWheelBucket bucket : wheel) {
                // XXX 该方法将未处理的任务
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            // 2. 清空待处理队列中的任务
            for (; ; ) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                // 仅添加未取消的任务
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            // 3. 处理剩余的已取消任务
            processCancelledTasks();
        }

        /**
         * 将待处理队列中的任务转移到对应bucket
         * 限制每次最多转移100000个任务，防止工作线程被阻塞
         * XXX 每到一个刻度都将待处理队列中的TimeOut加入到bucket中
         */
        private void transferTimeoutsToBuckets() {
            for (int i = 0; i < 100000; i++) {
                // 从待处理队列取出任务
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    // 队列空：退出循环
                    break;
                }
                // 已取消的任务：跳过
                // XXX 这里是处理待处理队列中的已取消任务
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    continue;
                }

                // 计算任务所属的总刻度数（截止时间 / 刻度时长）
                // XXX 这里的deadline 也是相对时间
                long calculated = timeout.deadline / tickDuration;
                // 计算剩余轮数：任务需要绕时间轮的圈数
                timeout.remainingRounds = (calculated - tick) / wheel.length;

                // 确保任务不会被调度到过去的刻度
                final long ticks = Math.max(calculated, tick);
                // 计算任务所在的bucket索引
                int stopIndex = (int) (ticks & mask);

                // 将任务添加到对应bucket
                HashedWheelBucket bucket = wheel[stopIndex];
                bucket.addTimeout(timeout);
            }
        }

        /**
         * 处理已取消的任务：从bucket中移除
         * XXX 处理已取消的任务，这里是处理bucket中的已取消的任务
         */
        private void processCancelledTasks() {
            for (; ; ) {
                // 从已取消队列取出任务
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    // 队列空：退出循环
                    break;
                }
                try {
                    // 从bucket中移除任务
                    timeout.remove();
                } catch (Throwable t) {
                    // 捕获异常：避免单个任务处理失败影响整体
                    if (log.isWarnEnabled()) {
                        log.warn("An exception was thrown while process a cancellation task", t);
                    }
                }
            }
        }

        /**
         * 等待下一个刻度到达：计算目标时间，睡眠直到到达
         *
         * @return Long.MIN_VALUE=收到停止请求；否则返回当前时间（纳秒）
         */
        private long waitForNextTick() {
            // 计算下一个刻度的截止时间（当前刻度+1 * 刻度时长）
            // XXX 这里的截至时间也是相对于启动时间的相对时间
            long deadline = tickDuration * (tick + 1);

            for (; ; ) {
                // 当前时间（相对于定时器启动时间）
                // XXX 这里的当前时间是相对于启动时间的相对时间
                final long currentTime = System.nanoTime() - startTime;
                // 计算需要睡眠的毫秒数（向上取整）
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                // 无需睡眠：已到下一个刻度
                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }
                // Windows系统优化：睡眠时长取整为10的倍数（避免精确睡眠导致高CPU）
                if (isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                }

                try {
                    // 睡眠指定时长
                    // XXX 没到下一个刻度，无任务处理，睡觉
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    // 被中断：检查是否需要停止
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        /**
         * 获取停止时未处理的任务集合（不可修改）
         * XXX 获得所有未处理的任务集合
         * @return 未处理任务集合
         */
        Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    /**
     * 超时任务句柄：实现Timeout接口，关联定时器、任务、截止时间等信息
     */
    private static final class HashedWheelTimeout implements Timeout {

        // 任务状态常量：0-初始化，1-已取消，2-已过期（执行）
        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        // CAS更新状态的原子更新器
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");

        // 关联的定时器
        private final HashedWheelTimer timer;
        // 关联的定时任务
        private final TimerTask task;
        // 任务截止时间（纳秒，相对于定时器启动时间）
        private final long deadline;

        /**
         * 任务状态（volatile保证可见性）：
         * 0-初始化，1-已取消，2-已过期
         */
        @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
        private volatile int state = ST_INIT;

        /**
         * 剩余轮数：任务需要绕时间轮的圈数，由Worker.transferTimeoutsToBuckets()设置
         */
        long remainingRounds;

        /**
         * 双向链表指针：用于HashedWheelBucket存储任务（仅工作线程访问，无需同步）
         */
        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        /**
         * 任务所属的bucket
         */
        HashedWheelBucket bucket;

        /**
         * 构造器：初始化超时任务句柄
         *
         * @param timer    关联的定时器
         * @param task     关联的任务
         * @param deadline 截止时间（纳秒）
         */
        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        /**
         * 获取关联的定时器
         */
        @Override
        public Timer timer() {
            return timer;
        }

        /**
         * 获取关联的任务
         */
        @Override
        public TimerTask task() {
            return task;
        }

        /**
         * 取消任务：CAS更新状态，添加到已取消队列
         *
         * @return true=取消成功，false=任务已取消/执行
         */
        @Override
        public boolean cancel() {
            // CAS将状态从INIT→CANCELLED
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            // 添加到已取消队列：等待Worker处理
            timer.cancelledTimeouts.add(this);
            return true;
        }

        /**
         * 从bucket中移除任务：清理链表指针，减少待处理任务数
         */
        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                // 从bucket中移除
                bucket.remove(this);
            } else {
                // 任务尚未转移到bucket：直接减少计数
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        /**
         * CAS更新任务状态
         *
         * @param expected 期望状态
         * @param state    目标状态
         * @return true=更新成功
         */
        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        /**
         * 获取任务状态
         *
         * @return 状态值（0/1/2）
         */
        public int state() {
            return state;
        }

        /**
         * 判断任务是否已取消
         */
        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        /**
         * 判断任务是否已过期（执行）
         */
        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        /**
         * 执行过期任务：CAS更新状态，调用任务的run方法
         */
        public void expire() {
            // CAS将状态从INIT→EXPIRED
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }

            try {
                // 执行任务的run方法
                task.run(this);
            } catch (Throwable t) {
                // 捕获异常：避免单个任务执行失败影响定时器
                if (log.isWarnEnabled()) {
                    log.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
                }
            }
        }

        /**
         * 重写toString：打印任务状态（截止时间、是否取消、任务信息）
         */
        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;
            String simpleClassName = ClassUtils.getSimpleName(this.getClass());

            StringBuilder buf = new StringBuilder(192)
                    .append(simpleClassName)
                    .append('(')
                    .append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining)
                        .append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining)
                        .append(" ns ago");
            } else {
                buf.append("now");
            }

            if (isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(", task: ")
                    .append(task())
                    .append(')')
                    .toString();
        }
    }

    /**
     * 时间轮桶：存储同一刻度的超时任务，基于双向链表实现（便于快速移除任务）
     */
    private static final class HashedWheelBucket {

        /**
         * 双向链表的头/尾节点
         */
        private HashedWheelTimeout head;
        private HashedWheelTimeout tail;

        /**
         * 添加超时任务到当前bucket
         *
         * @param timeout 要添加的任务
         */
        void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null; // 断言：任务未归属任何bucket
            timeout.bucket = this; // 关联bucket
            if (head == null) {
                // 空链表：头尾都指向该任务
                head = tail = timeout;
            } else {
                // 追加到链表尾部
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        /**
         * 执行当前bucket中已过期的任务
         *
         * @param deadline 当前刻度的截止时间（纳秒）
         */
        void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;

            // 遍历链表中的所有任务
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                // 剩余轮数<=0：任务已到执行时间
                if (timeout.remainingRounds <= 0) {
                    // 从链表中移除任务
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        // 执行任务
                        // XXX 轮数到，直接执行
                        timeout.expire();
                    } else {
                        // 异常：任务截止时间大于当前刻度（理论上不会发生）
                        throw new IllegalStateException(String.format(
                                "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                    // XXX 桶轮询的二次兜底，防止线程切换有任务被取消
                } else if (timeout.isCancelled()) {
                    // 任务已取消：移除
                    next = remove(timeout);
                } else {
                    // 剩余轮数-1：任务需要多绕一圈
                    timeout.remainingRounds--;
                }
                timeout = next;
            }
        }

        /**
         * 从链表中移除任务：清理指针，减少待处理任务数
         *
         * @param timeout 要移除的任务
         * @return 移除后的下一个任务
         */
        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            // 调整双向链表指针
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }

            // 处理头尾节点
            if (timeout == head) {
                // 任务是头节点
                if (timeout == tail) {
                    // 任务也是尾节点：链表空
                    tail = null;
                    head = null;
                } else {
                    // 头节点后移
                    head = next;
                }
            } else if (timeout == tail) {
                // 任务是尾节点：尾节点前移
                tail = timeout.prev;
            }
            // 清空指针和bucket关联：便于GC
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            // 减少待处理任务数
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        /**
         * 清空bucket，返回所有未过期/未取消的任务
         *
         * @param set 存储未处理任务的集合
         */
        void clearTimeouts(Set<Timeout> set) {
            for (; ; ) {
                // 取出链表头节点
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                // 跳过已过期/已取消的任务
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                // 添加到结果集合
                set.add(timeout);
            }
        }

        /**
         * 取出链表头节点（并移除）
         *
         * @return 头节点，无则返回null
         */
        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                // 链表只有一个节点：清空头尾
                tail = this.head = null;
            } else {
                // 头节点后移
                this.head = next;
                next.prev = null;
            }

            // 清空指针和bucket关联：便于GC
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }

    /**
     * 系统标识，判断是否为Windows系统
     */
    private static final boolean IS_OS_WINDOWS = System.getProperty(DynamicTpConst.OS_NAME_KEY, "").toLowerCase(Locale.US).contains(DynamicTpConst.OS_WIN_PREFIX);

    /**
     * 判断当前系统是否为Windows
     *
     * @return true=Windows系统
     */
    private boolean isWindows() {
        return IS_OS_WINDOWS;
    }
}