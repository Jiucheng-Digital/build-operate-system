package com.neucity.common.core.domain.feign.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/28 15:17
 */
@Data
public class FeignUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String code;

    /** 部门ID */
    private Long deptId;

    /** 用户账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 用户邮箱 */
    private String email;

    /** 手机号码 */
    private String phonenumber;

    /** 用户性别 */
    private String sex;

    /** 用户头像 */
    private String avatar;

    /** 密码 */
    private String password;

    /** 账号状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 角色ID */
    private Long roleId;

    private String faceImg;
    //人脸编码
    private String faceCode;

}
