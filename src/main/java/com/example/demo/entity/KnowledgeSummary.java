package com.example.demo.entity;

import lombok.Data;
import javax.persistence.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
public class KnowledgeSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String category_level1;
    private String category_level2;
    private String knowledge;
    private String subject;
    private String number;
    private String stage;


    public KnowledgeSummary() {
    }


    public KnowledgeSummary(Integer id, String category_level1, String category_level2, String knowledge, String subject, String number, String stage) {
        this.id = id;
        this.category_level1 = category_level1;
        this.category_level2 = category_level2;
        this.knowledge = knowledge;
        this.subject = subject;
        this.number = number;
        this.stage = stage;
    }

    public KnowledgeSummary(String category_level1, String category_level2, String knowledge, String subject, String number, String stage) {
        this.category_level1 = category_level1;
        this.category_level2 = category_level2;
        this.knowledge = knowledge;
        this.subject = subject;
        this.number = number;
        this.stage = stage;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategory_level1() {
        return category_level1;
    }

    public void setCategory_level1(String category_level1) {
        this.category_level1 = category_level1;
    }

    public String getCategory_level2() {
        return category_level2;
    }

    public void setCategory_level2(String category_level2) {
        this.category_level2 = category_level2;
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}