package com.example.demo.utils;

import org.springframework.util.StringUtils;

import java.io.File;

public class DetectPathUtil {
    /**
     * 从完整路径中提取目录部分
     */
    public static String extractDirectory(String fullPath) {
        if (!StringUtils.hasText(fullPath)) {
            return fullPath;
        }

        // 标准化路径分隔符
        String normalizedPath = fullPath.replace('\\', '/');

        // 找到最后一个斜杠的位置
        int lastSlashIndex = normalizedPath.lastIndexOf('/');

        if (lastSlashIndex == -1) {
            return normalizedPath;
        }

        return normalizedPath.substring(0, lastSlashIndex);
    }

    /**
     * 检查路径是否存在
     */
    public static boolean pathExists(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }

        File file = new File(path);
        return file.exists();
    }
}