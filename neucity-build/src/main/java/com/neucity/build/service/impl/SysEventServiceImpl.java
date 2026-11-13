package com.neucity.build.service.impl;

import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.domain.SysEvent;
import com.neucity.build.dto.EventCreateDTO;
import com.neucity.build.dto.EventPageDTO;
import com.neucity.build.integration.datacenter.service.DataCenterOutboxService;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.mapper.SysEventMapper;
import com.neucity.build.service.SysEventService;
import com.neucity.build.vo.EventOverviewVo;
import com.neucity.build.vo.SysEventVo;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 9:32
 */
@Slf4j
@Service
public class SysEventServiceImpl implements SysEventService {

    private static final ZoneId EVENT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    @Autowired
    private SysEventMapper eventMapper;

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DataCenterOutboxService dataCenterOutboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EventCreateDTO dto) {
        Date now = new Date();
        SysEvent event = new SysEvent();
        event.setDriveId(dto.getDriveId());
        event.setDeviceId(dto.getDeviceId());
        event.setEventId(dto.getEventId().trim());
        event.setEventName(dto.getEventName().trim());
        event.setEventTime(normalizeEventTime(dto.getEventTime(), now));
        event.setLevel(dto.getLevel());
        event.setExtend(dto.getExtend());
        event.setManufacturer(dto.getManufacturer());
        event.setClassify(dto.getClassify());
        fillEventSource(event);
        event.setHandlerState("apply");
        event.setHandlerBy("系统");

        LoginUser loginUser = SecurityUtils.getLoginUser();
        String username = loginUser == null || StringUtils.isBlank(loginUser.getUsername())
                ? "系统" : loginUser.getUsername();
        event.setCreateBy(username);
        event.setUpdateBy(username);
        event.setCreateTime(now);
        event.setUpdateTime(now);

        if (eventMapper.insert(event) != 1) {
            throw new ServiceException("事件创建失败");
        }
        dataCenterOutboxService.enqueueAlarms(Collections.singletonList(event));
        return event.getId();
    }

    private void fillEventSource(SysEvent event) {
        if (event.getDeviceId() != null) {
            DriveDevice device = driveDeviceMapper.selectById(event.getDeviceId());
            if (device == null) {
                throw new ServiceException("事件关联设备不存在");
            }
            if (event.getDriveId() != null && !event.getDriveId().equals(device.getDriveId())) {
                throw new ServiceException("事件关联设备不属于指定驱动");
            }
            event.setDriveId(device.getDriveId());
            event.setManufacturer(StringUtils.defaultIfBlank(event.getManufacturer(), device.getManufacturer()));
            event.setClassify(StringUtils.defaultIfBlank(event.getClassify(), device.getClassify()));
        } else if (event.getDriveId() != null) {
            DriveInfo drive = deviceInfoMapper.selectById(event.getDriveId());
            if (drive == null) {
                throw new ServiceException("事件来源驱动不存在");
            }
            event.setManufacturer(StringUtils.defaultIfBlank(event.getManufacturer(), drive.getType()));
            event.setClassify(StringUtils.defaultIfBlank(event.getClassify(), drive.getClassifyCode()));
        }
    }

    private String normalizeEventTime(String value, Date now) {
        if (StringUtils.isBlank(value)) {
            return now.toInstant().atZone(EVENT_ZONE).format(EVENT_TIME_FORMAT);
        }
        String time = value.trim();
        try {
            return OffsetDateTime.parse(time).atZoneSameInstant(EVENT_ZONE).format(EVENT_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            // 继续尝试不带时区的日期时间，按北京时间处理。
        }
        try {
            return LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(EVENT_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            // 继续尝试数据库日期时间格式。
        }
        try {
            return LocalDateTime.parse(time, EVENT_TIME_FORMAT).format(EVENT_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new ServiceException("事件时间格式错误，请使用yyyy-MM-dd HH:mm:ss或ISO 8601日期时间");
        }
    }

    @Override
    public List<SysEventVo> page(EventPageDTO dto) {
        return eventMapper.selectEvent(dto);
    }

    @Override
    public EventOverviewVo overview() {
        return eventMapper.selectEventOverview();
    }
}
