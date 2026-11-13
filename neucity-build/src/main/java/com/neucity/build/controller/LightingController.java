package com.neucity.build.controller;

import cn.hutool.json.JSONUtil;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.dto.LightingControlDTO;
import com.neucity.build.service.LightingService;
import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 14:57
 */
@Slf4j
@RestController
@RequestMapping("lighting")
public class LightingController extends BaseController {

    @Autowired
    private LightingService lightingService;

//    @Log(title = "设备-控制所有照明开关", businessType = BusinessType.UPDATE)
//    @PostMapping("controlAllSensor")
    public AjaxResult controlAllSensor(@RequestBody @Validated LightingControlDTO dto) {
        return success(lightingService.controlAllSensor(dto));
    }

    @Log(title = "设备-控制单个照明开关", businessType = BusinessType.UPDATE)
    @PostMapping("controlSensor")
    public AjaxResult controlSensor(@RequestBody @Validated LightingControlDTO dto) {
        log.info("LightingController.controlSensor dto：{}", JSONUtil.toJsonStr(dto));

        return success(lightingService.controlSensor(dto));
    }
}
