package com.neucity.build.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/7/1 9:49
 */
@Data
public class FaceCallbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /**
     *  推流id
     */
    private String stream_id;

    /**
     *  人脸信息
     */
    private List<FaceCallbackDataDTO> faces;

    /**
     *  是否识别到人脸
     */
    private String recognized;

    /**
     *  创建时间
     */
    private String created_at;

    /**
     *  抓拍时间
     */
    private String captured_at;

    @Data
    static class FaceCallbackDataDTO {

        private String subject;

        private String similarity;
    }
}