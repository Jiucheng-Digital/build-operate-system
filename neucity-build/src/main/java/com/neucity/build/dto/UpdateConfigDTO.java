package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/17 10:49
 */
@Data
public class UpdateConfigDTO implements Serializable {

    @NotNull(message = "driveId不能为空")
    private Long driveId;

    @NotNull(message = "configJson不能为空")
    private String configJson;

}
