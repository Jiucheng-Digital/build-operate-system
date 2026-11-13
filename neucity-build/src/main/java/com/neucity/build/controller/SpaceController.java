package com.neucity.build.controller;

import com.neucity.build.dto.SpaceDraftSaveDTO;
import com.neucity.build.dto.SpacePublishDTO;
import com.neucity.build.service.SpaceDocumentService;
import com.neucity.common.core.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/space")
public class SpaceController {
    private final SpaceDocumentService service;

    public SpaceController(SpaceDocumentService service) {
        this.service = service;
    }

    @GetMapping("/draft")
    public AjaxResult readDraft(@RequestParam("projectId") String projectId) {
        return result(service.readDraft(projectId));
    }

    @PutMapping("/draft")
    public AjaxResult saveDraft(@RequestParam("projectId") String projectId, @RequestBody SpaceDraftSaveDTO request) {
        return result(service.saveDraft(projectId, request));
    }

    @PostMapping("/publish")
    public AjaxResult publish(@RequestParam("projectId") String projectId, @RequestBody SpacePublishDTO request) {
        return result(service.publish(projectId, request));
    }

    @GetMapping("/published")
    public AjaxResult readPublished(@RequestParam("projectId") String projectId) {
        return result(service.readPublished(projectId));
    }

    private AjaxResult result(Object data) {
        // AjaxResult.success(null) omits data, but the editor needs an explicit null.
        AjaxResult result = AjaxResult.success();
        result.put(AjaxResult.DATA_TAG, data);
        return result;
    }
}
