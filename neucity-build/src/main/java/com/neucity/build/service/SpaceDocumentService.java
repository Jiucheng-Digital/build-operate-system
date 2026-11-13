package com.neucity.build.service;

import com.neucity.build.dto.SpaceDraftSaveDTO;
import com.neucity.build.dto.SpacePublishDTO;
import com.neucity.build.vo.SpaceDocumentVO;
import com.neucity.build.vo.SpaceDraftSavedVO;
import com.neucity.build.vo.SpacePublishedVO;

public interface SpaceDocumentService {
    SpaceDocumentVO readDraft(String projectId);

    SpaceDraftSavedVO saveDraft(String projectId, SpaceDraftSaveDTO request);

    SpacePublishedVO publish(String projectId, SpacePublishDTO request);

    SpaceDocumentVO readPublished(String projectId);
}
