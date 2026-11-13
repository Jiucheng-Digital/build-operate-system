package com.neucity.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/9 10:39
 */
@Data
public class UserDTO implements Serializable {

    private Long deptId;

    private String username;

    private Long userId;
}
