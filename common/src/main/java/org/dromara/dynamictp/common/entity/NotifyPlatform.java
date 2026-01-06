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

package org.dromara.dynamictp.common.entity;

import lombok.Data;

import java.net.Proxy;
import java.util.UUID;

/**
 * NotifyPlatform related
 * 通知的平台信息
 * @author yanhom
 * @since 1.0.0
 **/
@Data
public class NotifyPlatform {

    /**
     * 告警平台唯一标识ID
     * 默认值：自动生成UUID字符串，保证每个平台配置的唯一性
     */
    private String platformId = UUID.randomUUID().toString();

    /**
     * 告警平台名称（如钉钉：dingtalk、企业微信：wechat）
     */
    private String platform;

    /**
     * 告警推送URL的令牌/密钥（用于接口鉴权）
     */
    private String urlKey;

    /**
     * 告警平台密钥（可选，可能为null）
     * 如钉钉机器人的加签密钥、企业微信的Secret等
     */
    private String secret;

    /**
     * 告警推送的webhook地址（可选，可能为null）
     * 用于接收平台回调或直接推送告警消息的HTTP接口地址
     */
    private String webhook;

    /**
     * 告警接收人，多个接收人用英文逗号（,）分隔
     * 默认值："all"（推送给所有预设接收人）
     */
    private String receivers = "all";

    /**
     * HTTP请求超时时间（单位：毫秒）
     * 默认值：3000毫秒（3秒），超过该时间则判定推送失败
     */
    private Integer timeout = 3000;

    /**
     * HTTP请求代理类型
     * 默认值：Proxy.Type.DIRECT（直连，不使用代理）
     */
    private Proxy.Type proxyType = Proxy.Type.DIRECT;

    /**
     * HTTP请求代理服务器的主机地址（如127.0.0.1）
     * 仅当proxyType不为DIRECT时生效
     */
    private String proxyHost;

    /**
     * HTTP请求代理服务器的端口号（如8080）
     * 仅当proxyType不为DIRECT时生效
     */
    private int proxyPort;

}