package com.neucity.common.security.enums;

/**
 *  限流类型枚举
 *
 * @author ShuPF
 * @date 2025/10/5 13:47
 */

public enum LimitType {

    /**
     * 默认，根据方法进行限流
     */
    DEFAULT,
    /**
     * 根据IP进行限流
     */
    IP,
    /**
     * 根据用户进行限流
     */
    USER,
    /**
     * 自定义key限流
     */
    CUSTOM

}
