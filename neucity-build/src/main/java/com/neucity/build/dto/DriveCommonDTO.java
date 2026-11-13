package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/8 17:28
 */
@Data
public class DriveCommonDTO implements Serializable {

    @NotNull(message = "driveId不能为空")
    private Long driveId;
}
