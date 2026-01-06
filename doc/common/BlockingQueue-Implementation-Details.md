# 阻塞队列实现细节详解

## 概述

本文档详细介绍了 JUC 包下的阻塞队列实现以及 DynamicTp 框架中的自定义阻塞队列实现细节。包括数据结构、线程安全机制、核心方法实现、性能特点等。

## 目录

1. [JUC 包下的阻塞队列](#juc-包下的阻塞队列)
   - [ArrayBlockingQueue](#1-arrayblockingqueue)
   - [LinkedBlockingQueue](#2-linkedblockingqueue)
   - [PriorityBlockingQueue](#3-priorityblockingqueue)
   - [DelayQueue](#4-delayqueue)
   - [SynchronousQueue](#5-synchronousqueue)
   - [LinkedTransferQueue](#6-linkedtransferqueue)
   - [LinkedBlockingDeque](#7-linkedblockingdeque)
2. [自定义阻塞队列](#自定义阻塞队列)
   - [VariableLinkedBlockingQueue](#1-variablelinkedblockingqueue)
   - [MemorySafeLinkedBlockingQueue](#2-memorysafelinkedblockingqueue)
   - [TaskQueue](#3-taskqueue)

---

## JUC 包下的阻塞队列

### 1. ArrayBlockingQueue

#### 概述

`ArrayBlockingQueue` 是一个基于数组实现的有界阻塞队列，采用 FIFO（先进先出）原则。

#### 数据结构

```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    /** 存储元素的数组 */
    final Object[] items;
    
    /** 下一个 take, poll, peek 或 remove 操作的索引 */
    int takeIndex;
    
    /** 下一个 put, offer 或 add 操作的索引 */
    int putIndex;
    
    /** 队列中的元素数量 */
    int count;
    
    /** 控制所有访问的主锁 */
    final ReentrantLock lock;
    
    /** 等待 take 的条件 */
    private final Condition notEmpty;
    
    /** 等待 put 的条件 */
    private final Condition notFull;
    
    /** 可选公平锁 */
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }
}
```

#### 核心特点

1. **有界队列**: 初始化时必须指定容量，无法动态扩容
2. **单锁机制**: 使用一把 `ReentrantLock` 控制所有操作
3. **公平/非公平锁**: 支持公平锁模式，避免线程饥饿
4. **循环数组**: 使用 `takeIndex` 和 `putIndex` 实现循环数组

#### 核心方法实现

##### put(E e) - 阻塞插入

```java
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();  // 可中断获取锁
    try {
        while (count == items.length)  // 队列满时等待
            notFull.await();
        enqueue(e);  // 入队
    } finally {
        lock.unlock();
    }
}

private void enqueue(E x) {
    final Object[] items = this.items;
    items[putIndex] = x;
    if (++putIndex == items.length)  // 循环数组
        putIndex = 0;
    count++;
    notEmpty.signal();  // 唤醒等待的消费者
}
```

##### take() - 阻塞获取

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)  // 队列空时等待
            notEmpty.await();
        return dequeue();
    } finally {
        lock.unlock();
    }
}

private E dequeue() {
    final Object[] items = this.items;
    @SuppressWarnings("unchecked")
    E x = (E) items[takeIndex];
    items[takeIndex] = null;  // 帮助 GC
    if (++takeIndex == items.length)  // 循环数组
        takeIndex = 0;
    count--;
    notFull.signal();  // 唤醒等待的生产者
    return x;
}
```

#### 性能特点

- **优点**:
  - 内存占用固定，可预测
  - 数组访问性能好（缓存友好）
  - 支持公平锁，避免线程饥饿
- **缺点**:
  - 单锁机制，生产消费不能并行
  - 容量固定，无法动态调整
  - 并发性能一般

#### 适用场景

- 需要严格控制队列容量的场景
- 对内存占用有明确限制的场景
- 固定大小的任务缓冲池

---

### 2. LinkedBlockingQueue

#### 概述

`LinkedBlockingQueue` 是一个基于链表实现的可选界阻塞队列，采用 FIFO 原则。

#### 数据结构

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    /** 链表节点 */
    static class Node<E> {
        E item;
        Node<E> next;
        Node(E x) { item = x; }
    }
    
    /** 队列容量，默认为 Integer.MAX_VALUE */
    private final int capacity;
    
    /** 当前元素数量 */
    private final AtomicInteger count = new AtomicInteger();
    
    /** 链表头节点（head.item == null） */
    transient Node<E> head;
    
    /** 链表尾节点 */
    private transient Node<E> last;
    
    /** take, poll 等操作持有的锁 */
    private final ReentrantLock takeLock = new ReentrantLock();
    
    /** 等待 take 的条件 */
    private final Condition notEmpty = takeLock.newCondition();
    
    /** put, offer 等操作持有的锁 */
    private final ReentrantLock putLock = new ReentrantLock();
    
    /** 等待 put 的条件 */
    private final Condition notFull = putLock.newCondition();
}
```

#### 核心特点

1. **双锁机制**: 使用两把锁分别控制生产和消费，可并行执行
2. **可选界**: 默认无界（Integer.MAX_VALUE），也可指定容量
3. **链表结构**: 基于单向链表，动态分配内存
4. **高并发**: 生产消费可并行，性能优于 ArrayBlockingQueue

#### 核心方法实现

##### put(E e) - 阻塞插入

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        while (count.get() >= capacity) {  // 队列满时等待
            notFull.await();
        }
        enqueue(node);  // 入队
        c = count.getAndIncrement();  // 原子递增
        if (c + 1 < capacity)
            notFull.signal();  // 唤醒其他生产者
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();  // 如果之前队列为空，唤醒消费者
}

private void enqueue(Node<E> node) {
    last = last.next = node;  // 添加到链表尾部
}
```

##### take() - 阻塞获取

```java
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {  // 队列空时等待
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();  // 原子递减
        if (c > 1)
            notEmpty.signal();  // 唤醒其他消费者
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();  // 如果之前队列满，唤醒生产者
    return x;
}

private E dequeue() {
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h;  // 帮助 GC
    head = first;
    E x = first.item;
    first.item = null;  // 帮助 GC
    return x;
}
```

#### 性能特点

- **优点**:
  - 双锁机制，生产消费可并行
  - 容量灵活，可无界或有界
  - 高并发性能好
- **缺点**:
  - 链表节点动态分配，内存占用不固定
  - 迭代器遍历效率相对较低

#### 适用场景

- 任务量不可预估的场景
- 需要高并发生产消费的场景
- 通用的线程池任务队列

---

### 3. PriorityBlockingQueue

#### 概述

`PriorityBlockingQueue` 是一个基于数组实现的无界阻塞队列，元素按优先级排序。

#### 数据结构

```java
public class PriorityBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    /** 存储元素的数组（堆结构） */
    private transient Object[] queue;
    
    /** 元素数量 */
    private transient int size;
    
    /** 比较器，如果为 null 则使用自然序 */
    private transient Comparator<? super E> comparator;
    
    /** 控制所有访问的锁 */
    private final ReentrantLock lock;
    
    /** 等待 take 的条件 */
    private final Condition notEmpty;
    
    /** 自旋锁，用于分配数组 */
    private transient volatile int allocationSpinLock;
}
```

#### 核心特点

1. **堆结构**: 使用数组实现二叉堆（最小堆或最大堆）
2. **无界队列**: 容量可动态扩容，受内存限制
3. **优先级排序**: 元素按优先级排序，而非 FIFO
4. **单锁机制**: 使用一把锁控制所有操作

#### 核心方法实现

##### offer(E e) - 插入元素

```java
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        int n = size;
        if (n >= queue.length)
            grow();  // 扩容
        size = n + 1;
        if (n == 0)
            queue[0] = e;
        else
            siftUp(n, e);  // 上浮调整堆
        notEmpty.signal();
        return true;
    } finally {
        lock.unlock();
    }
}

private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x);
    else
        siftUpComparable(k, x);
}

private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;  // 父节点索引
        Object e = queue[parent];
        if (key.compareTo((E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}
```

##### take() - 获取最小元素

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    E result;
    try {
        while ( (result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;
}

private E dequeue() {
    int n = size - 1;
    if (n < 0)
        return null;
    else {
        Object[] array = queue;
        E result = (E) array[0];  // 堆顶元素
        E x = (E) array[n];
        array[n] = null;
        Comparator<? super E> cmp = comparator;
        if (cmp == null)
            siftDownComparable(0, x, array, n);
        else
            siftDownUsingComparator(0, x, array, n, cmp);
        size = n;
        return result;
    }
}

private void siftDownComparable(int k, E x, Object[] array, int n) {
    if (n > 0) {
        Comparable<? super E> key = (Comparable<? super E>)x;
        int half = n >>> 1;  // 非叶子节点数量
        while (k < half) {
            int child = (k << 1) + 1;  // 左子节点
            Object c = array[child];
            int right = child + 1;
            if (right < n &&
                ((Comparable<? super E>) c).compareTo((E) array[right]) > 0)
                c = array[child = right];
            if (key.compareTo((E) c) <= 0)
                break;
            array[k] = c;
            k = child;
        }
        array[k] = key;
    }
}
```

#### 性能特点

- **优点**:
  - 支持优先级排序
  - 无界队列，容量可动态扩容
  - 堆结构，插入和删除时间复杂度 O(log n)
- **缺点**:
  - 单锁机制，并发性能一般
  - 不保证同优先级元素的顺序
  - 堆调整有性能开销

#### 适用场景

- 需要按优先级处理任务的场景
- 紧急任务优先执行
- 定时任务排序

---

### 4. DelayQueue

#### 概述

`DelayQueue` 是一个基于 `PriorityBlockingQueue` 实现的无界阻塞队列，元素需实现 `Delayed` 接口。

#### 数据结构

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
        implements BlockingQueue<E> {
    
    /** 底层使用 PriorityBlockingQueue */
    private final PriorityBlockingQueue<E> q = new PriorityBlockingQueue<E>();
    
    /** 控制所有访问的锁 */
    private final transient ReentrantLock lock = new ReentrantLock();
    
    /** 等待队首元素的线程 */
    private final Condition available = lock.newCondition();
    
    /** 当前等待队首元素的线程（优化） */
    private Thread leader = null;
}
```

#### 核心特点

1. **延迟队列**: 元素仅在延迟时间到期后才可被消费
2. **基于优先级队列**: 底层使用 `PriorityBlockingQueue`，按延迟时间排序
3. **无界队列**: 容量无限制，受内存限制
4. **Leader-Follower 模式**: 优化多线程等待性能

#### 核心方法实现

##### offer(E e) - 插入元素

```java
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e);  // 插入到优先级队列
        if (q.peek() == e) {  // 如果新元素是队首
            leader = null;
            available.signal();  // 唤醒等待的线程
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

##### take() - 获取到期元素

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();  // 获取队首元素
            if (first == null)
                available.await();  // 队列空，等待
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();  // 延迟到期，返回
                first = null;  // 释放引用，帮助 GC
                if (leader != null)
                    available.await();  // 已有 leader，等待
                else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);  // 等待延迟时间
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

#### 性能特点

- **优点**:
  - 支持延迟执行
  - Leader-Follower 模式优化多线程等待
  - 无界队列
- **缺点**:
  - 单锁机制
  - 需要频繁检查延迟时间

#### 适用场景

- 定时任务
- 延迟通知
- 缓存过期清理
- ScheduledThreadPoolExecutor 的任务队列

---

### 5. SynchronousQueue

#### 概述

`SynchronousQueue` 是一个无缓冲的同步阻塞队列，每个 put 操作必须等待一个 take 操作。

#### 数据结构

```java
public class SynchronousQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    /** 传输器，实现生产消费匹配 */
    private transient volatile Transferer<E> transferer;
    
    /** 公平模式使用队列 */
    static final class TransferQueue<E> extends Transferer<E> {
        static final class QNode {
            volatile QNode next;
            volatile Object item;
            volatile Thread waiter;
            final boolean isData;
        }
        
        transient volatile QNode head;
        transient volatile QNode tail;
    }
    
    /** 非公平模式使用栈 */
    static final class TransferStack<E> extends Transferer<E> {
        static final class SNode {
            volatile SNode next;
            volatile SNode match;
            volatile Thread waiter;
            Object item;
            int mode;
        }
        
        volatile SNode head;
    }
}
```

#### 核心特点

1. **零容量**: 不存储任何元素
2. **同步传输**: 生产消费必须严格同步
3. **公平/非公平模式**: 支持两种模式
4. **高性能**: 无队列存储开销，性能极高

#### 核心方法实现

##### transfer(E e, boolean timed, long nanos) - 核心传输方法

```java
// 非公平模式（栈）
E transfer(E e, boolean timed, long nanos) {
    SNode s = null;
    int mode = (e == null) ? REQUEST : DATA;  // REQUEST=消费，DATA=生产
    
    for (;;) {
        SNode h = head;
        if (h == null || h.mode == mode) {  // 栈空或模式相同
            if (timed && nanos <= 0) {
                if (h != null && h.isCancelled())
                    casHead(h, h.next);
                else
                    return null;
            } else if (casHead(h, s = snode(s, e, h, mode))) {
                SNode m = awaitFulfill(s, timed, nanos);
                if (m == s) {
                    clean(s);
                    return null;
                }
                if ((h = head) != null && h.next == s)
                    casHead(h, s.next);
                return (E) ((mode == REQUEST) ? m.item : s.item);
            }
        } else if (!isFulfilling(h.mode)) {  // 模式不同，可以匹配
            if (h.isCancelled())
                casHead(h, h.next);
            else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                for (;;) {
                    SNode m = s.next;
                    if (m == null) {
                        casHead(s, null);
                        s = null;
                        break;
                    }
                    SNode mn = m.next;
                    if (m.tryMatch(s)) {
                        casHead(s, mn);
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    } else
                        s.casNext(m, mn);
                }
            }
        } else {
            SNode m = h.next;
            if (m == null)
                casHead(h, null);
            else {
                SNode mn = m.next;
                if (m.tryMatch(h))
                    casHead(h, mn);
                else
                    h.casNext(m, mn);
            }
        }
    }
}
```

#### 性能特点

- **优点**:
  - 零存储开销
  - 性能极高
  - 生产消费严格同步
- **缺点**:
  - 无缓冲，生产必须等待消费
  - 不适合生产消费速度不匹配的场景

#### 适用场景

- 生产消费严格同步的场景
- 任务需立即执行的场景
- CachedThreadPool 的默认队列

---

### 6. LinkedTransferQueue

#### 概述

`LinkedTransferQueue` 是一个基于链表实现的无界阻塞队列，支持 transfer 操作。

#### 数据结构

```java
public class LinkedTransferQueue<E> extends AbstractQueue<E>
        implements TransferQueue<E>, java.io.Serializable {
    
    /** 节点类型 */
    static final class Node {
        final boolean isData;  // true=数据节点，false=请求节点
        volatile Object item;
        volatile Node next;
        volatile Thread waiter;
    }
    
    /** 队列头节点 */
    transient volatile Node head;
    
    /** 队列尾节点 */
    private transient volatile Node tail;
}
```

#### 核心特点

1. **无锁实现**: 使用 CAS 操作实现无锁并发
2. **Transfer 操作**: 支持 transfer 方法，生产线程可阻塞直到元素被消费
3. **无界队列**: 容量无限制
4. **高性能**: CAS + 自旋，性能优于 LinkedBlockingQueue

#### 核心方法实现

##### transfer(E e) - 传输元素

```java
public void transfer(E e) throws InterruptedException {
    if (xfer(e, true, SYNC, 0) != null) {
        Thread.interrupted();
        throw new InterruptedException();
    }
}

private E xfer(E e, boolean haveData, int how, long nanos) {
    if (haveData && (e == null))
        throw new NullPointerException();
    Node s = null;
    boolean putData = haveData;
    
    for (;;) {
        Node h = head;
        Node t = tail;
        if (h == t || t.isData == putData) {  // 队列空或模式相同
            Node tn = t.next;
            if (t != tail)
                continue;
            if (tn != null) {
                advanceTail(t, tn);
                continue;
            }
            if (how == NOW)
                return putData ? e : null;
            if (s == null)
                s = new Node(e, putData);
            if (!t.casNext(null, s))
                continue;
            advanceTail(t, s);
            return awaitMatch(s, t, e, (how == TIMED), nanos);
        } else {  // 模式不同，可以匹配
            Node m = h.next;
            if (t != tail || m == null || h != head)
                continue;
            Object x = m.item;
            if (m.isData == putData ||
                (x != null && x != m) ||
                !m.casItem(x, e)) {
                advanceHead(h, m);
                continue;
            }
            advanceHead(h, m);
            LockSupport.unpark(m.waiter);
            return putData ? (E)x : e;
        }
    }
}
```

#### 性能特点

- **优点**:
  - 无锁实现，性能高
  - 支持 transfer 操作
  - 无界队列
- **缺点**:
  - 实现复杂
  - CAS 自旋可能消耗 CPU

#### 适用场景

- 高并发的生产消费场景
- 需要灵活控制元素转移策略的场景
- 高性能 RPC 框架的消息队列

---

### 7. LinkedBlockingDeque

#### 概述

`LinkedBlockingDeque` 是一个基于双向链表实现的可选界阻塞队列，支持从头部和尾部操作。

#### 数据结构

```java
public class LinkedBlockingDeque<E>
        extends AbstractQueue<E>
        implements BlockingDeque<E>, java.io.Serializable {
    
    /** 双向链表节点 */
    static final class Node<E> {
        E item;
        Node<E> prev;
        Node<E> next;
        Node(E x) {
            item = x;
        }
    }
    
    /** 队列头节点 */
    transient Node<E> first;
    
    /** 队列尾节点 */
    transient Node<E> last;
    
    /** 元素数量 */
    private transient int count;
    
    /** 队列容量 */
    private final int capacity;
    
    /** 控制所有访问的锁 */
    final ReentrantLock lock = new ReentrantLock();
    
    /** 等待 take 的条件 */
    private final Condition notEmpty = lock.newCondition();
    
    /** 等待 put 的条件 */
    private final Condition notFull = lock.newCondition();
}
```

#### 核心特点

1. **双向队列**: 支持从头部和尾部操作
2. **可选界**: 默认无界，也可指定容量
3. **单锁机制**: 使用一把锁控制所有操作
4. **灵活操作**: 可作为栈或队列使用

#### 核心方法实现

##### addFirst(E e) - 从头部添加

```java
public void addFirst(E e) {
    if (!offerFirst(e))
        throw new IllegalStateException("Deque full");
}

public boolean offerFirst(E e) {
    if (e == null) throw new NullPointerException();
    Node<E> node = new Node<E>(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return linkFirst(node);
    } finally {
        lock.unlock();
    }
}

private boolean linkFirst(Node<E> node) {
    if (count >= capacity)
        return false;
    Node<E> f = first;
    node.next = f;
    first = node;
    if (last == null)
        last = node;
    else
        f.prev = node;
    ++count;
    notEmpty.signal();
    return true;
}
```

##### takeLast() - 从尾部获取

```java
public E takeLast() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E x;
        while ( (x = unlinkLast()) == null)
            notEmpty.await();
        return x;
    } finally {
        lock.unlock();
    }
}

private E unlinkLast() {
    Node<E> l = last;
    if (l == null)
        return null;
    Node<E> p = l.prev;
    E item = l.item;
    l.item = null;
    l.prev = l;
    last = p;
    if (p == null)
        first = null;
    else
        p.next = null;
    --count;
    notFull.signal();
    return item;
}
```

#### 性能特点

- **优点**:
  - 支持双向操作
  - 灵活，可作为栈或队列
  - 可选界
- **缺点**:
  - 单锁机制
  - 双向链表维护开销

#### 适用场景

- 需要双向操作队列的场景
- 任务队列的头尾都可消费
- 生产者可插队到队首

---

## 自定义阻塞队列

### 1. VariableLinkedBlockingQueue

#### 概述

`VariableLinkedBlockingQueue` 是 `LinkedBlockingQueue` 的克隆版本，增加了 `setCapacity(int)` 方法，支持动态修改队列容量。

#### 数据结构

```java
public class VariableLinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    /** 队列容量（可变） */
    private int capacity;
    
    /** 当前元素数量 */
    private final AtomicInteger count = new AtomicInteger();
    
    /** 链表节点 */
    static class Node<E> {
        E item;
        Node<E> next;
        Node(E x) { item = x; }
    }
    
    /** 链表头节点 */
    transient Node<E> head;
    
    /** 链表尾节点 */
    private transient Node<E> last;
    
    /** take 锁 */
    private final ReentrantLock takeLock = new ReentrantLock();
    
    /** put 锁 */
    private final ReentrantLock putLock = new ReentrantLock();
    
    /** 等待 take 的条件 */
    private final Condition notEmpty = takeLock.newCondition();
    
    /** 等待 put 的条件 */
    private final Condition notFull = putLock.newCondition();
}
```

#### 核心特点

1. **动态容量**: 支持运行时修改队列容量
2. **双锁机制**: 与 `LinkedBlockingQueue` 相同的双锁设计
3. **完全兼容**: 与 `LinkedBlockingQueue` API 完全兼容

#### 核心方法实现

##### setCapacity(int capacity) - 动态设置容量

```java
/**
 * Set a new capacity for the queue. Increasing the capacity can
 * cause any waiting {@link #put(Object)} invocations to succeed if the new
 * capacity is larger than the queue.
 * @param capacity the new capacity for the queue
 */
public void setCapacity(int capacity) {
    final int oldCapacity = this.capacity;
    this.capacity = capacity;
    final int size = count.get();
    if (capacity > size && size >= oldCapacity) {
        signalNotFull();  // 如果新容量大于当前大小且旧容量已满，唤醒等待的生产者
    }
}
```

##### put(E e) - 阻塞插入

```java
@Override
public void put(E e) throws InterruptedException {
    if (e == null) {
        throw new NullPointerException();
    }
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        while (count.get() >= capacity) {  // 使用可变的 capacity
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        if (c + 1 < capacity) {
            notFull.signal();
        }
    } finally {
        putLock.unlock();
    }
    if (c == 0) {
        signalNotEmpty();
    }
}
```

#### 使用场景

在 DynamicTp 中，当线程池配置动态刷新时，可以动态调整队列容量：

```java
// DtpRegistry.java
private static void updateQueueProps(ExecutorAdapter<?> executor, DtpExecutorProps props) {
    val blockingQueue = executor.getQueue();
    if (blockingQueue instanceof VariableLinkedBlockingQueue) {
        int capacity = blockingQueue.size() + blockingQueue.remainingCapacity();
        if (!Objects.equals(capacity, props.getQueueCapacity())) {
            ((VariableLinkedBlockingQueue<Runnable>) blockingQueue).setCapacity(props.getQueueCapacity());
            executor.onRefreshQueueCapacity(props.getQueueCapacity());
        }
    }
}
```

#### 优势

- **动态调整**: 支持运行时修改容量，无需重建队列
- **无缝集成**: 与现有代码完全兼容
- **线程安全**: 使用双锁机制保证线程安全

---

### 2. MemorySafeLinkedBlockingQueue

#### 概述

`MemorySafeLinkedBlockingQueue` 继承自 `VariableLinkedBlockingQueue`，增加了内存安全检查，可以完全解决 `LinkedBlockingQueue` 导致的 OOM 问题。

#### 数据结构

```java
public class MemorySafeLinkedBlockingQueue<E> extends VariableLinkedBlockingQueue<E> {
    
    /** 默认最大空闲内存：16MB */
    public static final int THE_16_MB = 16 * 1024 * 1024;
    
    /** 最大空闲内存限制 */
    private int maxFreeMemory;
}
```

#### 核心特点

1. **内存安全**: 在入队前检查可用内存
2. **防止 OOM**: 当可用内存不足时拒绝入队
3. **动态监控**: 使用 `MemoryLimitCalculator` 实时监控可用内存

#### 核心方法实现

##### hasRemainedMemory() - 检查可用内存

```java
/**
 * determine if there is any remaining free memory.
 *
 * @return true if has free memory
 */
public boolean hasRemainedMemory() {
    if (MemoryLimitCalculator.maxAvailable() > maxFreeMemory) {
        return true;
    }
    throw new RejectedExecutionException("No more memory can be used.");
}
```

##### put(E e) - 带内存检查的插入

```java
@Override
public void put(final E e) throws InterruptedException {
    if (hasRemainedMemory()) {  // 先检查内存
        super.put(e);
    }
}
```

##### offer(E e) - 带内存检查的插入

```java
@Override
public boolean offer(final E e) {
    return hasRemainedMemory() && super.offer(e);
}
```

#### MemoryLimitCalculator 实现

```java
public class MemoryLimitCalculator {
    
    private static volatile long maxAvailable;
    
    private static final ScheduledExecutorService SCHEDULER = 
        Executors.newSingleThreadScheduledExecutor();
    
    static {
        refresh();  // 立即刷新
        // 每 50ms 刷新一次，提高性能
        SCHEDULER.scheduleWithFixedDelay(
            MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }
    
    private static void refresh() {
        maxAvailable = Runtime.getRuntime().freeMemory();
    }
    
    public static long maxAvailable() {
        return maxAvailable;
    }
}
```

#### 工作原理

1. **内存监控**: `MemoryLimitCalculator` 每 50ms 刷新一次可用内存
2. **入队检查**: 每次入队前检查 `Runtime.getRuntime().freeMemory()` 是否大于 `maxFreeMemory`
3. **拒绝策略**: 如果内存不足，抛出 `RejectedExecutionException`

#### 使用场景

```java
// QueueTypeEnum.java
public static BlockingQueue<Runnable> buildLbq(String name, int capacity, 
                                               boolean fair, int maxFreeMemory) {
    if (Objects.equals(name, MEMORY_SAFE_LINKED_BLOCKING_QUEUE.getName())) {
        blockingQueue = new MemorySafeLinkedBlockingQueue<>(
            capacity, maxFreeMemory * M_1);  // M_1 = 1024 * 1024
    }
}
```

#### 优势

- **防止 OOM**: 有效防止因队列无限增长导致的 OOM
- **内存可控**: 可配置最大空闲内存限制
- **实时监控**: 实时监控 JVM 可用内存

---

### 3. TaskQueue

#### 概述

`TaskQueue` 是 `EagerDtpExecutor` 专用的任务队列，继承自 `VariableLinkedBlockingQueue`，根据执行器状态智能决定是否入队。

#### 数据结构

```java
public class TaskQueue extends VariableLinkedBlockingQueue<Runnable> {
    
    /** 关联的 EagerDtpExecutor */
    private transient EagerDtpExecutor executor;
}
```

#### 核心特点

1. **智能入队**: 根据执行器状态决定是否入队
2. **快速响应**: 队列满时立即创建线程
3. **IO 密集型优化**: 适用于 IO 密集型场景

#### 核心方法实现

##### offer(Runnable runnable) - 智能入队

```java
@Override
public boolean offer(@NonNull Runnable runnable) {
    if (executor == null) {
        throw new RejectedExecutionException("The task queue does not have executor.");
    }
    
    // 1. 如果已达到最大线程数，直接入队
    if (executor.getPoolSize() == executor.getMaximumPoolSize()) {
        return super.offer(runnable);
    }
    
    // 2. 如果有空闲线程，将任务放入队列
    // submittedTaskCount <= poolSize 表示有线程空闲
    if (executor.getSubmittedTaskCount() <= executor.getPoolSize()) {
        return super.offer(runnable);
    }
    
    // 3. 如果没有空闲线程且未达到最大线程数，返回 false
    // 让执行器创建新线程处理任务
    if (executor.getPoolSize() < executor.getMaximumPoolSize()) {
        return false;  // 返回 false 触发创建新线程
    }
    
    // 4. 当前线程数 >= 最大线程数，入队
    return super.offer(runnable);
}
```

#### 工作原理

1. **检查最大线程数**: 如果已达到最大线程数，直接入队
2. **检查空闲线程**: 如果有空闲线程（`submittedTaskCount <= poolSize`），将任务放入队列
3. **创建新线程**: 如果没有空闲线程且未达到最大线程数，返回 `false`，触发 `ThreadPoolExecutor` 创建新线程
4. **兜底入队**: 如果当前线程数已满，入队等待

#### 使用场景

`TaskQueue` 主要用于 IO 密集型场景，在 `EagerDtpExecutor` 中使用：

```java
// EagerDtpExecutor.java
public EagerDtpExecutor(int corePoolSize,
                        int maximumPoolSize,
                        long keepAliveTime,
                        TimeUnit unit,
                        int queueCapacity,
                        ThreadFactory threadFactory,
                        RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
          new TaskQueue(queueCapacity), threadFactory, handler);
    ((TaskQueue) getQueue()).setExecutor(this);
}
```

#### 优势

- **快速响应**: 队列满时立即创建线程，避免任务等待
- **资源利用**: 充分利用线程资源，避免线程空闲
- **IO 优化**: 特别适合 IO 密集型场景

---

## 性能对比

### 吞吐量对比

| 队列类型 | 生产消费并行 | 吞吐量 | 适用场景 |
|---------|------------|--------|---------|
| ArrayBlockingQueue | ❌ | 中等 | 固定容量 |
| LinkedBlockingQueue | ✅ | 高 | 通用场景 |
| PriorityBlockingQueue | ❌ | 中等 | 优先级排序 |
| DelayQueue | ❌ | 中等 | 延迟任务 |
| SynchronousQueue | ✅ | 极高 | 同步传输 |
| LinkedTransferQueue | ✅ | 很高 | 高并发 |
| LinkedBlockingDeque | ❌ | 中等 | 双向操作 |
| VariableLinkedBlockingQueue | ✅ | 高 | 动态容量 |
| MemorySafeLinkedBlockingQueue | ✅ | 高 | 内存安全 |
| TaskQueue | ✅ | 高 | IO 密集型 |

### 内存占用对比

| 队列类型 | 内存占用 | 说明 |
|---------|---------|------|
| ArrayBlockingQueue | 固定 | 数组固定大小 |
| LinkedBlockingQueue | 动态 | 链表节点动态分配 |
| PriorityBlockingQueue | 动态 | 数组可扩容 |
| DelayQueue | 动态 | 基于 PriorityBlockingQueue |
| SynchronousQueue | 零 | 不存储元素 |
| LinkedTransferQueue | 动态 | 链表节点动态分配 |
| LinkedBlockingDeque | 动态 | 双向链表 |
| VariableLinkedBlockingQueue | 动态 | 继承 LinkedBlockingQueue |
| MemorySafeLinkedBlockingQueue | 动态+限制 | 有内存限制 |
| TaskQueue | 动态 | 继承 VariableLinkedBlockingQueue |

---

## 选择建议

### 根据场景选择

1. **固定容量场景**: `ArrayBlockingQueue`
2. **通用场景**: `LinkedBlockingQueue` 或 `VariableLinkedBlockingQueue`
3. **优先级场景**: `PriorityBlockingQueue`
4. **延迟任务**: `DelayQueue`
5. **同步传输**: `SynchronousQueue`
6. **高并发场景**: `LinkedTransferQueue`
7. **双向操作**: `LinkedBlockingDeque`
8. **内存受限**: `MemorySafeLinkedBlockingQueue`
9. **IO 密集型**: `TaskQueue`（配合 `EagerDtpExecutor`）

### 根据需求选择

- **需要动态调整容量**: `VariableLinkedBlockingQueue`
- **需要防止 OOM**: `MemorySafeLinkedBlockingQueue`
- **需要智能入队**: `TaskQueue`
- **需要高吞吐量**: `LinkedTransferQueue` 或 `SynchronousQueue`
- **需要公平性**: `ArrayBlockingQueue`（公平模式）

---

## 总结

本文档详细介绍了 JUC 包下的 7 种阻塞队列和 DynamicTp 框架中的 3 种自定义阻塞队列的实现细节。每种队列都有其特定的使用场景和性能特点，选择合适的队列类型对于系统性能至关重要。

### 关键要点

1. **JUC 队列**: 提供了丰富的阻塞队列实现，覆盖各种场景
2. **自定义队列**: DynamicTp 扩展了队列功能，支持动态调整和内存安全
3. **性能优化**: 通过双锁、CAS、Leader-Follower 等机制优化性能
4. **场景适配**: 不同场景需要选择不同的队列类型

### 最佳实践

1. **优先使用标准队列**: 优先考虑 JUC 提供的标准队列
2. **动态调整**: 需要动态调整容量时使用 `VariableLinkedBlockingQueue`
3. **内存安全**: 内存受限场景使用 `MemorySafeLinkedBlockingQueue`
4. **IO 优化**: IO 密集型场景使用 `TaskQueue` 配合 `EagerDtpExecutor`
5. **性能测试**: 根据实际场景进行性能测试，选择最优队列

