package com.example.demo.repository;

import com.example.demo.entity.KnowledgeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSummaryRepository extends JpaRepository<KnowledgeSummary, Integer> {
    // 测试方法
    @Query("SELECT k FROM KnowledgeSummary k WHERE k.category_level1 IS NOT NULL")
    List<KnowledgeSummary> findAllWithCategory();

    // 根据知识点、学科和学段查询统计记录
    List<KnowledgeSummary> findByKnowledgeAndSubjectAndStage(
            String knowledge, String subject, String stage);

    // 根据ID查询单个记录
    Optional<KnowledgeSummary> findById(Integer id);
}