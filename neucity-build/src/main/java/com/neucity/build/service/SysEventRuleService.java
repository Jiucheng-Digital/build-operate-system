package com.neucity.build.service;

import com.neucity.build.dto.EventRuleCreateDTO;
import com.neucity.build.dto.EventRulePageDTO;
import com.neucity.build.dto.StatusEditDTO;
import com.neucity.build.vo.SysEventRuleVo;

import java.util.List;

/**
 * Event rule service.
 *
 * @author Codex
 * @date 2026/6/18
 */
public interface SysEventRuleService {

    List<SysEventRuleVo> page(EventRulePageDTO dto);

    SysEventRuleVo getInfo(Long id);

    int create(EventRuleCreateDTO dto);

    int statusEdit(StatusEditDTO dto);
}
