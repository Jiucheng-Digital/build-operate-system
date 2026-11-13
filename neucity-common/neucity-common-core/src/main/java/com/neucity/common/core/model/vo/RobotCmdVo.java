package com.neucity.common.core.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/10 15:00
 */
@Data
public class RobotCmdVo implements Serializable {

    /**
     *  机器人指令类型
     */
    private String cmd;

    private boolean success;

    /**
     *  机器人指令结果
     */
    private Object result;

}
