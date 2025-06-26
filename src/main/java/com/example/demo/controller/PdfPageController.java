package com.example.demo.controller;

import com.example.demo.dto.SomeDto;
import com.example.demo.entity.PdfDocument;
import com.example.demo.entity.PdfPage;
import com.example.demo.entity.ViewQuestion;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.PdfPageService;
import com.example.demo.service.ViewQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;

@RestController
@RequestMapping("/api/pdf-pages")
@CrossOrigin(origins = "http://localhost:5173")

public class PdfPageController {

    @Autowired
    private PdfPageService pdfPageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ViewQuestionService viewQuestionService;

    /**
     * 获取所有分页数据
     */
    @GetMapping("/list")
    public ResponseEntity<List<PdfPage>> getAllPages() {
        List<PdfPage> pages = pdfPageService.getAllPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws IOException {
        Optional<PdfPage> pageOpt = pdfPageService.getPageById(id);
        if (!pageOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        String imagePath = pageOpt.get().getBookPath();

        // 打印路径信息
        System.out.println("尝试加载图片路径：" + imagePath);

        Path path = Paths.get(imagePath);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } else {
            // 再次打印错误路径
            System.err.println("文件不存在或不可读：" + imagePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
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
            @RequestParam Integer order) {

        Optional<PdfPage> pageOpt = pdfPageService.getPageById(imageId);
        if (!pageOpt.isPresent()) {
            //Map<String, String> error = Collections.singletonMap("error", "未找到原图");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String imagePath = pageOpt.get().getBookPath(); // 获取真实文件路径

        try {
            BufferedImage originalImage = ImageIO.read(new File(imagePath));
            if (originalImage == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "无法读取原始图像"));
            }

            // 获取目标目录结构
            File imageFile = new File(imagePath);
            File pageDir = imageFile.getParentFile(); // D:\pdf\45778985\page
            File isbnDir = pageDir.getParentFile();   // D:\pdf\45778985

            // 构建目标目录：D:\pdf\45778985\question
            File questionDir = new File(isbnDir, "question");
            // 自动创建目录（如果不存在）
            if (!questionDir.exists()) {
                questionDir.mkdirs();
            }

            String targetSubDirName = imageFile.getName().split("\\.")[0];
            File targetSubDir = new File(questionDir, targetSubDirName);
            if (!targetSubDir.exists()) {
                targetSubDir.mkdirs();
            }

            // 使用当前页内的顺序编号命名
            String fileName = order + ".png";
            String outputPath = new File(targetSubDir, fileName).getPath();

            // 执行裁剪
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            ImageIO.write(croppedImage, "png", new File(outputPath));


            String imageUrl = "http://localhost:8080/api/pdf-pages/image?path=" + outputPath;

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
    public ResponseEntity<Resource> getCroppedImage(@RequestParam String path) throws IOException {
        Path imagePath = Paths.get(path);
        Resource resource = new UrlResource(imagePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
                BufferedImage image = ImageIO.read(new File(new URI(path).getPath()));
                if (image != null) {
                    bufferedImages.add(image);
                }
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

            String outputDir = "cropped_images";
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            String fileName = "merged_" + System.currentTimeMillis() + ".png";
            String outputPath = outputDir + File.separator + fileName;

            ImageIO.write(combinedImage, "png", new File(outputPath));

            Map<String, String> result = new HashMap<>();
            result.put("mergedImageUrl", "http://localhost:8080/api/pdf-pages/image?path=" + outputPath);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/merge-cropped-images")
    public ResponseEntity<Map<String, String>>mergeCroppedImages(
            @RequestBody Map<String, Object> payload) {

        List<String> imageBase64List = (List<String>) payload.get("images");
        Integer baseNameIndex = (Integer) payload.get("baseName");
        String selectedImageId = (String) payload.get("selectedImageId"); // 从请求体中获取 selectedImageId
        Integer order = (Integer) payload.get("order");  // 新增字段

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
                } else {
                    System.err.println("Failed to decode image from base64" );
                }
            }

            Optional<PdfPage> pageOpt = pdfPageService.getPageById(Long.valueOf(selectedImageId));
            if (!pageOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "未找到原图"));
            }

            PdfPage pdfPage = pageOpt.get();
            String isbn = pdfPage.getISBN();

            String tempMergedDir = "zc"; // 定义 tempMergedDir 变量
            // 获取原始图片路径，用于确定输出目录

            File questionDir = new File("D:/pdf/" + isbn + "/question");
            File zcDir = new File(questionDir, "zc");

            if (!zcDir.exists()) {
                zcDir.mkdirs();
            }

            // 使用新的 mergeAndSaveImages 方法，并传入 order 参数
            String outputPath = fileStorageService.mergeAndSaveImages(bufferedImages, zcDir.getAbsolutePath(), "merged", order);


            return ResponseEntity.ok(Collections.singletonMap("outputPath", outputPath));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "合并失败：" + e.getMessage()));
        }
    }
    @PostMapping("/some-path")
    public ResponseEntity<?> someMethod(@RequestBody SomeDto dto) {
        System.out.println("Received data: " + dto.getField1() + ", " + dto.getField2());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/clear-temp-merged", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<String> clearTempMergedFolder(@RequestParam String isbn) {
        Path zcPath = Paths.get("D:/pdf", isbn, "question", "zc"); // 可以改为动态路径拼接
        if (Files.exists(zcPath)) {
            try {
                Files.walk(zcPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                return ResponseEntity.ok("临时合并图已删除");
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("删除失败：" + e.getMessage());
            }
        }
        return ResponseEntity.ok("无临时合并图可删除");
    }
    @PostMapping("/copy-file")
    public ResponseEntity<Map<String, String>> copyFile(@RequestBody Map<String, String> payload) {
        String srcPath = payload.get("srcPath");
        String destPath = payload.get("destPath");

        try {
            Path source = Paths.get(srcPath);
            Path destination = Paths.get(destPath);

            if (!Files.exists(source)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "源文件不存在"));
            }

            Files.createDirectories(destination.getParent());

            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

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
            name = name+".png";
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
            Path imagePath = Paths.get(path);
            Resource resource = new UrlResource(imagePath.toUri());
            
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
}
