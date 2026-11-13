package com.neucity.system;

import com.neucity.common.security.annotation.EnableCustomConfig;
import com.neucity.common.security.annotation.EnableRyFeignClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.StopWatch;

/**
 * 系统模块
 *
 * @author shupf
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.neucity.**"})
public class SystemApplication {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemApplication.class);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        SpringApplication.run(SystemApplication.class, args);
        stopWatch.stop();
        LOGGER.info("【九城数字】&【系统服务】启动成功! 耗时 [{}] 毫秒.", stopWatch.getLastTaskTimeMillis());
    }
}
