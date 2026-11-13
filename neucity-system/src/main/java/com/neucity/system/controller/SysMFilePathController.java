package com.neucity.system.controller;

import cn.hutool.core.io.FileUtil;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import com.neucity.common.file.service.FileStorageService;
import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.vo.UploadVo;
import com.neucity.system.domain.SysMFilePath;
import com.neucity.common.core.model.dto.AddFileDTO;
import com.neucity.system.dto.FileInfoDTO;
import com.neucity.system.dto.FileListDTO;
import com.neucity.system.service.SysMFilePathService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件类型存储
 *
 * @author ShuPF
 * @date 2025/9/28 15:41
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class SysMFilePathController extends BaseController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SysMFilePathService sysMFilePathService;

    @PostMapping("/upload/{type}")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file, @PathVariable("type") String type) {
        try {

            UploadVo fileKey = sysMFilePathService.upload(file, type);

            return AjaxResult.success(fileKey);
        } catch (Exception e) {
            return AjaxResult.error("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/getTemporaryDownloadUrl/{type}")
    public AjaxResult getTemporaryDownloadUrl(@PathVariable("type") String type, String key) throws IOException {
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(type);
        if (fileTypeEnum == null) {
            throw new RuntimeException("query failed: type is error");
        }

        return success(fileStorageService.getTemporaryDownloadUrl(fileTypeEnum, key));
    }

    /**
     * 文件下载接口
     *
     * @param fileKey 文件标识（存储时返回的key）
     * @return 文件响应
     */
    @GetMapping("/download/{type}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("type") String type, String fileKey) {

        FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(type);
        if (fileTypeEnum == null) {
            throw new RuntimeException("download failed: type is error");
        }

        try {
            // 从存储服务获取文件输入流
            InputStream inputStream = fileStorageService.download(fileTypeEnum, fileKey);

            // 编码文件名以支持中文
            String encodedFileName = URLEncoder.encode(fileKey, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            // 构建响应实体
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));

        } catch (IOException e) {
            // 处理文件不存在或读取失败的情况
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            // 处理其他异常
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 文件下载接口
     *
     * @param fileKey 文件标识（存储时返回的key）
     * @return 文件响应
     */
    @GetMapping("/stream/{type}")
    public ResponseEntity<StreamingResponseBody> streamFile(@PathVariable("type") String type, String fileKey) {
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(type);
        if (fileTypeEnum == null) {
            throw new RuntimeException("download failed: type is error");
        }

        try {
            // 编码文件名以支持中文
            String encodedFileName = URLEncoder.encode(fileKey, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            // 从存储服务获取文件输入流
            File file = fileStorageService.downloadFile(fileTypeEnum, fileKey);
            // 解码Base64字符串
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            // 使用更高效的方式获取 MIME 类型
            String mimeType = FileUtil.getMimeType(file.getPath());
            // 使用 StreamingResponseBody 进行流式传输
            StreamingResponseBody responseBody = outputStream -> {
                try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
                    // 8KB buffer
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .contentLength(file.length())
                    .body(responseBody);
        } catch (IOException e) {
            // 处理文件不存在或读取失败的情况
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            // 处理其他异常
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/downloadZip")
    public ResponseEntity<StreamingResponseBody> downloadZipStream(@RequestBody List<String> paths) {
        try {
            // 设置响应头
            String fileName = "batch_download_" + System.currentTimeMillis() + ".zip";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            // 创建流式响应体
            StreamingResponseBody streamingResponseBody = outputStream -> {
                // 调用你现有的 service 方法，但传递输出流
                fileStorageService.downloadZipStream(paths, outputStream);
            };
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"")
                    .header("Content-Type", "application/zip")
                    .body(streamingResponseBody);
        } catch (Exception e) {
            log.error("创建流式响应失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 添加文件
     *
     * @return 文件标识
     */
    @PostMapping("/add")
    public AjaxResult addFile(@Validated AddFileDTO form) {
        return sysMFilePathService.saveFile(form);
    }

    @GetMapping("/list/{type}")
    public TableDataInfo list(@PathVariable("type") String type, FileListDTO dto) {
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(type);
        if (fileTypeEnum == null) {
            throw new RuntimeException("query failed: type is error");
        }

        dto.setType(type);
        List<SysMFilePath> list = sysMFilePathService.findFileList(dto);

        return getDataTable(list);
    }

    @GetMapping("/info")
    public AjaxResult getFileInfo(FileInfoDTO dto) {
        if (StringUtils.isBlank(dto.getType())) {
            return error("文件类型不能为空");
        }
        return success(sysMFilePathService.getFileInfo(dto));
    }

    /**
     * 删除文件
     *
     * @return 文件标识
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteFileById(@PathVariable("id") Long id) {
        return sysMFilePathService.deleteFileById(id);
    }

}
