package com.neucity.system.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.model.dto.SysRoleDTO;
import com.neucity.common.core.model.dto.SysUserDTO;
import com.neucity.common.core.utils.Chinese2PinyinUtil;
import com.neucity.common.core.utils.DateUtils;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.core.utils.file.FileTypeUtils;
import com.neucity.common.core.utils.file.MimeTypeUtils;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.file.service.FileStorageService;
import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.dto.UploadDTO;
import com.neucity.common.file.service.model.vo.UploadVo;
import com.neucity.common.security.service.TokenService;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.system.domain.SysUser;
import com.neucity.system.dto.UpdateProfileDTO;
import com.neucity.system.mapper.SysUserRoleMapper;
import com.neucity.system.service.ISysPermissionService;
import com.neucity.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人信息 业务处理
 * 
 * @author shupf
 */
@RestController
@RequestMapping("/user/profile")
public class SysProfileController extends BaseController {

    @Autowired
    private ISysUserService userService;
    
    @Autowired
    private TokenService tokenService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private ISysPermissionService permissionService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile() {
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 设置用户登录设备信息
        user.setDeviceCode(loginUser.getLoginDeviceCode());
        user.setDeviceTypeName(loginUser.getSysUser().getDeviceTypeName());
        user.setDeviceTypeCode(loginUser.getSysUser().getDeviceTypeCode());
        user.setRobotAfterPush(loginUser.getSysUser().getRobotAfterPush());
        user.setRobotPrePush(loginUser.getSysUser().getRobotPrePush());
        user.setRobotPreUserId(loginUser.getSysUser().getRobotPreUserId());

        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(username));
        ajax.put("postGroup", userService.selectUserPostGroup(username));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody UpdateProfileDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUserDTO userDTO = loginUser.getSysUser();

        SysUser currentUser = cn.hutool.core.convert.Convert.convert(SysUser.class, userDTO);
        currentUser.setNickName(dto.getNickName());
        if (StringUtils.isNotEmpty(dto.getNickName())) {
            currentUser.setPinYin(Chinese2PinyinUtil.toFullPinyinLowerNoTone(dto.getNickName()));
        }
        currentUser.setEmail(dto.getEmail());
        currentUser.setPhonenumber(dto.getPhonenumber());
        currentUser.setAgentName(dto.getAgentName());
        currentUser.setAvatar(dto.getAvatar());
        currentUser.setBedNumber(dto.getBedNumber());
        currentUser.setRoomNo(dto.getRoomNo());

        if (StringUtils.isNotEmpty(dto.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(dto.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }

        List<Long> roleIds = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(userDTO.getRoles())) {
            roleIds = userDTO.getRoles().stream().map(SysRoleDTO::getRoleId).collect(Collectors.toList());
        }

        Long roleId = dto.getRoleId();
        if (roleId != null && (StringUtils.isEmpty(roleIds) || !roleIds.contains(roleId))) {
            // 更新角色
            sysUserRoleMapper.deleteUserRoleByUserId(userDTO.getUserId());
            sysUserRoleMapper.insetUserRole(userDTO.getUserId(), roleId);
            userDTO.setRoleId(roleId);
            // 角色集合
            Set<String> roles = permissionService.getRolePermission(currentUser);
            // 权限集合
            Set<String> permissions = permissionService.getMenuPermission(currentUser);
            loginUser.setRoles(roles);
            loginUser.setPermissions(permissions);
        }

        currentUser.setDeptId(dto.getDeptId());
        if (userService.updateUserProfile(currentUser)) {
            currentUser = userService.selectUserByUserName(userDTO.getUserName());
            userDTO = cn.hutool.core.convert.Convert.convert(SysUserDTO.class, currentUser);
            loginUser.setSysUser(userDTO);

            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }

        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserid();
        String password = loginUser.getSysUser().getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getSysUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getSysUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }
    
    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) {
        if (!file.isEmpty())
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            String extension = FileTypeUtils.getExtension(file);
            if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION))
            {
                return error("文件格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式");
            }

            String url = "";
            try {
                UploadDTO uploadDTO =  UploadDTO.builder()
                        .inputStream(file.getInputStream())
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .fileTypeEnum(FileTypeEnum.USER)
                        .isRename(true)
                        .build();
                UploadVo fileKey = fileStorageService.upload(uploadDTO);

                url = fileKey.getPath();
            } catch (Exception e) {
                return error("上传失败: " + e.getMessage());
            }

            if (userService.updateUserAvatar(loginUser.getUserid(), url)) {
                String oldAvatarUrl = loginUser.getSysUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatarUrl)) {
                    fileStorageService.delete(FileTypeEnum.USER, oldAvatarUrl);
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", url);
                // 更新缓存用户头像
                loginUser.getSysUser().setAvatar(url);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
