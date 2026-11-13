package com.neucity.common.core.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/5 16:51
 */
@Data
public class UserBaseVo implements Serializable {

    private Long userId;

    private String userName;

    private String nickName;

}
