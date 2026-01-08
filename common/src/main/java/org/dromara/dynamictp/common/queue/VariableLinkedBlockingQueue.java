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
// Copyright (c) 2007-2020 VMware, Inc. or its affiliates.  All rights reserved.
//
// This software, the RabbitMQ Java client library, is triple-licensed under the
// Mozilla Public License 2.0 ("MPL"), the GNU General Public License version 2
// ("GPL") and the Apache License version 2 ("ASL"). For the MPL, please see
// LICENSE-MPL-RabbitMQ. For the GPL, please see LICENSE-GPL2.  For the ASL,
// please see LICENSE-APACHE2.
//
// This software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
// either express or implied. See the LICENSE file for specific language governing
// rights and limitations of this software.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.

/*
 * Modifications Copyright 2015-2020 VMware, Inc. or its affiliates. and licenced as per
 * the rest of the RabbitMQ Java client.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * https://creativecommons.org/licenses/publicdomain
 */

package org.dromara.dynamictp.common.queue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 可变容量的链表实现的阻塞队列（基于JDK LinkedBlockingQueue扩展）
 * 核心扩展：支持动态修改队列容量（setCapacity方法）
 * 特性：
 * 1. 采用双锁分离机制（putLock/takeLock），读写操作分离，提高并发性能
 * 2. 基于链表实现，无固定容量限制（可动态调整）
 * 3. 阻塞队列特性：满时put阻塞，空时take阻塞
 * 4. 线程安全，支持高并发的生产消费模型
 */
public class VariableLinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    // 序列化版本号，保证序列化/反序列化的兼容性
    private static final long serialVersionUID = -6903933977591709194L;

    /*
     * 双锁队列算法的变体：
     * 1. putLock控制put/offer等入队操作，关联notFull条件变量（等待入队的线程）
     * 2. takeLock控制take/poll等出队操作，关联notEmpty条件变量（等待出队的线程）
     * 3. count原子变量记录元素数量，避免大部分场景下需要同时获取两把锁
     * 4. 采用级联通知机制：put操作唤醒take线程，take操作唤醒put线程，减少锁竞争
     * 5. remove/迭代器等操作需要同时获取两把锁（fullyLock）
     *
     * 读写可见性保证：
     * - 入队时获取putLock并更新count，读线程通过获取takeLock/putLock保证可见性
     * - 弱一致性迭代器通过节点自引用（h.next = h）帮助GC，避免内存泄漏
     */

    /**
     * 链表节点类：存储队列元素和后继节点引用
     */
    static class Node<E> {
        // 节点存储的元素
        E item;

        /**
         * 后继节点引用，有三种状态：
         * 1. 正常指向真正的后继节点
         * 2. 指向自身（this）：表示后继节点是head.next（节点已出队）
         * 3. null：表示当前是最后一个节点
         */
        Node<E> next;

        // 节点构造器：初始化元素，后继节点默认为null
        Node(E x) {
            item = x;
        }
    }

    /** 队列容量上限（可动态修改），默认Integer.MAX_VALUE */
    private int capacity;

    /** 队列当前元素数量（原子变量，保证并发下的计数准确性） */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * 队列头节点（哨兵节点）
     * 不变式：head.item == null（头节点不存储实际元素）
     */
    transient Node<E> head;

    /**
     * 队列尾节点
     * 不变式：last.next == null（尾节点的后继永远为null）
     */
    private transient Node<E> last;

    /** 出队操作锁（take/poll/peek等） */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** 出队等待条件：队列为空时，take线程等待在此条件上 */
    private final Condition notEmpty = takeLock.newCondition();

    /** 入队操作锁（put/offer等） */
    private final ReentrantLock putLock = new ReentrantLock();

    /** 入队等待条件：队列满时，put线程等待在此条件上 */
    private final Condition notFull = putLock.newCondition();

    /**
     * 唤醒等待的出队线程（仅在put/offer中调用，无需持有takeLock）
     * 核心逻辑：获取takeLock，唤醒一个等待的take线程
     * XXX 非公平
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock(); // 获取出队锁
        try {
            notEmpty.signal(); // 唤醒一个等待的出队线程
        } finally {
            takeLock.unlock(); // 释放锁（finally保证必释放）
        }
    }

    /**
     * 唤醒等待的入队线程（仅在take/poll中调用）
     * 核心逻辑：获取putLock，唤醒一个等待的put线程
     * XXX 依旧非公平
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // 获取入队锁
        try {
            notFull.signal(); // 唤醒一个等待的入队线程
        } finally {
            putLock.unlock(); // 释放锁
        }
    }

    /**
     * 将节点链接到队列尾部（入队核心操作）
     * 前置条件：必须持有putLock
     * @param node 要入队的节点
     */
    private void enqueue(Node<E> node) {
        // 1. last.next指向新节点 2. last引用更新为新节点（一行代码完成，保证原子性）
        // 断言：putLock.isHeldByCurrentThread() && last.next == null
        last = last.next = node;
    }

    /**
     * 从队列头部移除节点（出队核心操作）
     * 前置条件：必须持有takeLock
     * XXX 移除的是哨兵节点，拿到的数据是哨兵节点后一个实际节点的数据
     * @return 出队节点的元素
     */
    private E dequeue() {
        // 断言：takeLock.isHeldByCurrentThread() && head.item == null
        // 临时引用头节点（哨兵）
        Node<E> h = head;
        // 第一个实际存储元素的节点
        Node<E> first = h.next;
        // 原头节点自引用，帮助GC回收
        h.next = h;
        // 头节点更新为第一个实际节点
        head = first;
        // 获取出队元素
        E x = first.item;
        // 清空节点元素，新头节点变为哨兵节点
        first.item = null;
        // 返回出队元素
        return x;
    }

    /**
     * 全锁：同时获取入队锁和出队锁（用于需要遍历/修改整个队列的操作，如remove/clear）
     * 加锁顺序：先putLock后takeLock，解锁顺序相反，避免死锁
     */
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * 全解锁：释放入队锁和出队锁
     * 解锁顺序：先takeLock后putLock（与加锁顺序相反）
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    /**
     * 构造器1：创建容量为Integer.MAX_VALUE的无界队列（实际是超大界）
     */
    public VariableLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    /**
     * 构造器2：创建指定固定容量的队列
     * @param capacity 队列容量
     * @throws IllegalArgumentException 容量<=0时抛出
     */
    public VariableLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        // 初始化头尾节点：哨兵节点（item=null）
        last = head = new Node<E>(null);
    }

    /**
     * 构造器3：从集合初始化队列，容量为Integer.MAX_VALUE
     * @param c 初始化的元素集合
     * @throws NullPointerException 集合或元素为null时抛出
     */
    public VariableLinkedBlockingQueue(Collection<? extends E> c) {
        // 调用构造器2
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        // 获取入队锁（保证可见性，无竞争）
        putLock.lock();
        try {
            // 计数
            int n = 0;
            for (E e : c) {
                // 禁止null元素（阻塞队列规范）
                if (e == null) {
                    throw new NullPointerException();
                }
                // 队列已满
                if (n == capacity) {
                    throw new IllegalStateException("Queue full");
                }
                // 元素入队
                enqueue(new Node<E>(e));
                // 计数+1
                ++n;
            }
            // 原子更新队列大小
            count.set(n);
        } finally {
            // 释放锁
            putLock.unlock();
        }
    }

    /**
     * 获取队列当前元素数量（覆盖父类方法，移除超大集合的注释）
     * @return 元素数量
     */
    @Override
    public int size() {
        // 原子获取计数
        return count.get();
    }

    /**
     * 核心扩展方法：动态修改队列容量
     * 关键逻辑：扩容后如果当前队列大小>=原容量且<新容量，唤醒等待的put线程
     * XXX 关键的修改队列容量的方法在这里
     * @param capacity 新容量
     */
    public void setCapacity(int capacity) {
        // 原容量
        final int oldCapacity = this.capacity;
        // 更新容量
        this.capacity = capacity;
        // 当前队列大小
        final int size = count.get();
        // 扩容场景：新容量>当前大小 且 当前大小>=原容量（说明之前队列满，现在有空间）
        // XXX 这里如果size >= oldCapacity的话，就要求一个新元素入队，size就要++
        if (capacity > size && size >= oldCapacity) {
            signalNotFull(); // 唤醒等待的put线程
        }
    }

    /**
     * 获取队列剩余容量（容量-当前大小）
     * 注意：并发下剩余容量仅供参考，可能获取后立即被其他线程修改
     * @return 剩余容量
     */
    @Override
    public int remainingCapacity() {
        return capacity - count.get();
    }

    /**
     * 阻塞入队：将元素插入队列尾部，队列满时阻塞等待（可中断）
     * @param e 要插入的元素
     * @throws InterruptedException 线程被中断时抛出
     * @throws NullPointerException 元素为null时抛出
     */
    @Override
    public void put(E e) throws InterruptedException {
        // 禁止null元素
        if (e == null) {
            throw new NullPointerException();
        }
        // 约定：c初始化为-1，表示入队失败，成功则更新为入队前的计数
        int c = -1;
        // 创建节点
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        // 获取可中断的入队锁
        putLock.lockInterruptibly();
        try {
            /*
             * 等待条件：队列满时阻塞（while循环防止虚假唤醒）
             * count未被锁保护，但此时其他put线程被阻塞，count只会减少（take操作）
             */
            while (count.get() >= capacity) {
                // 等待队列有空间
                notFull.await();
            }
            // 入队操作
            enqueue(node);
            // 原子更新计数（返回更新前的值）
            c = count.getAndIncrement();
            // 如果入队后队列仍未满，唤醒下一个等待的put线程（级联通知）
            if (c + 1 < capacity) {
                // 级联通知
                notFull.signal();
            }
        } finally {
            putLock.unlock(); // 释放锁
        }
        // 入队前队列为空（c=0），唤醒等待的take线程
        // XXX 这里c初始化为-1，且当前线程只入队一个元素，所以c只可能是0或-1
        if (c == 0) {
            // XXX 0表示入队成功，唤醒消费者
            signalNotEmpty();
        }
    }

    /**
     * 超时阻塞入队：队列满时等待指定时间，超时返回false
     * XXX 含超时时间的阻塞入队
     * @param e 要插入的元素
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 成功返回true，超时返回false
     * @throws InterruptedException 线程被中断时抛出
     * @throws NullPointerException 元素为null时抛出
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (e == null) {
            throw new NullPointerException();
        }
        // 转换为纳秒
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            // 队列满时等待
            while (count.get() >= capacity) {
                // 超时
                // XXX 如果>0返回的是没有用完的时间
                if (nanos <= 0) {
                    return false;
                }
                // 超时等待（返回剩余时间）
                nanos = notFull.awaitNanos(nanos);
            }
            // 入队
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                // 唤醒下一个put线程
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            // 入队前为空，唤醒take线程
            signalNotEmpty();
        }
        return true;
    }

    /**
     * 非阻塞式入队
     * 非阻塞入队：队列未满时插入元素，满时直接返回false（不阻塞）
     * @param e 要插入的元素
     * @return 成功返回true，失败返回false
     * @throws NullPointerException 元素为null时抛出
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final AtomicInteger count = this.count;
        // 快速失败：先检查容量（无锁，减少锁竞争）
        if (count.get() >= capacity) {
            return false;
        }
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // 获取入队锁
        try {
            // 再次检查（双重检查，防止竞态）
            if (count.get() < capacity) {
                // 入队
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity) {
                    // 唤醒下一个put线程
                    notFull.signal();
                }
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            // 入队前为空，唤醒take线程
            signalNotEmpty();
        }
        // c>=0表示入队成功
        return c >= 0;
    }

    /**
     * 阻塞出队：获取并移除队列头元素，队列为空时阻塞等待（可中断）
     * @return 队列头元素
     * @throws InterruptedException 线程被中断时抛出
     */
    @Override
    public E take() throws InterruptedException {
        E x;
        // 出队前的计数，初始-1
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        // 获取可中断的出队锁
        takeLock.lockInterruptibly();
        try {
            // 等待条件：队列为空时阻塞（while防止虚假唤醒）
            while (count.get() == 0) {
                notEmpty.await();
            }
            // 出队操作
            x = dequeue();
            // 原子更新计数（返回更新前的值）
            c = count.getAndDecrement();
            // 出队后队列仍有元素，唤醒下一个take线程
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            // 释放锁
            takeLock.unlock();
        }
        // 出队前队列满（c>=capacity），唤醒put线程
        if (c >= capacity) {
            signalNotFull();
        }
        return x;
    }

    /**
     * 超时阻塞出队：队列为空时等待指定时间，超时返回null
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 成功返回头元素，超时返回null
     * @throws InterruptedException 线程被中断时抛出
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        // 转换为纳秒
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 队列为空时等待
            while (count.get() == 0) {
                // 超时
                if (nanos <= 0) {
                    return null;
                }
                // 超时等待
                nanos = notEmpty.awaitNanos(nanos);
            }
            // 出队
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1) {
                // 唤醒下一个take线程
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        // 出队前队列满，唤醒put线程
        if (c >= capacity) {
            signalNotFull();
        }
        return x;
    }

    /**
     * 非阻塞出队：队列为空时直接返回null（不阻塞）
     * @return 成功返回头元素，失败返回null
     */
    @Override
    public E poll() {
        final AtomicInteger count = this.count;
        // 快速失败：无锁检查
        if (count.get() == 0) {
            return null;
        }
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock(); // 获取出队锁
        try {
            // 双重检查
            if (count.get() > 0) {
                // 出队
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1) {
                    // 唤醒下一个take线程
                    notEmpty.signal();
                }
            }
        } finally {
            takeLock.unlock();
        }
        // 出队前队列满，唤醒put线程
        if (c >= capacity) {
            signalNotFull();
        }
        return x;
    }

    /**
     * 查看队列头元素（不出队），队列为空时返回null
     * @return 队列头元素（null表示空）
     */
    @Override
    public E peek() {
        // 快速失败
        if (count.get() == 0) {
            return null;
        }
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock(); // 获取出队锁（保证可见性）
        try {
            // 第一个实际节点
            Node<E> first = head.next;
            if (first == null) {
                return null;
            } else {
                // 返回元素（不出队）
                return first.item;
            }
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 解除内部节点链接（用于remove操作）
     * XXX 这里是移除某一个节点
     * 前置条件：持有全锁
     * @param p 要移除的节点
     * @param trail p的前驱节点
     */
    void unlink(Node<E> p, Node<E> trail) {
        // 断言：isFullyLocked()
        // 清空元素
        p.item = null;
        // 前驱节点指向后继节点
        trail.next = p.next;
        // 移除的是尾节点，更新last
        if (last == p) {
            last = trail;
        }
        // 如果移除前队列满，唤醒put线程
        if (count.getAndDecrement() >= capacity) {
            notFull.signal();
        }
    }

    /**
     * 移除指定元素（如果存在）
     * XXX 因为移除操作是遍历+修改操作
     * @param o 要移除的元素
     * @return 移除成功返回true，否则false
     */
    @Override
    public boolean remove(Object o) {
        // 队列无null元素，直接返回false
        if (o == null) {
            return false;
        }
        fullyLock(); // 获取全锁
        try {
            // 遍历队列：trail前驱，p当前
            for (Node<E> trail = head, p = trail.next;
                 p != null;
                 trail = p, p = p.next) {
                // 找到元素
                if (o.equals(p.item)) {
                    // 解除链接
                    unlink(p, trail);
                    // 移除成功
                    return true;
                }
            }
            // 未找到
            return false;
        } finally {
            // 释放全锁
            fullyUnlock();
        }
    }

    /**
     * 检查队列是否包含指定元素
     * @param o 要检查的元素
     * @return 存在返回true，否则false
     */
    @Override
    public boolean contains(Object o) {
        // 无null元素
        if (o == null) {
            return false;
        }
        // 全锁保证遍历一致性
        fullyLock();
        try {
            // 遍历队列
            for (Node<E> p = head.next; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * XXX 获得实时快照
     * 转换为Object数组（按队列顺序）
     * @return 包含所有元素的数组
     */
    @Override
    public Object[] toArray() {
        fullyLock(); // 全锁保证数组完整性
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            // 遍历队列填充数组
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 转换为指定类型数组（泛型版）
     * @param a 目标数组
     * @return 包含所有元素的数组
     * @throws ArrayStoreException 数组类型不兼容时抛出
     * @throws NullPointerException 数组为null时抛出
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            // 数组容量不足，创建新数组
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(
                        a.getClass().getComponentType(), size);
            }

            int k = 0;
            // 填充数组
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            // 数组有剩余空间，末尾置null
            if (a.length > k) {
                a[k] = null;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 重写toString：按队列顺序输出元素
     * @return 队列的字符串表示
     */
    @Override
    public String toString() {
        fullyLock();
        try {
            Node<E> p = head.next;
            // 空队列
            if (p == null) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (;;) {
                E e = p.item;
                // 防止自引用
                sb.append(e == this ? "(this Collection)" : e);
                p = p.next;
                if (p == null) {
                    // 遍历结束
                    return sb.append(']').toString();
                }
                sb.append(',').append(' ');
            }
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 清空队列：移除所有元素
     */
    @Override
    public void clear() {
        fullyLock(); // 全锁
        try {
            // 遍历队列，清空节点并帮助GC
            // XXX for循环先进行条件判断的
            // TODO 如果head为null呢
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                // 自引用
                h.next = h;
                // 清空元素
                p.item = null;
            }
            // 重置头尾节点为哨兵
            head = last;
            // 清空前队列满，唤醒put线程
            if (count.getAndSet(0) >= capacity) {
                notFull.signal();
            }
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 批量出队：将队列所有元素转移到集合（无数量限制）
     * @param c 目标集合
     * @return 转移的元素数量
     */
    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * 批量出队：最多转移maxElements个元素到集合
     * @param c 目标集合
     * @param maxElements 最大转移数量
     * @return 实际转移的数量
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            // 集合为null
            throw new NullPointerException();
        }
        if (c == this) {
            // 不能转移到自身
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            // 数量<=0
            return 0;
        }
        // 是否需要唤醒put线程
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock(); // 获取出队锁
        try {
            // 实际可转移数量
            int n = Math.min(maxElements, count.get());
            Node<E> h = head;
            int i = 0;
            try {
                // 转移n个元素
                while (i < n) {
                    Node<E> p = h.next;
                    // 添加到集合
                    c.add(p.item);
                    // 清空元素
                    p.item = null;
                    // 自引用帮助GC
                    h.next = h;
                    h = p;
                    ++i;
                }
                // 返回转移数量
                return n;
            } finally {
                // 即使c.add抛异常，也要恢复队列状态
                if (i > 0) {
                    // 更新头节点
                    head = h;
                    // 转移前队列满，唤醒put线程
                    signalNotFull = (count.getAndAdd(-i) >= capacity);
                }
            }
        } finally {
            // 释放锁
            takeLock.unlock();
            if (signalNotFull) {
                // 唤醒put线程
                signalNotFull();
            }
        }
    }

    /**
     * 返回队列的迭代器（弱一致性）
     * 弱一致性：迭代过程中不抛出ConcurrentModificationException，可能看到迭代后的修改
     * @return 迭代器
     */
    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 内部迭代器类：实现弱一致性迭代
     */
    private class Itr implements Iterator<E> {
        /*
         * 弱一致性迭代器设计：
         * 1. 始终持有下一个要返回的元素，避免竞态
         * 2. 遍历过程中跳过已移除/出队的节点
         */

        private Node<E> current; // 当前节点
        private Node<E> lastRet; // 最后返回的节点（用于remove）
        private E currentElement; // 当前节点的元素

        // 迭代器构造器：初始化当前节点
        Itr() {
            fullyLock(); // 全锁保证初始化一致性
            try {
                current = head.next; // 第一个实际节点
                if (current != null) {
                    currentElement = current.item; // 初始化当前元素
                }
            } finally {
                fullyUnlock();
            }
        }

        /**
         * 是否有下一个元素
         * @return 有返回true，否则false
         */
        @Override
        public boolean hasNext() {
            return current != null;
        }

        /**
         * 获取p的下一个有效节点（跳过已移除/出队的节点）
         * @param p 当前节点
         * @return 下一个有效节点
         */
        private Node<E> nextNode(Node<E> p) {
            for (;;) {
                // XXX 并不会发生死循环，head.next要么为null，要么一定不为p
                Node<E> s = p.next;
                if (s == p) { // 节点已出队（自引用），跳回头节点的下一个
                    return head.next;
                }
                // 找到有效节点（非null且元素非null）
                if (s == null || s.item != null) {
                    // XXX 这里要不为null，表示最后一个节点，要不为有效节点
                    return s;
                }
                // 跳过item为null的节点
                p = s;
            }
        }

        /**
         * 获取下一个元素
         * @return 下一个元素
         * @throws NoSuchElementException 无元素时抛出
         */
        @Override
        public E next() {
            fullyLock(); // 全锁保证遍历安全
            try {
                if (current == null) { // 无下一个元素
                    throw new NoSuchElementException();
                }
                E x = currentElement; // 保存要返回的元素
                lastRet = current; // 记录最后返回的节点
                current = nextNode(current); // 移动到下一个节点
                // 更新当前元素
                currentElement = (current == null) ? null : current.item;
                return x; // 返回元素
            } finally {
                fullyUnlock();
            }
        }

        /**
         * 移除最后返回的元素
         * @throws IllegalStateException 未调用next或已移除时抛出
         */
        @Override
        public void remove() {
            // 未调用next或已移除
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            // 全锁
            fullyLock();
            try {
                Node<E> node = lastRet;
                // 重置，防止重复移除
                lastRet = null;
                // 遍历找到节点并移除
                for (Node<E> trail = head, p = trail.next;
                     p != null;
                     trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail); // 解除链接
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }

    /**
     * 分割迭代器（Spliterator）：支持并行遍历（JDK1.8+）
     * 定制化的Spliterator实现，弱一致性，支持有限并行
     * XXX 纯炫技了
     */
    static final class LBQSpliterator<E> implements Spliterator<E> {
        // 最大批量大小（33554432）
        static final int MAX_BATCH = 1 << 25;
        // 关联的队列
        final VariableLinkedBlockingQueue<E> queue;
        // 当前节点（初始化前为null）
        Node<E> current;
        // 分割的批量大小
        int batch;
        // 是否遍历完毕
        boolean exhausted;
        // 元素数量估计值
        long est;

        // 构造器：初始化队列和估计大小
        LBQSpliterator(VariableLinkedBlockingQueue<E> queue) {
            this.queue = queue;
            this.est = queue.size();
        }

        /**
         * 估计元素数量
         * @return 估计值
         */
        @Override
        public long estimateSize() {
            return est;
        }

        /**
         * 尝试分割：将迭代器拆分为两个，支持并行处理
         * @return 分割后的Spliterator，null表示无法分割
         */
        @Override
        public Spliterator<E> trySplit() {
            Node<E> h;
            final VariableLinkedBlockingQueue<E> q = this.queue;
            // 计算批量大小：1~MAX_BATCH
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            // 未遍历完且有节点
            if (!exhausted &&
                    ((h = current) != null || (h = q.head.next) != null) &&
                    // XXX 至少有两个节点才可风格
                    h.next != null) {
                // 批量数组
                Object[] a = new Object[n];
                int i = 0;
                Node<E> p = current;
                q.fullyLock(); // 全锁
                try {
                    // 初始化当前节点
                    if (p != null || (p = q.head.next) != null) {
                        // 填充批量数组
                        do {
                            if ((a[i] = p.item) != null) {
                                ++i;
                            }
                        } while ((p = p.next) != null && i < n);
                    }
                } finally {
                    q.fullyUnlock(); // 解锁
                }
                // 更新状态
                if ((current = p) == null) {
                    // 遍历完所有节点
                    est = 0L;
                    exhausted = true;
                } else if ((est -= i) < 0L) {
                    // 剩余元素估计量est不能为负数
                    est = 0L;
                }
                // 有有效元素，创建新的Spliterator
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator(a, 0, i,
                            Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                }
            }
            return null; // 无法分割
        }

        /**
         * 遍历剩余元素并应用action
         * @param action 元素处理逻辑
         */
        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            final VariableLinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                exhausted = true; // 标记遍历完毕
                Node<E> p = current;
                do {
                    E e = null;
                    // XXX 遍历的过程中加锁，保证锁的粒度最小
                    q.fullyLock(); // 全锁
                    try {
                        // 初始化当前节点
                        if (p == null) {
                            p = q.head.next;
                        }
                        // 找到下一个有效元素
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null) {
                                break;
                            }
                        }
                    } finally {
                        q.fullyUnlock(); // 解锁
                    }
                    if (e != null) {
                        action.accept(e); // 处理元素
                    }
                } while (p != null);
            }
        }

        /**
         * 尝试处理下一个元素
         * @param action 元素处理逻辑
         * @return 处理成功返回true，否则false
         */
        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            final VariableLinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                E e = null;
                q.fullyLock(); // 全锁
                try {
                    // 初始化当前节点
                    if (current == null) {
                        current = q.head.next;
                    }
                    // 找到下一个有效元素
                    while (current != null) {
                        e = current.item;
                        current = current.next;
                        if (e != null) {
                            break;
                        }
                    }
                } finally {
                    q.fullyUnlock(); // 解锁
                }
                // 更新遍历状态
                if (current == null) {
                    exhausted = true;
                }
                if (e != null) {
                    action.accept(e); // 处理元素
                    return true;
                }
            }
            return false;
        }

        /**
         * Spliterator特性：有序、非null、并发
         * @return 特性组合
         */
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT;
        }
    }

    /**
     * 返回队列的Spliterator（JDK1.8+）
     * @return Spliterator实例
     */
    @Override
    public Spliterator<E> spliterator() {
        return new LBQSpliterator<E>(this);
    }

    /**
     * 序列化：将队列写入流
     * XXX 这里甚至是私有方法
     * @param s 输出流
     * @throws java.io.IOException IO异常
     * 序列化数据：容量 → 元素（按顺序） → null（哨兵）
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        fullyLock(); // 全锁保证序列化一致性
        try {
            // 写入默认字段（容量等）
            s.defaultWriteObject();

            // 写入所有元素
            for (Node<E> p = head.next; p != null; p = p.next) {
                s.writeObject(p.item);
            }
            // 写入null作为结束标记
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 反序列化：从流恢复队列
     * XXX 这里甚至是私有方法
     * @param s 输入流
     * @throws ClassNotFoundException 类未找到
     * @throws java.io.IOException IO异常
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // 读取默认字段（容量）
        s.defaultReadObject();

        // 重置计数
        count.set(0);
        // 重置头尾节点

        last = head = new Node<E>(null);
        // 读取元素并入队
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E) s.readObject();
            // 结束标记
            if (item == null) {
                break;
            }
            // 元素入队
            add(item);
        }
    }
}