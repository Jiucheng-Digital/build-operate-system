package com.neucity.build.task;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.service.DriveDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 驱动设备同步定时器
 *
 * @author ShuPF
 * @date 2026/6/10 15:14
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "build.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class DriveDeviceTask {

    /** 驱动mapper */
    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DriveDeviceService driveDeviceService;

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void syncDevice() {
        log.info("DriveDeviceTask-定时同步设备-开始");

        // 获取所有驱动信息
        List<DriveInfo> driveInfos = deviceInfoMapper.selectList(new QueryWrapper<DriveInfo>()
                .eq("status", "install_success")
        );
        if (CollectionUtil.isEmpty(driveInfos)) {
            log.info("DriveDeviceTask-定时同步设备-无驱动信息");
            return;
        }

        log.info("DriveDeviceTask-定时同步设备-驱动信息：size: [{}]", driveInfos.size());

        driveInfos.forEach(driveInfo -> {
            try {
                driveDeviceService.deviceSync(driveInfo.getId());
            } catch (Exception e) {
                log.error("DriveDeviceTask-定时同步设备-同步设备失败：driveInfo: [{}]", driveInfo, e);
            }
        });

        log.info("DriveDeviceTask-定时同步设备-结束");
    }

}
