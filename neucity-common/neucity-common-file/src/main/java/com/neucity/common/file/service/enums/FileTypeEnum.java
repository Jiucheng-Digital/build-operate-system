package com.neucity.common.file.service.enums;

/**
 *  文件类型枚举
 *
 * @author ShuPF
 * @date 2025/9/26 15:40
 */
public enum FileTypeEnum {

    /**
     *  安装包
     */
    INSTALL_PACKAGE("install_package", "installPackage"),

    /** 用户 */
    USER("user", "user"),

    /** 知识库 */
    KNOWLEDGE("knowledge", "knowledge"),

    /** 公共 */
    COMMON("common", "common"),

    /** skills */
    SKILLS("skills", "skills"),

    /** AI */
    AI("ai", "ai"),

    ;

    private String type;

    private String path;

    FileTypeEnum(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    /**
     *  根据type获取枚举
     * @param type 文件类型
     * @return 文件类型枚举
     */
    public static FileTypeEnum getByType(String type) {
        for (FileTypeEnum value : FileTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

}
