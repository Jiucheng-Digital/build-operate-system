package com.neucity.build.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Event rule page request.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Data
public class EventRulePageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleName;

    private String eventLevel;

    private String eventSource;

    private String actionType;

    private String status;

    private String beginTime;

    private String endTime;
}
