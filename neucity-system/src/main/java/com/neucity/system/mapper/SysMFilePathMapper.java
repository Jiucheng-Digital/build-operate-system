package com.neucity.system.mapper;

import com.neucity.common.mybatisplus.BaseMapperPlus;
import com.neucity.system.domain.SysMFilePath;
import com.neucity.system.domain.vo.SysMFilePathVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/28 15:34
 */
@Mapper
public interface SysMFilePathMapper extends BaseMapperPlus<SysMFilePath, SysMFilePathVo> {

    /**
     * 根据id查询文件路径
     * @param id 文件id
     * @return 文件路径
     */
    @Select("select file_path from sys_m_file_path where id = #{id}")
    String selectFilePathById(Long id);

}
