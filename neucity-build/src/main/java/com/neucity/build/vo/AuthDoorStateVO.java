package com.neucity.build.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 17:46
 */
@Data
public class AuthDoorStateVO implements Serializable {

    /**
     * 门禁点indexCode。
     */
    private String doorIndexCode;

    /**
     * 门状态；1-开门状态，2-关门状态，3-离线状态，4-常闭，5-反锁，6-常开，7-常开，8-常闭。
     */
    private Integer doorState;

    private String manufacturer;

}
