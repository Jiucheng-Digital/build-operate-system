package com.neucity.system.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.hutool.core.convert.Convert;
import com.neucity.common.core.model.dto.SysUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.neucity.common.core.constant.UserConstants;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.system.domain.SysRole;
import com.neucity.system.domain.SysUser;
import com.neucity.system.service.ISysMenuService;
import com.neucity.system.service.ISysPermissionService;
import com.neucity.system.service.ISysRoleService;

/**
 * 用户权限处理
 * 
 * @author shupf
 */
@Service
public class SysPermissionServiceImpl implements ISysPermissionService
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取角色数据权限
     * 
     * @return 角色权限信息
     */
    @Override
    public Set<String> getRolePermission(SysUser user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin(Convert.convert(SysUserDTO.class, user)))
        {
            roles.add("admin");
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     * 
     * @return 菜单权限信息
     */
    @Override
    public Set<String> getMenuPermission(SysUser user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin(Convert.convert(SysUserDTO.class, user)))
        {
            perms.add("*:*:*");
        }
        else
        {
            List<SysRole> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRole role : roles)
                {
                    if (StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && !role.isAdmin())
                    {
                        Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                        role.setPermissions(rolePerms);
                        perms.addAll(rolePerms);
                    }
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
