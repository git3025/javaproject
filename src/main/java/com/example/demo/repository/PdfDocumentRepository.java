package com.example.demo.repository;

import com.example.demo.entity.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    
    PdfDocument findByFileName(String fileName);
    

    List<PdfDocument> findByISBN(String isbn);
    
    @Query("SELECT d FROM PdfDocument d WHERE d.uploadTime >= :startDate AND d.uploadTime <= :endDate")
    List<PdfDocument> findByUploadTimeBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    boolean existsByFileName(String fileName);
    
    Optional<PdfDocument> findByIdAndFileName(Long id, String fileName);

} 