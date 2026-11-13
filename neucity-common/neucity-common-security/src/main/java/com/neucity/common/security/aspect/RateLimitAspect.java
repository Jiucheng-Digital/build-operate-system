package com.neucity.common.security.aspect;

import com.neucity.common.core.constant.CacheConstants;
import com.neucity.common.core.utils.ip.IpUtils;
import com.neucity.common.redis.constants.RedisLuaConstant;
import com.neucity.common.security.annotation.RateLimit;
import com.neucity.common.security.exception.RateLimitException;
import com.neucity.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 限流切面
 *
 * @author ShuPF
 * @date 2025/10/5 13:49
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private final ExpressionParser expressionParser = new SpelExpressionParser();


    private final RedisScript<Long> rateLimitScript = RedisScript.of(RedisLuaConstant.COUNT_RATE_LIMIT_SCRIPT, Long.class);

    @Around("@annotation(com.neucity.common.security.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("RateLimitAspect.around start");

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = AnnotationUtils.findAnnotation(method, RateLimit.class);

        if (rateLimit == null) {
            log.info("RateLimitAspect.around rateLimit is null");
            return joinPoint.proceed();
        }

        // 构建限流key
        String limitKey = buildLimitKey(rateLimit, method, joinPoint);
        log.info("RateLimitAspect.around rateLimit limitKey: {}", limitKey);

        // 获取当前时间戳（秒）
        long now = System.currentTimeMillis() / 1000;

        // 时间窗口转换为秒
        long timeWindowInSeconds = rateLimit.timeUnit().toSeconds(rateLimit.timeWindow());

        Object[] scriptArgs = new Object[] {
                cleanNumberString(rateLimit.qps()),          // 格式化为标准数字字符串
                cleanNumberString(timeWindowInSeconds),
                cleanNumberString(now)
//                String.valueOf(rateLimit.waitable()),
//                cleanNumberString(rateLimit.timeout())       // 超时时间也格式化
        };

        // 添加调试日志
//        log.info("传递给Lua脚本的参数: qps={}, timeWindow={}, now={}, waitable={}, timeout={}",
//                scriptArgs[0], scriptArgs[1], scriptArgs[2], scriptArgs[3], scriptArgs[4]);

        // 添加调试日志
        log.info("传递给Lua脚本的参数: qps={}, timeWindow={}, now={}", scriptArgs[0], scriptArgs[1], scriptArgs[2]);

        // 执行限流判断
        List<Object> keys = Collections.singletonList(limitKey);
        Long result = redisTemplate.execute(
                rateLimitScript,
                keys,
                scriptArgs
        );
        log.info("RateLimitAspect.around rateLimit result: {}", result);

        if (result != null && result == 1) {
            // 允许通过
            log.debug("限流通过: key={}, qps={}", limitKey, rateLimit.qps());
            return joinPoint.proceed();
        } else {
            // 限流
            log.warn("请求被限流: key={}, qps={}", limitKey, rateLimit.qps());
            throw new RateLimitException(rateLimit.message(), limitKey, rateLimit.qps());
        }
    }

    /**
     * 清理数字字符串格式，确保Lua可以正确识别
     */
    private String cleanNumberString(double number) {
        // 使用DecimalFormat避免科学计数法
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        df.setGroupingUsed(false); // 禁用千分位分隔符
        return df.format(number);
    }

    /**
     * 构建限流key
     */
    private String buildLimitKey(RateLimit rateLimit, Method method, ProceedingJoinPoint joinPoint) {
        StringBuilder keyBuilder = new StringBuilder(CacheConstants.RATE_LIMIT_KEY);

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        switch (rateLimit.limitType()) {
            case IP:
                keyBuilder.append(className).append(":").append(methodName).append(":")
                        .append(IpUtils.getIpAddr());
                break;
            case USER:
                keyBuilder.append(className).append(":").append(methodName).append(":")
                        .append(SecurityUtils.getUserId());
                break;
            case CUSTOM:
                if (rateLimit.key() != null && rateLimit.key().length > 0) {
                    String customKey = generateKey(joinPoint, rateLimit);
                    keyBuilder.append(className).append(":").append(methodName).append(":")
                            .append(customKey);
                } else {
                    keyBuilder.append(className).append(":").append(methodName);
                }
                break;
            default:
                keyBuilder.append(className).append(":").append(methodName);
        }

        return keyBuilder.toString();
    }

    /**
     * 生成缓存key
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit cacheable) {
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
        StringBuilder keyBuilder = new StringBuilder();

        Arrays.stream(cacheable.key())
                .forEach(suffix -> {
                    // 如果是SpEL表达式则解析，否则直接作为字符串
                    if (suffix.startsWith("#")) {
                        Object value = expressionParser.parseExpression(suffix).getValue(context);
                        keyBuilder.append(value != null ? value : "");
                    } else {
                        keyBuilder.append(suffix);
                    }
                });

        return keyBuilder.toString();
    }

}
