package com.neucity.build.controller;

import com.neucity.build.service.DriveDeviceService;
import com.neucity.build.task.DoorDriveTask;
import com.neucity.build.task.DriveDeviceTask;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/15 15:51
 */
@RestController
@ConditionalOnProperty(name = "build.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping(value = "test")
public class TestController extends BaseController {

    @Autowired
    private DriveDeviceTask driveDeviceTask;

    @Autowired
    private DriveDeviceService driveDeviceService;

    @Autowired
    private DoorDriveTask doorDriveTask;

    @GetMapping(value = "syncDevice")
    public AjaxResult syncDevice(Long driveId) {
        driveDeviceService.deviceSync(driveId);
        return success();
    }

    @GetMapping(value = "syncEvent")
    public AjaxResult syncEvent(String manufacturer) {
        if (StringUtils.isNotBlank(manufacturer) && "reformer".equals(manufacturer)) {
            doorDriveTask.syncLfEvent();
        }
        else {
            doorDriveTask.syncHkEvent();
        }

        return success();
    }

}
