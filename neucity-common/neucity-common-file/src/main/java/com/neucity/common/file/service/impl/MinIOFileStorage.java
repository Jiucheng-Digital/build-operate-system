package com.neucity.common.file.service.impl;

import com.neucity.common.file.config.MinioProperties;
import com.neucity.common.file.service.FileStorageService;
import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.dto.UploadDTO;
import com.neucity.common.file.service.model.vo.UploadVo;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * MinIO 文件存储实现。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "minio")
public class MinIOFileStorage implements FileStorageService {

    private static final long UNKNOWN_OBJECT_SIZE = -1L;
    private static final long MULTIPART_PART_SIZE = 10L * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final int MIN_PRESIGNED_EXPIRY_SECONDS = 1;
    private static final int MAX_PRESIGNED_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    /**
     * MinIO 文件操作客户端。
     *
     * <p>使用内部 endpoint，负责文件上传、下载、删除、文件状态查询以及
     * bucket 检查和创建等服务端操作。</p>
     */
    private final MinioClient minioClient;

    /**
     * MinIO 预签名地址客户端。
     *
     * <p>使用外部可访问的 presigned endpoint，仅用于生成临时下载地址。
     * 当内部和外部 endpoint 相同时，该字段与 {@link #minioClient} 指向同一个客户端。</p>
     */
    private final MinioClient presignedClient;

    /**
     * MinIO 文件存储配置。
     */
    private final MinioProperties properties;

    /**
     * 防止并发上传时重复创建 bucket 的同步锁。
     */
    private final Object bucketCreationLock = new Object();

    /**
     * 创建 MinIO 文件存储服务。
     *
     * @param minioClient 文件上传、下载、删除等内部操作客户端
     * @param presignedClient 生成外部临时下载地址的客户端
     * @param properties MinIO 配置
     */
    public MinIOFileStorage(
            @Qualifier("minioFileClient") MinioClient minioClient,
            @Qualifier("minioPresignedClient") MinioClient presignedClient,
            MinioProperties properties) {
        this.minioClient = minioClient;
        this.presignedClient = presignedClient;
        this.properties = properties;
    }

    @Override
    public UploadVo upload(UploadDTO uploadDTO) throws IOException {
        validateUpload(uploadDTO);
        ensureBucketExists();

        FileTypeEnum fileTypeEnum = uploadDTO.getFileTypeEnum();

        String oldFileName = uploadDTO.getFileName();
        String storedFileName = sanitizeFileName(oldFileName);
        if (uploadDTO.isRename()) {
            storedFileName = UUID.randomUUID().toString().replace("-", "") + getFileExtension(storedFileName);
        }
        String objectName = buildObjectName(fileTypeEnum, storedFileName);
        String contentType = StringUtils.hasText(uploadDTO.getContentType())
                ? uploadDTO.getContentType()
                : "application/octet-stream";

        log.info("MinIOFileStorage.upload bucket: {}, object: {}, contentType: {}",
                properties.getBucket(), objectName, contentType);
        try (InputStream inputStream = uploadDTO.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, UNKNOWN_OBJECT_SIZE, MULTIPART_PART_SIZE)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw storageException("MinIO 文件上传失败: " + objectName, e);
        }

        String path = encodeObjectName(objectName);
        if (fileTypeEnum.getType().equals(FileTypeEnum.COMMON.getType())) {
            path = properties.getPublicUrl() + path;
        }

        return UploadVo.builder()
                .oldFileName(oldFileName)
                .fileName(storedFileName)
                .path(path)
                .build();
    }

    @Override
    public InputStream download(FileTypeEnum fileTypeEnum, String fileKey) throws IOException {
        String objectName = resolveObjectName(fileTypeEnum, fileKey);
        log.info("MinIOFileStorage.download bucket: {}, object: {}", properties.getBucket(), objectName);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw storageException("MinIO 文件下载失败: " + objectName, e);
        }
    }

    @Override
    public File downloadFile(FileTypeEnum fileTypeEnum, String fileKey) throws IOException {
        String objectName = resolveObjectName(fileTypeEnum, fileKey);
        Path tempFile = Files.createTempFile("neucity-minio-", getFileExtension(objectName));
        try (InputStream inputStream = download(null, objectName);
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(
                     tempFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            copy(inputStream, outputStream);
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw storageException("MinIO 文件写入临时文件失败: " + objectName, e);
        }
        File file = tempFile.toFile();
        file.deleteOnExit();
        return file;
    }

    @Override
    public void downloadZipStream(List<String> paths, OutputStream outputStream) throws IOException {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("待打包文件列表不能为空");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("ZIP 输出流不能为空");
        }

        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outputStream));
        zipOutputStream.setLevel(Deflater.BEST_SPEED);
        Set<String> entryNames = new HashSet<>();
        int successCount = 0;
        try {
            for (String path : paths) {
                String objectName;
                try {
                    objectName = resolveObjectName(null, path);
                    String entryName = uniqueZipEntryName(getObjectFileName(objectName), entryNames);
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    try (InputStream inputStream = new BufferedInputStream(download(null, objectName))) {
                        copy(inputStream, zipOutputStream);
                    } finally {
                        zipOutputStream.closeEntry();
                    }
                    successCount++;
                } catch (Exception e) {
                    log.warn("MinIO ZIP 打包跳过文件: {}, error: {}", path, e.getMessage());
                }
            }
            zipOutputStream.finish();
            zipOutputStream.flush();
            log.info("MinIO ZIP 打包完成，成功处理 {} 个文件", successCount);
        } catch (IOException e) {
            throw new IOException("MinIO ZIP 打包失败", e);
        }
    }

    @Override
    public String getTemporaryDownloadUrl(
            FileTypeEnum fileTypeEnum, String fileKey, Integer expirySeconds) throws IOException {
        String objectName = resolveObjectName(fileTypeEnum, fileKey);
        int expiry = expirySeconds == null
                ? properties.getPresignedUrlExpirySeconds()
                : expirySeconds;
        validatePresignedExpiry(expiry);

        try {
            statObject(objectName);
            return presignedClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .expiry(expiry, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw storageException("MinIO 临时下载地址生成失败: " + objectName, e);
        }
    }

    @Override
    public boolean delete(FileTypeEnum fileTypeEnum, String fileKey) {
        try {
            return deleteObject(resolveObjectName(fileTypeEnum, fileKey));
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: {}", fileKey, e);
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            return deleteObject(resolveObjectName(null, path));
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: {}", path, e);
            return false;
        }
    }

    private boolean deleteObject(String objectName) throws Exception {
        try {
            statObject(objectName);
        } catch (FileNotFoundException e) {
            return false;
        }
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .build());
        log.info("MinIOFileStorage.delete bucket: {}, object: {}", properties.getBucket(), objectName);
        return true;
    }

    private void ensureBucketExists() throws IOException {
        if (!properties.isAutoCreateBucket()) {
            return;
        }
        try {
            if (bucketExists()) {
                return;
            }
            synchronized (bucketCreationLock) {
                if (!bucketExists()) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build());
                    log.info("MinIO bucket 创建成功: {}", properties.getBucket());
                }
            }
        } catch (Exception e) {
            throw storageException("MinIO bucket 检查或创建失败: " + properties.getBucket(), e);
        }
    }

    private boolean bucketExists() throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucket())
                .build());
    }

    private void statObject(String objectName) throws Exception {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                throw new FileNotFoundException("MinIO 文件不存在: " + objectName);
            }
            throw e;
        }
    }

    private void validateUpload(UploadDTO uploadDTO) {
        if (uploadDTO == null || uploadDTO.getInputStream() == null) {
            throw new IllegalArgumentException("上传参数或文件流不能为空");
        }
        if (!StringUtils.hasText(uploadDTO.getFileName())) {
            throw new IllegalArgumentException("文件名不能为空");
        }
    }

    private void validatePresignedExpiry(int expirySeconds) {
        if (expirySeconds < MIN_PRESIGNED_EXPIRY_SECONDS
                || expirySeconds > MAX_PRESIGNED_EXPIRY_SECONDS) {
            throw new IllegalArgumentException("临时下载地址有效期必须在 1 到 604800 秒之间");
        }
    }

    private String buildObjectName(FileTypeEnum fileTypeEnum, String fileName) {
        if (fileTypeEnum == null || !StringUtils.hasText(fileTypeEnum.getPath())) {
            return normalizeObjectName(fileName);
        }
        return normalizeObjectName(fileTypeEnum.getPath() + "/" + fileName);
    }

    private String resolveObjectName(FileTypeEnum fileTypeEnum, String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            throw new IllegalArgumentException("文件标识不能为空");
        }

        String objectName = fileKey.trim();

        if (fileTypeEnum != null && StringUtils.hasText(fileTypeEnum.getPath())) {
            String typePath = normalizeObjectName(fileTypeEnum.getPath());
            if (!objectName.equals(typePath) && !objectName.startsWith(typePath + "/")) {
                String nestedTypePath = "/" + typePath + "/";
                int nestedIndex = objectName.indexOf(nestedTypePath);
                objectName = nestedIndex >= 0
                        ? objectName.substring(nestedIndex + 1)
                        : typePath + "/" + objectName;
            }
        }
        return normalizeObjectName(objectName);
    }

    private String extractPath(String fileKey) {
        try {
            URI uri = URI.create(fileKey);
            if (uri.isAbsolute() && uri.getPath() != null) {
                return uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
            // 普通对象 key 可能包含 URI 不接受的字符，按原值处理。
        }
        int queryIndex = fileKey.indexOf('?');
        return queryIndex >= 0 ? fileKey.substring(0, queryIndex) : fileKey;
    }

    private String normalizeObjectName(String objectName) {
        String normalized = stripLeadingSlash(objectName.replace('\\', '/'));
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("MinIO 对象 key 不能为空");
        }
        return normalized;
    }

    private String stripLeadingSlash(String value) {
        String result = value == null ? "" : value.trim().replace('\\', '/');
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName.trim().replace('\\', '/');
        String result = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (!StringUtils.hasText(result) || ".".equals(result) || "..".equals(result)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return result;
    }

    private String encodeObjectName(String objectName) throws IOException {
        String[] segments = objectName.split("/");
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(encodePathSegment(segment));
        }
        return result.toString();
    }

    private String encodePathSegment(String segment) throws IOException {
        return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
    }

    private String getObjectFileName(String objectName) {
        int slashIndex = objectName.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? objectName.substring(slashIndex + 1) : objectName;
        return StringUtils.hasText(fileName) ? fileName : "file";
    }

    private String uniqueZipEntryName(String fileName, Set<String> entryNames) {
        if (entryNames.add(fileName)) {
            return fileName;
        }
        String extension = getFileExtension(fileName);
        String baseName = extension.isEmpty()
                ? fileName
                : fileName.substring(0, fileName.length() - extension.length());
        int index = 1;
        String candidate;
        do {
            candidate = baseName + "(" + index++ + ")" + extension;
        } while (!entryNames.add(candidate));
        return candidate;
    }

    private String getFileExtension(String fileName) {
        String name = getObjectFileName(fileName);
        int lastIndex = name.lastIndexOf('.');
        return lastIndex <= 0 ? "" : name.substring(lastIndex);
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    private IOException storageException(String message, Exception cause) {
        if (cause instanceof FileNotFoundException) {
            return (FileNotFoundException) cause;
        }
        if (cause instanceof ErrorResponseException && isNotFound((ErrorResponseException) cause)) {
            return new FileNotFoundException(message);
        }
        log.error(message, cause);
        return new IOException(message, cause);
    }

    private boolean isNotFound(ErrorResponseException exception) {
        if (exception.errorResponse() == null) {
            return false;
        }
        String code = exception.errorResponse().code();
        return "NoSuchKey".equals(code)
                || "NoSuchObject".equals(code)
                || "NoSuchBucket".equals(code);
    }
}
