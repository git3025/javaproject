package com.example.demo.entity;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Entity
@Table(name = "pdf_pages")
@Data
public class PdfPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("ISBN") // 明确指定JSON字段名
    @Column
    private String ISBN;

    @Column
    private String bookName;

    @Column
    private String subject;

    @Column
    private String bookPage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBookPage() {
        return bookPage;
    }

    public void setBookPage(String bookPage) {
        this.bookPage = bookPage;
    }

    public String getBookPath() {
        return bookPath;
    }

    public void setBookPath(String bookPath) {
        this.bookPath = bookPath;
    }


    @Column
    private String bookPath;

    public Integer getObjectDetection() {
        return objectDetection;
    }

    public void setObjectDetection(Integer objectDetection) {
        this.objectDetection = objectDetection;
    }

    @Column
    private Integer objectDetection;
} 