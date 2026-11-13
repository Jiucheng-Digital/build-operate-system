package com.neucity.build.controller;

import com.neucity.build.dto.EventCreateDTO;
import com.neucity.build.dto.EventPageDTO;
import com.neucity.build.service.SysEventService;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 9:30
 */
@RestController
@RequestMapping("/event")
public class SysEventController extends BaseController {

    @Autowired
    private SysEventService sysEventService;

    /**
     * 主动上报事件，直接进入事件中心。
     */
    @PostMapping
    public AjaxResult create(@RequestBody @Validated EventCreateDTO dto) {
        return success(sysEventService.create(dto));
    }

    @GetMapping("/page")
    public TableDataInfo page(EventPageDTO dto) {
        startPage();
        return getDataTable(sysEventService.page(dto));
    }

    @GetMapping("/overview")
    public AjaxResult overview() {
        return success(sysEventService.overview());
    }
}
