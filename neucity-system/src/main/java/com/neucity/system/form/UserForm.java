package com.neucity.system.form;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/9 10:39
 */
@Data
public class UserForm implements Serializable {

    private Long deptId;

    private List<Long> userIds;

    private String username;

    private Long notUserId;

    private Long notRoleId;

    private String notLoginDevice;
}
