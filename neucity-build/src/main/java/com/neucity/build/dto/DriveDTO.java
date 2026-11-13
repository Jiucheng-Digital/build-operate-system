package com.neucity.build.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/3 16:20
 */
@Data
public class DriveDTO implements Serializable {

    private Long id;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    /**
     * 异步上传时保存的临时文件路径，避免Tomcat请求临时文件被清理。
     */
    private String tempFilePath;

    /**
     * 异步上传时保留原始文件名，用于校验和生成驱动目录。
     */
    private String originalFilename;

    @NotNull(message = "分类不能为空")
    private String classifyCode;

    @NotNull(message = "供应商不能为空")
    private String type;

    private String version;

    private String remark;

    @NotNull(message = "名称不能为空")
    private String name;

    /**
     * 异步上传时传递当前操作用户，避免异步线程丢失请求上下文。
     */
    private Long userId;
}
