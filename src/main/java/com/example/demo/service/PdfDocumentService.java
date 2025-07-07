package com.example.demo.service;

import com.example.demo.entity.PdfDocument;
import com.example.demo.entity.PdfPage;
import com.example.demo.repository.PdfDocumentRepository;
import com.example.demo.utils.OssPathUtils;
import com.example.demo.utils.PdfUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PdfDocumentService {

    @Autowired
    private PdfDocumentRepository pdfDocumentRepository;

    public void updatePdfPages(Long id, String pdfFilePath) throws IOException {
        PdfDocument pdfDoc = pdfDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档未找到"));

        int pageCount = PdfUtils.getPdfPageCount(pdfFilePath);
        pdfDoc.setPages(pageCount);

        pdfDocumentRepository.save(pdfDoc);
    }

    String baseDir = "D:"+File.separator+"pdf";

    @Autowired
    private OssPathUtils ossPathUtils;
    @Autowired
    private OssService ossService;

    public PdfDocument saveAnotherDocument(PdfDocument document, MultipartFile file) throws IOException {
        // 保存到数据库，先生成ID
        PdfDocument savedDoc = pdfDocumentRepository.save(document);

        // 构建OSS路径 pdf/{isbn}/{fileName}.pdf
        String ossPath = ossPathUtils.buildPagePath(savedDoc.getISBN(), savedDoc.getFileName() + ".pdf");

        // 将MultipartFile保存为临时文件
        File tempFile = File.createTempFile(savedDoc.getFileName(), ".pdf");
        file.transferTo(tempFile);

        // 上传到OSS
        String ossUrl = ossService.uploadFile(tempFile.getAbsolutePath(), ossPath);
        tempFile.delete();

        // 更新数据库中的文件路径为OSS路径
        savedDoc.setFilePath(ossUrl);
        return pdfDocumentRepository.save(savedDoc);
    }

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PdfPageService pdfPageService;

    public List<PdfDocument> getAllDocuments() {
        return pdfDocumentRepository.findAll();
    }

    public Optional<PdfDocument> getDocumentById(Long id) {
        return pdfDocumentRepository.findById(id);
    }

    public Optional<PdfDocument> getDocumentByFileName(String fileName) {
        return Optional.ofNullable(pdfDocumentRepository.findByFileName(fileName));
    }

    public List<PdfDocument> searchByISBN(String ISBN) {
        return pdfDocumentRepository.findByISBN(ISBN);
    }

    public List<PdfDocument> searchByUploadTimeRange(LocalDateTime startDate, LocalDateTime endDate) {
        return pdfDocumentRepository.findByUploadTimeBetween(startDate, endDate);
    }

    public boolean existsByFileName(String fileName) {
        return pdfDocumentRepository.existsByFileName(fileName);
    }

    @Transactional
    public PdfDocument saveDocument(PdfDocument document, MultipartFile file) throws IOException {
        // 1. 校验文件名是否存在
        if (existsByFileName(file.getOriginalFilename())) {
            throw new RuntimeException("File with this name already exists");
        }

        // 2. 构建OSS路径 pdf/{isbn}/{fileName}.pdf
        String ossPath = ossPathUtils.buildPDFPath(document.getISBN(), document.getFileName() + ".pdf");

        // 3. 将MultipartFile保存为临时文件
        File tempFile = File.createTempFile(document.getFileName(), ".pdf");
        file.transferTo(tempFile);

        // 4. 上传到OSS
        String ossUrl = ossService.uploadFile(tempFile.getAbsolutePath(), ossPath);
        tempFile.delete();

        // 5. 设置文档属性
        document.setFileName(document.getFileName() + ".pdf");      // 设置文件名为书籍名称.pdf
        document.setFilePath(ossUrl);        // 设置文件路径为OSS路径
        document.setUploadTime(LocalDateTime.now());

        // 6. 保存文档到数据库
        return pdfDocumentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(Long id) throws Exception {
        Optional<PdfDocument> document = pdfDocumentRepository.findById(id);
        if (document.isPresent()) {
            //pdfPageService.deletePagesByDocumentId(id);
            fileStorageService.deleteFile(document.get().getFileName());
            pdfDocumentRepository.deleteById(id);
        }
    }

    @Transactional
    public PdfDocument updateDocument(Long id, PdfDocument document) {
        if (pdfDocumentRepository.existsById(id)) {
            document.setId(id);
            return pdfDocumentRepository.save(document);
        }
        throw new RuntimeException("Document not found with id: " + id);
    }

    public Optional<PdfDocument> findByIdAndFileName(Long id, String fileName) {
        return pdfDocumentRepository.findByIdAndFileName(id, fileName);
    }

    @Transactional
    public void updateById(Long id, PdfDocument updatedFields) {
        Optional<PdfDocument> optionalDoc = pdfDocumentRepository.findById(id);
        if (!optionalDoc.isPresent()) {
            throw new IllegalArgumentException("文档不存在，ID: " + id);
        }

        PdfDocument existingDoc = optionalDoc.get();

        // 只更新非空字段
        if (updatedFields.getTxtPath() != null) {
            // 修正：使用 setter 而非 getter
            existingDoc.setTxtPath(updatedFields.getTxtPath());
        }

        // 可以添加其他字段的更新逻辑

        pdfDocumentRepository.save(existingDoc);
    }

}