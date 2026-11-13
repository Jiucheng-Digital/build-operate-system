package com.neucity.build.controller;

import cn.hutool.json.JSONUtil;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.dto.VideoCamerasPreviewURLsDTO;
import com.neucity.build.dto.VideoPtzsControllingDTO;
import com.neucity.build.service.MonitorService;
import com.neucity.build.vo.VideoCamerasPreviewURLsVO;
import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 17:55
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController extends BaseController {

    @Autowired
    private MonitorService monitorService;

    @Log(title = "设备-监控-云台控制", businessType = BusinessType.INSERT)
    @PostMapping("/ptzs/control")
    public AjaxResult ptzsControl(@RequestBody @Validated VideoPtzsControllingDTO dto) {
        monitorService.ptzsControl(dto);
        return success("获取成功");
    }

    /**
     * 获取监控点预览取流URLv2
     */
    @PostMapping("/previewURLs")
    public AjaxResult videoCamerasPreviewURLs(@RequestBody @Validated VideoCamerasPreviewURLsDTO dto) {
        return success(monitorService.previewURLs(dto));
    }

    /**
     * 获取监控点预览取流URLv2
     */
    @GetMapping("/previewURLs/byName")
    public AjaxResult videoCamerasPreviewURLsByName(String name) {
        return success(monitorService.previewURLsByName(name));
    }
}
