package com.neucity.build.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/10 16:50
 */
@Data
public class DeviceOverviewVo implements Serializable {

    /** 设备总数 */
    private String totalNum;

    /** 在线设备数 */
    private String onlineNum;

    /** 离线设备数 */
    private String offlineNum;

    /** 掉线时长 */
    private String offTime;

}
