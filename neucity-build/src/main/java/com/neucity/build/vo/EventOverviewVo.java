package com.neucity.build.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 14:19
 */
@Data
public class EventOverviewVo implements Serializable {

    private int lowNum;

    private int applyLowNum;

    private int pendingLowNum;

    private int middleNum;

    private int applyMiddleNum;

    private int pendingMiddleNum;

    private int highNum;

    private int applyHighNum;

    private int pendingHighNum;

}
