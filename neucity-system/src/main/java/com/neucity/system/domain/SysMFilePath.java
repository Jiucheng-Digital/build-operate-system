/*
 * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
 */

package com.neucity.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *  文件实体
 * @Auther: shupf
 * @Date: 2025/09/26
 */
@TableName("sys_m_file_path")
@Data
public class SysMFilePath implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件编号
     */
    private String fileNum;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     *  文件大小 byte
     */
    private Long fileSize;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件分类
     */
    private String fileClassify;

    /**
     * 文件路径
     */
    private String filePath;


    /**
     * 压缩解压密码
     */
    private String compressPassword;

    /**
     * 描述
     */
    private String remark;

    /**
     *  扩展字段
     */
    private String expand;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 版本号
     */
    private Long version;

}
