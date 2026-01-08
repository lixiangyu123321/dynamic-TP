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

package org.dromara.dynamictp.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * DingSignUtil related
 * XXX 生成签名
 * @author yanhom
 * @since 1.0.0
 */
@Slf4j
public final class DingSignUtil {

    private DingSignUtil() { }

    /**
     * 签名计算时使用的默认字符编码（固定为UTF-8，符合钉钉接口要求）
     */
    private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    /**
     * 钉钉签名使用的加密算法（固定为HmacSHA256，钉钉官方指定）
     */
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 计算钉钉机器人的签名（用于验证请求合法性，防止伪造）
     * @param secret 钉钉机器人的密钥（从钉钉机器人配置页面获取）
     * @param timestamp 时间戳（毫秒级，需与请求中的timestamp一致）
     * @return 计算后的URL编码签名串，异常时返回空字符串
     */
    public static String dingSign(String secret, long timestamp) {
        // 1. 拼接待签名字符串：钉钉官方规定格式 = 时间戳 + 换行符 + 密钥
        // 换行符\n是钉钉签名的固定分隔符，不能省略或替换
        String stringToSign = timestamp + "\n" + secret;

        try {
            // 2. 获取HmacSHA256算法的Mac实例（Java加密框架的核心类，用于计算哈希消息认证码）
            Mac mac = Mac.getInstance(ALGORITHM);

            // 3. 初始化Mac对象：传入密钥的字节数组和算法名称
            // SecretKeySpec：将字节数组转换为加密框架识别的密钥对象
            // secret.getBytes(DEFAULT_ENCODING)：将密钥字符串转为UTF-8编码的字节数组（避免编码不一致导致签名错误）
            mac.init(new SecretKeySpec(secret.getBytes(DEFAULT_ENCODING), ALGORITHM));

            // 4. 计算签名：对拼接后的待签名字符串执行HmacSHA256加密
            // stringToSign.getBytes(DEFAULT_ENCODING)：待签名字符串转UTF-8字节数组
            // doFinal()：执行加密计算，返回原始字节数组形式的签名
            byte[] signData = mac.doFinal(stringToSign.getBytes(DEFAULT_ENCODING));

            // 5. 签名结果处理（钉钉官方要求两步：Base64编码 + URL编码）
            // Base64.encodeBase64(signData)：将原始字节签名转为Base64编码字节数组
            // new String(...)：Base64字节数组转字符串
            // URLEncoder.encode(...)：对Base64字符串做URL编码（避免特殊字符在URL中失效）
            return URLEncoder.encode(new String(Base64.encodeBase64(signData)), DEFAULT_ENCODING.name());

        } catch (Exception e) {
            // 6. 异常处理：捕获所有可能的异常（如算法不存在、密钥错误、编码异常等）
            // 打印异常日志（包含堆栈信息），便于排查问题
            log.error("DynamicTp, cal ding sign error", e);
            // 异常时返回空字符串，避免返回null导致上层代码空指针
            return "";
        }
    }
}
