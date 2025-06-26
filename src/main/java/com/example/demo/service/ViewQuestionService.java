package com.example.demo.service;

import com.example.demo.entity.ViewQuestion;
import com.example.demo.repository.ViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ViewQuestionService {

    @Autowired
    private ViewRepository viewRepository;

    public ViewQuestion save(ViewQuestion viewQuestion) {
        return viewRepository.save(viewQuestion);
    }

    public List<ViewQuestion> findByIsbnAndPage(String isbn, String page) {
        // 将 page 转换为 Integer
        Integer pageNum = Integer.parseInt(page);
        return viewRepository.findByISBNAndPages(isbn, pageNum);
    }

    public List<ViewQuestion> findAll() {
        return viewRepository.findAll();
    }

    public Optional<ViewQuestion> findById(Long id) {
        return viewRepository.findById(id);
    }

    public void deleteById(Long id) {
        viewRepository.deleteById(id);
    }
}