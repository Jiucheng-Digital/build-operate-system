package com.neucity.build.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpacePublishedVO {
    private Long revision;
    private Long publishedRevision;
    private String publishedAt;
}
