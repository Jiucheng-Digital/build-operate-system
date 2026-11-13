package com.neucity.build.mapper;


import com.neucity.build.domain.SysEventRecord;
import com.neucity.build.vo.SysEventRecordVo;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 九城ai智能体Mapper接口
 *
 * @author z
 * @date 2025-08-13
 */
@Mapper
public interface SysEventRecordMapper extends BaseMapperPlus<SysEventRecord, SysEventRecordVo> {

    int insertBatchIgnore(@Param("list") List<SysEventRecord> list);

    int markPromoted(@Param("ids") List<Long> ids, @Param("promotedTime") Date promotedTime);
}
