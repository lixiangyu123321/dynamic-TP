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

package org.dromara.dynamictp.common.em;

import lombok.AllArgsConstructor;
import org.dromara.dynamictp.common.ex.DtpException;
import org.dromara.dynamictp.common.queue.MemorySafeLinkedBlockingQueue;
import org.dromara.dynamictp.common.queue.VariableLinkedBlockingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.M_1;

/**
 * QueueTypeEnum related
 * 队列类型枚举
 * TODO 一些自定义的阻塞队列和阻塞队列实现细节
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
@Getter
@AllArgsConstructor
public enum QueueTypeEnum {

    /**
     * BlockingQueue type.
     */
    /**
     * 数组实现的有界阻塞队列
     * 核心特点：
     * 1. 底层基于定长数组实现，初始化时必须指定容量（有界），无法动态扩容；
     * 2. 采用独占锁（ReentrantLock）实现线程安全，生产/消费共用一把锁，并发性能一般；
     * 3. 支持公平/非公平锁模式（默认非公平），公平模式下按FIFO顺序访问锁，避免线程饥饿；
     * 4. 队列满时生产阻塞，队列空时消费阻塞；
     * 适用场景：需要严格控制队列容量、对内存占用有明确限制的场景（如固定大小的任务缓冲池）。
     */
    ARRAY_BLOCKING_QUEUE(1, "ArrayBlockingQueue"),

    /**
     * 链表实现的可选界阻塞队列
     * 核心特点：
     * 1. 底层基于单向链表实现，默认构造无界队列（Integer.MAX_VALUE），也可指定容量（有界）；
     * 2. 采用两把独占锁（ReentrantLock）分别控制生产和消费，生产/消费可并行执行，并发性能优于ArrayBlockingQueue；
     * 3. 队列满时生产阻塞，队列空时消费阻塞；
     * 4. 迭代器遍历效率高，内存占用比数组队列更灵活（链表节点动态创建）；
     * 适用场景：任务量不可预估、需要高并发生产/消费的场景（如通用的线程池任务队列）。
     */
    LINKED_BLOCKING_QUEUE(2, "LinkedBlockingQueue"),

    /**
     * 优先级排序的无界阻塞队列
     * 核心特点：
     * 1. 底层基于数组实现（可扩容），无界队列，容量无限制（受内存限制）；
     * 2. 队列元素按优先级排序（默认自然序，也可自定义Comparator），而非FIFO；
     * 3. 采用独占锁实现线程安全，生产/消费共用一把锁；
     * 4. 不保证同优先级元素的顺序，队列空时消费阻塞（无满队列状态，生产永不阻塞）；
     * 适用场景：需要按优先级处理任务的场景（如紧急任务优先执行、定时任务排序）。
     */
    PRIORITY_BLOCKING_QUEUE(3, "PriorityBlockingQueue"),

    /**
     * 延迟执行的无界阻塞队列
     * 核心特点：
     * 1. 底层基于PriorityBlockingQueue实现，无界队列，元素需实现Delayed接口（指定延迟时间）；
     * 2. 元素仅在延迟时间到期后才可被消费，队列按延迟时间排序；
     * 3. 消费时仅能获取延迟到期的元素，未到期元素无法获取，队列空时消费阻塞；
     * 4. 生产永不阻塞，消费阻塞直到有元素延迟到期；
     * 适用场景：定时任务、延迟通知、缓存过期清理等场景（如ScheduledThreadPoolExecutor的任务队列）。
     */
    DELAY_QUEUE(4, "DelayQueue"),

    /**
     * 无缓冲的同步阻塞队列
     * 核心特点：
     * 1. 无底层存储结构（零容量），生产操作必须等待消费操作，消费操作必须等待生产操作，即"一手交钱一手交货"；
     * 2. 支持公平/非公平模式（默认非公平），公平模式下按FIFO顺序匹配生产/消费线程；
     * 3. 不存储任何元素，每个put操作必须对应一个take操作，否则一直阻塞；
     * 4. 并发性能极高，无队列存储开销；
     * 适用场景：生产消费严格同步、任务需立即执行的场景（如CachedThreadPool的默认队列）。
     */
    SYNCHRONOUS_QUEUE(5, "SynchronousQueue"),

    /**
     * 支持转移的链表无界阻塞队列
     * 核心特点：
     * 1. 底层基于单向链表实现，无界队列，继承自SynchronousQueue并扩展了存储能力；
     * 2. 支持transfer方法：生产线程可阻塞直到元素被消费（比SynchronousQueue更灵活）；
     * 3. 支持tryTransfer（非阻塞）、transfer（阻塞）、put（无阻塞）等多种生产方式；
     * 4. 采用CAS+自旋实现无锁并发，并发性能优于LinkedBlockingQueue；
     * 适用场景：高并发的生产消费场景、需要灵活控制元素转移策略的场景（如高性能RPC框架的消息队列）。
     */
    LINKED_TRANSFER_QUEUE(6, "LinkedTransferQueue"),

    /**
     * 链表实现的双向阻塞队列
     * 核心特点：
     * 1. 底层基于双向链表实现，可选界队列（默认无界），支持从队列头部/尾部同时生产/消费；
     * 2. 提供addFirst/addLast、takeFirst/takeLast等双向操作方法，可作为栈/队列使用；
     * 3. 采用两把独占锁分别控制头部和尾部操作，并发性能高；
     * 4. 队列满时生产阻塞，队列空时消费阻塞；
     * 适用场景：需要双向操作队列的场景（如任务队列的头尾都可消费、生产者可插队到队首）。
     */
    LINKED_BLOCKING_DEQUE(7, "LinkedBlockingDeque"),

    VARIABLE_LINKED_BLOCKING_QUEUE(8, "VariableLinkedBlockingQueue"),

    MEMORY_SAFE_LINKED_BLOCKING_QUEUE(9, "MemorySafeLinkedBlockingQueue");

    private final int code;

    private final String name;

    public static BlockingQueue<Runnable> buildLbq(String name, int capacity) {
        return buildLbq(name, capacity, false, 256);
    }

    /**
     * 基于队列名 创建对应的队列
     * @param name
     * @param capacity
     * @param fair
     * @param maxFreeMemory
     * @return
     */
    @SuppressWarnings("all")
    public static BlockingQueue<Runnable> buildLbq(String name, int capacity, boolean fair, int maxFreeMemory) {
        BlockingQueue<Runnable> blockingQueue = null;
        if (Objects.equals(name, ARRAY_BLOCKING_QUEUE.getName())) {
            blockingQueue = new ArrayBlockingQueue<>(capacity);
        } else if (Objects.equals(name, LINKED_BLOCKING_QUEUE.getName())) {
            blockingQueue = new LinkedBlockingQueue<>(capacity);
        } else if (Objects.equals(name, PRIORITY_BLOCKING_QUEUE.getName())) {
            blockingQueue = new PriorityBlockingQueue<>(capacity);
        } else if (Objects.equals(name, DELAY_QUEUE.getName())) {
            blockingQueue = new DelayQueue();
        } else if (Objects.equals(name, SYNCHRONOUS_QUEUE.getName())) {
            blockingQueue = new SynchronousQueue<>(fair);
        } else if (Objects.equals(name, LINKED_TRANSFER_QUEUE.getName())) {
            blockingQueue = new LinkedTransferQueue<>();
        } else if (Objects.equals(name, LINKED_BLOCKING_DEQUE.getName())) {
            blockingQueue = new LinkedBlockingDeque<>(capacity);
        } else if (Objects.equals(name, VARIABLE_LINKED_BLOCKING_QUEUE.getName())) {
            blockingQueue = new VariableLinkedBlockingQueue<>(capacity);
        } else if (Objects.equals(name, MEMORY_SAFE_LINKED_BLOCKING_QUEUE.getName())) {
            blockingQueue = new MemorySafeLinkedBlockingQueue<>(capacity, maxFreeMemory * M_1);
        }
        if (blockingQueue != null) {
            return blockingQueue;
        }

        log.error("Cannot find specified BlockingQueue {}", name);
        throw new DtpException("Cannot find specified BlockingQueue " + name);
    }
}
