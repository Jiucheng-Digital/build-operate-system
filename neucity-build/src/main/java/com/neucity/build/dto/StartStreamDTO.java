package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 15:55
 */
@Data
public class StartStreamDTO implements Serializable {

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @NotNull(message = "url不能为空")
    private String url;

    private String name;

    @NotNull(message = "门禁编码不能为空")
    private String doorIndexCode;

    @NotNull(message = "门禁驱动ID不能为空")
    private Long doorDriveId;

}
