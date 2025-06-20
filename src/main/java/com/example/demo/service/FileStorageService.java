package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get("cropped_images").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + fileExtension;

        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return targetLocation.toString(); // 返回完整路径
    }

    public void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
        Files.deleteIfExists(filePath);
    }

    /**
     * 将框选的区域切割为图片并保存到指定路径
     *
     * @param originalImage 原始图像
     * @param x 框选区域起始X坐标
     * @param y 框选区域起始Y坐标
     * @param width 框选区域宽度
     * @param height 框选区域高度
     */

    public void cropAndSaveImage(
            String imagePath,
            String outputDir,
            BufferedImage originalImage,
            int x, int y, int width, int height,
            String fileName) throws IOException {

        // 创建目标目录
        Path outputPath = Paths.get(outputDir, fileName);
        if (!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        // 切割图像
        BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
        ImageIO.write(croppedImage, "png", outputPath.toFile());

        // 保存切割后的图像并验证是否成功
        boolean success = ImageIO.write(croppedImage, "png", outputPath.toFile());
        if (!success) {
            throw new IOException("Failed to write image file.");
        }

        // 打印成功信息
        System.out.println("Cropped image saved to: " + outputPath.toAbsolutePath());
    }

    public BufferedImage mergeImages(List<BufferedImage> images) {
        int totalWidth = images.stream().mapToInt(BufferedImage::getWidth).sum();
        int maxHeight = images.stream().mapToInt(BufferedImage::getHeight).max().orElse(0);

        BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combined.createGraphics();

        int x = 0;
        for (BufferedImage img : images) {
            g2d.drawImage(img, x, 0, null);
            x += img.getWidth();
        }

        g2d.dispose();
        return combined;
    }

    /**
     * 合并多张图片并保存为固定文件名 merged.png
     */
    /**
     * 合并多张图片并保存为固定文件名 merged.png（竖向拼接）
     */
    private static int currentMergeIndex = 1;
    public String mergeAndSaveImages(List<BufferedImage> images, String outputDir, String baseName,int baseOrder) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("没有可合并的图片");
        }

        // 竖向拼接：总高度 = 所有图片高度之和，宽度取最大值
        int totalHeight = images.stream().mapToInt(BufferedImage::getHeight).sum();
        int maxWidth = images.stream().mapToInt(BufferedImage::getWidth).max().orElse(0);

        // 创建空白画布
        BufferedImage combinedImage = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combinedImage.createGraphics();

        int y = 0;
        for (BufferedImage img : images) {
            g2d.drawImage(img, 0, y, null);  // x=0 固定，y 递增
            y += img.getHeight();           // 下一张图从当前图下方开始
        }
        g2d.dispose();

        // 创建输出目录
        Path outputFolderPath = Paths.get(outputDir);
        if (!Files.exists(outputFolderPath)) {
            Files.createDirectories(outputFolderPath);
        }

        // 构建文件名：以 baseName 开头，如 merged_001.png
        File outputFile = new File(outputDir, baseName + "_" + String.format("%03d", currentMergeIndex++) + ".png");

        // 写入文件
        ImageIO.write(combinedImage, "png", outputFile);

        return outputFile.getAbsolutePath();  // 返回完整路径
    }
} 