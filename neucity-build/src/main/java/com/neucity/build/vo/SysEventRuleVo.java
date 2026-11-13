package com.neucity.build.vo;

import com.neucity.build.domain.SysEventRule;
import lombok.Data;

import java.io.Serializable;

/**
 * Event rule view.
 *
 * @author Codex
 * @date 2026/6/18
 */
@Data
public class SysEventRuleVo extends SysEventRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventSourceName;
}
