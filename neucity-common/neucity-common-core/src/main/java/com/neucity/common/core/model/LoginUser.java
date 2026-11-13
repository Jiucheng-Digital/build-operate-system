package com.neucity.common.core.model;

import com.neucity.common.core.model.dto.SysUserDTO;

import java.io.Serializable;
import java.util.Set;

/**
 * 用户信息
 *
 * @author shupf
 */
public class LoginUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 用户名id
     */
    private Long userid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 角色列表
     */
    private Set<String> roles;

    /**
     * 用户信息
     */
    private SysUserDTO sysUser;

    /**
     * 登录地点
     */
    private String loginLocation;

    private String staffCategName;

    public String suborDeptCode;

    private String macAddress;

    private String loginDeviceCode;

    private String loginDevice;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    private String nickName;

    public LoginUser()
    {
    }

    public LoginUser(SysUserDTO user, Set<String> permissions)
    {
        this.sysUser = user;
        this.permissions = permissions;
    }

    public LoginUser(Long userId, Long deptId, SysUserDTO user, Set<String> permissions)
    {
        this.userid = userId;
        this.userid = deptId;
        this.sysUser = user;
        this.permissions = permissions;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public Long getUserid()
    {
        return userid;
    }

    public void setUserid(Long userid)
    {
        this.userid = userid;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public Long getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Long loginTime)
    {
        this.loginTime = loginTime;
    }

    public Long getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Long expireTime)
    {
        this.expireTime = expireTime;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public Set<String> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<String> permissions)
    {
        this.permissions = permissions;
    }

    public Set<String> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<String> roles)
    {
        this.roles = roles;
    }

    public SysUserDTO getSysUser()
    {
        return sysUser;
    }

    public void setSysUser(SysUserDTO sysUser)
    {
        this.sysUser = sysUser;
    }

    public String getSuborDeptCode() {
        return suborDeptCode;
    }

    public void setSuborDeptCode(String suborDeptCode) {
        this.suborDeptCode = suborDeptCode;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getLoginDevice() {
        return loginDevice;
    }

    public void setLoginDevice(String loginDevice) {
        this.loginDevice = loginDevice;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getStaffCategName() {
        return staffCategName;
    }

    public void setStaffCategName(String staffCategName) {
        this.staffCategName = staffCategName;
    }

    public String getLoginLocation() {
        return loginLocation;
    }

    public void setLoginLocation(String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getLoginDeviceCode() {
        return loginDeviceCode;
    }

    public void setLoginDeviceCode(String loginDeviceCode) {
        this.loginDeviceCode = loginDeviceCode;
    }
}
