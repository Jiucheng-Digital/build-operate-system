package com.neucity.build.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** One complete editor snapshot per business project/building. */
@Data
public class BuildSpaceDocument {
    private String spaceId;
    private String projectId;
    private String name;
    private String draftJson;
    private Long revision;
    private String publishedJson;
    private Long publishedRevision;
    private LocalDateTime publishedAt;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
