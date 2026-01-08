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

package org.dromara.dynamictp.common.queue;

import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * XXX 再VariableLinkedBlockingQueue的基础上加上内容监控
 * 内存安全的链表阻塞队列，继承自 VariableLinkedBlockingQueue（自定义的链表阻塞队列）
 * 核心特性：在添加元素前检查剩余可用内存，避免内存耗尽
 * @param <E> 队列中元素的类型
 */
public class MemorySafeLinkedBlockingQueue<E> extends VariableLinkedBlockingQueue<E> {

    // 序列化版本号：保证序列化/反序列化时类结构兼容
    private static final long serialVersionUID = 8032578371739960142L;

    // 常量：默认的最小可用内存阈值（16MB），单位字节
    public static final int THE_16_MB = 16 * 1024 * 1024;

    // 核心成员变量：允许的最小剩余可用内存阈值（单位字节）
    // 当系统可用内存低于该值时，拒绝添加元素
    private int maxFreeMemory;

    /**
     * 无参构造方法：使用默认的16MB内存阈值初始化
     */
    public MemorySafeLinkedBlockingQueue() {
        // 调用本类的单参构造方法，传入默认16MB阈值
        this(THE_16_MB);
    }

    /**
     * 单参构造方法：指定内存阈值，队列容量使用父类的Integer.MAX_VALUE（无界）
     * @param maxFreeMemory 允许的最小剩余可用内存（字节）
     */
    public MemorySafeLinkedBlockingQueue(final int maxFreeMemory) {
        // 调用父类构造方法，设置队列容量为Integer.MAX_VALUE（几乎无界）
        super(Integer.MAX_VALUE);
        // 初始化内存阈值
        this.maxFreeMemory = maxFreeMemory;
    }

    /**
     * 双参构造方法：指定队列容量和内存阈值
     * @param capacity 队列的最大容量（元素个数）
     * @param maxFreeMemory 允许的最小剩余可用内存（字节）
     */
    public MemorySafeLinkedBlockingQueue(final int capacity, final int maxFreeMemory) {
        // 调用父类构造方法，设置队列固定容量
        super(capacity);
        // 初始化内存阈值
        this.maxFreeMemory = maxFreeMemory;
    }

    /**
     * 集合初始化构造方法：从已有集合初始化队列元素，并指定内存阈值
     * @param c 初始化队列的元素集合
     * @param maxFreeMemory 允许的最小剩余可用内存（字节）
     */
    public MemorySafeLinkedBlockingQueue(final Collection<? extends E> c, final int maxFreeMemory) {
        // 调用父类构造方法，将集合c的元素初始化到队列中
        super(c);
        // 初始化内存阈值
        this.maxFreeMemory = maxFreeMemory;
    }

    /**
     * 设置最大可用内存阈值（修改允许的最小剩余可用内存）
     * @param maxFreeMemory 新的内存阈值（字节）
     */
    public void setMaxFreeMemory(final int maxFreeMemory) {
        // 修改成员变量，支持运行时动态调整内存阈值
        this.maxFreeMemory = maxFreeMemory;
    }

    /**
     * 获取当前设置的最大可用内存阈值（允许的最小剩余可用内存）
     * @return 内存阈值（字节）
     */
    public int getMaxFreeMemory() {
        // 返回当前内存阈值
        return maxFreeMemory;
    }

    /**
     * 核心判断方法：检查系统剩余可用内存是否满足阈值要求
     * @return true - 可用内存充足，允许添加元素
     * @throws RejectedExecutionException 可用内存不足时抛出该异常，拒绝添加元素
     */
    public boolean hasRemainedMemory() {
        // MemoryLimitCalculator.maxAvailable()：获取当前系统可用内存（字节）
        // 判断可用内存是否大于设定的阈值
        if (MemoryLimitCalculator.maxAvailable() > maxFreeMemory) {
            // 内存充足，返回true
            return true;
        }
        // 内存不足，抛出拒绝执行异常，明确提示内存耗尽
        throw new RejectedExecutionException("No more memory can be used.");
    }

    /**
     * 重写父类put方法：添加元素（阻塞式），添加前先检查内存
     * put特性：队列满时会阻塞，直到队列有空间；本类额外增加内存检查
     * @param e 要添加的元素（不能为null，父类会校验）
     * @throws InterruptedException 线程被中断时抛出
     * @throws RejectedExecutionException 内存不足时抛出
     */
    @Override
    public void put(final E e) throws InterruptedException {
        // 第一步：检查内存是否充足（不足会直接抛异常）
        if (hasRemainedMemory()) {
            // 第二步：内存充足，调用父类put方法添加元素
            super.put(e);
        }
    }

    /**
     * 重写父类offer方法：添加元素（带超时的阻塞式），添加前先检查内存
     * offer特性：队列满时阻塞指定时间，超时后返回false；本类额外增加内存检查
     * @param e 要添加的元素
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @return true-添加成功，false-超时/内存不足/队列满
     * @throws InterruptedException 线程被中断时抛出
     * @throws RejectedExecutionException 内存不足时抛出
     */
    @Override
    public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
        // 短路逻辑：先检查内存（不足抛异常），再调用父类offer方法
        // 内存充足时，执行父类逻辑；内存不足时，直接抛异常，返回值无意义
        return hasRemainedMemory() && super.offer(e, timeout, unit);
    }

    /**
     * 重写父类offer方法：添加元素（非阻塞式），添加前先检查内存
     * offer特性：队列满时直接返回false，不阻塞；本类额外增加内存检查
     * @param e 要添加的元素
     * @return true-添加成功，false-队列满/内存不足
     * @throws RejectedExecutionException 内存不足时抛出
     */
    @Override
    public boolean offer(final E e) {
        // 短路逻辑：先检查内存（不足抛异常），再调用父类offer方法
        return hasRemainedMemory() && super.offer(e);
    }
}
