package com.example.demo.repository;

import com.example.demo.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends
        JpaRepository<Question, Long>,
        JpaSpecificationExecutor<Question> {


    List<Question> findByKnowledgeAndSubjectAndStage(String knowledge, String subject, String stage);

    List<Question> findByKnowledge(String knowledge);

}