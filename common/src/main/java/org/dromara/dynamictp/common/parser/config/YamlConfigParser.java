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

package org.dromara.dynamictp.common.parser.config;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.common.em.ConfigFileTypeEnum;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YamlConfigParser related
 *
 * @author yanhom
 * @since 1.0.0
 **/
public class YamlConfigParser extends AbstractConfigParser {

    private static final List<ConfigFileTypeEnum> CONFIG_TYPES = Arrays.asList(
            ConfigFileTypeEnum.YML, ConfigFileTypeEnum.YAML);

    @Override
    public List<ConfigFileTypeEnum> types() {
        return CONFIG_TYPES;
    }

    @Override
    public Map<Object, Object> doParse(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptyMap();
        }

        Yaml yaml = new Yaml();
        Map<Object, Object> loadedYaml = yaml.load(content);

        if (MapUtils.isEmpty(loadedYaml)) {
            return Collections.emptyMap();
        }

        Map<Object, Object> flattenedMap = new LinkedHashMap<>();
        flattenMap(flattenedMap, loadedYaml, null);
        return flattenedMap;
    }

    /**
     * 扁平化处理Map
     * source
     * ├── name: 张三
     * ├── age: 25
     * ├── address
     * │   ├── province: 广东省
     * │   ├── city: 深圳市
     * │   └── details (List)
     * │       ├── [0]: 南山区
     * │       └── [1]: 科技园路
     * └── hobbies (List)
     *     ├── [0]: 篮球
     *     └── [1] (List)
     *         ├── [0]: 看电影
     *         └── [1]: 听音乐
     *
     * name = 张三
     * age = 25
     * address.province = 广东省
     * address.city = 深圳市
     * address.details[0] = 南山区
     * address.details[1] = 科技园路
     * hobbies[0] = 篮球
     * hobbies[1][0] = 看电影
     * hobbies[1][1] = 听音乐
     * @param result
     * @param source
     * @param path
     */
    @SuppressWarnings("unchecked")
    private void flattenMap(Map<Object, Object> result, Map<Object, Object> source, String path) {
        source.forEach((key, value) -> {
            String fullPath = (path != null ? path + "." + key : key.toString());
            if (value instanceof Map) {
                flattenMap(result, (Map<Object, Object>) value, fullPath);
            } else if (value instanceof Collection) {
                for (int i = 0; i < ((Collection<?>) value).size(); i++) {
                    flattenMap(result, Collections.singletonMap("[" + i + "]", ((List<?>) value).get(i)), fullPath);
                }
            } else {
                fullPath = fullPath.replaceAll("\\.\\[", "[");
                result.put(fullPath, value != null ? value.toString() : null);
            }
        });
    }
}
