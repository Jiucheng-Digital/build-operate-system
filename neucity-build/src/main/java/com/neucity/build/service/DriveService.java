package com.neucity.build.service;

import com.neucity.build.dto.DriveCommonDTO;
import com.neucity.build.dto.DriveDTO;
import com.neucity.build.dto.DriveListDTO;
import com.neucity.build.dto.UpdateConfigDTO;
import com.neucity.build.vo.DriveInfoVo;
import com.neucity.build.vo.DriveMonitorVo;

import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/3 16:26
 */
public interface DriveService {

    /**
     *  上传驱动
     */
    void uploadDrive(DriveDTO dto);

    /**
     *  删除驱动
     */
    int removeDrive(Long driveId);

    /**
     *  安装驱动
     */
    int installDrive(Long driveId);

    List<DriveInfoVo> list(DriveListDTO dto);

    List<DriveInfoVo> listByClassify(String classifyCode);

    Object getConfig(DriveCommonDTO dto);

    /**
     * 获取驱动所在服务器监控信息
     */
    DriveMonitorVo monitor(Long driveId);

    void updateConfig(UpdateConfigDTO dto);

    Object getApi(DriveCommonDTO dto);
}
