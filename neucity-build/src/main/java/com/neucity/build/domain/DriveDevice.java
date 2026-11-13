package com.neucity.build.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 14:00
 */
@Data
public class DriveDevice extends BaseDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 驱动ID
     */
    @TableField("drive_id")
    private Long driveId;

    /**
     * 厂商
     */
    @TableField("manufacturer")
    private String manufacturer;

    /**
     * 类型
     */
    @TableField("classify")
    private String classify;

    /**
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * 位置
     */
    @TableField("address")
    private String address;

    /**
     * IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 序列号
     */
    @TableField("serial_number")
    private String serialNumber;

    /**
     * 状态 online-在线 offline-离线
     */
    @TableField("status")
    private String status;

    /**
     *  第三方状态
     */
    private String state;

    /**
     * 离线时间
     */
    @TableField("offline_time")
    private Date offlineTime;

    /**
     * 扩展字段 (存放整个驱动设备对象数据)
     */
    @TableField("extend")
    private String extend;

    /**
     *  扩展字段1
     */
    private String attribute1;

    private String attribute2;

}
