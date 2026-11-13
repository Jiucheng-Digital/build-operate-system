package com.neucity.common.core.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/5/12 17:00
 */
@Data
public class InspectCheckReportVo implements Serializable {

    private String type;

    private String name;

    private String cwdm;

    private Long userId;

    private String userName;
}
