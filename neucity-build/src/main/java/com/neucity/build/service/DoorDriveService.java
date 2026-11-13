package com.neucity.build.service;

import com.neucity.build.dto.DoorDoControlDTO;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 12:15
 */

public interface DoorDriveService {

    /**
     *  门禁点反控
     */
    void doorDoControl(DoorDoControlDTO request);
}
