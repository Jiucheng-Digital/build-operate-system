package com.neucity.build.integration.datacenter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.SysEvent;
import com.neucity.build.integration.datacenter.config.DataCenterSyncProperties;
import com.neucity.build.integration.datacenter.domain.IntegrationOutbox;
import com.neucity.build.integration.datacenter.dto.DataCenterIngestEnvelope;
import com.neucity.build.integration.datacenter.mapper.DataCenterOutboxMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 在业务事务内生成数据中心消息并写入发件箱。调用方事务失败时，业务数据和消息一起回滚。
 */
@Service
public class DataCenterOutboxService {

    public static final String DEVICE_UPSERT = "DEVICE_UPSERT";
    public static final String DEVICE_STATUS_CHANGED = "DEVICE_STATUS_CHANGED";
    public static final String ALARM_CREATED = "ALARM_CREATED";

    private static final String SCHEMA_VERSION = "1.0";
    private static final String STATUS_PENDING = "PENDING";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NORMAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataCenterOutboxMapper outboxMapper;
    private final DataCenterSyncProperties properties;
    private final ObjectMapper objectMapper;

    public DataCenterOutboxService(DataCenterOutboxMapper outboxMapper,
                                   DataCenterSyncProperties properties,
                                   ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void enqueueDeviceUpserts(Collection<? extends DriveDevice> devices, Date occurredAt) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        List<IntegrationOutbox> messages = new ArrayList<>(devices.size());
        for (DriveDevice device : devices) {
            if (device != null && device.getId() != null) {
                messages.add(deviceMessage(device, DEVICE_UPSERT, occurredAt));
            }
        }
        insertMessages(messages);
    }

    public void enqueueDeviceStatusChanges(Collection<? extends DriveDevice> devices, Date occurredAt) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        List<IntegrationOutbox> messages = new ArrayList<>(devices.size());
        for (DriveDevice device : devices) {
            if (device != null && device.getId() != null) {
                messages.add(deviceMessage(device, DEVICE_STATUS_CHANGED, occurredAt));
            }
        }
        insertMessages(messages);
    }

    public void enqueueAlarms(Collection<? extends SysEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<IntegrationOutbox> messages = new ArrayList<>(events.size());
        for (SysEvent event : events) {
            if (event != null && event.getId() != null) {
                messages.add(alarmMessage(event));
            }
        }
        insertMessages(messages);
    }

    private IntegrationOutbox deviceMessage(DriveDevice device, String eventType, Date occurredAt) {
        String externalId = String.valueOf(device.getId());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("externalDeviceId", externalId);
        payload.put("status", limited(defaultValue(device.getStatus(), "UNKNOWN").toUpperCase(Locale.ROOT), 32));

        if (DEVICE_UPSERT.equals(eventType)) {
            payload.put("name", limited(defaultValue(device.getName(), "未命名设备"), 200));
            payload.put("category", category(device.getClassify()));
            putText(payload, "manufacturer", device.getManufacturer(), 100);
            putText(payload, "serialNumber", device.getSerialNumber(), 128);

            ObjectNode location = objectMapper.createObjectNode();
            putText(location, "address", device.getAddress(), 500);
            if (location.size() > 0) {
                payload.set("location", location);
            }

            ObjectNode network = objectMapper.createObjectNode();
            putText(network, "ip", device.getIp(), 128);
            if (device.getPort() != null) {
                network.put("port", device.getPort());
            }
            if (network.size() > 0) {
                payload.set("network", network);
            }

            ObjectNode attributes = objectMapper.createObjectNode();
            putNumber(attributes, "driveId", device.getDriveId());
            putText(attributes, "sourceClassify", device.getClassify(), 128);
            putText(attributes, "state", device.getState(), 128);
            putText(attributes, "attribute1", device.getAttribute1(), 1000);
            putText(attributes, "attribute2", device.getAttribute2(), 1000);
            putRawJson(attributes, "raw", device.getExtend());
            payload.set("attributes", attributes);
        }

        return outbox("DEVICE", externalId, eventType, occurredAt, payload);
    }

    private IntegrationOutbox alarmMessage(SysEvent event) {
        String externalId = String.valueOf(event.getId());
        Date occurredAt = parseEventTime(event.getEventTime(), event.getCreateTime());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("externalAlarmId", externalId);
        if (event.getDeviceId() != null) {
            payload.put("externalDeviceId", String.valueOf(event.getDeviceId()));
        }
        payload.put("alarmCode", limited(defaultValue(event.getEventId(), "BUILD_EVENT"), 128));
        payload.put("alarmName", limited(defaultValue(event.getEventName(), "建筑系统告警"), 200));
        payload.put("severity", severity(event.getLevel()));
        payload.put("sourceStatus", sourceStatus(event.getHandlerState()));
        payload.put("occurredAt", format(occurredAt));
        putText(payload, "description", event.getEventName(), 2000);

        ObjectNode attributes = objectMapper.createObjectNode();
        putNumber(attributes, "driveId", event.getDriveId());
        putText(attributes, "manufacturer", event.getManufacturer(), 100);
        putText(attributes, "classify", event.getClassify(), 128);
        putText(attributes, "handlerBy", event.getHandlerBy(), 128);
        putRawJson(attributes, "raw", event.getExtend());
        payload.set("attributes", attributes);

        return outbox("ALARM", externalId, ALARM_CREATED, occurredAt, payload);
    }

    private IntegrationOutbox outbox(String aggregateType, String aggregateId, String eventType,
                                      Date occurredAt, JsonNode payload) {
        Date now = new Date();
        Date eventTime = occurredAt == null ? now : occurredAt;
        Date producedAt = now.before(eventTime) ? eventTime : now;
        String sourceCode = normalizedSourceCode();
        String messageId = sourceCode + ":" + UUID.randomUUID().toString();

        DataCenterIngestEnvelope envelope = new DataCenterIngestEnvelope();
        envelope.setMessageId(messageId);
        envelope.setSourceCode(sourceCode);
        envelope.setSchemaVersion(SCHEMA_VERSION);
        envelope.setEventType(eventType);
        envelope.setOccurredAt(format(eventTime));
        envelope.setProducedAt(format(producedAt));
        envelope.setPayload(payload);

        IntegrationOutbox outbox = new IntegrationOutbox();
        outbox.setMessageId(messageId);
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setEventType(eventType);
        outbox.setTopicKey(sourceCode + ":" + aggregateId);
        outbox.setPayloadJson(write(envelope));
        outbox.setStatus(STATUS_PENDING);
        outbox.setAttemptCount(0);
        outbox.setNextAttemptAt(now);
        outbox.setCreateTime(now);
        outbox.setUpdateTime(now);
        return outbox;
    }

    private void insertMessages(List<IntegrationOutbox> messages) {
        if (!messages.isEmpty()) {
            outboxMapper.insertBatch(messages, 500);
        }
    }

    private String normalizedSourceCode() {
        String sourceCode = StringUtils.trimToEmpty(properties.getSourceCode()).toUpperCase(Locale.ROOT);
        if (!sourceCode.matches("^[A-Z][A-Z0-9_-]{0,31}$")) {
            throw new IllegalStateException("data-center.sync.source-code 配置不合法");
        }
        return sourceCode;
    }

    private String write(DataCenterIngestEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化数据中心消息失败", exception);
        }
    }

    private void putRawJson(ObjectNode target, String field, String raw) {
        if (StringUtils.isBlank(raw)) {
            return;
        }
        try {
            target.set(field, objectMapper.readTree(raw));
        } catch (JsonProcessingException exception) {
            target.put(field, raw);
        }
    }

    private void putText(ObjectNode target, String field, String value, int maxLength) {
        if (StringUtils.isNotBlank(value)) {
            target.put(field, limited(value, maxLength));
        }
    }

    private void putNumber(ObjectNode target, String field, Long value) {
        if (value != null) {
            target.put(field, value);
        }
    }

    private String category(String classify) {
        String value = StringUtils.lowerCase(StringUtils.trimToEmpty(classify), Locale.ROOT);
        if (value.contains("soil")) {
            return "SOIL_MONITOR";
        }
        if (value.contains("water")) {
            return "WATER_MONITOR";
        }
        if (value.contains("weather") || value.contains("meteorolog") || value.contains("environment")) {
            return "WEATHER_STATION";
        }
        if (value.contains("camera") || value.contains("video") || value.contains("monitor")) {
            return "CAMERA";
        }
        if (value.contains("sensor")) {
            return "SENSOR";
        }
        if (value.contains("gateway")) {
            return "GATEWAY";
        }
        return "OTHER";
    }

    private String severity(String level) {
        String value = StringUtils.lowerCase(StringUtils.trimToEmpty(level), Locale.ROOT);
        if ("high".equals(value)) {
            return "HIGH";
        }
        if ("middle".equals(value) || "medium".equals(value)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String sourceStatus(String handlerState) {
        String value = StringUtils.lowerCase(StringUtils.trimToEmpty(handlerState), Locale.ROOT);
        if ("completed".equals(value)) {
            return "CLOSED";
        }
        if ("pending".equals(value)) {
            return "PROCESSING";
        }
        return "OPEN";
    }

    private Date parseEventTime(String value, Date fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback == null ? new Date() : fallback;
        }
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException ignored) {
            // 继续尝试无时区的本地时间。
        }
        try {
            return Date.from(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(DEFAULT_ZONE).toInstant());
        } catch (DateTimeParseException ignored) {
            // 继续尝试数据库常用时间格式。
        }
        try {
            return Date.from(LocalDateTime.parse(value, NORMAL_DATE_TIME).atZone(DEFAULT_ZONE).toInstant());
        } catch (DateTimeParseException ignored) {
            return fallback == null ? new Date() : fallback;
        }
    }

    private String format(Date date) {
        return date.toInstant().atZone(DEFAULT_ZONE).toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.isBlank(value) ? fallback : value.trim();
    }

    private String limited(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
