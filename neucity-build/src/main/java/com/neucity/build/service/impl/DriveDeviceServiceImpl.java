package com.neucity.build.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.dto.DevicePageDTO;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.integration.datacenter.service.DataCenterOutboxService;
import com.neucity.build.service.DriveDeviceService;
import com.neucity.build.vo.DeviceOverviewVo;
import com.neucity.build.vo.DriveDeviceVo;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.redis.service.RedisService;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.common.security.utils.WebUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 14:05
 */
@Slf4j
@Service
public class DriveDeviceServiceImpl implements DriveDeviceService {

    private static final String STREAM_KEY = "face:stream:";

    private static final int PAGE_SIZE = 1000;
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";

    /** 驱动设备mapper */
    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    /** 驱动mapper */
    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private DataCenterOutboxService dataCenterOutboxService;

    /**
     * 根据驱动ID 同步设备信息
     * 1. 查询驱动信息，获取IP:端口
     * 2. 调用 /drive/device/search 获取设备列表 根据pageNo pageSize分页参数每次获取1000条数据，获取全部数据
     * 3. 获取当前驱动数据库所有设备信息进行对比，不存在则插入，（拉取的数据不存在，本地存在的数据则标记状态离线）
     * 4. DriveDevice 驱动设备实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deviceSync(Long driveId) {
        if (driveId == null) {
            throw new ServiceException("驱动ID不能为空");
        }

        DriveInfo driveInfo = deviceInfoMapper.selectById(driveId);
        if (driveInfo == null) {
            throw new ServiceException("驱动不存在");
        }
        if (StringUtils.isBlank(driveInfo.getUrl())) {
            throw new ServiceException("驱动调用地址不能为空");
        }

        log.info("deviceSync 开始同步驱动[{}]设备信息", driveInfo.getName());

        List<DriveDevice> remoteDevices = fetchDriveDevices(driveInfo.getUrl());
        if (remoteDevices == null) {
            return;
        }

        List<DriveDevice> localDevices = driveDeviceMapper.selectList(new QueryWrapper<DriveDevice>()
                .eq("drive_id", driveId)
                .eq("manufacturer", driveInfo.getType())
                .eq("classify", driveInfo.getClassifyCode())
        );


        Map<String, DriveDevice> localDeviceMap = new HashMap<>();
        for (DriveDevice localDevice : localDevices) {
            String key = buildDeviceKey(localDevice);
            if (StringUtils.isNotBlank(key)) {
                localDeviceMap.put(key, localDevice);
            }
        }

        Date now = new Date();
        Set<String> remoteKeys = new HashSet<>();
        List<DriveDevice> insertDevices = new ArrayList<>();
        List<DriveDevice> updateDevices = new ArrayList<>();
        List<DriveDevice> changedDevices = new ArrayList<>();
        List<DriveDevice> offlineDevices = new ArrayList<>();

        for (DriveDevice remoteDevice : remoteDevices) {
            remoteDevice.setDriveId(driveId);
            String key = buildDeviceKey(remoteDevice);
            if (StringUtils.isBlank(key)) {
                log.warn("deviceSync 驱动[{}]设备缺少唯一标识，已跳过：{}", driveId, JSONUtil.toJsonStr(remoteDevice));
                continue;
            }
            if (!remoteKeys.add(key)) {
                log.warn("deviceSync 驱动[{}]设备唯一标识重复，已跳过重复数据：{}", driveId, key);
                continue;
            }

            DriveDevice localDevice = localDeviceMap.get(key);
            if (StringUtils.isBlank(remoteDevice.getManufacturer())) {
                remoteDevice.setManufacturer(driveInfo.getType());
            }
            if (StringUtils.isBlank(remoteDevice.getClassify())) {
                remoteDevice.setClassify(driveInfo.getClassifyCode());
            }
            normalizeOnlineDevice(remoteDevice);
            if (STATUS_OFFLINE.equals(remoteDevice.getStatus()) && remoteDevice.getOfflineTime() == null) {
                remoteDevice.setOfflineTime(localDevice != null && STATUS_OFFLINE.equals(localDevice.getStatus())
                        ? localDevice.getOfflineTime() : now);
            }
            if (localDevice == null) {
                remoteDevice.setDriveId(driveId);
                remoteDevice.setManufacturer(driveInfo.getType());
                remoteDevice.setClassify(driveInfo.getClassifyCode());

                prepareInsertParams(remoteDevice, now);
                insertDevices.add(remoteDevice);
            } else {
                boolean changed = hasSynchronizedChange(remoteDevice, localDevice);
                copyDeviceInfo(remoteDevice, localDevice);
                WebUserUtil.prepareUpdateParams(localDevice);
                updateDevices.add(localDevice);
                if (changed) {
                    changedDevices.add(localDevice);
                }
            }
        }

        for (DriveDevice localDevice : localDevices) {
            String key = buildDeviceKey(localDevice);
            if (StringUtils.isBlank(key) || remoteKeys.contains(key)) {
                continue;
            }
            if (STATUS_OFFLINE.equals(localDevice.getStatus())) {
                continue;
            }
            localDevice.setStatus(STATUS_OFFLINE);
            localDevice.setOfflineTime(now);
            WebUserUtil.prepareUpdateParams(localDevice);
            updateDevices.add(localDevice);
            offlineDevices.add(localDevice);
        }

        if (!insertDevices.isEmpty()) {
            driveDeviceMapper.insertBatch(insertDevices, PAGE_SIZE);
        }
        if (!updateDevices.isEmpty()) {
            driveDeviceMapper.updateBatchById(updateDevices, PAGE_SIZE);
        }
        dataCenterOutboxService.enqueueDeviceUpserts(insertDevices, now);
        dataCenterOutboxService.enqueueDeviceUpserts(changedDevices, now);
        dataCenterOutboxService.enqueueDeviceStatusChanges(offlineDevices, now);

        log.info("deviceSync 驱动[{}]同步完成，远端[{}]条，新增[{}]条，更新[{}]条",
                driveId, remoteDevices.size(), insertDevices.size(), updateDevices.size());
    }

    private List<DriveDevice> fetchDriveDevices(String driveUrl) {
        String requestUrl = StringUtils.removeEnd(driveUrl, "/") + DriverAPI.DEVICE_SEARCH_URL;
        log.info("deviceSync 调用驱动设备查询接口：{}", requestUrl);

        List<DriveDevice> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("pageNo", pageNo);
            paramMap.put("pageSize", PAGE_SIZE);

            log.info("deviceSync 调用驱动设备查询接口：{}，pageNo={}，pageSize={}", requestUrl, pageNo, PAGE_SIZE);
            String response = HttpUtils.sendPost(requestUrl, JSONUtil.toJsonStr(paramMap), MediaType.APPLICATION_JSON_VALUE);
            if (StringUtils.isBlank(response)) {
                return null;
            }
            JSONObject responseObj = JSONUtil.parseObj(response);
            checkDriveResponse(responseObj);

            List<DriveDevice> records = JSONUtil.toList(responseObj.getJSONArray("data"), DriveDevice.class);
            if (records == null || records.isEmpty()) {
                break;
            }

            for (Object record : records) {
                if (record == null) {
                    continue;
                }
                JSONObject recordObj = JSONUtil.parseObj(record);
                DriveDevice driveDevice = parseDriveDevice(recordObj);
                driveDevice.setExtend(JSONUtil.toJsonStr(recordObj));
                result.add(driveDevice);
            }

            if (records.size() < PAGE_SIZE) {
                break;
            }

            pageNo++;
        }
        return result;
    }

    private void checkDriveResponse(JSONObject responseObj) {
        Object successValue = responseObj.get("success");
        if (successValue != null && !Boolean.parseBoolean(String.valueOf(successValue))) {
            throw new ServiceException(responseObj.getStr("message", "驱动设备查询失败"));
        }
        Object codeValue = responseObj.get("code");
        if (successValue == null && codeValue != null
                && !"200".equals(String.valueOf(codeValue))
                && !"0".equals(String.valueOf(codeValue))) {
            throw new ServiceException(responseObj.getStr("msg", responseObj.getStr("message", "驱动设备查询失败")));
        }
    }
    private DriveDevice parseDriveDevice(JSONObject recordObj) {
        DriveDevice driveDevice = JSONUtil.toBean(recordObj, DriveDevice.class);
        driveDevice.setManufacturer(defaultString(driveDevice.getManufacturer(), recordObj, "manufacturer", "manufacturerName", "厂商"));
        driveDevice.setClassify(defaultString(driveDevice.getClassify(), recordObj, "classify", "classifyName", "type", "类型"));
        driveDevice.setName(defaultString(driveDevice.getName(), recordObj, "name", "deviceName", "名称"));
        driveDevice.setAddress(defaultString(driveDevice.getAddress(), recordObj, "address", "position", "location", "位置"));
        driveDevice.setIp(defaultString(driveDevice.getIp(), recordObj, "ip", "host", "deviceIp", "IP"));
        driveDevice.setPort(defaultInteger(driveDevice.getPort(), recordObj, "port", "devicePort", "端口"));
        driveDevice.setSerialNumber(defaultString(driveDevice.getSerialNumber(), recordObj, "serialNumber", "serial_number", "sn", "deviceSn", "序列号"));
        driveDevice.setStatus(defaultString(driveDevice.getStatus(), recordObj, "status", "状态"));
        driveDevice.setAttribute1(defaultString(driveDevice.getAttribute1(), recordObj, "attribute1", "扩展字段1"));
        return driveDevice;
    }

    private String defaultString(String currentValue, JSONObject recordObj, String... keys) {
        if (StringUtils.isNotBlank(currentValue)) {
            return currentValue;
        }
        for (String key : keys) {
            String value = recordObj.getStr(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return currentValue;
    }

    private Integer defaultInteger(Integer currentValue, JSONObject recordObj, String... keys) {
        if (currentValue != null) {
            return currentValue;
        }
        for (String key : keys) {
            Integer value = recordObj.getInt(key);
            if (value != null) {
                return value;
            }
        }
        return currentValue;
    }

    private void normalizeOnlineDevice(DriveDevice device) {
        if (StringUtils.isBlank(device.getStatus())) {
            device.setStatus(STATUS_ONLINE);
        }
        if (STATUS_ONLINE.equals(device.getStatus())) {
            device.setOfflineTime(null);
        }
    }

    private void copyDeviceInfo(DriveDevice source, DriveDevice target) {
        target.setManufacturer(source.getManufacturer());
        target.setClassify(source.getClassify());
        target.setName(source.getName());
        target.setAddress(source.getAddress());
        target.setIp(source.getIp());
        target.setPort(source.getPort());
        target.setSerialNumber(source.getSerialNumber());
        target.setStatus(source.getStatus());
        target.setOfflineTime(source.getOfflineTime());
        target.setExtend(source.getExtend());
        target.setState(source.getState());
        target.setAttribute1(source.getAttribute1());
        target.setAttribute2(source.getAttribute2());
    }

    private boolean hasSynchronizedChange(DriveDevice source, DriveDevice target) {
        return !Objects.equals(source.getManufacturer(), target.getManufacturer())
                || !Objects.equals(source.getClassify(), target.getClassify())
                || !Objects.equals(source.getName(), target.getName())
                || !Objects.equals(source.getAddress(), target.getAddress())
                || !Objects.equals(source.getIp(), target.getIp())
                || !Objects.equals(source.getPort(), target.getPort())
                || !Objects.equals(source.getSerialNumber(), target.getSerialNumber())
                || !Objects.equals(source.getStatus(), target.getStatus())
                || !Objects.equals(source.getState(), target.getState())
                || !Objects.equals(source.getExtend(), target.getExtend())
                || !Objects.equals(source.getAttribute1(), target.getAttribute1())
                || !Objects.equals(source.getAttribute2(), target.getAttribute2());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceState(DriveDevice device, String state, String status) {
        if (device == null || device.getId() == null || device.getDriveId() == null) {
            return;
        }
        boolean statusChanged = !Objects.equals(device.getStatus(), status);
        int rows;
        if ("reformer".equals(device.getManufacturer())) {
            rows = driveDeviceMapper.updateDeviceStateByAttribute(device.getDriveId(), state, status,
                    device.getAttribute1());
        } else {
            rows = driveDeviceMapper.updateDeviceState(device.getDriveId(), state, status,
                    device.getSerialNumber());
        }
        if (rows > 0 && statusChanged) {
            device.setState(state);
            device.setStatus(status);
            device.setOfflineTime(STATUS_OFFLINE.equals(status) ? new Date() : null);
            dataCenterOutboxService.enqueueDeviceStatusChanges(Collections.singletonList(device), new Date());
        }
    }

    private void prepareInsertParams(DriveDevice driveDevice, Date now) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String username = loginUser == null || StringUtils.isBlank(loginUser.getUsername())
                ? "admin" : loginUser.getUsername();
        driveDevice.setCreateBy(username);
        driveDevice.setUpdateBy(username);
        driveDevice.setCreateTime(now);
        driveDevice.setUpdateTime(now);
    }

    private String buildDeviceKey(DriveDevice device) {
        if (device == null) {
            return null;
        }
        if (StringUtils.isNotBlank(device.getSerialNumber())) {
            return "serialNumber:" + StringUtils.trim(device.getSerialNumber());
        }
        if (StringUtils.isNotBlank(device.getAttribute1())) {
            return "attribute1:" + StringUtils.trim(device.getAttribute1());
        }
        if (StringUtils.isNotBlank(device.getAttribute2())) {
            return "attribute2:" + StringUtils.trim(device.getAttribute2());
        }
        if (StringUtils.isNotBlank(device.getIp()) && device.getPort() != null) {
            return "network:" + StringUtils.trim(device.getIp()) + ':' + device.getPort();
        }
        return StringUtils.isBlank(device.getName()) ? null : "name:" + StringUtils.trim(device.getName());
    }

    @Override
    public List<DriveDeviceVo> page(DevicePageDTO dto) {
        List<DriveDeviceVo> list = driveDeviceMapper.selectDriveDevice(dto);
        list.forEach(device -> {
            String key = STREAM_KEY + device.getId();
            device.setStreamSwitch(redisService.hasKey(key));
        });

        return list;
    }

    @Override
    public DeviceOverviewVo overview(DevicePageDTO dto) {
        return driveDeviceMapper.selectDeviceOverview(dto);
    }
}
