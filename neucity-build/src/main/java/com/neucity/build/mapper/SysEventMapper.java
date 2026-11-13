package com.neucity.build.mapper;


import com.neucity.build.domain.SysEvent;
import com.neucity.build.dto.EventPageDTO;
import com.neucity.build.vo.EventOverviewVo;
import com.neucity.build.vo.SysEventRecordVo;
import com.neucity.build.vo.SysEventVo;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 九城ai智能体Mapper接口
 *
 * @author z
 * @date 2025-08-13
 */
@Mapper
public interface SysEventMapper extends BaseMapperPlus<SysEvent, SysEventVo> {


    List<SysEventVo> selectEvent(EventPageDTO dto);

    EventOverviewVo selectEventOverview();
}
