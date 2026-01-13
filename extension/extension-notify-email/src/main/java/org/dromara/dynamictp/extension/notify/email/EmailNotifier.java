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

package org.dromara.dynamictp.extension.notify.email;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.em.NotifyPlatformEnum;
import org.dromara.dynamictp.common.entity.NotifyPlatform;
import org.dromara.dynamictp.common.notifier.AbstractNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * EmailNotifier related
 * XXX Email通知器底层的通知器
 * @author ljinfeng
 * @since 1.0.8
 */
@Slf4j
public class EmailNotifier extends AbstractNotifier {

    @Value("${spring.mail.username}")
    private String sendFrom;

    @Value("${spring.mail.title:ThreadPool Notify}")
    private String title;

    /**
     * Java用于发邮件的工具类
     */
    @Resource
    private JavaMailSender javaMailSender;

    /**
     * Thymeleaf 的html模板操作
     */
    @Resource
    private TemplateEngine templateEngine;

    @Override
    public String platform() {
        return NotifyPlatformEnum.EMAIL.name().toLowerCase();
    }

    /**
     * XXX 由AbstractNotifier调用
     * @param platform platform
     * @param content content
     */
    @SneakyThrows
    @Override
    protected void send0(NotifyPlatform platform, String content) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        messageHelper.setSubject(title);
        messageHelper.setFrom(sendFrom);
        messageHelper.setTo(platform.getReceivers());
        messageHelper.setSentDate(new Date());
        messageHelper.setText(content, true);
        javaMailSender.send(mimeMessage);
        log.info("DynamicTp notify, {} send success.", platform());
    }

    /**
     * 处理动态渲染的页面
     * @param template 模板对应的文件名
     * @param context 动态配置的数据
     * @return 返回渲染的模板
     */
    public String processTemplateContent(String template, Context context) {
        return templateEngine.process(template, context);
    }

}
