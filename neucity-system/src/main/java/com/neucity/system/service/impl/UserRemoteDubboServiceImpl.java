package com.neucity.system.service.impl;
import com.neucity.api.system.UserRemoteDubboService;
import com.neucity.api.system.vo.UserVo;
import com.neucity.system.dto.UserByDeviceTypeDTO;
import com.neucity.system.mapper.SysUserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/** 为建筑服务提供大屏等设备类型对应的用户列表。 */
@DubboService
public class UserRemoteDubboServiceImpl implements UserRemoteDubboService {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<UserVo> selectUserByDeviceType(List<String> deviceTypes) {
        UserByDeviceTypeDTO dto = new UserByDeviceTypeDTO();
        dto.setDeviceTypes(deviceTypes);
        return sysUserMapper.selectByDeviceType(dto);
    }
}
