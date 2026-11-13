package com.neucity.build.integration.datacenter.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/** 与数据中心 IngestEnvelope 1.0 对齐的 JSON 信封。 */
@Data
public class DataCenterIngestEnvelope {

    private String messageId;
    private String sourceCode;
    private String schemaVersion;
    private String eventType;
    private String occurredAt;
    private String producedAt;
    private String traceId;
    private JsonNode payload;
}
