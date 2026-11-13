package com.neucity.common.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 *  缓存注解
 *
 * @author shupf
 * @date 2025-09-24 10:08
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RedisCache {

    /**
     * 缓存key的前缀
     */
    String prefix() default "";

    /**
     * 缓存key的动态后缀数组，支持SpEL表达式
     * 所有元素会按顺序拼接成完整后缀
     */
    String[] suffix() default {};

    /**
     * 缓存过期时间
     */
    long expire() default 3600;

    /**
     * 过期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     *  分布式锁过期时间(毫秒) 30s
     */
    long lockExpire() default 30000;

    /**
     *  获取锁的等待时间(毫秒) 5s
     */
    long lockWaitTime() default 5000;

    /**
     *  获取锁失败后的重试间隔(毫秒) 0.2秒
     */
    long lockSleepTime() default 200;
}
