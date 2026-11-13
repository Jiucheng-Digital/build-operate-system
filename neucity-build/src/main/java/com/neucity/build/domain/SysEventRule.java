package com.neucity.build.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * Event rule.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Data
public class SysEventRule extends BaseDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Rule name.
     */
    private String ruleName;

    /**
     * Rule description.
     */
    private String ruleDesc;

    /**
     * Event level, for example low/middle/high.
     */
    private String eventLevel;

    /**
     * Event source code.
     */
    private String eventSource;

    /**
     * Condition relation, for example and/or.
     */
    private String conditionRelation;

    /**
     * Trigger conditions JSON.
     */
    private String triggerConditions;

    /**
     * Action type, for example dept/app.
     */
    private String actionType;

    /**
     * Action targets JSON.
     */
    private String actionTargets;

    /**
     * Rule status, enable/disable.
     */
    private String status;

    private String remark;
}
