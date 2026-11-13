package com.neucity.common.core.domain.feign.dto;

import com.neucity.common.core.domain.feign.vo.FeignUserVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/17 15:44
 */
@Data
public class NeucityAlarmMessageDTO implements Serializable {

    /** 告警类型 */
    private String alarmType;

    /** 告警规则ID */
    private Long ruleId;

    /** 模版ID */
    private Long templateId;

    /** 内容 */
    private String content;

    /** 覆盖数量 */
    private Long totalNum;

    private Long sendUserId;

    private List<FeignUserVo> userVos;

}
