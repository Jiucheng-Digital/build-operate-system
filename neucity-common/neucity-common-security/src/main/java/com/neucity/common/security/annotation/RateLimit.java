package com.neucity.common.security.annotation;

import com.neucity.common.security.enums.LimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 *  QPS限流注解
 *
 * @author ShuPF
 * @date 2025/10/5 13:46
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流key，支持SpEL表达式
     */
    String[] key() default "";

    /**
     * 每秒允许的请求数 (QPS)
     */
    int qps() default 100;

    /**
     * 时间窗口大小
     */
    long timeWindow() default 1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 是否等待，如果为true则在超时时间内等待，否则立即返回
     */
    boolean waitable() default false;

    /**
     * 等待超时时间（毫秒）
     */
    long timeout() default 0;
}
