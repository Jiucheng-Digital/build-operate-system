package com.neucity.common.security.aspect;

import com.neucity.common.core.constant.SecurityConstants;
import com.neucity.common.core.exception.InnerAuthException;
import com.neucity.common.core.utils.ServletUtils;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.security.annotation.InnerAuth;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 内部服务调用验证处理
 * 
 * @author shupf
 */
@Slf4j
@Aspect
@Component
public class InnerAuthAspect implements Ordered {

    public static final String TRACE_ID = "traceId";

    @Around("@annotation(innerAuth)")
    public Object innerAround(ProceedingJoinPoint point, InnerAuth innerAuth) throws Throwable {
        HttpServletRequest request = ServletUtils.getRequest();

        String source = request.getHeader(SecurityConstants.FROM_SOURCE);
        // 内部请求验证
        if (!StringUtils.equals(SecurityConstants.INNER, source))
        {
            throw new InnerAuthException("没有内部访问权限，不允许访问");
        }

        String traceId = request.getHeader(SecurityConstants.X_Trace_Id);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(traceId)) {
            MDC.put(TRACE_ID, traceId);
            log.info("InnerAuthAspect uri: {} 设置外部traceId：traceId: {}",request.getRequestURI(), traceId);
        }

        String userid = request.getHeader(SecurityConstants.DETAILS_USER_ID);
        String username = request.getHeader(SecurityConstants.DETAILS_USERNAME);
        // 用户信息验证
        if (innerAuth.isUser() && (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username)))
        {
            throw new InnerAuthException("没有设置用户信息，不允许访问 ");
        }
        return point.proceed();
    }

    /**
     * 确保在权限认证aop执行前执行
     */
    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
