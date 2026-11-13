package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 15:02
 */
@Data
public class LightingControlDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
//    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    private String name;

    /**
     * 控制值 off-关闭，on-打开
     */
    @NotNull(message = "控制值不能为空")
    private String vvv;

    private Long userId;
}
