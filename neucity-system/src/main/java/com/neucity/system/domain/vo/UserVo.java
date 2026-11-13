package com.neucity.system.domain.vo;

import cn.hutool.core.collection.CollectionUtil;
import com.neucity.common.core.constant.RoleConstants;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.model.dto.SysRoleDTO;
import com.neucity.common.security.utils.SecurityUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/9 10:41
 */
@Data
public class UserVo {

    private Long userId;

    private String nickName;

    private String avatar;

    private String userName;

    private String deptName;

    /** 在线状态 0-离线 1-在线 */
    private Integer onlineStatus = 0;

    /** 在线设备 */
    private String onlineDevice = "";

    private String staffCategName;

    private String majorSkillPostName;

    private Long roleId;

    // all 音视频都支持，voice=音频， video=视频
    private String callScope = "all";

    public String getCallScope() {
        LoginUser loginUser =  SecurityUtils.getLoginUser();
        List<Long> roles = new ArrayList<>();
        if (loginUser != null && CollectionUtil.isNotEmpty(loginUser.getSysUser().getRoles())) {
            roles = loginUser.getSysUser().getRoles().stream().map(SysRoleDTO::getRoleId).collect(Collectors.toList());
        }
        if ((roleId != null && RoleConstants.CTP_ROLE_ID - roleId == 0) ||
                roles.contains(RoleConstants.CTP_ROLE_ID)) {
            return "voice";
        }

        return callScope;
    }
}
