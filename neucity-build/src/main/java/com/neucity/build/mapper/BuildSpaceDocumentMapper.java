package com.neucity.build.mapper;

import com.neucity.build.domain.BuildSpaceDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BuildSpaceDocumentMapper {
    String selectSpaceId(@Param("projectId") String projectId);

    BuildSpaceDocument selectDraft(@Param("projectId") String projectId);

    BuildSpaceDocument selectDraftForUpdate(@Param("projectId") String projectId);

    BuildSpaceDocument selectPublished(@Param("projectId") String projectId);

    int insert(BuildSpaceDocument document);

    int updateDraft(@Param("document") BuildSpaceDocument document,
                    @Param("baseRevision") long baseRevision);

    int publish(@Param("projectId") String projectId, @Param("baseRevision") long baseRevision,
                @Param("snapshotJson") String snapshotJson, @Param("publishedAt") LocalDateTime publishedAt,
                @Param("username") String username);
}
