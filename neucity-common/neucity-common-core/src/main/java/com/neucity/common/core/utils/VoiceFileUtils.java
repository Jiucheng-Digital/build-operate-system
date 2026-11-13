package com.neucity.common.core.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;

public class VoiceFileUtils {
    /**
     * 校验文件格式（仅支持PCM）
     */
    public static boolean checkFormat(String fileName) {
        if (fileName == null) return false;
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return "pcm".equals(suffix);
    }

    /**
     * 保存MultipartFile到本地
     */
    public static void saveFile(MultipartFile file, File dest) throws Exception {
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(file.getBytes());
        }
    }
}
