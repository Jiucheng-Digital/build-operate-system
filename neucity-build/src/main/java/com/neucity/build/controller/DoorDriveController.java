package com.neucity.build.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.dto.DoorDoControlByNameDTO;
import com.neucity.build.dto.DoorDoControlDTO;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.service.DoorDriveService;
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

import java.util.Collections;

/**
 * 门禁驱动接口
 *
 * @author ShuPF
 * @date 2026/6/11 12:00
 */
@Slf4j
@RestController
@RequestMapping("/door")
public class DoorDriveController extends BaseController {

    @Autowired
    private DoorDriveService driveService;

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    /**
     * 门禁点反控。
     */
    @Log(title = "设备-控制-门禁点反控", businessType = BusinessType.DELETE)
    @PostMapping("/doControl")
    public AjaxResult doorDoControl(@RequestBody @Validated DoorDoControlDTO request) {
        log.info("DoorDriveController.doorDoControl dto：{}", JSONUtil.toJsonStr(request));

        if (CollectionUtil.isEmpty(request.getDoorIndexCodes())) {
            return error("门禁点编号不能为空");
        }
        driveService.doorDoControl(request);
       return success("操作成功");
    }

    /**
     * 门禁点反控。
     */
    @Log(title = "设备-控制-门禁点反控", businessType = BusinessType.DELETE)
    @PostMapping("/doControl/byName")
    public AjaxResult doorDoControlByName(@RequestBody @Validated DoorDoControlByNameDTO request) {
        log.info("DoorDriveController.doorDoControlByName dto：{}", JSONUtil.toJsonStr(request));

        DriveDevice driveDevice = driveDeviceMapper.selectVoOne(new QueryWrapper<DriveDevice>()
                .eq("name", request.getName())
                .eq("classify", "access_control")
                .last("limit 1")
        );
        if (driveDevice == null) {
            return error("设备不存在");
        }

        DoorDoControlDTO doorDoControlDTO = new DoorDoControlDTO();
        doorDoControlDTO.setDoorIndexCodes(Collections.singletonList(driveDevice.getSerialNumber()));
        doorDoControlDTO.setControlType(request.getControlType());
        doorDoControlDTO.setDriveId(driveDevice.getId());

        driveService.doorDoControl(doorDoControlDTO);

        return success("操作成功");
    }

}
