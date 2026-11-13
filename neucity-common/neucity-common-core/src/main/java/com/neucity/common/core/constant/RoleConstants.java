package com.neucity.common.core.constant;

import java.util.Set;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/19 9:43
 */
public class RoleConstants {

    /**
     *  主机角色ID
     */
    public static final Long HOST_ROLE_ID = 105L;

    /**
     *  床头屏角色ID
     */
    public static final Long CTP_ROLE_ID = 101L;

    /**
     *  医生角色ID
     */
    public static final Long DOCTOR_ROLE_ID = 102L;

    /**
     *  护士角色ID
     */
    public static final Long NURSE_ROLE_ID = 103L;

    /**
     *  机器人角色ID
     */
    public static final Long ROBOT_ROLE_ID = 104L;

    /**
     *  中夜班用户角色
     */
    public static final Long ZYB_ROLE_ID = 108L;

    /**
     *  展厅角色code
     */
    public static final String SHOWROOM_ROLE_CODE = "showroom";

    /**
     *  管理员角色code
     */
    public static final String ADMIN_ROLE_CODE = "admin";

    /**
     *  护士角色code
     */
    public static final String NURSE_ROLE_CODE = "nurse";

    /**
     *  老师角色code
     */
    public static final String TEACHER_ROLE_CODE = "teacher";

    /**
     *  业主角色code
     */
    public static final String OWNER_ROLE_CODE = "owner";


    /**
     *  判断是否是展厅角色
     */
    public static boolean isShowroom(Set<String> roleCodes) {
        return roleCodes.contains(SHOWROOM_ROLE_CODE);
    }
}
