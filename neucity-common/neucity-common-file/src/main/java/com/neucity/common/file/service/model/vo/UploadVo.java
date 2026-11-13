package com.neucity.common.file.service.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/23 15:34
 */
@Builder
@Data
public class UploadVo implements Serializable {

    private static final long serialVersionUID = 4690227543068100564L;

    private String oldFileName;

    private String fileName;

    private String path;

}
