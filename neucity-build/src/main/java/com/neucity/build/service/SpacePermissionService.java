package com.neucity.build.service;

import com.neucity.build.exception.SpaceDocumentException;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.security.utils.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

/** Reuse the login session's RBAC grants; never infer ownership from create_by. */
@Component
public class SpacePermissionService {
    public String require(String projectId, String action) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            throw new SpaceDocumentException(401, "请先登录后再访问空间");
        }
        Set<String> permissions = user.getPermissions();
        String permission = "build:space:" + action;
        if (permissions != null) {
            for (String grant : permissions) {
                if (StringUtils.hasText(grant) && ("*:*:*".equals(grant)
                        || PatternMatchUtils.simpleMatch(grant, permission)
                        || PatternMatchUtils.simpleMatch(grant, permission + ":" + projectId))) {
                    return user.getUsername();
                }
            }
        }
        throw new SpaceDocumentException(403, "没有该项目的空间" + action + "权限，请联系管理员授权");
    }
}
