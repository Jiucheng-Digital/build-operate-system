package com.neucity.build.params;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 17:34
 */
@Data
public class DeviceParam implements Serializable {

    private String classify;

    private String manufacturer;

}
