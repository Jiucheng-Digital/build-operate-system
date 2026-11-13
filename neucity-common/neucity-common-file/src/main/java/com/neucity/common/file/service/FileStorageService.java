package com.neucity.common.file.service;

import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.dto.UploadDTO;
import com.neucity.common.file.service.model.vo.UploadVo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *  文件存储
 *
 * @author ShuPF
 * @date 2025/9/23 14:17
 */
public interface FileStorageService {

    /**
     * 上传文件
     */
    UploadVo upload(UploadDTO dto) throws IOException;

    /**
     * 下载文件
     * @param fileKey 文件标识（路径或key）
     * @return 输出流
     */
    InputStream download(FileTypeEnum fileTypeEnum, String fileKey) throws IOException;

    /**
     * 下载文件
     * @param fileKey 文件标识（路径或key）
     * @return 输出流
     */
    File downloadFile(FileTypeEnum fileTypeEnum, String fileKey) throws IOException;

    void downloadZipStream(List<String> paths, OutputStream outputStream) throws IOException;

    /**
     * 使用默认有效期获取临时下载地址。
     *
     * @param fileTypeEnum 文件类型
     * @param fileKey 文件标识（路径或 key）
     * @return 临时下载地址
     */
    default String getTemporaryDownloadUrl(FileTypeEnum fileTypeEnum, String fileKey) throws IOException {
        return getTemporaryDownloadUrl(fileTypeEnum, fileKey, null);
    }

    /**
     * 获取临时下载地址。
     *
     * @param fileTypeEnum 文件类型
     * @param fileKey 文件标识（路径或 key）
     * @param expirySeconds 有效期，单位秒；为空时使用存储实现的默认值
     * @return 临时下载地址
     */
    default String getTemporaryDownloadUrl(FileTypeEnum fileTypeEnum, String fileKey, Integer expirySeconds)
            throws IOException {
        throw new UnsupportedOperationException("当前文件存储类型不支持临时下载地址");
    }

    /**
     * 删除文件
     * @param fileKey 文件标识
     * @return 是否删除成功
     */
    boolean delete(FileTypeEnum fileTypeEnum, String fileKey);

    /**
     * 删除文件
     * @param path 文件路径
     * @return 是否删除成功
     */
    boolean delete(String path);

}
