package com.neucity.build.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/8 9:55
 */
@Data
public class DriveInfo extends BaseDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 驱动名称
     */
    private String name;

    /**
     * 安装路径
     */
    private String installPath;

    /**
     * 驱动调用地址
     */
    private String url;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 驱动类型，厂商类型
     */
    private String type;

    /**
     * 分类ID
     */
    private String classifyCode;

    /**
     * 配置信息
     */
    private String config;

    /**
     * 状态 install_success,  uninstall-卸载
     */
    private String status;

    /**
     * 扩展字段
     */
    private String extend;

    private String version;

    private String remark;
}
