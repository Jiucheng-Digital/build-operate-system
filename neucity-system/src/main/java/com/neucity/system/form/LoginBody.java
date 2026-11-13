package com.neucity.system.form;

import lombok.Data;

/**
 * 用户登录对象
 * 
 * @author shupf
 */
@Data
public class LoginBody {
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    private String macAddress;

    private String deviceTypeCode;
}
