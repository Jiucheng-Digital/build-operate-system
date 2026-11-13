package com.neucity.build.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/12 9:36
 */
@Data
public class EventPageDTO implements Serializable {

    private String level;

    private String handleState;

}
