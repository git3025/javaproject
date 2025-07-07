package com.example.demo.controller;

import com.example.demo.entity.PdfDocument;
import com.example.demo.entity.PdfPage;
import com.example.demo.service.OssService;
import com.example.demo.service.PdfDocumentService;
import com.example.demo.service.PdfPageService; // 导入 PdfPageService
import com.example.demo.utils.OssPathUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mysql.cj.conf.PropertyKey.logger;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin
public class PdfUploadController {

    @Autowired
    private PdfDocumentService pdfDocumentService;

    @Autowired
    private PdfPageService pdfPageService; // 注入 PdfPageService

    @Autowired
    private OssPathUtils ossPathUtils;

    @Autowired
    private OssService ossService;

    @GetMapping
    public ResponseEntity<List<PdfDocument>> getAllDocuments() {
        List<PdfDocument> documents = pdfDocumentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/upload")
    public ResponseEntity<PdfDocument> uploadPdf(
            @RequestParam("fileName") String fileName,
            @RequestParam("subject") String subject,
            @RequestParam("isbn") String isbn,
            @RequestParam("grade") String grade,
            @RequestParam("file") MultipartFile file) {
        try {
            PdfDocument document = new PdfDocument();
            document.setFileName(fileName);
            document.setSubject(subject);
            document.setISBN(isbn);
            document.setGrade(grade);

            PdfDocument saved = pdfDocumentService.saveDocument(document, file);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/split")
    public ResponseEntity<String> splitPdf(@RequestParam("id") Long id) {
        try {
            Optional<PdfDocument> documentOpt = pdfDocumentService.getDocumentById(id);
            if (!documentOpt.isPresent()) {
                return ResponseEntity.badRequest().body("文档不存在");
            }

            PdfDocument document = documentOpt.get();

            // 判断是否已经切割过
            if (document.getSlicing() == 0) {
                return ResponseEntity.badRequest().body("该文件已切割，不允许重复切割");
            }

            // 1. 先从 OSS 下载 PDF 到本地临时文件
            String ossUrl = document.getFilePath();
            File tempPdf = File.createTempFile("pdf_download_", ".pdf");
            try (java.io.InputStream in = new java.net.URL(ossUrl).openStream()) {
                java.nio.file.Files.copy(in, tempPdf.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 用 PDFBox 处理本地临时文件
            org.apache.pdfbox.pdmodel.PDDocument pdDocument = org.apache.pdfbox.pdmodel.PDDocument.load(tempPdf);
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(pdDocument);
            int pageCount = pdDocument.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300); // DPI=300
                // 1. 生成临时本地图片
                File tempFile = File.createTempFile("page" + (i + 1), ".png");
                ImageIO.write(image, "PNG", tempFile);

                // 2. 构建 OSS 路径
                String ossPath = ossPathUtils.buildPagePath(document.getISBN(), "page" + (i + 1) + ".png");

                // 3. 上传到 OSS
                String ossUrlPage = ossService.uploadFile(tempFile.getAbsolutePath(), ossPath);

                // 4. 保存 OSS 路径到数据库
                PdfPage page = new PdfPage();
                page.setISBN(document.getISBN());
                page.setBookName(document.getFileName());
                page.setSubject(document.getSubject());
                page.setBookPage("page" + (i + 1));
                page.setGrade(document.getGrade());
                page.setBookPath(ossUrlPage); // 这里必须是完整的OSS URL
                page.setObjectDetection(1); // 默认启用
                pdfPageService.updatePage(page);

                // 5. 删除临时文件
                tempFile.delete();
            }
            pdDocument.close();
            tempPdf.delete();

            // 更新数据库字段 slicing = 0 表示"已切割"
            document.setSlicing(0);
            document.setPages(pageCount);
            pdfDocumentService.updateDocument(document.getId(), document);

            return ResponseEntity.ok("PDF 已成功转换为图片，已上传至 OSS");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("切割失败：" + e.getMessage());
        }
    }

    private int countFilesInDirectory(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }
        File[] files = folder.listFiles();
        return (files == null) ? 0 : files.length;
    }

    private List<String> convertPdfToImages(String pdfPath) throws IOException {
        PDDocument document = PDDocument.load(new File(pdfPath));
        PDFRenderer renderer = new PDFRenderer(document);

        List<String> imagePaths = new ArrayList<>();
        String outputDir = "uploads/images/";

        File dir = new File(outputDir);
        boolean created = dir.mkdirs();
        if (!created) {
            System.err.println("Failed to create directory: " + dir.getAbsolutePath());
        }

        int pageCount = document.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300); // DPI=300
            String imagePath = outputDir + "page-" + (i + 1) + ".png";
            ImageIO.write(image, "PNG", new File(imagePath));
            imagePaths.add(imagePath);
        }

        document.close();
        return imagePaths;
    }
}