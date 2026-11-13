package com.neucity.common.datasource.aspect;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 动态数据源切面
 * 自动根据方法名判断使用主库还是从库
 * 查询方法走从库，增删改方法走主库
 *
 * @author shupf
 */
@Slf4j
@Aspect
@Order(1) // 确保在 @DS 注解之前执行
@Component
public class DynamicDataSourceAspect {

    /**
     * 定义切点：拦截所有 Mapper 接口的方法
     */
    @Pointcut("execution(* com.neucity..mapper..*.*(..))")
    public void mapperPointcut() {
    }

    @Around("mapperPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        // 如果方法上已经有 @DS、@Master 或 @Slave 注解，则不处理，让注解优先
        if (method.isAnnotationPresent(com.baomidou.dynamic.datasource.annotation.DS.class)
                || method.isAnnotationPresent(com.neucity.common.datasource.annotation.Master.class)
                || method.isAnnotationPresent(com.neucity.common.datasource.annotation.Slave.class)) {
            return point.proceed();
        }

        // 检查类上是否有注解
        Class<?> targetClass = point.getTarget().getClass();
        if (targetClass.isAnnotationPresent(com.baomidou.dynamic.datasource.annotation.DS.class)
                || targetClass.isAnnotationPresent(com.neucity.common.datasource.annotation.Master.class)
                || targetClass.isAnnotationPresent(com.neucity.common.datasource.annotation.Slave.class)) {
            return point.proceed();
        }

        try {
            // 根据方法名判断使用哪个数据源
            if (isQueryMethod(methodName)) {
                // 查询方法使用从库
                DynamicDataSourceContextHolder.push("slave");
//                log.info("切换到从库 slave，方法：{}", methodName);
            } else {
                // 增删改方法使用主库
                DynamicDataSourceContextHolder.push("master");
//                log.info("切换到主库 master，方法：{}", methodName);
            }
            return point.proceed();
        } finally {
            // 清理数据源上下文
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * 判断是否为查询方法
     * 根据方法名前缀判断
     */
    private boolean isQueryMethod(String methodName) {
        // 查询方法的常见前缀
        String[] queryPrefixes = {"select", "get", "query", "find", "list", "count", "check"};
        
        for (String prefix : queryPrefixes) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
}
