package com.neucity.common.core.enums;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/1/19 17:29
 */

public enum CommonMsgEnum {

    REMINDER("reminder", "消息提醒")

    ;

    private String code;

    private String desc;

    private CommonMsgEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CommonMsgEnum getByCode(String code) {
        for (CommonMsgEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
