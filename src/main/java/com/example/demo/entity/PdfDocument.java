package com.example.demo.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pdf_documents")
@Data
public class PdfDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;


    @Column
    private String subject;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Integer slicing = 1;

    private String slicingPath;

    private String txtPath;

    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @Column
    private String ISBN;

    @Column
    private long pages;

    @Column
    private String grade;

    public PdfDocument() {
    }

    public PdfDocument(Long id, String title, String subject, String fileName, String filePath, Integer slicing, String slicingPath, String txtPath, LocalDateTime uploadTime, String ISBN, long pages) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.fileName = fileName;
        this.filePath = filePath;
        this.slicing = slicing;
        this.slicingPath = slicingPath;
        this.txtPath = txtPath;
        this.uploadTime = uploadTime;
        this.ISBN = ISBN;
        this.pages = pages;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getSlicing() {
        return slicing;
    }

    public void setSlicing(Integer slicing) {
        this.slicing = slicing;
    }

    public String getSlicingPath() {
        return slicingPath;
    }

    public void setSlicingPath(String slicingPath) {
        this.slicingPath = slicingPath;
    }

    public String getTxtPath() {
        return txtPath;
    }

    public void setTxtPath(String txtPath) {
        this.txtPath = txtPath;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }
}