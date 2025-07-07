package com.example.demo.controller;

import com.example.demo.dto.SomeDto;
import com.example.demo.entity.PdfPage;
import com.example.demo.entity.ViewQuestion;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.OssService;
import com.example.demo.service.PdfPageService;
import com.example.demo.service.ViewQuestionService;
import com.example.demo.utils.OssPathUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/pdf-pages")
@CrossOrigin(origins = "http://localhost:5173")

public class PdfPageController {
    @Autowired
    private OssService ossService;
    @Autowired
    private OssPathUtils ossPathUtils;
    @Autowired
    private PdfPageService pdfPageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ViewQuestionService viewQuestionService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取所有分页数据
     */
    @GetMapping("/list")
    public ResponseEntity<List<PdfPage>> getAllPages() {
        List<PdfPage> pages = pdfPageService.getAllPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Void> getImage(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Optional<PdfPage> pageOpt = pdfPageService.getPageById(id);
        if (!pageOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        String imagePath = pageOpt.get().getBookPath();
        System.out.println("尝试加载图片路径：" + imagePath);

        // 如果是 OSS URL，直接重定向
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            response.sendRedirect(imagePath);
            return null;
        }

        // 否则按本地文件处理
        Path path = Paths.get(imagePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            response.setContentType("image/png");
            IOUtils.copy(resource.getInputStream(), response.getOutputStream());
        } else {
            System.err.println("文件不存在或不可读：" + imagePath);
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
        return null;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<PdfPage> getImages(@PathVariable Long id) {
        System.out.println("===============================================");

        // 查找指定 ID 的 PdfPage 记录
        Optional<PdfPage> pageOpt = pdfPageService.getPageById(id);

        // 打印查询结果（用于调试）
        System.out.println("查询结果: " + (pageOpt.isPresent() ? pageOpt.get() : "未找到记录"));

        // 如果记录存在，返回 200 OK 和 PdfPage 对象
        if (pageOpt.isPresent()) {
            return ResponseEntity.ok(pageOpt.get());
        }
        // 如果记录不存在，返回 404 Not Found
        else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据 ID 获取单个分页
     */
    @GetMapping("/{id}")
    public ResponseEntity<PdfPage> getPageById(@PathVariable Long id) {
        Optional<PdfPage> pageOpt = pdfPageService.getPageById(id);
        return pageOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新 objectDetection 字段
     */
    @PostMapping("/update-object-detection")
    public ResponseEntity<Void> updateObjectDetection(@RequestParam Long id,
                                                      @RequestParam Integer objectDetection) {
        Optional<PdfPage> pageOpt = pdfPageService.getPageById(id);
        if (pageOpt.isPresent()) {
            PdfPage page = pageOpt.get();
            page.setObjectDetection(objectDetection);
            pdfPageService.updatePage(page);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/crop")
    public ResponseEntity<Map<String, String>> cropImage(
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam Long imageId,
            @RequestParam Integer order,
            @RequestParam String ossPath) {

        Optional<PdfPage> pageOpt = pdfPageService.getPageById(imageId);
        if (!pageOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String localImagePath = pageOpt.get().getBookPath();
        BufferedImage originalImage = null;

        try {
                if (localImagePath.startsWith("http://") || localImagePath.startsWith("https://")) {
                    // OSS图片，使用URL流读取
                    try (InputStream in = new URL(localImagePath).openStream()) {
                        originalImage = ImageIO.read(in);
                    }
                } else {
                    // 本地图片
                    originalImage = ImageIO.read(new File(localImagePath));
                }
                if (originalImage == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.singletonMap("error", "无法读取原始图像"));
                }

            // 获取 ISBN
//            String isbn = pageOpt.get().getISBN();
//            String pages = pageOpt.get().getBookPage();
//            String fileName = order + ".png";
//            // OSS 路径 /pdf/{isbn}/page/question/{pages}/{order}.png
//            String ossPath = ossPathUtils.buildQuestionPath(isbn, pages + "/" + fileName);

            // 执行裁剪
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            File tempFile = Files.createTempFile("cropped-", ".png").toFile();
            ImageIO.write(croppedImage, "png", tempFile);

            String imageUrl = ossService.uploadFile(tempFile.getAbsolutePath(), ossPath);
            tempFile.delete();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image cropped and saved successfully!");
            response.put("croppedImagePath", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = Collections.singletonMap("error", "裁剪失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/image")
    public void getImage(@RequestParam String url, HttpServletResponse response) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            response.setContentType("image/png"); // 或根据实际类型设置
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    @PostMapping("/upload-cropped")
    public ResponseEntity<Map<String, String>> uploadCroppedImage(@RequestParam("file") MultipartFile file) throws IOException {
        // 使用 FileStorageService 保存文件
        String fileName = fileStorageService.storeFile(file);
        //  String filePath = Paths.get("cropped_images", fileName).toString();

        Map<String, String> response = new HashMap<>();
        response.put("filePath", "http://localhost:8080/api/pdf-pages/image?path=" + fileName);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/merge-images")
    public ResponseEntity<Map<String, String>> mergeCroppedImages(@RequestParam("paths") String pathsParam) {
        String[] paths = pathsParam.split(",");

        try {
            List<BufferedImage> bufferedImages = new ArrayList<>();
            for (String path : paths) {
                BufferedImage image;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    try (InputStream in = new URL(path).openStream()) {
                        image = ImageIO.read(in);
                    }
                } else {
                    image = ImageIO.read(new File(path));
                }
                if (image == null) {
                    throw new IOException("无法读取图片: " + path);
                }
                bufferedImages.add(image);
            }

            if (bufferedImages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            int totalWidth = bufferedImages.stream().mapToInt(BufferedImage::getWidth).sum();
            int maxHeight = bufferedImages.stream().mapToInt(BufferedImage::getHeight).max().orElse(0);

            BufferedImage combinedImage = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = combinedImage.createGraphics();

            int currentX = 0;
            for (BufferedImage img : bufferedImages) {
                g2d.drawImage(img, currentX, 0, null);
                currentX += img.getWidth();
            }
            g2d.dispose();

            // 保存合并图片到本地临时文件
            File tempFile = Files.createTempFile("merged-", ".png").toFile();
            ImageIO.write(combinedImage, "png", tempFile);

            // 上传到 OSS，路径可根据业务自定义
            String ossPath = "pdf/merged/merged_" + System.currentTimeMillis() + ".png";
            String ossUrl = ossService.uploadFile(tempFile.getAbsolutePath(), ossPath);
            tempFile.delete();

            Map<String, String> result = new HashMap<>();
            result.put("mergedImageUrl", ossUrl);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/merge-cropped-images")
    public ResponseEntity<Map<String, String>> mergeCroppedImages(@RequestBody Map<String, Object> payload) {
        if (payload.containsKey("images")) {
            List<String> imageBase64List = (List<String>) payload.get("images");
            String selectedImageId = (String) payload.get("selectedImageId");
            Integer order = (Integer) payload.get("order");

            if (imageBase64List == null || imageBase64List.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "没有图片数据"));
            }
            try {
                List<BufferedImage> bufferedImages = new ArrayList<>();
                for (String base64 : imageBase64List) {
                    String base64Data = base64.replaceFirst("^data:image/.+;base64,", "");
                    byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
                    if (image != null) {
                        bufferedImages.add(image);
                    }
                }
                Optional<PdfPage> pageOpt = pdfPageService.getPageById(Long.valueOf(selectedImageId));
                if (!pageOpt.isPresent()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "未找到原图"));
                }
                PdfPage pdfPage = pageOpt.get();

                String isbn = pdfPage.getISBN();
                String pages = pdfPage.getBookPage();
                // 合并图片
                int totalHeight = bufferedImages.stream().mapToInt(BufferedImage::getHeight).sum();
                int maxWidth = bufferedImages.stream().mapToInt(BufferedImage::getWidth).max().orElse(0);
                BufferedImage combinedImage = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = combinedImage.createGraphics();
                int y = 0;
                for (BufferedImage img : bufferedImages) {
                    g2d.drawImage(img, 0, y, null);
                    y += img.getHeight();
                }
                g2d.dispose();

                // 1. 先上传到 zc 目录
                String fileName = pages + "_" + order + ".png";
                String zcPath = ossPathUtils.buildZcPath(isbn, fileName); // /pdf/{isbn}/page/zc/{pages}_{order}.png
                File tempFile = Files.createTempFile("merged-", ".png").toFile();
                ImageIO.write(combinedImage, "png", tempFile);
                String zcUrl = ossService.uploadFile(tempFile.getAbsolutePath(), zcPath);

                // 2. 再复制一份到 question 目录
                //String ossPath = payload.get("path");
               /* String name = payload.get("name") != null ? payload.get("name").toString() : (order != null ? order.toString(): "1");
                String questionPath = ossPathUtils.buildQuestionPath(isbn, pages + "/" + name+".png"); // /pdf/{isbn}/page/question/{pages}/{order}.png
                File tempCopy = File.createTempFile("copy-", ".png");
                try (InputStream in = new URL(zcUrl).openStream()) {
                    Files.copy(in, tempCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                String questionUrl = ossService.uploadFile(tempCopy.getAbsolutePath(), questionPath);
                tempCopy.delete();
                tempFile.delete();*/

                // 返回 question 目录下的路径
                return ResponseEntity.ok(Collections.singletonMap("outputPath", zcUrl));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "合并失败：" + e.getMessage()));
            }
        }
        if (payload.containsKey("srcPath") && payload.containsKey("destPath")) {
            String srcPath = (String) payload.get("srcPath");
            String destPath = (String) payload.get("destPath");
            try {
                // 下载 srcPath 到本地临时文件
                File tempFile = File.createTempFile("oss_copy_", ".png");
                try (InputStream in = new URL(srcPath).openStream()) {
                    Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // 上传到 OSS 新路径
                String ossUrl = ossService.uploadFile(tempFile.getAbsolutePath(), destPath);
                tempFile.delete();
                return ResponseEntity.ok(Collections.singletonMap("outputPath", ossUrl));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "复制合并图失败：" + e.getMessage()));
            }
        }
        // 参数不对
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("error", "参数错误"));
    }

    @PostMapping("/some-path")
    public ResponseEntity<?> someMethod(@RequestBody SomeDto dto) {
        System.out.println("Received data: " + dto.getField1() + ", " + dto.getField2());
        return ResponseEntity.ok().build();
    }


    @RequestMapping(value = "/clear-temp-merged", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<String> clearTempMergedFolder(@RequestParam String isbn) {
        try {
            // 删除OSS上的zc文件夹
            String zcOssPath = "pdf/" + isbn + "/page/zc/";
            boolean deleted = ossService.deleteFolder(zcOssPath);
            
            if (deleted) {
                return ResponseEntity.ok("OSS临时合并图已删除");
            } else {
                return ResponseEntity.ok("无OSS临时合并图可删除");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除OSS临时文件夹失败：" + e.getMessage());
        }
    }

    @PostMapping("/copy-file")
    public ResponseEntity<Map<String, String>> copyFile(@RequestBody Map<String, String> payload) {
        String srcPath = payload.get("srcPath");
        String destPath = payload.get("destPath");

        try {
            Path source;
            File tempFile = null;
            if (srcPath.startsWith("http://") || srcPath.startsWith("https://")) {
                tempFile = File.createTempFile("oss_download_", ".png");
                try (InputStream in = new URL(srcPath).openStream()) {
                    Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                source = tempFile.toPath();
            } else {
                source = Paths.get(srcPath);
            }

            Path destination = Paths.get(destPath);

            if (!Files.exists(source)) {
                if (tempFile != null) tempFile.delete();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "源文件不存在"));
            }

            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            if (tempFile != null) tempFile.delete();

            return ResponseEntity.ok(Collections.singletonMap("message", "文件复制成功"));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "复制失败：" + e.getMessage()));
        }
    }

    @PostMapping("/create-dir")
    public ResponseEntity<Map<String, String>> createDir(@RequestParam String dir) {
        try {
            File targetDir = new File(dir);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            return ResponseEntity.ok(Collections.singletonMap("message", "目录已创建"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "创建目录失败：" + e.getMessage()));
        }
    }

    @PostMapping("/view-question")
    public ResponseEntity<Void> saveView(@RequestBody Map<String, Object> payload) {
        try {
            String isbn = (String) payload.get("ISBN");
            String pages = (String) payload.get("pages");
            int pageNum = Integer.parseInt(pages.replaceAll("\\D+", ""));
            String name = String.valueOf(payload.get("name"));
            name = name + ".png";
            int questionNumber;
            Object questionObj = payload.get("question_number");
            if (questionObj instanceof Integer) {
                questionNumber = (Integer) questionObj;
            } else if (questionObj instanceof String) {
                questionNumber = Integer.parseInt((String) questionObj);
            } else {
                questionNumber = Integer.parseInt(String.valueOf(questionObj));
            }
            String path = (String) payload.get("path");


            ViewQuestion view = new ViewQuestion();
            view.setISBN(isbn);
            view.setPages(pageNum);
            view.setName(name);
            view.setQuestionNumber(questionNumber);
            view.setPath(path);

            // 设置新增字段
            if (payload.containsKey("file")) {
                view.setFile((String) payload.get("file"));
            }
            if (payload.containsKey("grade")) {
                view.setGrade((String) payload.get("grade"));
            } else {
                // 自动补全 grade 字段
                Optional<PdfPage> pageOpt = pdfPageService.getPageByISBNAndPage(isbn, pages);
                if (pageOpt.isPresent()) {
                    view.setGrade(pageOpt.get().getGrade());
                }
            }
            if (payload.containsKey("subject")) {
                view.setSubject((String) payload.get("subject"));
            }
            if (payload.containsKey("points")) {
                Object pointsObj = payload.get("points");
                if (pointsObj != null) {
                    if (pointsObj instanceof Integer) {
                        view.setPoints((Integer) pointsObj);
                    } else if (pointsObj instanceof String) {
                        view.setPoints(Integer.parseInt((String) pointsObj));
                    }
                }
            }
            if (payload.containsKey("topic")) {
                view.setTopic((String) payload.get("topic"));
            }

            viewQuestionService.save(view);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-page")
    public ResponseEntity<List<ViewQuestion>> getQuestionsByPage(
            @RequestParam String isbn,
            @RequestParam String page) {
        try {
            // 添加参数验证
            if (isbn == null || isbn.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<ViewQuestion> questions = viewQuestionService.findByIsbnAndPage(isbn, page);

            // 如果没有找到数据，返回空列表而不是null
            if (questions == null) {
                questions = Collections.emptyList();
            }

            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/question-image")
    public ResponseEntity<Resource> getQuestionImage(@RequestParam String path) {
        try {
            Resource resource;
            if (path.startsWith("http://") || path.startsWith("https://")) {
                // 网络图片
                resource = new UrlResource(path);
            } else {
                // 本地图片
                Path imagePath = Paths.get(path);
                resource = new UrlResource(imagePath.toUri());
            }

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import-questions")
    public ResponseEntity<String> importQuestions(
            @RequestParam String isbn,
            @RequestParam String page
    ) {
        // 处理逻辑
        return ResponseEntity.ok("Imported questions successfully");
    }

    @PutMapping("/update-question")
    public ResponseEntity<Void> updateQuestion(@RequestBody Map<String, Object> payload) {
        try {
            Long id = Long.valueOf(String.valueOf(payload.get("id")));
            Optional<ViewQuestion> questionOpt = viewQuestionService.findById(id);

            if (!questionOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ViewQuestion question = questionOpt.get();

            // 更新字段
            if (payload.containsKey("question_number")) {
                Object questionNumberObj = payload.get("question_number");
                if (questionNumberObj instanceof Integer) {
                    question.setQuestionNumber((Integer) questionNumberObj);
                } else if (questionNumberObj instanceof String) {
                    question.setQuestionNumber(Integer.parseInt((String) questionNumberObj));
                }
            }

            if (payload.containsKey("answer")) {
                question.setAnswer((String) payload.get("answer"));
            }

            if (payload.containsKey("analysis")) {
                question.setAnalysis((String) payload.get("analysis"));
            }

            if (payload.containsKey("knowledge")) {
                question.setKnowledge((String) payload.get("knowledge"));
            }

            if (payload.containsKey("merge_graph")) {
                question.setMergeGraph((String) payload.get("merge_graph"));
            }

            // 更新新增字段
            if (payload.containsKey("file")) {
                question.setFile((String) payload.get("file"));
            }
            if (payload.containsKey("grade")) {
                question.setGrade((String) payload.get("grade"));
            }
            if (payload.containsKey("subject")) {
                question.setSubject((String) payload.get("subject"));
            }
            if (payload.containsKey("points")) {
                Object pointsObj = payload.get("points");
                if (pointsObj != null) {
                    if (pointsObj instanceof Integer) {
                        question.setPoints((Integer) pointsObj);
                    } else if (pointsObj instanceof String) {
                        question.setPoints(Integer.parseInt((String) pointsObj));
                    }
                }
            }
            if (payload.containsKey("topic")) {
                question.setTopic((String) payload.get("topic"));
            }

            viewQuestionService.save(question);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/workflow-detail")
    public ResponseEntity<Map<String, Object>> workflowDetail(@RequestBody Map<String, Object> payload) {
        try {
            Long questionId = Long.valueOf(payload.get("id").toString());
            Optional<ViewQuestion> questionOpt = viewQuestionService.findById(questionId);
            if (!questionOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            ViewQuestion question = questionOpt.get();

            // 构造工作流输入
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("file", question.getPath());
            parameters.put("grade", question.getGrade());
            parameters.put("subject", question.getSubject());

            // 构造请求体
            Map<String, Object> body = new HashMap<>();
            body.put("workflow_id", "7509392015203205131");
            body.put("parameters", parameters);

            // 调用 Coze 工作流
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer pat_IGe7yBZIbmrjXA88Oh6OvYwPZZ8JSBFm5yDTuCyPp3JUCMTpI3NHUsOjW5ufr8KD");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            // ResponseEntity<Map> response = restTemplate.postForEntity("https://api.coze.cn/v1/workflow/run", entity, Map.class);

            // 解析输出并写回数据库
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://api.coze.cn/v1/workflow/run",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );

            Map<String, Object> bodys = response.getBody();

            if (bodys != null) {
                Object dataObj = bodys.get("data");
                if (dataObj instanceof String) {
                    try {
                        // 将字符串反序列化为 Map
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = objectMapper.readValue((String) dataObj, Map.class);

                        if (result != null) {
                            if (result.get("analysis") != null) question.setKnowledge(result.get("analysis").toString());
                            if (result.get("answer") != null) question.setAnswer(result.get("answer").toString());
                            if (result.get("points") != null) question.setAnalysis(result.get("points").toString());
                            if (result.get("topic") != null) question.setTopic(result.get("topic").toString());
                            viewQuestionService.save(question);
                        }

                        return ResponseEntity.ok(result != null ? result : Collections.emptyMap());

                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("error", "解析 data 失败：" + e.getMessage()));
                    }
                }else if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) dataObj;

                    if (result != null) {
                        if (result.get("analysis") != null) question.setKnowledge(result.get("analysis").toString());
                        if (result.get("answer") != null) question.setAnswer(result.get("answer").toString());
                        if (result.get("points") != null) question.setAnalysis(result.get("points").toString());
                        if (result.get("topic") != null) question.setTopic(result.get("topic").toString());
                        viewQuestionService.save(question);
                    }

                    return ResponseEntity.ok(result != null ? result : Collections.emptyMap());
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.singletonMap("error", "data 格式不支持"));
                }
            }

            return  ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
