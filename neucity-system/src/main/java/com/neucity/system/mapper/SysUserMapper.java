package com.neucity.system.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neucity.api.system.vo.UserVo;
import com.neucity.common.annotation.annotation.DataScope;
import com.neucity.system.dto.UserByDeviceTypeDTO;
import com.neucity.system.form.UserForm;
import org.apache.ibatis.annotations.Param;
import com.neucity.system.domain.SysUser;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 数据层
 * 
 * @author shupf
 */
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    public int updateUserByCode(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 修改用户状态
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 结果
     */
    public int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 更新用户登录信息（IP和登录时间）
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateLoginInfo(SysUser user);

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);

    /**
     * 查询用户的facecode
     */
    List<SysUser> selectFaceCode();

    /**
     * 根据拼音查询用户
     */
    List<UserVo> selectUserVoByPinyin(String pinyin);

    List<UserVo> selectUserByDeviceType(@Param("deviceTypes") List<String> deviceTypes);

    @DataScope(deptAlias = "u")
    List<UserVo> selectByDeviceType(UserByDeviceTypeDTO dto);

    List<UserVo> selectUserBySpaceCode(@Param("spaceCodes") List<String> spaceCodes);

    List<com.neucity.system.domain.vo.UserVo> selectOrgStructure(UserForm userForm);

    List<Long> selectPreFirUserIds(Long userId);

    List<UserVo> selectUserByRoleId(Long roleId);

    @Select("select * from sys_user where bed_number is not null")
    List<UserVo> selectCtpUser();

    @Select("select * from sys_user where bed_number = #{code}")
    UserVo selectCtpUserByCode(String code);

    @Select("SELECT @@port AS 数据库端口, @@server_id AS 服务ID, @@hostname AS 主机名;")
    Map<String, Object> selectDb();

    List<String> selectUserCode(@Param("userIds") List<Long> userIds);
}
