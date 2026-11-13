package com.neucity.common.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 文件存储配置。
 */
@Data
@ConfigurationProperties(prefix = "file.storage.minio")
public class MinioProperties {

    /**
     * 服务端访问地址，例如 http://127.0.0.1:9000。
     */
    private String endpoint = "http://127.0.0.1:9000";

    /**
     * 生成预签名地址时使用的外部可访问地址。为空时使用 endpoint。
     */
    private String presignedEndpoint;

    /**
     * MinIO access key。
     */
    private String accessKey;

    /**
     * MinIO secret key。
     */
    private String secretKey;

    /**
     * 文件存储桶。
     */
    private String bucket = "neucity";

    /**
     * 文件公开访问地址，需指向 bucket 根路径，例如 http://127.0.0.1:9000/neucity。
     */
    private String publicUrl;

    /**
     * 上传时是否自动创建不存在的 bucket。
     */
    private boolean autoCreateBucket = true;

    /**
     * 临时下载地址默认有效期，单位秒。
     */
    private int presignedUrlExpirySeconds = 3600;
}
