package com.neucity.common.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * MinIO 客户端配置。
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(name = "file.storage.type", havingValue = "minio")
public class MinioConfiguration {

    /**
     * MinIO 文件操作客户端。
     *
     * <p>使用 {@code file.storage.minio.endpoint} 连接 MinIO，负责上传、下载、
     * 删除、检查 bucket 等服务端内部操作。容器部署时通常配置为内部地址，
     * 例如 {@code http://minio:9000}。</p>
     */
    @Bean("minioFileClient")
    @ConditionalOnMissingBean(name = "minioFileClient")
    public MinioClient minioFileClient(MinioProperties properties) {
        validate(properties);
        return buildClient(properties.getEndpoint(), properties);
    }

    /**
     * MinIO 预签名地址客户端。
     *
     * <p>使用 {@code file.storage.minio.presigned-endpoint} 生成浏览器或外部客户端
     * 可访问的临时下载地址，例如 {@code https://file.example.com}。预签名 URL
     * 会包含客户端配置的主机地址，因此不能在生成后直接替换域名。</p>
     *
     * <p>未配置外部地址，或者外部地址与内部 endpoint 相同时，直接复用
     * {@code minioFileClient}，避免创建功能相同的客户端。</p>
     */
    @Bean("minioPresignedClient")
    @ConditionalOnMissingBean(name = "minioPresignedClient")
    public MinioClient minioPresignedClient(
            MinioProperties properties,
            @Qualifier("minioFileClient") MinioClient minioFileClient) {
        if (!StringUtils.hasText(properties.getPresignedEndpoint())
                || properties.getEndpoint().equals(properties.getPresignedEndpoint())) {
            return minioFileClient;
        }
        return buildClient(properties.getPresignedEndpoint(), properties);
    }

    private MinioClient buildClient(String endpoint, MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    private void validate(MinioProperties properties) {
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalStateException("file.storage.minio.endpoint 不能为空");
        }
        if (!StringUtils.hasText(properties.getAccessKey())) {
            throw new IllegalStateException("file.storage.minio.access-key 不能为空");
        }
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("file.storage.minio.secret-key 不能为空");
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("file.storage.minio.bucket 不能为空");
        }
    }
}
