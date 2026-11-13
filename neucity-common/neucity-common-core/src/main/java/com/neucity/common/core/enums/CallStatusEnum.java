package com.neucity.common.core.enums;

/**
 * 通话状态枚举
 */
public enum CallStatusEnum {
    CALLING(0, "呼叫中"),
    CONNECTED(1, "接通"),
    HANGUP(2, "已挂断"),
    UNANSWERED(3, "未接听");

    private final int code;
    private final String desc;

    CallStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CallStatusEnum getByCode(int code) {
        for (CallStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}