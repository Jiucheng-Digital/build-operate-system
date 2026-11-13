package com.neucity.build.service;

import com.neucity.build.dto.DevicePageDTO;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.vo.DeviceOverviewVo;
import com.neucity.build.vo.DriveDeviceVo;

import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 14:04
 */

public interface DriveDeviceService {

    /**
     * 同步设备信息
     */
    void deviceSync(Long driveId);

    /**
     * 更新驱动设备状态；状态发生变化时同步记录数据中心消息。
     */
    void updateDeviceState(DriveDevice device, String state, String status);

    /**
     * 分页查询设备信息
     */
    List<DriveDeviceVo> page(DevicePageDTO dto);

    /**
     * 设备总览
     */
    DeviceOverviewVo overview(DevicePageDTO dto);
}
