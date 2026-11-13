package com.neucity.build;

import com.neucity.common.security.annotation.EnableRyFeignClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StopWatch;

/**
 * 智慧医院服务
 *
 * @author shupf
 */
@ComponentScan(basePackages = {"com.neucity.**"})
@EnableRyFeignClients
@SpringBootApplication
@EnableScheduling
public class BuildApplication {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildApplication.class);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        SpringApplication.run(BuildApplication.class, args);
        stopWatch.stop();
        LOGGER.info("【九城数字】&【建筑操作系统】启动成功! 耗时 [{}] 毫秒.", stopWatch.getLastTaskTimeMillis());
    }
}
