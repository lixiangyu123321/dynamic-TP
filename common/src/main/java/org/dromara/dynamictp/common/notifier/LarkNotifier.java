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
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.constant.LarkNotifyConst;
import org.dromara.dynamictp.common.em.NotifyPlatformEnum;
import org.dromara.dynamictp.common.entity.NotifyPlatform;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.dromara.dynamictp.common.constant.LarkNotifyConst.SIGN_PARAM_PREFIX;
import static org.dromara.dynamictp.common.constant.LarkNotifyConst.SIGN_REPLACE;

/**
 * LarkNotifier
 *
 * @author fxbin
 * @version v1.0
 * @since 2022/4/28 23:25
 */
@Slf4j // Lombok注解，自动生成日志对象log，无需手动声明private static final Logger log
public class LarkNotifier extends AbstractHttpNotifier {

    /**
     * 换行符常量（LF），用于签名生成时拼接字符串
     */
    public static final String LF = "\n";

    /**
     * HmacSHA256加密算法常量，指定签名生成使用的哈希算法
     */
    public static final String HMAC_SHA_256 = "HmacSHA256";

    /**
     * 实现抽象方法，返回当前通知平台的标识
     * @return 飞书平台的名称（小写），如"lark"
     */
    @Override
    public String platform() {
        // 获取枚举类中LARK的名称并转为小写，统一平台标识格式
        return NotifyPlatformEnum.LARK.name().toLowerCase();
    }

    /**
     * 生成飞书webhook所需的签名
     * 飞书为了验证请求合法性，要求基于timestamp和secret生成HmacSHA256签名
     *
     * @param secret    飞书机器人的密钥（secret），从飞书开放平台获取
     * @param timestamp 时间戳（秒级），用于签名的时效性验证
     * @return 生成的Base64编码后的签名字符串
     * @throws NoSuchAlgorithmException 当指定的HmacSHA256算法不存在时抛出（理论上不会发生）
     * @throws InvalidKeyException      当密钥无效或初始化Mac对象失败时抛出
     */
    protected String genSign(String secret, Long timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        // 1. 拼接待签名字符串：格式为 时间戳 + 换行符 + 密钥
        String stringToSign = timestamp + LF + secret;

        // 2. 获取HmacSHA256算法的Mac实例（消息认证码）
        Mac mac = Mac.getInstance(HMAC_SHA_256);

        // 3. 初始化Mac对象：传入待签名字符串的字节数组（UTF-8编码）和算法名称
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));

        // 4. 执行加密计算（传入空字节数组表示对初始化的密钥进行计算），得到签名的字节数组
        byte[] signData = mac.doFinal(new byte[]{});

        // 5. 将签名字节数组进行Base64编码，转为字符串返回（飞书要求的签名格式）
        return new String(Base64.encodeBase64(signData));
    }

    /**
     * 构建飞书通知的消息体（核心逻辑：如果配置了secret则添加签名参数）
     *
     * @param platform 通知平台配置（包含secret、webhook等信息）
     * @param content  原始消息内容（可能包含签名占位符）
     * @return 最终发送的消息体（带签名参数，或原始内容）
     */
    @Override
    protected String buildMsgBody(NotifyPlatform platform, String content) {
        // 1. 如果平台配置中没有secret，无需生成签名，直接返回原始内容
        if (StringUtils.isBlank(platform.getSecret())) {
            return content;
        }

        try {
            // 2. 获取秒级时间戳（飞书要求的时间戳格式，System.currentTimeMillis()是毫秒级，需/1000）
            val secondsTimestamp = System.currentTimeMillis() / 1000;

            // 3. 调用genSign生成签名
            val sign = genSign(platform.getSecret(), secondsTimestamp);

            // 4. 替换消息内容中的签名占位符：将占位符替换为 时间戳&签名 的格式
            // SIGN_REPLACE是签名占位符常量，SIGN_PARAM_PREFIX是拼接格式（如"timestamp=%s&sign=%s"）
            content = content.replaceFirst(SIGN_REPLACE, String.format(SIGN_PARAM_PREFIX, secondsTimestamp, sign));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // 5. 捕获签名生成异常，记录错误日志（不中断流程，返回原始内容）
            log.error("DynamicTp notify, lark generate signature failed...", e);
        }

        // 6. 返回最终的消息体（带签名或原始内容）
        return content;
    }

    /**
     * 构建飞书通知的最终请求URL（拼接webhook和urlKey）
     *
     * @param platform 通知平台配置（包含webhook、urlKey等信息）
     * @return 最终的请求URL
     */
    @Override
    protected String buildUrl(NotifyPlatform platform) {
        // 1. 如果没有配置urlKey，直接返回平台的webhook
        if (StringUtils.isBlank(platform.getUrlKey())) {
            return platform.getWebhook();
        }

        // 2. 构建URL：优先使用平台配置的webhook，否则使用飞书默认webhook（LarkNotifyConst.LARK_WEBHOOK）
        UrlBuilder builder = UrlBuilder.of(Optional.ofNullable(platform.getWebhook()).orElse(LarkNotifyConst.LARK_WEBHOOK));

        // 3. 获取URL的路径分段（如https://open.feishu.cn/webhook/v2/xxx 拆分为["webhook", "v2", "xxx"]）
        List<String> segments = builder.getPath().getSegments();

        // 4. 检查最后一个路径分段是否等于urlKey，避免重复拼接
        if (!Objects.equals(platform.getUrlKey(), segments.get(segments.size() - 1))) {
            // 5. 拼接urlKey到URL路径末尾
            builder.addPath(platform.getUrlKey());
        }

        // 6. 构建并返回最终的URL字符串
        return builder.build();
    }
}
