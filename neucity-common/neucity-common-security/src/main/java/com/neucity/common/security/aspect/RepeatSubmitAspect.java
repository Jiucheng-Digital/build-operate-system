package com.neucity.common.security.aspect;

import cn.hutool.crypto.SecureUtil;
import com.neucity.common.core.constant.CacheConstants;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.redis.service.RedisService;
import com.neucity.common.security.annotation.RepeatSubmit;
import com.neucity.common.security.utils.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * RepeatSubmitAspect
 *
 * @author cd
 * @since 2025/7/9 9:15
 */
@Aspect
@Component
public class RepeatSubmitAspect {

    @Resource
    private RedisService cacheService;

    @Before("@annotation(repeatSubmit)")
    public void doBefore(JoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        long interval = repeatSubmit.interval();

        if (interval < 1) {
            interval = 500;
        }

        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            return;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String uri = request.getRequestURI();
        String args = Arrays.toString(joinPoint.getArgs());
        String submitKey = SecureUtil.md5(uri + ":" + args + ":" + user.getUserid());
        String cacheRepeatKey = CacheConstants.REPEAT_SUBMIT_KEY + submitKey;

        Object object = cacheService.getCacheObject(cacheRepeatKey);
        if (object != null) {
            throw new RuntimeException(repeatSubmit.message());
        }

        cacheService.setIfAbsentCache(cacheRepeatKey, "1", interval, TimeUnit.MILLISECONDS);
    }


}
