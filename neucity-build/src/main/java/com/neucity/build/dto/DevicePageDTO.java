package com.neucity.build.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 16:21
 */
@Data
public class DevicePageDTO implements Serializable {

    private List<String> name;

    private Long driveId;

    private String manufacturer;

    private String classify;

}
