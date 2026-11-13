package com.neucity.system.controller;

import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.domain.R;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.utils.JwtUtils;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.security.auth.AuthUtil;
import com.neucity.common.security.service.TokenService;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.system.form.LoginBody;
import com.neucity.system.form.RegisterBody;
import com.neucity.system.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/** 建筑操作系统的账号登录、令牌刷新和退出接口。 */
@RestController
public class TokenController {
    @Autowired
    private TokenService tokenService;
    @Autowired
    private SysLoginService sysLoginService;

    @PostMapping("login")
    @Log(title = "用户登录", businessType = BusinessType.OTHER)
    public R<?> login(@RequestBody LoginBody form) {
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
        userInfo.setMacAddress(form.getMacAddress());
        userInfo.setLoginDevice(form.getDeviceTypeCode());
        userInfo.getSysUser().setDeviceTypeCode(form.getDeviceTypeCode());
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    @Log(title = "用户退出", businessType = BusinessType.OTHER)
    public R<?> logout(HttpServletRequest request) {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            String username = JwtUtils.getUserName(token);
            AuthUtil.logoutByToken(token);
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser != null) {
            tokenService.refreshToken(loginUser);
        }
        return R.ok();
    }

    @PostMapping("register")
    @Log(title = "用户注册", businessType = BusinessType.OTHER)
    public R<?> register(@RequestBody RegisterBody form) {
        sysLoginService.register(form.getUsername(), form.getPassword());
        return R.ok();
    }
}
