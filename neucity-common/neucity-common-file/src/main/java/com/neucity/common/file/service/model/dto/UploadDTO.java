package com.neucity.common.file.service.model.dto;

import com.neucity.common.file.service.enums.FileTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/23 15:32
 */
@Builder
@Data
public class UploadDTO implements Serializable {

    private static final long serialVersionUID = 8102887129850620172L;

    /**
     *  文件输入流
     */
    private InputStream inputStream;

    /**
     *  文件名
     */
    private String fileName;

    /**
     *  文件类型枚举
     */
    private FileTypeEnum fileTypeEnum;

    /**
     *  文件类型
     */
    private String contentType;

    /**
     *  是否需要重命名
     */
    private boolean isRename = true;
}
