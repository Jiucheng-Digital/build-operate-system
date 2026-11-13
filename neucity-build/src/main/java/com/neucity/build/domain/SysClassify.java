package com.neucity.build.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/8 10:37
 */
@Data
public class SysClassify extends BaseDomain implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 上级编码
     */
    private String parentCode;

    /**
     * 类型
     */
    private String type;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态 enable-正常，disable-停用
     */
    private String status;

    /**
     * 是否内置 0-否 1-是
     */
    private Integer builtIn;

    /**
     * 备注
     */
    private String remark;

}
