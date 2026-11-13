package com.neucity.build.controller;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.neucity.build.dto.DevicePageDTO;
import com.neucity.build.service.DriveDeviceService;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 16:19
 */
@Slf4j
@RestController
@RequestMapping("/device")
public class DeviceController extends BaseController {

    @Autowired
    private DriveDeviceService driveDeviceService;

    @GetMapping("/page")
    public TableDataInfo page(DevicePageDTO dto) {
        log.info("DeviceController.page dto：{}", JSONUtil.toJsonStr(dto));
        startPage();
        return getDataTable(driveDeviceService.page(dto));
    }

    @GetMapping("/overview")
    public AjaxResult overview(DevicePageDTO dto) {
        return success(driveDeviceService.overview(dto));
    }
}
