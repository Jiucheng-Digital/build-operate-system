package com.neucity.system.service.impl;

import com.neucity.common.core.model.dto.SysUserDTO;
import org.springframework.stereotype.Service;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.core.model.LoginUser;
import com.neucity.system.domain.SysUserOnline;
import com.neucity.system.service.ISysUserOnlineService;

/**
 * 在线用户 服务层处理
 * 
 * @author shupf
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService
{
    /**
     * 通过登录地址查询信息
     * 
     * @param ipaddr 登录地址
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     * 
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user)
    {
        if (StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     * 
     * @param ipaddr 登录地址
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     * 
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user)
    {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getSysUser()))
        {
            return null;
        }
        SysUserDTO userDTO = user.getSysUser();

        SysUserOnline sysUserOnline = new SysUserOnline();
        sysUserOnline.setTokenId(user.getToken());
        sysUserOnline.setUserName(user.getUsername());
        sysUserOnline.setIpaddr(user.getIpaddr());
        sysUserOnline.setUserId(user.getUserid());
        sysUserOnline.setLoginLocation(user.getLoginLocation());
        sysUserOnline.setLoginDevice(user.getLoginDevice());
        sysUserOnline.setSuborDeptCode(user.getSuborDeptCode());
        sysUserOnline.setNickName(userDTO.getNickName());
        sysUserOnline.setStaffCategName(user.getStaffCategName());
        sysUserOnline.setMacAddress(user.getMacAddress());
        sysUserOnline.setBrowser(user.getBrowser());
        sysUserOnline.setOs(user.getOs());
        sysUserOnline.setLoginTime(user.getLoginTime());
        sysUserOnline.setDeviceCode(userDTO.getDeviceCode());
        sysUserOnline.setDeviceTypeCode(userDTO.getDeviceTypeCode());
        sysUserOnline.setDeviceTypeName(userDTO.getDeviceTypeName());
        sysUserOnline.setAvatar(userDTO.getAvatar());
        if (StringUtils.isNotNull(userDTO.getDept())) {
            sysUserOnline.setDeptName(userDTO.getDept().getDeptName());
        }
        if (userDTO.getLoginType() != null && userDTO.getLoginType() == 3) {
            // 设备登录
            sysUserOnline.setMacAddress(userDTO.getUserName());
        }

        return sysUserOnline;
    }
}
