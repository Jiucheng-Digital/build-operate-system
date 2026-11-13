package com.neucity.common.security.config;

import com.neucity.common.security.interceptor.HeaderInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 拦截器配置
 *
 * @author shupf
 */
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 不需要拦截地址
     */
    public static final String[] excludeUrls = {"/login", "/logout", "/refresh"};

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getHeaderInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(excludeUrls)
                .order(-10);
    }

    // 配置日期字符串转Date的转换器（适配@RequestParam/@PathVariable）
    @Bean
    public Converter<String, Date> dateConverter() {
        return new Converter<String, Date>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            {
                // 关键：设置时区
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
            }
            @Override
            public Date convert(String source) {
                if (source == null || source.trim().isEmpty()) {
                    return null;
                }
                try {
                    return sdf.parse(source);
                } catch (ParseException e) {
                    throw new IllegalArgumentException("日期格式错误，要求：yyyy-MM-dd HH:mm:ss，实际：" + source);
                }
            }
        };
    }

    /**
     * 自定义请求头拦截器
     */
    public HeaderInterceptor getHeaderInterceptor() {
        return new HeaderInterceptor();
    }
}
