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

package org.dromara.dynamictp.core.metric;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * LimitedUniformReservoir related
 * Reservoir 接口定义了一个 “指标样本蓄水池”（也可称为 “样本存储容器”） 的规范，其核心职责是收集、存储一组动态变化的数值型指标样本，并支持生成样本快照用于后续统计分析。
 * 简单理解：它就像一个 “蓄水池”，不断接收应用运行时产生的指标数据（如接口单次响应时间）并存储，当需要统计分析（如计算平均值、分位数）时，它能提供一份 “冻结” 的样本快照，避免统计过程中样本数据被修改。
 * @author yanhom
 * @since 1.1.5
 */
@SuppressWarnings("all")
public class LimitedUniformReservoir implements Reservoir {

    private static final int DEFAULT_SIZE = 4096;

    private static final int BITS_PER_LONG = 63;

    private final AtomicLong count = new AtomicLong();

    private volatile AtomicLongArray values = new AtomicLongArray(DEFAULT_SIZE);

    @Override
    public int size() {
        final long c = count.get();
        if (c > values.length()) {
            return values.length();
        }
        return (int) c;
    }

    @Override
    public void update(long value) {
        final long c = count.incrementAndGet();
        if (c <= values.length()) {
            values.set((int) c - 1, value);
        } else {
            // 超过数组长度，随机选择位置替换
            final long r = nextLong(c);
            if (r < values.length()) {
                values.set((int) r, value);
            }
        }
    }

    @Override
    public Snapshot getSnapshot() {
        final int s = size();
        final List<Long> copy = new ArrayList<>(s);
        for (int i = 0; i < s; i++) {
            copy.add(values.get(i));
        }
        // 返回排序后的存储值
        return new UniformSnapshot(copy);
    }

    public void reset() {
        count.set(0);
        values = new AtomicLongArray(DEFAULT_SIZE);
    }

    /**
     * 基于无偏候选随机数生成概率相等的随机数
     * @param n
     * @return
     */
    private static long nextLong(long n) {
        long bits;
        long val;
        do {
            bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
            val = bits % n;
        } while (bits - val + (n - 1) < 0L);
        return val;
    }
}
