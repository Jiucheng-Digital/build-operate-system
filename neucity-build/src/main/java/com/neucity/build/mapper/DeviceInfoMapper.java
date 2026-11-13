package com.neucity.build.mapper;


import com.neucity.build.domain.DriveInfo;
import com.neucity.build.dto.DriveListDTO;
import com.neucity.build.vo.DriveInfoVo;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 九城ai智能体Mapper接口
 *
 * @author z
 * @date 2025-08-13
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapperPlus<DriveInfo, DriveInfoVo> {


    List<DriveInfoVo> selectDeviceInfoList(DriveListDTO dto);

    @Select("select max(port) from drive_info")
    Integer selectMaxPort();
}
