package com.neucity.common.core.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/28 16:08
 */
@Data
public class AddFileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *  文件名称
     */
    private String name;

    /**
     *  文件类型
     */
    @NotBlank(message = "文件类型不能为空")
    private String type;

    /**
     *  文件分类
     *   type = install_package
     *      unity, web, android
     */
    private String fileClassify;

    private String remark;

    /**
     *  文件
     */
    private MultipartFile file;

}
