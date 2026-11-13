package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class VideoPtzsControllingDTO {
    /**
     * 监控点唯一标识，分页获取监控点资源接口获取返回参数cameraIndexCode
     */
    @NotNull(message = "监控点唯一标识不能为空")
    private String cameraIndexCode;
    /**
     *  0-开始
     *  1-停止
     *  注：GOTO_PRESET命令下填任意值均可转到预置点,建议填0即可
     */
    @NotNull(message = "操作不能为空")
    private Integer action;
    /**
     *  LEFT 左转
     * RIGHT右转
     * UP 上转
     * DOWN 下转
     * ZOOM_IN 焦距变大
     * ZOOM_OUT 焦距变小
     * LEFT_UP 左上
     * LEFT_DOWN 左下
     * RIGHT_UP 右上
     * RIGHT_DOWN 右下 FOCUS_NEAR 焦点前移
     * FOCUS_FAR 焦点后移 IRIS_ENLARGE 光圈扩大
     * IRIS_REDUCE 光圈缩小
     * WIPER_SWITCH 接通雨刷开关
     * START_RECORD_TRACK开始记录路线
     * STOP_RECORD_TRACK 停止记录路线
     * START_TRACK 开始路线
     * STOP_TRACK 停止路线
     *
     * 以下命令presetIndex不可
     * 为空：
     * GOTO_PRESET到预置点
     */
    @NotNull(message = "命令不能为空")
    private String command;
    /**
     *  云台速度，取值范围为1-100，默认50
     */
    private Integer speed;
    /**
     *  预置点编号，整数，通常在300以内
     */
    private Integer presetIndex;

    /**
     *  驱动ID
     */
    @NotNull(message = "驱动ID不能为空")
    private Long driveId;
}
