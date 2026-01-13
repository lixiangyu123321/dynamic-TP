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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.dynamictp.common.util.CommonUtil;
import org.dromara.dynamictp.extension.limiter.redis.em.RateLimitEnum;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SlidingWindowRateLimiter related
 *
 * @author yanhom
 * @since 1.0.8
 **/
@Slf4j
public class SlidingWindowRateLimiter extends AbstractRedisRateLimiter {

    public static final int LUA_RES_REMAIN_INDEX = 2;

    public SlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        super(RateLimitEnum.SLIDING_WINDOW.getScriptName(), stringRedisTemplate);
    }

    @Override
    public List<String> getKeys(final String key) {
        // XXX 服务名.dtp.key
        // XXX 这里返回数组只是为了适配lua脚本的执行接口
        String cacheKey = CommonUtil.getInstance().getServiceName() + ":" + PREFIX + ":" + key;
        return Collections.singletonList(cacheKey);
    }

    @Override
    public String[] getArgs(String key, long windowSize, int limit) {
        // XXX 生成唯一成员Key：IP + 自增计数器（用于Redis ZSet存储请求时间戳）
        String memberKey = CommonUtil.getInstance().getIp() + ":" + COUNTER.incrementAndGet();
        return new String[]{
                // XXX  滑动窗口大小（如1000ms）
                doubleToString(windowSize),
                // XXX  窗口内最大请求数（如100次）
                doubleToString(limit),
                // XXX  当前时间戳（秒级）
                doubleToString(Instant.now().getEpochSecond()),
                // XXX  唯一请求标识（用于ZSet去重/计数）
                memberKey
        };
    }

    /**
     * 外部调用的方法
     * @param name the key 键名
     * @param interval the interval 时间窗口
     * @param limit the limit 时间窗口内允许访问次数
     * @return
     */
    @Override
    public boolean tryPass(String name, long interval, int limit) {
        try {
            // XXX 调用父亲方法，获得lua脚本执行结果
            // XXX 返回键名，ttl，剩余可用数
            val res = isAllowed(name, interval, limit);
            // XXX 结果为空，允许通过
            if (CollectionUtils.isEmpty(res)) {
                return true;
            }
            // XXX 剩余可用请求数 <= 0，拦截请求
            if (Objects.isNull(res.get(LUA_RES_REMAIN_INDEX)) || (long) res.get(LUA_RES_REMAIN_INDEX) <= 0) {
                if (log.isDebugEnabled()) {
                    log.debug("DynamicTp notify, trigger redis rate limit, limitKey:{}, res:{}", name, res);
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("DynamicTp notify, redis rate limit check failed, limitKey:{}", name, e);
            return true;
        }
    }

    private String doubleToString(final double param) {
        return String.valueOf(param);
    }
}
