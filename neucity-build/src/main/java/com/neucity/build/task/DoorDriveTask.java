package com.neucity.build.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.domain.SysEventRecord;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.mapper.SysEventRecordMapper;
import com.neucity.build.integration.datacenter.support.SourceEventKey;
import com.neucity.build.params.DeviceParam;
import com.neucity.build.service.DriveDeviceService;
import com.neucity.build.vo.AuthDoorStateVO;
import com.neucity.build.vo.DriveDeviceVo;
import com.neucity.build.vo.DriveResultVO;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.security.utils.WebUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 门禁驱动定时器
 *
 * @author ShuPF
 * @date 2026/6/11 17:24
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "build.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class DoorDriveTask {

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    @Autowired
    private SysEventRecordMapper sysEventRecordMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DriveDeviceService driveDeviceService;

    /**
     *  同步门禁状态
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void syncState() {
        log.info("syncState 同步门禁状态同步-开始");
        DeviceParam param = new DeviceParam();
        param.setClassify("access_control");
        List<DriveDeviceVo> driveDeviceVos = driveDeviceMapper.selectInstalledDevice(param);
        if (CollectionUtil.isEmpty(driveDeviceVos)) {
            log.info("syncState 同步门禁状态同步-结束 size is 0");
            return;
        }

        log.info("syncState 门禁设备数量：{}", driveDeviceVos.size());

        // 根据地址转换为Map
        Map<String, List<DriveDeviceVo>> map = driveDeviceVos.stream().collect(Collectors.groupingBy(DriveDeviceVo::getUrl));
        for (String url : map.keySet()) {
            List<DriveDeviceVo> driveDeviceVos1 = map.get(url);
            // 一次性最大200个
            List<List<DriveDeviceVo>> partition = Lists.partition(driveDeviceVos1, 200);
            for (List<DriveDeviceVo> driveDeviceVos2 : partition) {
                Map<String, DriveDeviceVo> devicesByCode = driveDeviceVos2.stream()
                        .filter(device -> StringUtils.isNotBlank("reformer".equals(device.getManufacturer())
                                ? device.getAttribute1() : device.getSerialNumber()))
                        .collect(Collectors.toMap(
                                device -> "reformer".equals(device.getManufacturer())
                                        ? device.getAttribute1() : device.getSerialNumber(),
                                device -> device,
                                (oldValue, newValue) -> oldValue));
                Set<String> doorIndexCodes = driveDeviceVos2.stream()
                        .map(driveDeviceVo -> "reformer".equals(driveDeviceVo.getManufacturer())
                                ? driveDeviceVo.getAttribute1()
                                : driveDeviceVo.getSerialNumber())
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());
                Map<String, Object> params = new HashMap<>(1);
                params.put("doorIndexCodes", doorIndexCodes);

                String result = HttpUtils.sendPost(url + DriverAPI.DoorApi.QUERY_STATE,
                        JSONUtil.toJsonStr(params), MediaType.APPLICATION_JSON_VALUE);
                if (StringUtils.isBlank(result)) {
                    continue;
                }
                DriveResultVO driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);
                List<AuthDoorStateVO> list = JSONUtil.toList(driveResultVO.getData().toString(), AuthDoorStateVO.class);
                for (AuthDoorStateVO authDoorStateVO : list) {
                    Integer doorState = authDoorStateVO.getDoorState();
                    String status = "online";
                    if (Integer.valueOf(3).equals(doorState)) {
                        status = "offline";
                    }
                    DriveDeviceVo device = devicesByCode.get(authDoorStateVO.getDoorIndexCode());
                    if (device != null) {
                        driveDeviceService.updateDeviceState(device, String.valueOf(doorState), status);
                    }
                }
            }
        }

        log.info("syncState 同步门禁状态同步-结束");
    }

    /**
     *  同步立方门禁事件
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void syncLfEvent() {
        log.info("syncState 同步立方门禁事件同步-开始");

        List<DriveInfo> driveDeviceVos = deviceInfoMapper.selectList(new QueryWrapper<DriveInfo>()
                .eq("classify_code", "access_control")
                .eq("type", "reformer")
        );

        if (CollectionUtil.isEmpty(driveDeviceVos)) {
            log.info("syncState 同步立方门禁事件同步-结束 size is 0");
            return;
        }

        long time = System.currentTimeMillis() - 1000 * 60 * 30;
        String startTime = DateUtil.format(new Date(time), DatePattern.NORM_DATETIME_PATTERN);

        log.info("syncState 立方门禁设备数量：{}", driveDeviceVos.size());
        // 根据地址转换为Map
        for (DriveInfo driveInfo : driveDeviceVos) {
            String url = driveInfo.getUrl() + DriverAPI.QUERY_EVENT;
            Map<String, Object> params = new HashMap<>(4);
            params.put("startTime", startTime);
            params.put("endTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
            params.put("pageNo", 1);
            params.put("pageSize", 1000);

            String result = HttpUtils.sendPost(url, JSONUtil.toJsonStr(params), MediaType.APPLICATION_JSON_VALUE);
            if (StringUtils.isBlank(result)) {
                continue;
            }
            DriveResultVO driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);

            List<SysEventRecord> list = JSONUtil.toList(driveResultVO.getData().toString(), SysEventRecord.class);
            if (CollectionUtil.isNotEmpty(list)) {
                // 获取当前驱动下的设备
                List<DriveDevice> devices = driveDeviceMapper.selectList(new QueryWrapper<DriveDevice>()
                        .eq("drive_id", driveInfo.getId())
                );

                Map<String, Long> map = devices.stream()
                        .collect(Collectors.toMap(
                                DriveDevice::getAttribute1,
                                DriveDevice::getId,
                                (oldValue, newValue) -> oldValue // 冲突时保留旧值（第一条数据的ID）
                        ));

                for (SysEventRecord sysEventRecord : list) {
                    sysEventRecord.setDriveId(driveInfo.getId());
                    sysEventRecord.setManufacturer("reformer");
                    sysEventRecord.setClassify("access_control");
                    sysEventRecord.setHandlerState("apply");
                    sysEventRecord.setDeviceId(map.get(sysEventRecord.getEventId()));
                    sysEventRecord.setSourceEventKey(SourceEventKey.of(sysEventRecord));
                    WebUserUtil.prepareInsertParams(sysEventRecord);
                }
                sysEventRecordMapper.insertBatchIgnore(list);
            }

        }

        log.info("syncState 同步立方门禁事件同步-结束");
    }

    /**
     *  同步海康门禁事件
     */
    @Scheduled(cron = "0 0/40 * * * ?")
    public void syncHkEvent() {
        log.info("syncHkEvent 同步海康门禁事件同步-开始");

        DeviceParam param = new DeviceParam();
        param.setClassify("access_control");
        param.setManufacturer("hikvision");
        List<DriveDeviceVo> driveDeviceVos = driveDeviceMapper.selectOnlineDevice(param);
        if (CollectionUtil.isEmpty(driveDeviceVos)) {
            log.info("syncHkEvent 同步海康门禁事件同步-结束 size is 0");
            return;
        }

        long time = System.currentTimeMillis() - 1000 * 60 * 40;
        String startTime = DateUtil.format(new Date(time), DatePattern.UTC_SIMPLE_PATTERN) + "+08:00";

        log.info("syncHkEvent 海康门禁设备数量：{}", driveDeviceVos.size());

        // 根据地址转换为Map
        for (DriveDeviceVo driveDeviceVo : driveDeviceVos) {
            String url = driveDeviceVo.getUrl() + DriverAPI.QUERY_EVENT;
            Map<String, Object> params = new HashMap<>(4);
            params.put("startTime", startTime);
            params.put("endTime", DateUtil.format(new Date(), DatePattern.UTC_SIMPLE_PATTERN) + "+08:00");
            params.put("pageNo", 1);
            params.put("pageSize", 1000);
            params.put("indexCodes", Collections.singletonList(driveDeviceVo.getSerialNumber()));

            String result = HttpUtils.sendPost(url, JSONUtil.toJsonStr(params), MediaType.APPLICATION_JSON_VALUE);
            if (StringUtils.isBlank(result)) {
                continue;
            }
            DriveResultVO driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);

            List<SysEventRecord> list = JSONUtil.toList(driveResultVO.getData().toString(), SysEventRecord.class);
            if (CollectionUtil.isNotEmpty(list)) {
                List<SysEventRecord> failureEvents = new ArrayList<>();
                for (SysEventRecord sysEventRecord : list) {
                    String name = StringUtils.defaultString(sysEventRecord.getEventName()).toLowerCase(Locale.ROOT);
                    if (!name.contains("fail") && !name.contains("error")) {
                        continue;
                    }

                    sysEventRecord.setDriveId(driveDeviceVo.getDriveId());
                    sysEventRecord.setManufacturer("hikvision");
                    sysEventRecord.setClassify("access_control");
                    sysEventRecord.setHandlerState("apply");
                    sysEventRecord.setDeviceId(driveDeviceVo.getId());
                    sysEventRecord.setSourceEventKey(SourceEventKey.of(sysEventRecord));
                    WebUserUtil.prepareInsertParams(sysEventRecord);
                    failureEvents.add(sysEventRecord);
                }
                if (CollectionUtil.isNotEmpty(failureEvents)) {
                    sysEventRecordMapper.insertBatchIgnore(failureEvents);
                }
            }

        }

        log.info("syncHkEvent 同步海康门禁事件同步-结束");

    }
}
