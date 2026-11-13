package com.neucity.api.system;

import com.neucity.common.core.constant.SecurityConstants;
import com.neucity.common.core.constant.ServiceNameConstants;
import com.neucity.common.core.domain.R;
import com.neucity.common.core.model.dto.SysOperLogDTO;
import com.neucity.api.system.factory.RemoteLogFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 日志服务
 *
 * @author shupf
 */
@FeignClient(contextId = "remoteLogService", value = ServiceNameConstants.SYSTEM_SERVICE,
        path = "/api/system", url = "${system.http.url:}",
        fallbackFactory = RemoteLogFallbackFactory.class)
public interface RemoteLogService {

    /**
     * 保存系统日志
     *
     * @param sysOperLog 日志实体
     * @param source     请求来源
     * @return 结果
     */
    @PostMapping("/operlog")
    R<Boolean> saveLog(@RequestBody SysOperLogDTO sysOperLog,
                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source) throws Exception;
}
