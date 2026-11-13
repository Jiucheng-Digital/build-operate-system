package com.neucity.api.system.factory;

import com.neucity.common.core.domain.R;
import com.neucity.common.core.model.dto.SysOperLogDTO;
import com.neucity.api.system.RemoteLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 日志服务降级处理
 *
 * @author shupf
 */
@Component
public class RemoteLogFallbackFactory implements FallbackFactory<RemoteLogService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteLogFallbackFactory.class);

    @Override
    public RemoteLogService create(Throwable throwable) {
        log.error("日志服务调用失败:{}", throwable.getMessage());
        return new RemoteLogService() {
            @Override
            public R<Boolean> saveLog(SysOperLogDTO sysOperLog, String source) {
                return R.fail("保存操作日志失败:" + throwable.getMessage());
            }
        };
    }
}
