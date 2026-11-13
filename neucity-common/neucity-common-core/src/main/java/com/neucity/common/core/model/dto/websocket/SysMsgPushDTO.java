package com.neucity.common.core.model.dto.websocket;

import com.neucity.common.core.model.vo.UserBaseVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/5 16:49
 */
@Data
public class SysMsgPushDTO implements Serializable {

    /**
     *  提醒消息主键ID
     */
    private Long id;

    private String title;

    /**
     *  推送用户
     */
    private List<UserBaseVo> users;

}
