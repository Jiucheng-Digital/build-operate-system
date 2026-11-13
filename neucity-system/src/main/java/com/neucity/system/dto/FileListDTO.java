package com.neucity.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/10/29 16:38
 */
@Data
public class FileListDTO implements Serializable {
    private static final long serialVersionUID = -7615559101218402366L;

    private String type;

    private String fileClassify;

    /** 关键字 */
    private String keywords;

    private String startTime;

    private String endTime;

}
