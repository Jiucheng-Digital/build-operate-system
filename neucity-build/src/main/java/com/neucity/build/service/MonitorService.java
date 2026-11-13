package com.neucity.build.service;

import com.neucity.build.dto.VideoCamerasPreviewURLsDTO;
import com.neucity.build.dto.VideoPtzsControllingDTO;
import com.neucity.build.vo.VideoCamerasPreviewURLsVO;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/15 9:51
 */

public interface MonitorService {
    /**
     * 获取预览地址
     */
    VideoCamerasPreviewURLsVO previewURLs(VideoCamerasPreviewURLsDTO dto);

    /**
     * 云台控制
     */
    void ptzsControl(VideoPtzsControllingDTO dto);

    VideoCamerasPreviewURLsVO previewURLsByName(String name);
}
