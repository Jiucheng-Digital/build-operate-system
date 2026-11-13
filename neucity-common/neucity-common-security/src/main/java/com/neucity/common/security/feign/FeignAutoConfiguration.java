package com.neucity.common.security.feign;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置注册
 *
 * @author shupf
 **/
@Configuration
public class FeignAutoConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignRequestInterceptor();
    }

    /**
     * 配置Feign的超时参数（底层仍会映射到Ribbon/LoadBalancer）
     */
    @Bean
    public Request.Options feignRequestOptions() {
        // 参数1：连接超时时间（毫秒），参数2：读取超时时间（毫秒），参数3：是否支持HTTPS
        return new Request.Options(60000, 60000, true);
    }
}
