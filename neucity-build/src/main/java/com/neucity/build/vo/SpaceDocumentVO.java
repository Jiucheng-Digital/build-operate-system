package com.neucity.build.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SpaceDocumentVO {
    private String spaceId;
    private Long revision;
    private String updatedAt;
    private JsonNode snapshot;
    private Long publishedRevision;
    private String publishedAt;
}
