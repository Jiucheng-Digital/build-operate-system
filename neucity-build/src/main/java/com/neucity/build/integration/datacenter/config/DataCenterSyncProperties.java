package com.neucity.build.integration.datacenter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 数据中心 Kafka 同步配置。enabled 只控制发件箱投递，不影响业务事务内记录待发送消息。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "data-center.sync")
public class DataCenterSyncProperties {

    private boolean enabled = false;
    private String sourceCode = "NEUCITY_BUILD";
    private String deviceTopic = "data-center.device-events.v1";
    private String alarmTopic = "data-center.alarm-events.v1";
    private int batchSize = 100;
    private long publishDelayMs = 1000L;
    private int leaseSeconds = 180;
    private int sendTimeoutSeconds = 130;
    private long retryBaseSeconds = 30L;
    private long retryMaxSeconds = 3600L;
    private int publishedRetentionDays = 30;
}
