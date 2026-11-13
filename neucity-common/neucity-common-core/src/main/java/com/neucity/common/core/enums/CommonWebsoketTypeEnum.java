package com.neucity.common.core.enums;


/**
 *  公共websocket类型枚举
 *
 * @author ShuPF
 * @date 2026/1/30 15:20
 */
public enum CommonWebsoketTypeEnum {

    PING("PING", "心跳"),

    MSG_REMINDER("MSG_REMINDER", "提醒消息"),

    MSG_SYS("MSG_SYS", "系统消息"),

    MSG_ALARM("MSG_ALARM", "告警消息"),

    AUDIO_VIDEO("AUDIO_VIDEO", "音视频消息"),

    MEETING("MEETING", "会议消息"),

    MODE_CHANGE("MODE_CHANGE", "床头屏模式切换"),

    MSG_ROBOT("MSG_ROBOT", "机器人消息"),

    FROM_AND_TO("FROM_AND_TO", "多端推送"),

    GAS_PUSH("GAS_PUSH", "气体推送"),

    DRIVE_UPLOAD("DRIVE_UPLOAD", "驱动上传"),

    BUILD_DEVICE("BUILD_DEVICE", "建筑设备"),

    WAKEUP_ARBITRATION("WAKEUP_ARBITRATION", "讯飞语音唤醒"),

    ;

    private String type;

    private String desc;

    CommonWebsoketTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static CommonWebsoketTypeEnum getByType(String type) {
        for (CommonWebsoketTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
