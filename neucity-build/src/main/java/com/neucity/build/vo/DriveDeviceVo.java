package com.neucity.build.vo;

import com.neucity.build.domain.DriveDevice;
import lombok.Data;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 14:02
 */
@Data
public class DriveDeviceVo extends DriveDevice {

    private String url;

    /**
     *  流开关
     */
    private boolean streamSwitch;
}
