package com.neucity.build.exception;

/** HTTP status and AjaxResult.code must agree for editor requests. */
public class SpaceDocumentException extends RuntimeException {
    private final int status;

    public SpaceDocumentException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static SpaceDocumentException invalid(String message) {
        return new SpaceDocumentException(400, message);
    }

    public static SpaceDocumentException conflict() {
        return new SpaceDocumentException(409, "空间草稿已变更或空间 ID 不一致，请重新加载并处理差异后再操作");
    }
}
