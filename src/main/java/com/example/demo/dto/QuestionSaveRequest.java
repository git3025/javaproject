package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionSaveRequest {
    private Integer id;
    private String knowledge;  // 知识点
    private String subject;    // 学科
    private String stage;      // 学段

    private List<QuestionData> choice;      // 选择题
    private List<QuestionData> fill;        // 填空题
    private List<QuestionData> judge;       // 判断题
    private List<QuestionData> comprehensive; // 综合体

    @Data
    public static class QuestionData {
        private String difficulty;    // 难度
        private String question;      // 题目内容
        private String answer;        // 答案
        private String analysis;      // 解析（原 analyze）
        private List<String> option;  // 选项(仅选择题)
    }
}
