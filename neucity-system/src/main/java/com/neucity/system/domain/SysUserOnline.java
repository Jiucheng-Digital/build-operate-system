package com.neucity.system.domain;

import lombok.Data;

/**
 * 当前在线会话
 * 
 * @author shupf
 */
@Data
public class SysUserOnline {
    /** 会话编号 */
    private String tokenId;

    private Long userId;
    /** 部门名称 */
    private String deptName;

    private String avatar;

    /** 用户名称 */
    private String userName;

    /** 登录IP地址 */
    private String ipaddr;

    /** 登录地址 */
    private String loginLocation;

    private String nickName;

    /* 设备编号 */
    private String deviceCode;
    /* 设备类型名称 */
    private String deviceTypeName;
    /* 设备类型编号 */
    private String deviceTypeCode;

    /** 浏览器类型 */
    private String browser;
    /** 平台类型 */
    private String macAddress;

    private String loginDevice;
    private String staffCategName;

    /** 操作系统 */
    private String os;

    /** 登录时间 */
    private Long loginTime;

    private String suborDeptCode;

}
