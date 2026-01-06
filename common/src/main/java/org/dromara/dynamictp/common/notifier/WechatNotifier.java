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

package org.dromara.dynamictp.common.notifier;

import cn.hutool.core.net.url.UrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.constant.WechatNotifyConst;
import org.dromara.dynamictp.common.em.NotifyPlatformEnum;
import org.dromara.dynamictp.common.entity.MarkdownReq;
import org.dromara.dynamictp.common.entity.NotifyPlatform;
import org.dromara.dynamictp.common.util.JsonUtil;

import java.util.Optional;

/**
 * WechatNotifier related
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class WechatNotifier extends AbstractHttpNotifier {

    @Override
    public String platform() {
        return NotifyPlatformEnum.WECHAT.name().toLowerCase();
    }

    @Override
    protected String buildMsgBody(NotifyPlatform platform, String content) {
        MarkdownReq markdownReq = new MarkdownReq();
        markdownReq.setMsgtype("markdown");
        MarkdownReq.Markdown markdown = new MarkdownReq.Markdown();
        markdown.setContent(content);
        markdownReq.setMarkdown(markdown);
        return JsonUtil.toJson(markdownReq);
    }

    @Override
    protected String buildUrl(NotifyPlatform platform) {
        // 1. 优先判断：如果 urlKey 为空，直接返回原始 webhook（无需拼接参数）
        if (StringUtils.isBlank(platform.getUrlKey())) {
            return platform.getWebhook();
        }

        // 2. 构建 URL 基础地址：
        // - 如果 platform 的 webhook 不为空，用该值；
        // - 如果 webhook 为空，使用企业微信默认的 webhook 地址（WechatNotifyConst.WECHAT_WEB_HOOK）
        UrlBuilder builder = UrlBuilder.of(Optional.ofNullable(platform.getWebhook()).orElse(WechatNotifyConst.WECHAT_WEB_HOOK));

        // 3. 检查 URL 的查询参数中是否已包含 KEY_PARAM（如 "key"）：
        // - StringUtils.isBlank：如果该参数值为空/不存在，进入逻辑
        if (StringUtils.isBlank(builder.getQuery().get(WechatNotifyConst.KEY_PARAM))) {
            // 4. 给 URL 添加查询参数：KEY_PARAM = urlKey（如 key=xxx，用于接口鉴权）
            builder.addQuery(WechatNotifyConst.KEY_PARAM, platform.getUrlKey());
        }

        // 5. 构建最终的 URL 字符串并返回
        return builder.build();
    }
}
