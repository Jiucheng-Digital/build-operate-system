package com.neucity.build.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.domain.SysEventRule;
import com.neucity.build.dto.EventRuleCreateDTO;
import com.neucity.build.dto.EventRulePageDTO;
import com.neucity.build.dto.StatusEditDTO;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.mapper.SysEventRuleMapper;
import com.neucity.build.service.SysEventRuleService;
import com.neucity.build.vo.SysEventRuleVo;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Event rule service impl.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Service
public class SysEventRuleServiceImpl implements SysEventRuleService {

    private static final String DEFAULT_CONDITION_RELATION = "and";
    private static final String DEFAULT_ACTION_TYPE = "dept";
    private static final String DEFAULT_STATUS = "enable";
    private static final String DEFAULT_USERNAME = "admin";

    @Autowired
    private SysEventRuleMapper eventRuleMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Override
    public List<SysEventRuleVo> page(EventRulePageDTO dto) {
        return eventRuleMapper.selectEventRule(dto);
    }

    @Override
    public SysEventRuleVo getInfo(Long id) {
        return eventRuleMapper.selectEventRuleById(id);
    }

    @Override
    public int create(EventRuleCreateDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getEventSource());
        if (driveInfo == null) {
            throw new ServiceException("事件来源驱动不存在");
        }

        long count = eventRuleMapper.selectCount(new QueryWrapper<SysEventRule>()
                .eq("event_source", driveInfo.getId())
                .eq("event_level", dto.getEventLevel())
        );
        if (count > 0) {
            throw new ServiceException("事件来源驱动已存在");
        }

        SysEventRule eventRule = new SysEventRule();
        eventRule.setRuleName(dto.getRuleName());
        eventRule.setRuleDesc(dto.getRuleDesc());
        eventRule.setEventLevel(dto.getEventLevel());
        eventRule.setEventSource(dto.getEventSource());
        eventRule.setConditionRelation(defaultIfBlank(dto.getConditionRelation(), DEFAULT_CONDITION_RELATION));
        eventRule.setTriggerConditions(toJsonText(dto.getTriggerConditions(), "triggerConditions"));
        eventRule.setActionType(defaultIfBlank(dto.getActionType(), DEFAULT_ACTION_TYPE));
        eventRule.setActionTargets(toJsonText(dto.getActionTargets(), "actionTargets"));
        eventRule.setStatus(defaultIfBlank(dto.getStatus(), DEFAULT_STATUS));
        eventRule.setRemark(dto.getRemark());
        prepareInsertParams(eventRule);
        return eventRuleMapper.insert(eventRule);
    }

    @Override
    public int statusEdit(StatusEditDTO dto) {
        SysEventRule eventRule = new SysEventRule();
        eventRule.setId(dto.getId());
        eventRule.setStatus(dto.getStatus());

        return eventRuleMapper.updateById(eventRule);
    }

    private String toJsonText(Object value, String fieldName) {
        if (value == null) {
            throw new ServiceException(fieldName + " cannot be null");
        }
        String jsonText;
        if (value instanceof String) {
            jsonText = StringUtils.trim((String) value);
        } else {
            jsonText = JSONUtil.toJsonStr(value);
        }
        if (StringUtils.isBlank(jsonText)) {
            throw new ServiceException(fieldName + " cannot be empty");
        }
        return jsonText;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private void prepareInsertParams(SysEventRule eventRule) {
        Date now = new Date();
        String username = getUsername();
        eventRule.setCreateBy(username);
        eventRule.setUpdateBy(username);
        eventRule.setCreateTime(now);
        eventRule.setUpdateTime(now);
    }

    private String getUsername() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || StringUtils.isBlank(loginUser.getUsername())) {
            return DEFAULT_USERNAME;
        }
        return loginUser.getUsername();
    }
}
