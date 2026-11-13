package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/19 9:59
 */
@Data
public class ModeChangeDTO implements Serializable {

    /**
     * 夜间模式 开-no, 关-off
     */
    private String nightMode;

    /**
     *  补光模式 开-no, 关-off
     */
    private String fillLightMode;

}
