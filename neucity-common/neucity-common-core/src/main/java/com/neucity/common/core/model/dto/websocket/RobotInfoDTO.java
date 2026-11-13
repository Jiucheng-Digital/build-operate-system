package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/4/17 11:45
 */
@Data
public class RobotInfoDTO implements Serializable {

    private Long robotId;

    private String robotName;

    private String robotPrePullUrl;

    private String robotAfterPullUrl;

    private String robotPreUserId;

    private String status;

    private String power;

    private Integer taskNum;

    private String map;

}
