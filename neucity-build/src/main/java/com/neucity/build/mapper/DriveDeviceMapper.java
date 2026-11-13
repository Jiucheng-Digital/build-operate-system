package com.neucity.build.mapper;


import com.neucity.build.domain.DriveDevice;
import com.neucity.build.dto.DevicePageDTO;
import com.neucity.build.params.DeviceParam;
import com.neucity.build.vo.DeviceOverviewVo;
import com.neucity.build.vo.DriveDeviceVo;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 九城ai智能体Mapper接口
 *
 * @author z
 * @date 2025-08-13
 */
@Mapper
public interface DriveDeviceMapper extends BaseMapperPlus<DriveDevice, DriveDeviceVo> {

    DriveDeviceVo selectDriveDeviceById(Long id);

    List<DriveDeviceVo> selectDriveDevice(DevicePageDTO dto);

    DeviceOverviewVo selectDeviceOverview(DevicePageDTO dto);

    List<DriveDeviceVo> selectOnlineDevice(DeviceParam param);

    List<DriveDeviceVo> selectInstalledDevice(DeviceParam param);

    int updateDeviceState(@Param("driveId") Long driveId, @Param("state") String state,
                          @Param("status") String status, @Param("serialNumber") String serialNumber);

    int updateDeviceStateByAttribute(@Param("driveId") Long driveId, @Param("state") String state,
                                     @Param("status") String status, @Param("attribute1") String attribute1);

    List<DriveDevice> selectOfflineDevices(@Param("eventSource") String eventSource, @Param("offlineTime") Integer offlineTime);
}
