package com.neucity.build.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 16:57
 */
@Data
public class SysEventRecord extends BaseDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long driveId;

    private Long deviceId;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 事件时间
     */
    private String eventTime;

    /**
     * 事件等级 low-低 middle-中 high-高
     */
    private String level;

    /**
     * 扩展字段 原始json字符串（库中text存储）
     */
    private String extend;

    /**
     * 处理状态 处理状态 apply-待处理 pending-处理中 completed-已完成
     */
    private String handlerState;

    private String manufacturer;

    private String classify;

    /** 上游事件指纹，用于跨定时窗口去重。 */
    private String sourceEventKey;

    /** 是否已经转换为正式告警。 */
    private Boolean promoted;

    private Date promotedTime;

}
