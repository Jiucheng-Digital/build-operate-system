package com.neucity.build.integration.datacenter.task;

import com.neucity.build.integration.datacenter.config.DataCenterSyncProperties;
import com.neucity.build.integration.datacenter.domain.IntegrationOutbox;
import com.neucity.build.integration.datacenter.mapper.DataCenterOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 将已提交的发件箱消息可靠发布到数据中心 Kafka。 */
@Slf4j
@Component
public class DataCenterOutboxPublisher {

    private final DataCenterOutboxMapper outboxMapper;
    private final DataCenterSyncProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DataCenterOutboxPublisher(DataCenterOutboxMapper outboxMapper,
                                     DataCenterSyncProperties properties,
                                     KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMapper = outboxMapper;
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${data-center.sync.publish-delay-ms:1000}")
    public void publish() {
        if (!properties.isEnabled()) {
            return;
        }

        Date now = new Date();
        int batchSize = Math.max(1, Math.min(properties.getBatchSize(), 1000));
        List<IntegrationOutbox> messages = outboxMapper.selectDue(now, batchSize);
        for (IntegrationOutbox message : messages) {
            if (!properties.isEnabled()) {
                break;
            }
            publishOne(message);
        }
    }

    @Scheduled(cron = "${data-center.sync.cleanup-cron:0 15 * * * ?}")
    public void cleanupPublished() {
        int retentionDays = Math.max(1, properties.getPublishedRetentionDays());
        Date before = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays));
        for (int i = 0; i < 10; i++) {
            int deleted = outboxMapper.deletePublishedBefore(before, 5000);
            if (deleted < 5000) {
                break;
            }
        }
    }

    private void publishOne(IntegrationOutbox message) {
        Date now = new Date();
        String token = UUID.randomUUID().toString();
        int sendTimeoutSeconds = Math.max(1, properties.getSendTimeoutSeconds());
        long leaseMillis = Math.max(Math.max(30, properties.getLeaseSeconds()), sendTimeoutSeconds + 10L) * 1000L;
        Date claimUntil = new Date(now.getTime() + leaseMillis);
        if (outboxMapper.claim(message.getId(), token, now, claimUntil) != 1) {
            return;
        }

        try {
            String topic = topic(message);
            kafkaTemplate.send(topic, message.getTopicKey(), message.getPayloadJson())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            outboxMapper.markPublished(message.getId(), token, new Date());
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            int attempt = message.getAttemptCount() == null ? 1 : message.getAttemptCount() + 1;
            long retrySeconds = retrySeconds(attempt);
            Date failedAt = new Date();
            Date nextAttemptAt = new Date(failedAt.getTime() + retrySeconds * 1000L);
            String error = limited(rootMessage(exception), 1000);
            outboxMapper.markRetry(message.getId(), token, nextAttemptAt, error, failedAt);
            log.warn("数据中心消息发布失败，messageId={}，attempt={}，{} 秒后重试：{}",
                    message.getMessageId(), attempt, retrySeconds, error);
        }
    }

    private String topic(IntegrationOutbox message) {
        String topic = "DEVICE".equals(message.getAggregateType())
                ? properties.getDeviceTopic() : properties.getAlarmTopic();
        if (StringUtils.isBlank(topic)) {
            throw new IllegalStateException("数据中心 Kafka topic 未配置");
        }
        return topic.trim();
    }

    private long retrySeconds(int attempt) {
        long base = Math.max(1L, properties.getRetryBaseSeconds());
        long max = Math.max(base, properties.getRetryMaxSeconds());
        int exponent = Math.max(0, Math.min(attempt - 1, 20));
        long calculated;
        try {
            calculated = Math.multiplyExact(base, 1L << exponent);
        } catch (ArithmeticException exception) {
            calculated = max;
        }
        return Math.min(calculated, max);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = StringUtils.defaultIfBlank(current.getMessage(), current.getClass().getSimpleName());
        return current.getClass().getSimpleName() + ": " + message;
    }

    private String limited(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
