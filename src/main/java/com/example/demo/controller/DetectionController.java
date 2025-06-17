package com.example.demo.controller;




import com.example.demo.entity.DetectionRequest;
import com.example.demo.entity.DetectionResponse;
import com.example.demo.service.DetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DetectionController {
    private static final Logger logger = LoggerFactory.getLogger(DetectionController.class);
    private final DetectionService detectionService;

    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping("/detect/folder")
    public ResponseEntity<DetectionResponse> detectFolder(@RequestBody DetectionRequest request) {
        logger.info("接收到检测请求: {}", request);

        // 打印参数用于调试
        System.out.println("接收到的id: " + request.getId());
        System.out.println("接收到的folderPath: " + request.getFolderPath());
        System.out.println("接收到的folderIdentifier: " + request.getFolderIdentifier());
        System.out.println("接收到的ISBN: " + request.getISBN());
        System.out.println("接收到的slicing: " + request.getSlicing());

        // 判断page文件夹是否存在
        File pageFolder = new File(request.getFolderPath());
        if (!pageFolder.exists()) {
            logger.error("页面文件夹不存在: {}", request.getFolderPath());
            return ResponseEntity.badRequest()
                    .body(new DetectionResponse(false, "页面文件夹不存在"));
        }

        // 判断page文件夹是否为目录
        if (!pageFolder.isDirectory()) {
            logger.error("页面路径不是一个目录: {}", request.getFolderPath());
            return ResponseEntity.badRequest()
                    .body(new DetectionResponse(false, "页面路径不是一个有效的目录"));
        }

        // 创建labelstxt文件夹路径
        File labelFolder = new File(request.getFolderIdentifier());

        // 如果labelstxt文件夹不存在，则创建它
        if (!labelFolder.exists()) {
            logger.info("创建标签文件夹: {}", request.getFolderIdentifier());
            boolean created = labelFolder.mkdirs();
            if (!created) {
                logger.error("无法创建标签文件夹: {}", request.getFolderIdentifier());
                return ResponseEntity.badRequest()
                        .body(new DetectionResponse(false, "无法创建标签文件夹"));
            }
        }

        // 调用检测服务
        DetectionResponse response = detectionService.detect(request);

        if (response.isSuccess()) {
            // 根据ISBN字段查询pdf_pages表中符合条件的数据，将符合条件的字段的object_detection的值改为0


            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 将 Files.readString 替换为兼容旧版 Java 的代码
    @GetMapping("/files/read")
    public ResponseEntity<String> readTxtFile(@RequestParam String path) {
        try {
            // 安全检查：确保路径在允许的范围内
            // 建议使用更严格的路径验证或白名单机制
            if (!path.startsWith("D:/pdf/")) {
                return ResponseEntity.badRequest().body("禁止访问该路径");
            }

            // 使用 UTF-8 编码读取文件
            Path filePath = Paths.get(path);
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            String content = String.join("\n", lines);

            // 明确指定内容类型为文本
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("读取文件失败: " + e.getMessage());
        }
    }

}