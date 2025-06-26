package com.example.demo.service;

import com.example.demo.entity.DetectionRequest;
import com.example.demo.entity.DetectionResponse;
import com.example.demo.service.PdfPageService;
import com.example.demo.entity.PdfPage;
import com.example.demo.repository.PdfPageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class PdfPageService {

    @Autowired
    private PdfPageRepository pdfPageRepository;

    public List<PdfPage> getAllPages() {
        return pdfPageRepository.findAll();
    }

    public Optional<PdfPage> getPageById(Long id) {
        return pdfPageRepository.findById(id);
    }
    public List<PdfPage> getPageByISBN(String ISBN) {
        return pdfPageRepository.findByISBN(ISBN);
    }

    public PdfPage updatePage(PdfPage page) {
        return pdfPageRepository.save(page);
    }

    /**
     * 根据 ISBN 和页码查询 PDF 页面数据
     *
     * @param isbn 书籍的 ISBN 码
     * @param page 页码（例如 "page1"）
     * @return Optional<PdfPage> 包含匹配的 PdfPage 对象或为空
     */
    public Optional<PdfPage> getPageByISBNAndPage(String isbn, String page) {
        if (isbn == null || isbn.isEmpty() || page == null || page.isEmpty()) {
            throw new IllegalArgumentException("ISBN 或页码不能为空");
        }

        // 调用 repository 方法
        return pdfPageRepository.findByISBNAndBookPage(isbn, page);
    }
}
