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
import lombok.val;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * ReflectionUtil related
 *
 * @author yanhom
 * @since 1.0.6
 */
@Slf4j
public final class ReflectionUtil {

    private ReflectionUtil() { }

    /**
     * 获得目标类对应字段的值
     * @param fieldName 字段名
     * @param targetObj 目标类
     * @return 对应值
     */
    public static Object getFieldValue(String fieldName, Object targetObj) {
        val field = getField(targetObj.getClass(), fieldName);
        if (Objects.isNull(field)) {
            return null;
        }
        try {
            return FieldUtils.readField(field, targetObj, true);
        } catch (IllegalAccessException e) {
            log.error("Failed to read field '{}' from object '{}'", fieldName, targetObj, e);
            return null;
        }
    }

    /**
     * 指定类型获得对应目标字段值
     * @param targetClass 目标类类型
     * @param fieldName 字段名
     * @param targetObj 目标值
     * @return 目标值字段值
     */
    public static Object getFieldValue(Class<?> targetClass, String fieldName, Object targetObj) {
        val field = getField(targetClass, fieldName);
        if (Objects.isNull(field)) {
            return null;
        }
        try {
            return FieldUtils.readField(field, targetObj, true);
        } catch (IllegalAccessException e) {
            log.error("Failed to read field '{}' from object '{}'", fieldName, targetObj, e);
            return null;
        }
    }

    /**
     * 设置目标类的字段值
     * @param fieldName 字段名
     * @param targetObj 目标对象
     * @param targetVal 目标值
     * @return 设置成功否
     */
    public static boolean setFieldValue(String fieldName, Object targetObj, Object targetVal) {
        return setFieldValue(targetObj.getClass(), fieldName, targetObj, targetVal);
    }

    /**
     * 设置目标类的字段值
     * @param targetClass 目标类类型 XXX 设置类型，可精确空值字段查找的类层级
     * @param fieldName 字段名
     * @param targetObj 目标对象
     * @param targetVal 目标值
     * @return 设置成功否
     */
    public static boolean setFieldValue(Class<?> targetClass, String fieldName, Object targetObj, Object targetVal) {
        val field = getField(targetClass, fieldName);
        if (Objects.isNull(field)) {
            return false;
        }
        try {
            FieldUtils.writeField(field, targetObj, targetVal, true);
            return true;
        } catch (IllegalAccessException e) {
            log.error("Failed to write value '{}' to field '{}' in object '{}'", targetVal, fieldName, targetObj, e);
            return false;
        }
    }

    /**
     * 不要目标类类型
     * @param field 字段信息
     * @param targetObj 目标类
     * @param targetVal 目标值
     * @return 是否设置成功
     */
    public static boolean setFieldValue(Field field, Object targetObj, Object targetVal) {
        try {
            FieldUtils.writeField(field, targetObj, targetVal, true);
            return true;
        } catch (IllegalAccessException e) {
            log.error("Failed to write value '{}' to field '{}' in object '{}'", targetVal, field.getName(), targetObj, e);
            return false;
        }
    }

    /**
     * 基于apache common 强制获得字段信息
     * @param targetClass 目标类
     * @param fieldName 字段名
     * @return 字段信息
     */
    public static Field getField(Class<?> targetClass, String fieldName) {
        Field field = FieldUtils.getField(targetClass, fieldName, true);
        if (Objects.isNull(field)) {
            log.warn("Field '{}' not found in class '{}'", fieldName, targetClass.getName());
            return null;
        }
        return field;
    }

    /**
     * 查找目标类的指定参数的方法
     * @param targetClass 目标类
     * @param methodName 方法名
     * @param parameterTypes 方法参数类型
     * @return 方法
     */
    public static Method findMethod(Class<?> targetClass, String methodName, Class<?>... parameterTypes) {
        Method method = MethodUtils.getMatchingMethod(targetClass, methodName, parameterTypes);
        if (Objects.isNull(method)) {
            log.warn("Method '{}' with parameters '{}' not found in class '{}'", methodName, parameterTypes, targetClass.getName());
            return null;
        }
        return method;
    }

    /**
     * 获得目标类的所有字段信息
     * @param targetClass 目标类
     * @return 所有字段信息
     */
    public static List<Field> getAllFields(Class<?> targetClass) {
        return FieldUtils.getAllFieldsList(targetClass);
    }
}
