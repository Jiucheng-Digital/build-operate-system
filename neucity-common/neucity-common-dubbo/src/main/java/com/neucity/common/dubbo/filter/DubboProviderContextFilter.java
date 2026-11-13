/*
 * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
 */

package com.neucity.common.dubbo.filter;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.neucity.common.core.constant.SecurityConstants;
import com.neucity.common.core.context.SecurityContextHolder;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Restores Dubbo provider-side logging and security context from invocation attachments.
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class DubboProviderContextFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    private static final String SPAN_ID_KEY = "spanId";

    private static final String LOGIN_USER_KEY = SecurityConstants.LOGIN_USER;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        log.info("DubboProviderContextFilter.invoke()");

        String previousTraceId = MDC.get(TRACE_ID_KEY);
        String previousSpanId = MDC.get(SPAN_ID_KEY);
        Map<String, Object> previousSecurityContext = new HashMap<>(SecurityContextHolder.getLocalMap());

        String traceId = invocation.getAttachments().get(TRACE_ID_KEY);
        String spanId = invocation.getAttachments().get(SPAN_ID_KEY);

        if (StringUtils.isNotEmpty(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        }

        if (StringUtils.isEmpty(spanId)) {
            spanId = generateTraceId();
        }
        MDC.put(SPAN_ID_KEY, spanId);

        String loginUserJson = invocation.getAttachments().get(LOGIN_USER_KEY);
        if (StringUtils.isNotEmpty(loginUserJson)) {
            try {
                setLoginUserContext(JSONUtil.toBean(loginUserJson, LoginUser.class));
            } catch (Exception e) {
                log.warn("Parse dubbo login user context failed", e);
            }
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            restoreMdc(TRACE_ID_KEY, previousTraceId);
            restoreMdc(SPAN_ID_KEY, previousSpanId);
            restoreSecurityContext(previousSecurityContext);
        }
    }

    private void setLoginUserContext(LoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
        if (loginUser.getUserid() != null) {
            SecurityContextHolder.setUserId(String.valueOf(loginUser.getUserid()));
        }
        if (StringUtils.isNotEmpty(loginUser.getUsername())) {
            SecurityContextHolder.setUserName(loginUser.getUsername());
        }
        if (StringUtils.isNotEmpty(loginUser.getToken())) {
            SecurityContextHolder.setUserKey(loginUser.getToken());
        }
    }

    private void restoreMdc(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private void restoreSecurityContext(Map<String, Object> previousSecurityContext) {
        if (previousSecurityContext == null || previousSecurityContext.isEmpty()) {
            SecurityContextHolder.remove();
        } else {
            SecurityContextHolder.setLocalMap(previousSecurityContext);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
