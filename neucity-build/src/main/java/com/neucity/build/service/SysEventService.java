package com.neucity.build.service;

import com.neucity.build.dto.EventCreateDTO;
import com.neucity.build.dto.EventPageDTO;
import com.neucity.build.vo.EventOverviewVo;
import com.neucity.build.vo.SysEventVo;

import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 9:31
 */

public interface SysEventService {

    /** 主动创建正式事件，返回事件主键。 */
    Long create(EventCreateDTO dto);

    List<SysEventVo> page(EventPageDTO dto);

    EventOverviewVo overview();
}
