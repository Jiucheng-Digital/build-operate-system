package com.neucity.build.service;

import com.neucity.build.dto.LightingControlDTO;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 15:06
 */

public interface LightingService {

    /**
     *  控制所有开关
     */
    Object controlAllSensor(LightingControlDTO dto);

    /**
     *  控制单个开关
     */
    Object controlSensor(LightingControlDTO dto);
}
