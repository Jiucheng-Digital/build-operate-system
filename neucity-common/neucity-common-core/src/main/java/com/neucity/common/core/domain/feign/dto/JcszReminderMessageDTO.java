package com.neucity.common.core.domain.feign.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/1/19 14:37
 */
@Data
public class JcszReminderMessageDTO implements Serializable {

    /**
     * 常量定义 - 消息类型
     */
    public static final String TYPE_DELAY_MSG = "delay_msg"; // 延时消息

    public static final String TYPE_SYS_MSG = "sys_msg"; // 推送消息

    /**
     * 常量定义 - 消息状态
     */
    public static final String STATUS_RECEIVED = "received"; // 已接收
    public static final String STATUS_PUSHED = "pushed";     // 已推送

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息ID
     */
    private String msgId;

    /**
     * 消息类型：delay_msg 延时消息
     */
    private String type;

    private String title;

    /**
     * 发送用户ID
     */
    private Long sendUserId;

    /**
     * 发送用户名称
     */
    private String sendUserName;

    /**
     * 接收用户ID
     */
    private Long toUserId;

    /**
     * 接收用户名称
     */
    private String toUserName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态：received-已接收，pushed-已推送
     */
    private String status;

    /**
     * 阅读状态
     */
    private boolean readStatus;

    /**
     * 扩展字段
     */
    private String extend;

    /**
     * 到期时间
     */
    private Date endTime;

}
