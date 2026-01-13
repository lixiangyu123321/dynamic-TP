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

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbstractRedisRateLimiter related
 * XXX 实现执行lua脚本的操作
 * @author yanhom
 * @since 1.0.8
 **/
@SuppressWarnings("all")
public abstract class AbstractRedisRateLimiter implements RedisRateLimiter<List<Long>> {

    /**
     * Lua脚本存放的类路径前缀（固定路径，所有限流脚本都放/scripts/下）
     */
    private static final String SCRIPT_PATH = "/scripts/";
    /**
     * Redis Key的统一前缀（避免和其他业务Key冲突）
     */
    protected static final String PREFIX = "dtp";
    /**
     * 加载好的Lua脚本对象（泛型指定返回值为List<Long>）
     */
    private final RedisScript<List<Long>> script;
    /**
     * Redis操作模板（注入使用，所有子类共享）
     */
    protected final StringRedisTemplate stringRedisTemplate;
    /**
     * 原子计数器（可用于请求计数/分片等，子类可复用）
     */
    protected static final AtomicInteger COUNTER = new AtomicInteger(0);

    public AbstractRedisRateLimiter(String scriptName, StringRedisTemplate stringRedisTemplate) {
        DefaultRedisScript redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(SCRIPT_PATH + scriptName)));
        redisScript.setResultType(List.class);
        this.script = redisScript;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public RedisScript<List<Long>> getScript() {
        return script;
    }

    /**
     * 执行lua脚本
     * @param key
     * @param windowSize
     * @param limit
     * @return
     */
    public List<Object> isAllowed(String key, long windowSize, int limit) {
        RedisScript<?> script = this.getScript();
        List<String> keys = this.getKeys(key);
        String[] values = this.getArgs(key, windowSize, limit);
        // XXX 执行lua脚本，键可以多个，值可以多个
        // XXX execute(RedisScript<T> script, List<K> keys, Object... args)
        return Collections.unmodifiableList((List) Objects.requireNonNull(stringRedisTemplate.execute(script, keys,
                values)));
    }

}
