package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 *  描述
 * @author ShuPF
 * @date 2026/1/30 16:18
 * 
 */
@Data
public class ReminderMsgDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *  提醒消息主键ID
     */
    private Long id;

    /**
     *  未读数量
     */
    private String num;

    /**
     *  当前消息内容
     */
    private String content;

}
