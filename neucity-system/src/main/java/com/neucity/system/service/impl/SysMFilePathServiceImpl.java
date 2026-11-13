package com.neucity.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Throwables;
import com.neucity.common.core.utils.NoUtil;
import com.neucity.common.core.utils.PageUtils;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.file.service.FileStorageService;
import com.neucity.common.file.service.enums.FileTypeEnum;
import com.neucity.common.file.service.model.dto.UploadDTO;
import com.neucity.common.file.service.model.vo.UploadVo;
import com.neucity.common.security.utils.WebUserUtil;
import com.neucity.system.domain.SysMFilePath;
import com.neucity.common.core.model.dto.AddFileDTO;
import com.neucity.system.dto.FileInfoDTO;
import com.neucity.system.dto.FileListDTO;
import com.neucity.system.mapper.SysMFilePathMapper;
import com.neucity.system.service.SysMFilePathService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * 文件服务路径 实现
 *
 * @author ShuPF
 * @date 2025/9/28 15:37
 */
@Slf4j
@Service
public class SysMFilePathServiceImpl implements SysMFilePathService {

    @Autowired
    private SysMFilePathMapper sysMFilePathMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public AjaxResult saveFile(AddFileDTO form) {
        try {
            UploadVo fileKey = this.upload(form.getFile(), form.getType());

            String fileName = form.getName();
            if (StringUtils.isBlank(fileName)) {
                fileName = fileKey.getOldFileName();
            }

            SysMFilePath sysMFilePath = new SysMFilePath();
            sysMFilePath.setFileName(fileName);
            sysMFilePath.setRemark(form.getRemark());
            sysMFilePath.setFilePath(fileKey.getPath());
            sysMFilePath.setFileType(form.getType());
            sysMFilePath.setFileNum(NoUtil.generateNo());
            sysMFilePath.setExpand(fileKey.getFileName());
            sysMFilePath.setFileClassify(form.getFileClassify());
            sysMFilePath.setFileSize(form.getFile().getSize());
            WebUserUtil.prepareInsertParams(sysMFilePath);

            sysMFilePathMapper.insert(sysMFilePath);

            return AjaxResult.success(sysMFilePath);

        } catch (Exception e) {
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }


    @Override
    public List<SysMFilePath> findFileList(FileListDTO dto) {
        QueryWrapper queryWrapper = new QueryWrapper<SysMFilePath>();
        queryWrapper.orderByDesc("create_time");
        queryWrapper.eq("file_type", dto.getType());

        String fileClassify = dto.getFileClassify();
        if (StringUtils.isNotBlank(fileClassify)) {
            queryWrapper.eq("file_classify", fileClassify);
        }

        String keywords = dto.getKeywords();
        if (StringUtils.isNotBlank(keywords)) {
            queryWrapper.like("file_name", keywords);
        }

        String startTime = dto.getStartTime();
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge("create_time", startTime);
        }
        String endTime = dto.getEndTime();
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le("create_time", endTime);
        }

        PageUtils.startPage();
        return sysMFilePathMapper.selectList(queryWrapper);
    }


    @Override
    public UploadVo upload(MultipartFile file, String type) {
        try {
            FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(type);
            if (fileTypeEnum == null) {
                throw new RuntimeException("query failed: type is error");
            }

            boolean isRename = true;
            if (fileTypeEnum.getType().equals(FileTypeEnum.AI.getType())) {
                isRename = false;
            }

            UploadDTO uploadDTO =  UploadDTO.builder()
                    .inputStream(file.getInputStream())
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileTypeEnum(fileTypeEnum)
                    .isRename(isRename)
                    .build();

            return fileStorageService.upload(uploadDTO);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response, String tempName) {
        String resourceName = "template/" + tempName;
        ClassPathResource classPathResource = new ClassPathResource(resourceName);
        try (OutputStream out = response.getOutputStream();
             InputStream in = classPathResource.getInputStream();) {
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(tempName, "UTF-8"));
            response.setContentType("application/octet-stream");
            byte[] buff = new byte[2048];
            int len;
            while ((len = in.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("模板下载失败! {}", Throwables.getStackTraceAsString(e));
        }
    }


    @Override
    public AjaxResult deleteFileById(Long id) {
        SysMFilePath sysMFilePath = sysMFilePathMapper.selectById(id);
        if(sysMFilePath==null){
            return AjaxResult.error("文件不存在");
        }

        //删除文件记录
        if(sysMFilePathMapper.deleteById(sysMFilePath.getId())<=0){
            return AjaxResult.error("操作失败");
        }

        //删除文件
        try {
            fileStorageService.delete(FileTypeEnum.getByType(sysMFilePath.getFileType()), sysMFilePath.getFilePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return AjaxResult.success();
    }

    @Override
    public SysMFilePath getFileInfo(FileInfoDTO dto) {

        QueryWrapper queryWrapper = new QueryWrapper<SysMFilePath>();
        queryWrapper.orderByDesc("create_time");
        queryWrapper.eq("file_type", dto.getType());

        String fileClassify = dto.getFileClassify();
        if (StringUtils.isNotBlank(fileClassify)) {
            queryWrapper.eq("file_classify", fileClassify);
        }

        queryWrapper.last("limit 1");

        return sysMFilePathMapper.selectOne(queryWrapper);
    }
}
