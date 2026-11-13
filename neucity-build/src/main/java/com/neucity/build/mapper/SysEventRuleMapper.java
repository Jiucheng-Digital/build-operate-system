package com.neucity.build.mapper;

import com.neucity.build.domain.SysEventRule;
import com.neucity.build.dto.EventRulePageDTO;
import com.neucity.build.vo.SysEventRuleVo;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Event rule mapper.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Mapper
public interface SysEventRuleMapper extends BaseMapperPlus<SysEventRule, SysEventRuleVo> {

    List<SysEventRuleVo> selectEventRule(EventRulePageDTO dto);

    SysEventRuleVo selectEventRuleById(Long id);
}
