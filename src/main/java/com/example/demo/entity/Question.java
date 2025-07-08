package com.example.demo.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge", nullable = false)
    private String knowledge;  // 知识点

    @Column(name = "subject", nullable = false)
    private String subject;    // 学科

    @Column(name = "stage", nullable = false)
    private String stage;      // 学段

    @Column(name = "type", nullable = false)
    private String type;       // 题目类型(choice/fill/judge/comprehensive)

    @Column(name = "difficulty", nullable = false)
    private String difficulty; // 难度(简单/中等/困难)

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText; // 题目内容

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;    // 选项内容(仅选择题使用，用 | 分隔)

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;     // 正确答案

    // 修改字段名和列名
    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;    // 题目解析

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;


    public Question() {
    }

    public Question(Long id, String knowledge, String subject, String stage, String type, String difficulty, String questionText, String options, String answer, String analysis, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.knowledge = knowledge;
        this.subject = subject;
        this.stage = stage;
        this.type = type;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.options = options;
        this.answer = answer;
        this.analysis = analysis;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(String knowledge) {
        this.knowledge = knowledge;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}