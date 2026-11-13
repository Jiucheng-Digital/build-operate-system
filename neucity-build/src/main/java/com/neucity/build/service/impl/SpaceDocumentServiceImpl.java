package com.neucity.build.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neucity.build.domain.BuildSpaceDocument;
import com.neucity.build.dto.SpaceDraftSaveDTO;
import com.neucity.build.dto.SpacePublishDTO;
import com.neucity.build.exception.SpaceDocumentException;
import com.neucity.build.mapper.BuildSpaceDocumentMapper;
import com.neucity.build.service.SpaceDocumentService;
import com.neucity.build.service.SpacePermissionService;
import com.neucity.build.validation.SpaceSnapshotValidator;
import com.neucity.build.vo.SpaceDocumentVO;
import com.neucity.build.vo.SpaceDraftSavedVO;
import com.neucity.build.vo.SpacePublishedVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class SpaceDocumentServiceImpl implements SpaceDocumentService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset OFFSET = ZoneOffset.ofHours(8);
    private final BuildSpaceDocumentMapper mapper;
    private final ObjectMapper objectMapper;
    private final SpaceSnapshotValidator validator;
    private final SpacePermissionService permissions;

    public SpaceDocumentServiceImpl(BuildSpaceDocumentMapper mapper, ObjectMapper objectMapper,
                                    SpaceSnapshotValidator validator, SpacePermissionService permissions) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.permissions = permissions;
    }

    @Override
    public SpaceDocumentVO readDraft(String projectId) {
        checkProjectId(projectId);
        permissions.require(projectId, "query");
        BuildSpaceDocument document = mapper.selectDraft(projectId);
        return document == null ? null : view(document, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceDraftSavedVO saveDraft(String projectId, SpaceDraftSaveDTO request) {
        checkProjectId(projectId);
        String username = permissions.require(projectId, "save");
        if (request == null) {
            throw SpaceDocumentException.invalid("请求体不能为空");
        }
        long baseRevision = checkRevision(request.getBaseRevision(), true);
        JsonNode snapshot = request.getSnapshot();
        validator.validate(snapshot);
        LocalDateTime now = now();
        BuildSpaceDocument document = new BuildSpaceDocument();
        document.setSpaceId(snapshot.get("id").textValue());
        document.setProjectId(projectId);
        document.setName(snapshot.get("name").textValue());
        document.setDraftJson(writeSnapshot(snapshot));
        document.setRevision(baseRevision + 1);
        document.setUpdateBy(username);
        document.setUpdateTime(now);
        if (baseRevision == 0) {
            document.setCreateBy(username);
            document.setCreateTime(now);
            try {
                mapper.insert(document);
            } catch (DuplicateKeyException exception) {
                // Both a concurrent first save and reuse of another project's space ID are conflicts.
                throw SpaceDocumentException.conflict();
            }
        } else {
            // MySQL commonly uses a case-insensitive collation for varchar UUIDs.
            // Compare in Java as well so even a case-only ID change cannot be saved.
            if (!document.getSpaceId().equals(mapper.selectSpaceId(projectId))
                    || mapper.updateDraft(document, baseRevision) != 1) {
                throw SpaceDocumentException.conflict();
            }
        }
        return new SpaceDraftSavedVO(document.getSpaceId(), document.getRevision(), format(now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpacePublishedVO publish(String projectId, SpacePublishDTO request) {
        checkProjectId(projectId);
        String username = permissions.require(projectId, "publish");
        if (request == null) {
            throw SpaceDocumentException.invalid("请求体不能为空");
        }
        long baseRevision = checkRevision(request.getBaseRevision(), false);
        BuildSpaceDocument document = mapper.selectDraftForUpdate(projectId);
        if (document == null) {
            throw new SpaceDocumentException(404, "尚未保存空间草稿，请先保存后发布");
        }
        if (document.getRevision() != baseRevision) {
            throw SpaceDocumentException.conflict();
        }
        ObjectNode snapshot = (ObjectNode) readSnapshot(document.getDraftJson());
        validator.validate(snapshot);
        LocalDateTime publishedAt = now();
        snapshot.put("publishedAt", format(publishedAt));
        if (mapper.publish(projectId, baseRevision, writeSnapshot(snapshot), publishedAt, username) != 1) {
            throw SpaceDocumentException.conflict();
        }
        return new SpacePublishedVO(baseRevision + 1, baseRevision + 1, format(publishedAt));
    }

    @Override
    public SpaceDocumentVO readPublished(String projectId) {
        checkProjectId(projectId);
        permissions.require(projectId, "query");
        BuildSpaceDocument document = mapper.selectPublished(projectId);
        return document == null ? null : view(document, true);
    }

    private SpaceDocumentVO view(BuildSpaceDocument document, boolean published) {
        SpaceDocumentVO view = new SpaceDocumentVO();
        view.setSpaceId(document.getSpaceId());
        view.setRevision(published ? document.getPublishedRevision() : document.getRevision());
        view.setUpdatedAt(format(published ? document.getPublishedAt() : document.getUpdateTime()));
        view.setSnapshot(readSnapshot(published ? document.getPublishedJson() : document.getDraftJson()));
        view.setPublishedRevision(document.getPublishedRevision());
        view.setPublishedAt(format(document.getPublishedAt()));
        return view;
    }

    private JsonNode readSnapshot(String json) {
        try {
            JsonNode snapshot = objectMapper.readTree(json);
            if (snapshot == null || !snapshot.isObject()) {
                throw new IllegalStateException("数据库中的空间快照不是完整 JSON 对象");
            }
            return snapshot;
        } catch (JsonProcessingException exception) {
            // Never turn corrupt data or a database failure into an empty canvas.
            throw new IllegalStateException("数据库中的空间快照损坏", exception);
        }
    }

    private String writeSnapshot(JsonNode snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw SpaceDocumentException.invalid("snapshot 无法序列化为 JSON");
        }
    }

    private void checkProjectId(String projectId) {
        if (!StringUtils.hasText(projectId) || projectId.length() > 64 || !projectId.equals(projectId.trim())) {
            throw SpaceDocumentException.invalid("projectId 必须为 1 到 64 位的稳定项目或建筑 ID，且不能有首尾空格");
        }
    }

    private long checkRevision(Long revision, boolean allowZero) {
        if (revision == null || revision < (allowZero ? 0 : 1) || revision == Long.MAX_VALUE) {
            throw SpaceDocumentException.invalid(allowZero ? "baseRevision 必须为非负整数" : "baseRevision 必须为正整数");
        }
        return revision;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.atOffset(OFFSET).toString();
    }
}
