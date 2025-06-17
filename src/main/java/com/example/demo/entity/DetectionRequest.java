package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DetectionRequest {
    private Long id;
    private String folderPath;
    private String folderIdentifier;
    @JsonProperty("ISBN") // 明确指定JSON字段名
    private String ISBN;
    private Integer slicing;


    public DetectionRequest() {
    }

    public DetectionRequest(Long id, String folderPath, String folderIdentifier, String ISBN, Integer slicing) {
        this.id = id;
        this.folderPath = folderPath;
        this.folderIdentifier = folderIdentifier;
        this.ISBN = ISBN;
        this.slicing = slicing;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getFolderIdentifier() {
        return folderIdentifier;
    }

    public void setFolderIdentifier(String folderIdentifier) {
        this.folderIdentifier = folderIdentifier;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public Integer getSlicing() {
        return slicing;
    }

    public void setSlicing(Integer slicing) {
        this.slicing = slicing;
    }
}




