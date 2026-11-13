package com.neucity.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/17 14:31
 */
@Data
public class UpdateProfileDTO implements Serializable {

    private String phonenumber;

    private String nickName;

    private String agentName;

    private String avatar;

    private String email;

    private String bedNumber;

    private Long roleId;

    private Long deptId;

    /** 房间号 */
    private String roomNo;
}
