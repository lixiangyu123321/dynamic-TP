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

package org.dromara.dynamictp.common.parser.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author <a href = "mailto:kamtohung@gmail.com">KamTo Hung</a>
 */
public class JacksonCreator {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 创建一个定制化配置的 Jackson ObjectMapper 实例，专门解决 JSON 序列化 / 反序列化中的常见问题
     * @return
     */
    protected static ObjectMapper createMapper() {
        // JavaTimeModule是专门用来处理Java8LocalDateTime时间类的模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        return JsonMapper.builder()
                // 让 transient 关键字的效果在继承关系中生效。
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                // 反序列化时，如果 JSON 中有 Java 对象不存在的字段，不会抛出异常（这是开发中最常用的配置之一）。
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 序列化一个没有任何可序列化字段的空对象时，不会抛出异常（比如一个类只有 transient 字段）。
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                // 序列化时，忽略值为「空」的字段（包括 null、空字符串、空集合 / 数组等）。
                .serializationInclusion(JsonInclude.Include.NON_EMPTY)
                // 添加 Java 8 时间模块（处理 LocalDateTime 等）
                .addModules(javaTimeModule)
                .addModules(new JavaTimeModule())
                // 序列化时，忽略值为「空」的字段（包括 null、空字符串、空集合 / 数组等）。
                .defaultDateFormat(new SimpleDateFormat(DATE_FORMAT))
                .build();
    }

}
