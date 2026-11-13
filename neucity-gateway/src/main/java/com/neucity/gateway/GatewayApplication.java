package com.neucity.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.util.StopWatch;

/**
 * 网关启动程序
 * 
 * @author shupf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class GatewayApplication {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        SpringApplication.run(GatewayApplication.class, args);
        stopWatch.stop();
        LOGGER.info("【九城数字】&【网关服务】启动成功! 耗时 [{}] 毫秒.", stopWatch.getLastTaskTimeMillis());
    }
}
