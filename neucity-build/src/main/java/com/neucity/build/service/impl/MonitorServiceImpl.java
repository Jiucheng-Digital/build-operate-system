package com.neucity.build.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.dto.VideoCamerasPreviewURLsDTO;
import com.neucity.build.dto.VideoPtzsControllingDTO;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.service.MonitorService;
import com.neucity.build.vo.DriveResultVO;
import com.neucity.build.vo.VideoCamerasPreviewURLsVO;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.utils.http.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/15 9:52
 */
@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {

    @Value("${monitor.url}")
    private String monitorUrl;

    @Value("${spring.profiles.active}")
    private String active;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;


    @Override
    public VideoCamerasPreviewURLsVO previewURLs(VideoCamerasPreviewURLsDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getDriveId());
        if (driveInfo == null) {
            throw new ServiceException("未找到对应驱动数据");
        }


        String url = driveInfo.getUrl() + DriverAPI.MotorApi.GET_CAMERA_URL;
        Map<String, Object> params = BeanUtil.beanToMap(dto,false, true);
        params.remove("driveId");

        DriveResultVO driveResultVO = getResult(params, url);
        VideoCamerasPreviewURLsVO bean = JSONUtil.toBean(driveResultVO.getData().toString(), VideoCamerasPreviewURLsVO.class);
        if (active.equals("dev")) {
            bean.setUrl(bean.getUrl().replaceAll("http://172.11.19.121:83", monitorUrl));
        }
        else {
            bean.setUrl(bean.getUrl().replaceAll("172.11.19.121", monitorUrl));
        }

        return bean;
    }

    @Override
    public VideoCamerasPreviewURLsVO previewURLsByName(String name) {
        DriveDevice driveDevice = driveDeviceMapper.selectOne(new QueryWrapper<DriveDevice>()
                .eq("name", name)
                .eq("classify", "monitor")
                .last("limit 1")
        );
        if (driveDevice == null) {
            throw new ServiceException("未找到对应设备数据");
        }

        DriveInfo driveInfo = deviceInfoMapper.selectById(driveDevice.getDriveId());
        if (driveInfo == null) {
            throw new ServiceException("未找到对应驱动数据");
        }

        VideoCamerasPreviewURLsDTO dto = new VideoCamerasPreviewURLsDTO();
        dto.setCameraIndexCode(driveDevice.getSerialNumber());
        dto.setStreamType(1);
        dto.setProtocol("hls");

        String url = driveInfo.getUrl() + DriverAPI.MotorApi.GET_CAMERA_URL;
        Map<String, Object> params = BeanUtil.beanToMap(dto,false, true);
        params.remove("driveId");

        DriveResultVO driveResultVO = getResult(params, url);
        VideoCamerasPreviewURLsVO bean = JSONUtil.toBean(driveResultVO.getData().toString(), VideoCamerasPreviewURLsVO.class);
        if (active.equals("dev")) {
            bean.setUrl(bean.getUrl().replaceAll("http://172.11.19.121:83", monitorUrl));
        }
        else {
            bean.setUrl(bean.getUrl().replaceAll("172.11.19.121", monitorUrl));
        }

        return bean;
    }

    @Override
    public void ptzsControl(VideoPtzsControllingDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getDriveId());
        if (driveInfo == null) {
            throw new ServiceException("未找到对应驱动数据");
        }

        String url = driveInfo.getUrl() + DriverAPI.MotorApi.PTZS_CONTROL_URL;
        Map<String, Object> params = BeanUtil.beanToMap(dto, false, true);
        params.remove("driveId");

        this.getResult(params, url);
    }

    private DriveResultVO getResult(Map<String, Object> params, String url) {
        String body = JSONUtil.toJsonStr(params);
        log.info("MonitorServiceImpl.getResult 请求参数：url: [{}], params: {}",url, body);
        String result = HttpUtils.sendPostJSON(url, body);
        log.info("MonitorServiceImpl.getResult 响应结果：{}", result);

        DriveResultVO driveResultVO = null;
        try {
            driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);
        } catch (Exception e){
            throw new ServiceException("操作失败");
        }

        if (!driveResultVO.isSuccess()) {
            throw new ServiceException(driveResultVO.getMessage());
        }

        return driveResultVO;
    }
}
