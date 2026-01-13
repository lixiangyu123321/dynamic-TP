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

package org.dromara.dynamictp.extension.limiter.redis.ratelimiter;

import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * RedisRateLimiter related.
 * XXX 基于 Redis 实现的限流算法抽象接口
 * @param <T>
 * @author yanhom
 * @since 1.0.8
 **/
public interface RedisRateLimiter<T> {

    /**
     * Get script.
     * XXX 获取限流的Lua脚本
     * @return the script
     */
    RedisScript<T> getScript();

    /**
     * Get keys.
     * XXX 限流相关key的获取
     * @param key the key
     * @return the keys
     */
    List<String> getKeys(String key);

    /**
     * Get args.
     * XXX 限流的核心参数：
     * XXX - windowSize：限流窗口大小（如 1 秒）
     * XXX - limit：窗口内最大请求数（如 100 次）
     * XXX 该方法把这些参数组装成 Lua 脚本能识别的格式
     * @param key the key
     * @param windowSize the window size
     * @param limit the limit
     * @return the args
     */
    String[] getArgs(String key, long windowSize, int limit);

    /**
     * Acquires permission of an invocation only if it is available at the time of invoking.
     * XXX 尝试获取一次限流许可：
     * XXX - name：限流标识（如接口名、用户 ID）
     * XXX - interval：限流时间窗口（同 windowSize）
     * XXX - limit：窗口内限流阈值
     * XXX - 返回 true= 允许通过，false= 限流拦截
     * @param name the key
     * @param interval the interval
     * @param limit the limit
     * @return the result
     */
    boolean tryPass(String name, long interval, int limit);

}
