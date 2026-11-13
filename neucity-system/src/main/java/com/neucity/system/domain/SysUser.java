package com.neucity.system.domain;

import java.util.Date;
import java.util.List;
import javax.validation.constraints.*;

import com.baomidou.mybatisplus.annotation.TableField;
import com.neucity.common.core.model.dto.SysUserDTO;
import lombok.Data;
import com.neucity.common.core.annotation.Excel;
import com.neucity.common.core.annotation.Excel.ColumnType;
import com.neucity.common.core.annotation.Excel.Type;
import com.neucity.common.core.constant.UserConstants;
import com.neucity.common.core.annotation.Excels;
import com.neucity.common.core.web.domain.BaseEntity;

/**
 * 用户对象 sys_user
 * 
 * @author shupf
 */
@Data
public class SysUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Excel(name = "用户序号", type = Type.EXPORT, cellType = ColumnType.NUMERIC, prompt = "用户编号")
    private Long userId;

    /** 部门ID */
    @Excel(name = "部门编号", type = Type.IMPORT)
    private Long deptId;

    /** 用户账号 */
    @Excel(name = "登录名称")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户名称")
    private String nickName;

    /** 用户拼音 */
    private String pinYin;

    private String code;

    /** 用户邮箱 */
    @Excel(name = "用户邮箱")
    private String email;

    /** 手机号码 */
    @Excel(name = "手机号码", cellType = ColumnType.TEXT)
    private String phonenumber;

    /** 用户性别 */
    @Excel(name = "用户性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;

    /** 用户头像 */
    private String avatar;

    /** 密码 */
    private String password;

    /** 账号状态（0正常 1停用） */
    @Excel(name = "账号状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    private String agentName;

    /** 最后登录IP */
    @Excel(name = "最后登录IP", type = Type.EXPORT)
    private String loginIp;

    /** 最后登录时间 */
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date loginDate;

    private String loginDevice;

    @TableField(exist = false)
    /** 密码最后更新时间 */
    private Date pwdUpdateDate;

    @TableField(exist = false)
    /** 部门对象 */
    @Excels({
        @Excel(name = "部门名称", targetAttr = "deptName", type = Type.EXPORT),
        @Excel(name = "部门负责人", targetAttr = "leader", type = Type.EXPORT)
    })
    private SysDept dept;

    @TableField(exist = false)
    /** 角色对象 */
    private List<SysRole> roles;

    @TableField(exist = false)
    /** 角色组 */
    private Long[] roleIds;

    @TableField(exist = false)
    /** 岗位组 */
    private Long[] postIds;

    @TableField(exist = false)
    /** 角色ID */
    private Long roleId;

    private String faceImg;
    //人脸编码
    private String faceCode;

    private Integer loginType;

    private String suborDeptCode;

    private String suborDeptName;

    private String staffCode;

    private String staffCategName;

    /* 设备编号 */
    private String deviceCode;
    /* 设备类型名称 */
    private String deviceTypeName;
    /* 设备类型编号 */
    private String deviceTypeCode;

    private String bedNumber;

    /**
     *  机器人前屏推流地址
     */
    private String robotPrePush;

    /**
     * 机器人后屏推流地址
     */
    private String robotAfterPush;

    private String robotPreUserId;

    private String roleCode;

    /** 房间号 */
    private String roomNo;

    public SysUser()
    {

    }

    public SysUser(Long userId)
    {
        this.userId = userId;
    }

    public static boolean isAdmin(SysUserDTO sysUserDTO)
    {
        return UserConstants.isAdmin(sysUserDTO);
    }
}
