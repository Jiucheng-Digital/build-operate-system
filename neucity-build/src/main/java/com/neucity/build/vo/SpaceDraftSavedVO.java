package com.neucity.build.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceDraftSavedVO {
    private String spaceId;
    private Long revision;
    private String updatedAt;
}
