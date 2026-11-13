package com.neucity.build.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.api.system.UserRemoteDubboService;
import com.neucity.api.system.vo.UserVo;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.dto.DoorDoControlDTO;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.service.DoorDriveService;
import com.neucity.build.vo.DriveResultVO;
import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.dto.websocket.AudioVideoCallDTO;
import com.neucity.common.core.model.dto.websocket.BuildDeviceDTO;
import com.neucity.common.core.model.dto.websocket.CommonMsgDTO;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.redis.service.RedisService;
import com.neucity.common.security.service.TokenService;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.build.constants.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 12:15
 */
@Slf4j
@Service
public class DoorDriveServiceImpl implements DoorDriveService {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    @Autowired
    private TokenService tokenService;

    @DubboReference(url = "${system.dubbo.url:}")
    private UserRemoteDubboService userRemoteDubboService;

    @Override
    public void doorDoControl(DoorDoControlDTO request) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(request.getDriveId());
        if (driveInfo == null) {
            log.info("DoorDriveServiceImpl.doorDoControl 未找到对应驱动数据");
            throw new ServiceException("未找到对应驱动数据");
        }

        if (CollectionUtil.isEmpty(request.getDoorIndexCodes())) {
            log.info("DoorDriveServiceImpl.doorDoControl doorIndexCodes 不能为空");
            throw new ServiceException("门禁点索引不能为空");
        }

        String url = driveInfo.getUrl() + DriverAPI.DoorApi.OPEN_URL;
        Map<String, Object> params = BeanUtil.beanToMap(request);
        params.remove("driveId");

        String body = JSONUtil.toJsonStr(params);
        log.info("doorDoControl 请求参数：url: [{}], params: {}",url, body);
        String result = HttpUtils.sendPost(url, body, MediaType.APPLICATION_JSON_VALUE);
        log.info("doorDoControl 响应结果：{}", result);

        DriveResultVO driveResultVO = null;
        try {
            driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);
        } catch (Exception e){
            throw new ServiceException("操作失败");
        }

        if (!driveResultVO.isSuccess()) {
            throw new ServiceException(driveResultVO.getMessage());
        }


        CompletableFuture.runAsync(() -> {
            String swatch = "off";
            Integer controlType = request.getControlType();
            if (controlType == 0 || controlType == 2) {
                swatch = "on";
            }

            List<String> doorIndexCodes = request.getDoorIndexCodes();
            List<DriveDevice> serialNumber = driveDeviceMapper.selectList(new QueryWrapper<DriveDevice>().in("serial_number", doorIndexCodes));
            for (DriveDevice driveDevice : serialNumber) {
                this.sendOcLargeScreen(swatch, driveDevice.getId() + "", driveDevice.getName(), request.getUserId());
            }
        });
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

            // 测试先写死
            commonMsgDTO.setUserId(8992L);

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
