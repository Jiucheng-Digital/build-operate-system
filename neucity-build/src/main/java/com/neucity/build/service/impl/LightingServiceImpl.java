package com.neucity.build.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.api.system.UserRemoteDubboService;
import com.neucity.api.system.vo.UserVo;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.dto.LightingControlDTO;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.service.LightingService;
import com.neucity.build.vo.DriveDeviceVo;
import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.dto.websocket.BuildDeviceDTO;
import com.neucity.common.core.model.dto.websocket.CommonMsgDTO;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.security.service.TokenService;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.build.constants.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 15:07
 */
@Slf4j
@Service
public class LightingServiceImpl implements LightingService {

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TokenService tokenService;

    @DubboReference(url = "${system.dubbo.url:}")
    private UserRemoteDubboService userRemoteDubboService;

    @Override
    public Object controlAllSensor(LightingControlDTO dto) {
        DriveDeviceVo driveDevice = driveDeviceMapper.selectDriveDeviceById(dto.getDeviceId());
        if (driveDevice == null) {
            throw new ServiceException("未找到对应设备");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("vvv", dto.getVvv());

        String requestUrl = driveDevice.getUrl() + DriverAPI.Lighting.CONTROL_ALL_SENSOR;

        return this.sendRequest(map, requestUrl);
    }

    @Override
    public Object controlSensor(LightingControlDTO dto) {
        log.info("controlSensor 控制传感器 dto: {}", JSONUtil.toJsonStr(dto));
        DriveDeviceVo driveDevice = null;
        if (dto.getDeviceId() != null) {
            driveDevice = driveDeviceMapper.selectDriveDeviceById(dto.getDeviceId());
        } else if (StringUtils.isNotBlank(dto.getName())) {
            driveDevice = driveDeviceMapper.selectVoOne(new QueryWrapper<DriveDevice>()
                    .eq("name", dto.getName())
                    .eq("classify", "lighting")
                    .last("limit 1")
            );
        }

        if (driveDevice == null) {
            throw new ServiceException("未找到对应设备");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("vvv", dto.getVvv());
        map.put("dtuCode", driveDevice.getAttribute1());
        map.put("deviceCode", driveDevice.getSerialNumber());
        map.put("sensorCode", driveDevice.getAttribute2());

        String requestUrl = driveDevice.getUrl() + DriverAPI.Lighting.CONTROL_SENSOR;
        boolean flag = this.sendRequest(map, requestUrl);
        log.info("controlSensor 控制传感器结果: {}", flag);
        if (flag) {
            driveDevice.setState(dto.getVvv());
            driveDeviceMapper.updateById(driveDevice);
        }

        this.sendOcLargeScreen(dto.getVvv(), driveDevice.getId() + "", driveDevice.getName(), dto.getUserId());

        return driveDevice;
    }

    private boolean sendRequest(Map<String, Object> map, String requestUrl) {
        String response = HttpUtils.sendPost(requestUrl, JSONUtil.toJsonStr(map), MediaType.APPLICATION_JSON_VALUE);
        try {
            JSONObject object = JSONUtil.parseObj(response);
            return object.getBool("success");
        } catch (Exception e) {
            log.error("请求异常: {}", e.getMessage());
            return false;
        }
    }

    private void sendOcLargeScreen(String swatch, String id, String name, Long userId) {
        if (userId == null) {
            userId = SecurityUtils.getUserId();
        }

        if (userId != null) {
            tokenService.setLoginUser(userId);
        }

        // 获取OC 大屏用户
        List<UserVo> toUserList = userRemoteDubboService.selectUserByDeviceType(Collections.singletonList("large"));
        for (UserVo toUser : toUserList) {
            // 发送kafka消息到通用webSocket
            CommonMsgDTO commonMsgDTO = new CommonMsgDTO();
            commonMsgDTO.setType(CommonWebsoketTypeEnum.BUILD_DEVICE);
            commonMsgDTO.setUserId(toUser.getUserId());

            BuildDeviceDTO callDTO = new BuildDeviceDTO();
            callDTO.setCmd("door");
            callDTO.setSwatch(swatch);
            callDTO.setId(id);
            callDTO.setName(name);

            commonMsgDTO.setData(callDTO);

            String json = JSONUtil.toJsonStr(commonMsgDTO);
            log.info("doorDoControl 发送消息：{}", json);

            kafkaTemplate.send(KafkaTopicConstant.COMMON_MSG_TOPIC, json);
        }
    }

}
