package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/29 11:48
 */
@Data
public class StatusEditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private String status;

}
