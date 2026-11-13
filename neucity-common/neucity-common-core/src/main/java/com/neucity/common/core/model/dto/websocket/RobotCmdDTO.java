package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 * 机器人指令消息
 *
 * @author ShuPF
 * @date 2026/3/10 14:29
 */
@Data
public class RobotCmdDTO implements Serializable {

    /**
     *  机器人指令类型
     */
    private String cmd;

    /**
     *  机器人指令结果
     */
    private Object result;

    /**
     *  机器人指令响应类型
     */
    private String respCmd;

    /**
     *  机器人指令响应结果
     */
    private String respResult;
}
