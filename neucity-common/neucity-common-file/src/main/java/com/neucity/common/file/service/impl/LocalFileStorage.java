package com.neucity.common.file.service.impl;

import com.neucity.common.file.service.FileStorageService;
import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.dto.UploadDTO;
import com.neucity.common.file.service.model.vo.UploadVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/23 14:19
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local")
public class LocalFileStorage implements FileStorageService {

    // 增大缓冲区大小（8KB，大文件推荐4KB-64KB）
    private static final int BUFFER_SIZE = 8192;

    @Value("${file.storage.local.base-path:D:\\file}")
    private String basePath;

    @Value("${file.storage.url:http://192.168.0.56/file}")
    private String baseUrl;

    @Override
    public UploadVo upload(UploadDTO uploadDTO) throws IOException {
        if (uploadDTO == null || uploadDTO.getInputStream() == null) {
            throw new IllegalArgumentException("上传参数或文件流不能为空");
        }

        String oldFileName = uploadDTO.getFileName();
        if (oldFileName == null || oldFileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 1. 构建存储路径
        FileTypeEnum fileType = uploadDTO.getFileTypeEnum();
        String path = basePath;
        if (fileType != null) {
            path = basePath + File.separator + fileType.getPath();
        }
        log.info("LocalFileStorage.upload basePath: {}", path);

        // 2. 创建存储目录（捕获目录创建异常）
        Path storagePath = Paths.get(path);
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                log.info("LocalFileStorage.upload 存储目录创建成功: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("LocalFileStorage.upload创建存储目录失败: {}", storagePath, e);
            throw new IOException("创建存储目录失败：" + storagePath, e);
        }

        // 3. 生成唯一文件名
        String uniqueFileName = uploadDTO.getFileName();
        if (uploadDTO.isRename()) {
            uniqueFileName = UUID.randomUUID().toString().replaceAll("-", "") + getFileExtension(uploadDTO.getFileName());
        }
        Path filePath = storagePath.resolve(uniqueFileName);
        log.info("LocalFileStorage.upload 文件将写入路径: {}", filePath);

        // 4. 检查文件是否已存在（可选，根据业务需求）
        if (Files.exists(filePath)) {
            log.warn("LocalFileStorage.upload 文件已存在，将覆盖: {}", filePath);
            // 也可抛出异常：throw new IOException("文件已存在：" + filePath);
        }

        InputStream inputStream = uploadDTO.getInputStream();
        // 重置流（如果支持），避免流指针已到末尾
        if (inputStream.markSupported()) {
            inputStream.reset();
        }

        long originalFileSize = 0;
        long writtenFileSize = 0;
        // 5. 写入文件（try-with-resources自动关闭流）
        try (InputStream is = inputStream;
             OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // 记录原始流大小（可选，若流支持available()）
            originalFileSize = is.available();
            log.info("LocalFileStorage.upload 待写入文件大小: {} bytes", originalFileSize);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                writtenFileSize += bytesRead;
            }
            os.flush(); // 强制刷出缓冲区数据
            log.info("LocalFileStorage.upload 文件写入完成，实际写入大小: {} bytes", writtenFileSize);

        } catch (IOException e) {
            log.error("LocalFileStorage.upload 文件写入失败: {}", filePath, e);
            // 写入失败时删除临时文件
            if (Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
            }
            throw new IOException("文件写入失败：" + filePath, e);
        }

        // 6. 校验文件是否完整写入
        if (Files.exists(filePath)) {
            long actualFileSize = Files.size(filePath);
            log.info("LocalFileStorage.upload 文件最终大小: {} bytes", actualFileSize);
            if (actualFileSize != writtenFileSize) {
                Files.deleteIfExists(filePath);
                throw new IOException("文件写入不完整，实际大小: " + actualFileSize + ", 写入大小: " + writtenFileSize);
            }
            // 若能获取原始文件大小，可进一步校验
            if (originalFileSize > 0 && actualFileSize != originalFileSize) {
                Files.deleteIfExists(filePath);
                throw new IOException("文件大小不匹配，原始大小: " + originalFileSize + ", 实际大小: " + actualFileSize);
            }
        } else {
            throw new IOException("文件写入后不存在: " + filePath);
        }

        return UploadVo.builder()
                .oldFileName(oldFileName)
                .fileName(uniqueFileName)
                .path(baseUrl + fileType.getPath() + File.separator + uniqueFileName)
                .build();
    }

    @Override
    public InputStream download(FileTypeEnum fileTypeEnum, String fileKey) throws IOException {
        log.info("LocalFileStorage.upload fileKey: {}", fileKey);

        String path = basePath;
        if (fileTypeEnum != null) {
            path = basePath + File.separator + fileTypeEnum.getPath();
        }

        Path filePath = Paths.get(path, fileKey);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + fileKey);
        }
        return Files.newInputStream(filePath);
    }

    @Override
    public File downloadFile(FileTypeEnum fileTypeEnum, String fileKey) throws IOException {
        log.info("LocalFileStorage.upload fileKey: {}", fileKey);

        String path = basePath;
        if (fileTypeEnum != null) {
            path = basePath + File.separator + fileTypeEnum.getPath();
        }

        // 正确的做法：确保路径正确拼接
        String fullPath = path.endsWith(File.separator) ? path + fileKey : path + File.separator + fileKey;
        Path filePath = Paths.get(fullPath);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + fileKey);
        }
        return new File(fullPath);
    }

    /**
     * 新增：流式下载方法
     */
    @Override
    public void downloadZipStream(List<String> paths, OutputStream outputStream) throws IOException {
        // 使用你现有的 downloadFileToZip 方法，只是输出流不同
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            // 设置更快的压缩级别，提高速度
            zipOut.setLevel(Deflater.BEST_SPEED);
            log.info("开始流式打包，文件数量: {}", paths.size());
            int successCount = 0;
            for (String fileUrl : paths) {
                try {
                    // 重用你现有的 downloadFileToZip 方法
                    downloadFileToZip(fileUrl, zipOut);
                    successCount++;

                    // 每处理10个文件刷新一次，防止内存占用过高
                    if (successCount % 10 == 0) {
                        zipOut.flush();
                        log.debug("已处理 {} 个文件", successCount);
                    }

                } catch (Exception e) {
                    log.warn("处理文件失败，跳过: {}, 错误: {}", fileUrl, e.getMessage());
                    // 跳过失败的文件，继续处理其他
                }
            }
            zipOut.finish();
            outputStream.flush();
            log.info("流式打包完成，成功处理 {} 个文件", successCount);
        } catch (Exception e) {
            log.error("流式打包失败", e);
            throw new RuntimeException("流式打包失败: " + e.getMessage());
        }
    }

    /**
     * 从URL下载文件并写入ZIP输出流
     * @param fileUrl 文件URL
     * @param zipOut ZIP输出流
     */
    private void downloadFileToZip(String fileUrl, ZipOutputStream zipOut) throws Exception {
        // 解析URL获取文件名（例如从"http://xxx/abc.jpg"中提取"abc.jpg"）
        String fileName = new File(new URL(fileUrl).getPath()).getName();
        if (fileName.isEmpty()) {
            fileName = "file_" + System.currentTimeMillis(); // 处理无文件名的情况
        }

        // 创建ZIP条目（即ZIP中的一个文件）
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        // 连接URL并获取输入流
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 连接超时时间
        connection.setReadTimeout(10000);   // 读取超时时间

        try (InputStream inputStream = connection.getInputStream()) {
            // 缓冲区，用于高效读写
            byte[] buffer = new byte[1024 * 8];
            int len;
            // 将文件内容写入ZIP
            while ((len = inputStream.read(buffer)) != -1) {
                zipOut.write(buffer, 0, len);
            }
        } finally {
            connection.disconnect(); // 关闭连接
            zipOut.closeEntry();     // 关闭当前ZIP条目
        }
    }

    @Override
    public boolean delete(FileTypeEnum fileTypeEnum, String fileKey) {
        log.info("LocalFileStorage.upload delete: {}", fileKey);

        try {
            String path = basePath;
            if (fileTypeEnum != null) {
                path = basePath + File.separator + fileTypeEnum.getPath();
            }

            Path filePath = Paths.get(path, fileKey);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        path = basePath + this.getPathAfterFile(path);
        log.info("LocalFileStorage.upload delete: {}", path);

        try {
            Path filePath = Paths.get(path);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 提取 URL 中 /file/ 后面的内容
     */
    private String getPathAfterFile(String url) {
        try {
            String prefix = "/file";
            int index = url.indexOf(prefix);
            return index > 0 ? url.substring(index + prefix.length()) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return lastIndex == -1 ? "" : fileName.substring(lastIndex);
    }

    public static void main(String[] args) {
        String url = "https://test-ai.neucityai.com/file/knowledge/cb7d10d93b444e91a1b97375b8221c56.mp4";
        FileStorageService storageService = new LocalFileStorage();
        storageService.delete(url);
    }

}
