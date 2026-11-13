package com.neucity.system.service;

import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.file.service.model.vo.UploadVo;
import com.neucity.system.domain.SysMFilePath;
import com.neucity.common.core.model.dto.AddFileDTO;
import com.neucity.system.dto.FileInfoDTO;
import com.neucity.system.dto.FileListDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件路径存储服务
 *
 * @author ShuPF
 * @date 2025/9/28 15:36
 */
public interface SysMFilePathService {

    /**
     *  保存文件
     * @param form 文件信息
     * @return 保存文件
     */
    AjaxResult saveFile(AddFileDTO form);


    /**
     * 查询文件列表
     * @param dto 参数
     * @return 文件列表
     */
    List<SysMFilePath> findFileList(FileListDTO dto);


    /**
     * 上传文件
     * @param file 文件
     * @param type 类型
     * @return 数据
     */
    UploadVo upload(MultipartFile file, String type);

    /**
     * 下载模板
     * @param response 响应
     */
    void downloadTemplate(HttpServletResponse response, String tempName);

    /**
     * 删除文件
     * @param id
     * @return
     */
    AjaxResult deleteFileById(Long id);

    /**
     *  获取单个文件信息
     * @param dto 参数
     * @return 结果
     */
    SysMFilePath getFileInfo(FileInfoDTO dto);
}
