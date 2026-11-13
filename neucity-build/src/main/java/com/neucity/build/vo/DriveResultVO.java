package com.neucity.build.vo;

import lombok.Data;

@Data
public class DriveResultVO<T> {
    private boolean success;
    private String message;
    private Integer errorCode;
    private T data;
}
