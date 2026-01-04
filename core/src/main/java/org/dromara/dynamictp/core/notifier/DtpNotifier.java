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

package org.dromara.dynamictp.core.notifier;

import org.dromara.dynamictp.common.entity.TpMainFields;
import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.entity.NotifyPlatform;

import java.util.List;

/**
 * DtpNotifier related
 * 通知器接口，定义了通知器的标准方法，用于向不同的平台（钉钉，企业微信，飞书）发送告警和通知消息
 * @author yanhom
 * @since 1.0.0
 **/
public interface DtpNotifier {

    /**
     * Get the platform name.
     *
     * @return platform
     */
    String platform();

    /**
     * Send change notify message.
     * 发送动态线程池配置信息的改变
     * @param notifyPlatform notify platform
     * @param oldFields      old properties
     * @param diffs          the changed keys
     */
    void sendChangeMsg(NotifyPlatform notifyPlatform, TpMainFields oldFields, List<String> diffs);

    /**
     * Send alarm message.
     * 发送告警信息
     * TODO 这里的告警信息会不会太单薄了
     * @param notifyPlatform notify platform
     * @param notifyItemEnum notify item enum
     */
    void sendAlarmMsg(NotifyPlatform notifyPlatform, NotifyItemEnum notifyItemEnum);
}
