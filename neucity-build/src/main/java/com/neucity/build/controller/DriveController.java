package com.neucity.build.controller;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.neucity.build.dto.DriveCommonDTO;
import com.neucity.build.dto.DriveDTO;
import com.neucity.build.dto.DriveListDTO;
import com.neucity.build.dto.UpdateConfigDTO;
import com.neucity.build.mapper.SysClassifyMapper;
import com.neucity.build.service.DriveService;
import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import com.neucity.common.core.model.dto.websocket.CommonMsgDTO;
import com.neucity.common.core.utils.http.HttpUtils;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import com.neucity.common.security.annotation.RepeatSubmit;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.build.constants.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/3 16:19
 */
@Slf4j
@RestController
@RequestMapping("/drive")
public class DriveController extends BaseController {

    @Value(value = "${environmental.monitoring.enable:false}")
    private String envMonitorEnable;
    
    @Value(value = "${environmental.monitoring.url:http://jcsz-envget01.local/api/data}")
    private String envMonitorUrl;

    @Value("${file.storage.url:http://192.168.0.56/file}")
    private String baseUrl;

    @Autowired
    private DriveService driveService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Log(title = "驱动上传/更新", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/upload")
    public AjaxResult upload(@Validated DriveDTO dto) {
        DriveDTO asyncDto = copyDriveDto(dto);
        CompletableFuture.runAsync(() -> {
            driveService.uploadDrive(asyncDto);
        });
        return success("驱动上传中");
    }

    @GetMapping("/list")
    public TableDataInfo list(DriveListDTO dto) {
        return getDataTable(driveService.list(dto));
    }

    @GetMapping("/listByClassify/{classifyCode}")
    public AjaxResult listByClassify(@PathVariable("classifyCode") String classifyCode) {
        return success(driveService.listByClassify(classifyCode));
    }

    /**
     *  驱动监控
     * @param driveId 驱动ID
     */
    @GetMapping("/monitor")
    public AjaxResult monitor(@RequestParam(value = "driveId", required = false) String driveId) {
        return success(driveService.monitor(parseDriveId(driveId)));
    }

    @Log(title = "驱动卸载", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/remove/{driveId}")
    public AjaxResult remove(@PathVariable("driveId") Long driveId) {
        driveService.removeDrive(driveId);
        return success("操作成功");
    }

    @Log(title = "驱动安装", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PutMapping("/install/{driveId}")
    public AjaxResult install(@PathVariable("driveId") Long driveId) {
        driveService.installDrive(driveId);
        return success("操作成功");
    }


    /** ================================ 驱动公共接口 =================================== */
    @GetMapping("/getConfig")
    public AjaxResult getConfig(@Validated DriveCommonDTO dto) {
        return success(driveService.getConfig(dto));
    }

    @PutMapping("/updateConfig")
    public AjaxResult updateConfig(@RequestBody @Validated UpdateConfigDTO dto) {
        driveService.updateConfig(dto);
        return success("操作成功");
    }

    @GetMapping("/api")
    public AjaxResult getApi(@Validated DriveCommonDTO dto) {
        return success(driveService.getApi(dto));
    }

    @GetMapping("/getDevelopmentGuide")
    public AjaxResult getDevelopmentGuide() {
        return AjaxResult.success("", baseUrl + "/驱动开发指南.md");
    }

    @GetMapping("/getEnvMonitorInfo")
    public AjaxResult getEnvMonitorInfo() {
        if (!BooleanUtil.toBoolean(envMonitorEnable)) {
            throw new RuntimeException("环境监控未启用");
        }

        log.info("开始获取环境监控信息, url: {}", envMonitorUrl);
        String s = HttpUtils.sendSSLGet(envMonitorUrl);
        log.info("获取环境监控信息成功, result: {}", s);

        return success(JSONUtil.parseObj(s).getJSONObject("data"));
    }

    private Long parseDriveId(String driveId) {
        if (driveId == null || driveId.trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(driveId);
    }

    private DriveDTO copyDriveDto(DriveDTO dto) {
        MultipartFile file = dto.getFile();
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("drive-upload-", ".zip");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            DriveDTO asyncDto = new DriveDTO();
            asyncDto.setId(dto.getId());
            asyncDto.setClassifyCode(dto.getClassifyCode());
            asyncDto.setType(dto.getType());
            asyncDto.setUserId(SecurityUtils.getUserId());
            asyncDto.setOriginalFilename(file.getOriginalFilename());
            asyncDto.setTempFilePath(tempFile.toString());
            return asyncDto;
        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException deleteException) {
                    log.warn("删除驱动上传临时文件失败：{}", tempFile, deleteException);
                }
            }
            throw new RuntimeException("驱动文件缓存失败：" + e.getMessage(), e);
        }
    }

    @PostMapping("/kafkaMsg")
    public AjaxResult kafkaMsg() {
        // 发送kafka消息到通用webSocket
        CommonMsgDTO commonMsgDTO = new CommonMsgDTO();
        commonMsgDTO.setType(CommonWebsoketTypeEnum.DRIVE_UPLOAD);
        commonMsgDTO.setUserId(8911L);

        Map<String, String> data = new HashMap<>();
        data.put("message", "");
        data.put("cmd", "install_success");

        commonMsgDTO.setData(data);
        kafkaTemplate.send(KafkaTopicConstant.COMMON_MSG_TOPIC, JSONUtil.toJsonStr(commonMsgDTO));
        return success("kafkaMsg");
    }

}
