package com.neucity.common.core.model.dto.websocket;

import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/1/30 16:24
 */
@Data
public class CommonMsgVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *  消息类型（大类）
     */
    private CommonWebsoketTypeEnum type;


    /**
     *  具体的消息对象
     */
    private Object data;

}
