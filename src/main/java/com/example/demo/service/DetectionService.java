package com.example.demo.service;


import com.example.demo.entity.DetectionRequest;
import com.example.demo.entity.DetectionResponse;
import com.example.demo.entity.PdfDocument;
import com.example.demo.entity.PdfPage;
import com.example.demo.repository.PdfPageRepository;
import com.example.demo.utils.DetectPathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class DetectionService {
    private static final Logger logger = LoggerFactory.getLogger(DetectionService.class);

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl;
    private final int requestTimeout;

    @Autowired
    private PdfPageRepository pdfPagesRepository;

    public DetectionService(RestTemplate restTemplate,
                            @Value("${python.service.url}") String pythonServiceUrl,
                            @Value("${python.service.timeout:300}") int requestTimeout) {
        this.restTemplate = restTemplate;
        this.pythonServiceUrl = pythonServiceUrl;
        this.requestTimeout = requestTimeout;
    }

    /**
     * 调用Python服务进行目标检测
     */
    public DetectionResponse detect(DetectionRequest request) {
        logger.info("开始目标检测，请求参数: {}", request);

        // 检查参数
        if (request == null ||
                request.getFolderPath() == null ||
                request.getFolderIdentifier() == null) {
            logger.error("检测请求参数不完整");
            return createErrorResponse("检测请求参数不完整");
        }

        // 检查文件夹路径是否存在
        if (!DetectPathUtil.pathExists(request.getFolderPath())) {
            logger.error("文件夹路径不存在: {}", request.getFolderPath());
            return createErrorResponse("文件夹路径不存在");
        }

        try {
            // 构建请求参数
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("folder_identifier", request.getFolderIdentifier());
            requestBody.add("folder_path", request.getFolderPath());

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setConnection("keep-alive");
            headers.set("User-Agent", "Mozilla/5.0");

            // 构建请求实体
            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<>(requestBody, headers);

            // 发送请求到Python服务
            logger.info("发送检测请求到Python服务: {}", pythonServiceUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    pythonServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 处理响应
            if (response.getStatusCode() == HttpStatus.OK) {
                // 解析响应JSON
                // 这里假设Python服务返回标准的DetectionResponse格式
                // 实际应用中可能需要使用Jackson等工具解析JSON
                int updatedCount = pdfPagesRepository.updateObjectDetectionByIsbn(request.getISBN());
                logger.info("更新了{}条记录的object_detection状态为0", updatedCount);
                return createSuccessResponse(
                        "检测完成",
                        request.getFolderIdentifier(),
                        0.0
                );
            } else {
                logger.error("Python服务返回错误状态码: {}", response.getStatusCodeValue());
                return createErrorResponse("Python服务返回错误状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            logger.error("调用Python服务失败", e);
            return createErrorResponse("调用Python服务失败: " + e.getMessage());
        }
    }

    /**
     * 创建成功响应
     */
    private DetectionResponse createSuccessResponse(String message, String txtPath, double processingTime) {
        DetectionResponse response = new DetectionResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTxtPath(txtPath);
        response.setProcessingTime(processingTime);
        return response;
    }

    /**
     * 创建错误响应
     */
    private DetectionResponse createErrorResponse(String message) {
        DetectionResponse response = new DetectionResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }


}
