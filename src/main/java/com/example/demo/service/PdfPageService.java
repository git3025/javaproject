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

    public PdfPage updatePage(PdfPage page) {
        return pdfPageRepository.save(page);
    }





}