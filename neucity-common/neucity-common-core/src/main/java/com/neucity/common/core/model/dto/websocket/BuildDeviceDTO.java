package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/7/27 14:25
 */
@Data
public class BuildDeviceDTO implements Serializable {

    /** door-门禁，lighting-灯 */
    private String cmd;

    /** 开关状态 off-关，on-开 */
    private String swatch;

    private String id;

    private String name;

}
