package com.neucity.build.controller;

import com.neucity.build.dto.EventRuleCreateDTO;
import com.neucity.build.dto.EventRulePageDTO;
import com.neucity.build.dto.StatusEditDTO;
import com.neucity.build.service.SysEventRuleService;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Event rule controller.
 *
 * @author Codex
 * @date 2026/6/18
 */
@RestController
@RequestMapping("/event/rule")
public class SysEventRuleController extends BaseController {

    @Autowired
    private SysEventRuleService eventRuleService;

    @GetMapping("/page")
    public TableDataInfo page(EventRulePageDTO dto) {
        startPage();
        return getDataTable(eventRuleService.page(dto));
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(eventRuleService.getInfo(id));
    }

    @PostMapping
    public AjaxResult create(@RequestBody @Validated EventRuleCreateDTO dto) {
        return toAjax(eventRuleService.create(dto));
    }

    @PutMapping("/status/edit")
    public AjaxResult statusEdit(@RequestBody @Validated StatusEditDTO dto) {
        return success(eventRuleService.statusEdit(dto));
    }
}
