/*
 * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
 */

package com.neucity.common.core.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author lixu
 * @since 2019/10/17 11:16
 */
public class NoUtil {

    private static final String DEFAULT_PREFIX = "YHJ";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMddHHmmssSSS");

    private NoUtil() {
    }

    public static String generateNo(String prefix) {
        return StrUtil.isNotEmpty(prefix) ? prefix + getTimestamp() : generateNo();
    }

    public static String generateNo() {
        return DEFAULT_PREFIX + getTimestamp();
    }

    public static String getTimestamp() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now()) + randomNumbers(5);
    }

    public static String getFlowId(String prefix) {
        return prefix + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + randomNumbers(5);
    }

    public static String getNanoTimeId(String prefix) {
        return prefix + System.nanoTime();
    }

    public static Long str2Long(String str) {
        Long num = 0L;
        try {
            num = Long.valueOf(str);
        } catch (Exception e) {
            // do nothing
        }
        return num;
    }

    /**
     * 生成N位纯数字安全随机数
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    private static String randomNumbers(int length) {
        SecureRandom secureRandom = RandomUtil.getSecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

}
