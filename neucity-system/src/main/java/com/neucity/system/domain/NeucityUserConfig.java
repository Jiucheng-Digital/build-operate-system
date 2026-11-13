package com.neucity.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * ota管理对象 neucity_ota
 * 
 * @author shupf
 * @date 2026-01-26
 */
@TableName("neucity_user_config")
@Data
public class NeucityUserConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 系统消息通知状态 0-关闭 1-开启 */
    private Integer sysMsgStatus;

    /** 告警消息通知状态 0-关闭 1-开启 */
    private Integer alarmMsgStatus;

    /** 互动消息通知状态 0-关闭 1-开启 */
    private Integer interactMsgStatus;

    /** 延时消息通知状态 0-关闭 1-开启 */
    private Integer delayMsgStatus;

    /** 语音唤醒开关 0-关闭 1-开启 */
    private Integer voiceAwakenSwitch;
}
