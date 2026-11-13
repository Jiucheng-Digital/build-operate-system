package com.neucity.common.core.enums;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/3/21 16:28
 */
public enum AiAgentEnum {

    EXHIBITION_NURSE("exhibition-nurse", "展厅护士智能体"),

    EXHIBITION_PATIENT("exhibition-patient", "展厅患者智能体"),

    EXHIBITION_TEACHER("exhibition-teacher", "展厅教师智能体"),

    EXHIBITION_RESIDENT("exhibition-resident", "展厅住户智能体"),

    NURSE("nurse", "护士智能体"),

    PATIENT("patient", "患者智能体"),

    TEACHER("teacher", "教师智能体"),

    RESIDENT("resident", "住户智能体"),

    WEBSITE("website", "九城数字官网智能体"),

    ROBOT("robot", "机器人智能体"),

    ;

    private String code;
    private String name;

    AiAgentEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static AiAgentEnum getByCode(String code) {
        for (AiAgentEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
