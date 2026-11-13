package com.neucity.build.task;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.domain.DriveDevice;
import com.neucity.build.domain.SysEvent;
import com.neucity.build.domain.SysEventRecord;
import com.neucity.build.domain.SysEventRule;
import com.neucity.build.mapper.DriveDeviceMapper;
import com.neucity.build.mapper.SysEventMapper;
import com.neucity.build.mapper.SysEventRecordMapper;
import com.neucity.build.mapper.SysEventRuleMapper;
import com.neucity.build.integration.datacenter.service.DataCenterOutboxService;
import com.neucity.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件定时器，根据事件规则定时触发事件
 *
 * @author ShuPF
 * @date 2026/6/18 15:28
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "build.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class EventTask {

    @Autowired
    private SysEventRuleMapper eventRuleMapper;

    @Autowired
    private SysEventRecordMapper eventRecordMapper;

    @Autowired
    private SysEventMapper sysEventMapper;

    @Autowired
    private DriveDeviceMapper driveDeviceMapper;

    @Autowired
    private DataCenterOutboxService dataCenterOutboxService;

    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void execute() {
        log.info("EventTask 定时扫描事件-开始");

        //1. 查询驱动规则列表
        List<SysEventRule> sysEventRules = eventRuleMapper.selectList(new QueryWrapper<SysEventRule>().eq("status", "enable"));
        if (CollectionUtil.isEmpty(sysEventRules)) {
            log.info("EventTask 无触发规则");
            return;
        }

        //2. 获取驱动条件触发规则，没有条件规则则返回所有同步的事件记录
        for (SysEventRule sysEventRule : sysEventRules) {
            String triggerConditions = sysEventRule.getTriggerConditions();
            this.handleEvent(sysEventRule.getEventSource(), sysEventRule.getEventLevel(), triggerConditions);
        }

        log.info("定时扫描事件-结束");
    }

    /**
     *  [{"key":"disconnect","condition":"大于","value":50}]
     *  disconnect-离线时长
     */
    private void handleEvent(String eventSource, String eventLevel, String triggerConditions) {
        log.info("EventTask-handleEvent eventSource: {}, triggerConditions: {}", eventSource, triggerConditions);
        if (StringUtils.isEmpty(triggerConditions)) {
            // 查询所有告警记录
            long time = System.currentTimeMillis() - 1000 * 60 * 5;
            String startTime = DateUtil.format(new Date(time), DatePattern.UTC_SIMPLE_PATTERN);
            List<SysEventRecord> eventRecords = eventRecordMapper.selectList(new QueryWrapper<SysEventRecord>()
                    .le("create_time", startTime)
                    .eq("drive_id", eventSource)
                    .eq("promoted", 0)
            );
            if (CollectionUtil.isNotEmpty(eventRecords)) {
                List<SysEvent> sysEvents = BeanUtil.copyToList(eventRecords, SysEvent.class);
                Date date = new Date();
                sysEvents.forEach(sysEvent -> {
                    sysEvent.setId(null);
                    sysEvent.setCreateTime(date);
                    sysEvent.setUpdateTime(date);
                    sysEvent.setLevel(eventLevel);
                    sysEvent.setHandlerBy("系统");
                });
                sysEventMapper.insertBatch(sysEvents);
                dataCenterOutboxService.enqueueAlarms(sysEvents);
                List<Long> recordIds = eventRecords.stream().map(SysEventRecord::getId)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toList());
                if (!recordIds.isEmpty()) {
                    eventRecordMapper.markPromoted(recordIds, date);
                }
            }
        }
        else {
            // 目前只实现 掉线告警
            JSONArray jsonArray = JSONUtil.parseArray(triggerConditions);
            JSONObject disconnect = null;
            for (Object item : jsonArray) {
                JSONObject condition = JSONUtil.parseObj(item);
                if ("disconnect".equals(condition.getStr("key"))) {
                    disconnect = condition;
                    break;
                }
            }
            if (disconnect == null) {
                return;
            }
            Integer value = disconnect.getInt("value");
            if (value == null || value <= 0) {
                return;
            }

            // 获取掉线超过 value 设备 生成设备离线告警
            List<DriveDevice> offLineDevices = driveDeviceMapper.selectOfflineDevices(eventSource, value);
            if (CollectionUtil.isEmpty(offLineDevices)) {
                return;
            }

            List<SysEvent> events = new ArrayList<>();
            offLineDevices.forEach(device -> {
                SysEvent event = new SysEvent();
                event.setDriveId(device.getDriveId());
                event.setDeviceId(device.getId());
                event.setEventName("设备掉线");
                event.setEventTime(DateUtil.format(new Date(), DatePattern.UTC_SIMPLE_PATTERN));
                event.setLevel(eventLevel);
                event.setHandlerState("apply");
                event.setHandlerBy("系统");
                event.setManufacturer(device.getManufacturer());
                event.setClassify(device.getClassify());
                event.setEventId("device.disconnect.event");
                events.add(event);
            });

            sysEventMapper.insertBatch(events);
            dataCenterOutboxService.enqueueAlarms(events);
        }
    }

}
