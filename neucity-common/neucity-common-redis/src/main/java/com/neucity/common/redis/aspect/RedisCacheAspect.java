package com.neucity.common.redis.aspect;

import com.neucity.common.redis.annotation.RedisCache;
import com.neucity.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 缓存自定义注解
 *
 * @author shupf
 * @date 2025-09-24 10:08
 **/
@Slf4j
@Aspect
@Component
public class RedisCacheAspect {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 定义AOP签名 (切入所有使用鉴权注解的方法)
     */

    @Autowired
    private RedisService redisService;

    /**
     * 环绕通知，实现缓存逻辑
     *
     **/
    @Around("@annotation(redisCache)")
    public Object around(ProceedingJoinPoint joinPoint, RedisCache redisCache) throws Throwable {
        // 1. 生成缓存key
        String key = generateKey(joinPoint, redisCache);
        log.info("RedisCacheAspect.around key：{}", key);

        // 2. 从Redis获取数据
        Object value = redisService.getCacheObject(key);
        if (value != null) {
            // 缓存存在，直接返回
            return value;
        }

        log.info("RedisCacheAspect.around key：{}, not exist cache", key);

        // 3. 缓存未命中，准备获取分布式锁
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();

        try {
            // 4. 尝试获取分布式锁
            boolean locked = redisService.setIfAbsentCache(
                    lockKey, lockValue,
                    redisCache.lockExpire(),
                    TimeUnit.MILLISECONDS
            );

            // 5. 获取锁成功，执行数据库查询 将结果存入Redis
            if (locked) {
                value = joinPoint.proceed();
                if (value != null) {
                    redisService.setCacheObject(key, value, redisCache.expire(), redisCache.timeUnit());
                }
                return value;
            }

            // 6. 获取锁失败，等待重试
            long start = System.currentTimeMillis();
            while (true) {
                // 检查是否超时
                if (System.currentTimeMillis() - start > redisCache.lockWaitTime()) {
                    throw new RuntimeException("获取分布式锁超时");
                }

                // 等待一段时间后重试获取缓存
                TimeUnit.MILLISECONDS.sleep(redisCache.lockSleepTime());
                value = redisService.getCacheObject(key);
                if (value != null) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.error("RedisCacheAspect.around error：", e);
        } finally {
            // 7. 释放分布式锁（确保只有持有者才能释放）
            String currentValue = (String) redisService.getCacheObject(lockKey);
            if (lockValue.equals(currentValue)) {
                redisService.deleteObject(lockKey);
            }
        }

        return value;
    }

    /**
     * 生成缓存key
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RedisCache cacheable) {
        // 解析方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 构建SpEL上下文
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // 拼接前缀和所有后缀元素
        StringBuilder keyBuilder = new StringBuilder(cacheable.prefix());

        Arrays.stream(cacheable.suffix())
                .forEach(suffix -> {
                    // 如果是SpEL表达式则解析，否则直接作为字符串
                    if (suffix.startsWith("#")) {
                        Object value = parser.parseExpression(suffix).getValue(context);
                        keyBuilder.append(value != null ? value : "");
                    } else {
                        keyBuilder.append(suffix);
                    }
                });

        return keyBuilder.toString();
    }
}
