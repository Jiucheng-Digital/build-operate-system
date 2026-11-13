/*
 * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
 */

package com.neucity.common.dubbo.filter;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.neucity.common.core.constant.SecurityConstants;
import com.neucity.common.core.context.SecurityContextHolder;
import com.neucity.common.core.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

/**
 * Dubbo消费者上下文过滤器
 *
 */
@Activate(group = CommonConstants.CONSUMER)
@Slf4j
public class DubboConsumerContextFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    private static final String LOGIN_USER_KEY = SecurityConstants.LOGIN_USER;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        log.info("DubboConsumerContextFilter.invoke()");

        // 从MDC（或你的追踪上下文）中获取traceId
        String traceId = MDC.get(TRACE_ID_KEY);

        // 如果当前没有traceId（例如，消费者本身是入口），可以考虑生成一个
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId(); // 生成traceId的方法
            log.info("DubboConsumerContextFilter.invoke() traceId重新生成 traceId: {}", traceId);
        }

        // 将traceId作为Attachment添加到Dubbo调用中
        invocation.getAttachments().put(TRACE_ID_KEY, traceId);

        LoginUser loginUser = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
        if (loginUser != null) {
            invocation.getAttachments().put(LOGIN_USER_KEY, JSONUtil.toJsonStr(loginUser));
        }

        return invoker.invoke(invocation);

    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
