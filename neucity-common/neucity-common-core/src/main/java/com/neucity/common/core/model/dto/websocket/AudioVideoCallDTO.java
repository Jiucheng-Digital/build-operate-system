package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/4 11:34
 */
@Data
public class AudioVideoCallDTO {

    /**
     *  消息类型 call-呼叫, aiCall-AI呼叫
     */
    private String type = "call";

    /**
     *  呼叫类型 1: 语音 2: 视频
     */
    private Integer callType;

    /**
     *  发送用户ID
     */
    private Long sendId;

    /**
     *  发送用户设备类型
     */
    private String sendDeviceTypeCode;

    /**
     *  接收用户ID
     */
    private Long receiveId;

    /**
     *  接收用户设备类型
     */
    private String receiveDeviceTypeCode;

}
