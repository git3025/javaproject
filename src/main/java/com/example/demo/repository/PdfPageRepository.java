package com.example.demo.repository;

import com.example.demo.entity.PdfPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface PdfPageRepository extends JpaRepository<PdfPage, Long> {
    // 根据ISBN更新object_detection状态
    @Modifying
    @Transactional
    @Query("UPDATE PdfPage p SET p.objectDetection = 0 WHERE p.ISBN = :isbn")
    int updateObjectDetectionByIsbn(String isbn);

}