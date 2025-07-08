package com.example.demo.service;

import com.example.demo.dto.QuestionSaveRequest;
import com.example.demo.entity.KnowledgeSummary;
import com.example.demo.entity.Question;
import com.example.demo.repository.KnowledgeSummaryRepository;
import com.example.demo.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final KnowledgeSummaryRepository knowledgeSummaryRepository;

    @Transactional
    public SaveResult saveQuestions(QuestionSaveRequest request) {
        if (request == null) {
            return new SaveResult(0, 0, "请求数据不能为空");
        }

        int totalCount = getTotalQuestionCount(request);
        int savedCount = 0;

        // 保存选择题
        if (request.getChoice() != null) {
            savedCount += saveQuestionList(request.getChoice(), request.getKnowledge(),
                    request.getSubject(), request.getStage(), "choice");
        }

        // 保存填空题
        if (request.getFill() != null) {
            savedCount += saveQuestionList(request.getFill(), request.getKnowledge(),
                    request.getSubject(), request.getStage(), "fill");
        }

        // 保存判断题
        if (request.getJudge() != null) {
            savedCount += saveQuestionList(request.getJudge(), request.getKnowledge(),
                    request.getSubject(), request.getStage(), "judge");
        }

        // 保存综合体
        if (request.getComprehensive() != null) {
            savedCount += saveQuestionList(request.getComprehensive(), request.getKnowledge(),
                    request.getSubject(), request.getStage(), "comprehensive");
        }

//        根据request.getKnowledge(),request.getSubject(), request.getStage()三个字段更新knowledge_summary数据表中的number值，number为savedCount
        // 更新知识点统计信息
//        updateKnowledgeSummary(request.getId(),request.getKnowledge(), request.getSubject(), request.getStage());

        return new SaveResult(totalCount, savedCount,
                "成功保存 " + savedCount + " 道题目，共 " + totalCount + " 道题目");
    }

    private int saveQuestionList(List<QuestionSaveRequest.QuestionData> questionDataList,
                                 String knowledge, String subject, String stage, String type) {
        if (questionDataList == null || questionDataList.isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        List<Question> questions = new ArrayList<>();

        for (QuestionSaveRequest.QuestionData data : questionDataList) {
            Question question = new Question();
            question.setKnowledge(knowledge);
            question.setSubject(subject);
            question.setStage(stage);
            question.setType(type);
            question.setDifficulty(data.getDifficulty());
            question.setQuestionText(data.getQuestion());
            question.setAnswer(data.getAnswer());
            question.setAnalysis(data.getAnalysis()); // 使用新的字段名

            // 处理选择题选项
            if ("choice".equals(type) && data.getOption() != null) {
                question.setOptions(String.join("|", data.getOption()));
            }

            questions.add(question);
        }
        System.out.println("=================questions============");
        System.out.println(questions);
        List<Question> savedQuestions = questionRepository.saveAll(questions);
        savedCount = savedQuestions.size();

        return savedCount;
    }

    private int getTotalQuestionCount(QuestionSaveRequest request) {
        int count = 0;
        if (request.getChoice() != null) {
            count += request.getChoice().size();
        }
        if (request.getFill() != null) {
            count += request.getFill().size();
        }
        if (request.getJudge() != null) {
            count += request.getJudge().size();
        }
        if (request.getComprehensive() != null) {
            count += request.getComprehensive().size();
        }
        return count;
    }


//    // 更新知识点统计信息
//    private void updateKnowledgeSummary(Integer id, String knowledge, String subject, String stage) {
//        // 使用findById而不是findAllById，因为ID应该是唯一的
//        Optional<KnowledgeSummary> summaryOptional = knowledgeSummaryRepository.findById(id);
//
//        List<Question> questions = questionRepository.findByKnowledgeAndSubjectAndStage(knowledge, subject, stage);
//
//        int count = questions.size();
//
//        if (!summaryOptional.isPresent()) {
//            throw new IllegalStateException("未发现ID为 " + id + " 的知识点统计记录");
//        }
//
//        KnowledgeSummary summary = summaryOptional.get();
//        summary.setNumber(String.valueOf(count));
//        knowledgeSummaryRepository.save(summary);
//    }


    public static class SaveResult {
        private int totalCount;      // 题目总数
        private int savedCount;      // 成功保存数量
        private String message;      // 结果信息

        public SaveResult(int totalCount, int savedCount, String message) {
            this.totalCount = totalCount;
            this.savedCount = savedCount;
            this.message = message;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSavedCount() {
            return savedCount;
        }

        public String getMessage() {
            return message;
        }
    }


//    详情页面查询数据
    public List<Question> getQuestionsByKnowledgeSubjectStage(String knowledge, String subject, String stage) {
        System.out.println("=========================getQuestionsByKnowledgeSubjectStage===============");
        return questionRepository.findByKnowledgeAndSubjectAndStage(knowledge, subject, stage);
    }
}