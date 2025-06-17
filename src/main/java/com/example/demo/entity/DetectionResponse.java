package com.example.demo.entity;

import lombok.Data;

@Data
public class DetectionResponse {
    private boolean success;
    private String message;
    private String txtPath;
    private double processingTime;


    public DetectionResponse() {
    }

    public DetectionResponse(boolean success, String message, String txtPath, double processingTime) {
        this.success = success;
        this.message = message;
        this.txtPath = txtPath;
        this.processingTime = processingTime;
    }

    // 添加这个两个参数的构造函数
    public DetectionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        // 可选：为其他字段设置默认值
        this.txtPath = "";
        this.processingTime = 0.0;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTxtPath() {
        return txtPath;
    }

    public void setTxtPath(String txtPath) {
        this.txtPath = txtPath;
    }

    public double getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(double processingTime) {
        this.processingTime = processingTime;
    }
}