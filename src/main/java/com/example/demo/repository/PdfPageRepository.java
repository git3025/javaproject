package com.example.demo.repository;

import com.example.demo.entity.PdfPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

@Repository
public interface PdfPageRepository extends JpaRepository<PdfPage, Long> {
    // 根据ISBN更新object_detection状态
    @Modifying
    @Transactional
    @Query("UPDATE PdfPage p SET p.objectDetection = 0 WHERE p.ISBN = :ISBN")
    int updateObjectDetectionByIsbn(String ISBN);
    /**
     * 根据 ISBN 和 bookPage 查询 PdfPage
     *
     * @param ISBN 书籍的 ISBN
     * @param bookPage 页码（如 "page1"）
     * @return Optional<PdfPage>
     */
    Optional<PdfPage> findByISBNAndBookPage(String ISBN, String bookPage);
    List<PdfPage> findByISBN(String ISBN);
}

