package com.neucity.common.core.domain.feign.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.neucity.common.core.domain.feign.vo.FeignUserVo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/25 15:09
 */
@Data
public class NeucitySysMessageDTO implements Serializable {

    /** 系统消息类型-系统公告 */
    public static final String SYS_MESSAGE_TYPE_OTA = "sys_message_ota";

    /** 系统消息类型-系统通知 */
    public static final String SYS_MESSAGE_TYPE_NOTICE = "sys_message_notice";

    /** 推送方式 promptly-立即，timing-定时 */
    public static final String PUSH_WAY_PROMPLY = "promptly";

    public static final String PUSH_WAY_TIMING = "timing";

    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 消息类型 */
    private String type;

    /** 内容 */
    private String content;

    /** 推送设备 */
    private String pushDevice;

    /** 推送总数 */
    private Integer totalNum;

    /** 推送方式 promptly-立即，timing-定时 */
    private String pushWay;

    /** 到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态 success-成功，push-已推送 wait-等待中，cancel-已取消， fail-失败 */
    private String status;

    private String extend;

    /** 模板id */
    private Long templateId;

    private String otaTitle;

    private String otaVersion;

    private String otaRemark;

    private Long sendUserId;

    private List<FeignUserVo> userVos;
}