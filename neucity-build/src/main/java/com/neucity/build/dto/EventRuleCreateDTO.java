package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Event rule create request.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Data
public class EventRuleCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    private String ruleDesc;

    @NotBlank(message = "事件等级不能为空")
    private String eventLevel;

    @NotBlank(message = "事件来源不能为空")
    private String eventSource;

    private String conditionRelation;

    @NotNull(message = "触发条件不能为空")
    private Object triggerConditions;

    private String actionType;

    @NotNull(message = "执行动作配置不能为空")
    private Object actionTargets;

    private String status;

    private String remark;
}
