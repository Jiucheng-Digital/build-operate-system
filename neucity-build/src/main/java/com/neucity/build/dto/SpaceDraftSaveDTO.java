package com.neucity.build.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.neucity.build.validation.SpaceRevisionDeserializer;
import lombok.Data;

@Data
public class SpaceDraftSaveDTO {
    /** 0 creates a draft; subsequent writes use the last server revision. */
    @JsonDeserialize(using = SpaceRevisionDeserializer.class)
    private Long baseRevision;
    /** Keep the complete JSON tree, including fields unknown to this backend. */
    private JsonNode snapshot;
}
