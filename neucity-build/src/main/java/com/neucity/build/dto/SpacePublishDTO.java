package com.neucity.build.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.neucity.build.validation.SpaceRevisionDeserializer;
import lombok.Data;

@Data
public class SpacePublishDTO {
    @JsonDeserialize(using = SpaceRevisionDeserializer.class)
    private Long baseRevision;
}
