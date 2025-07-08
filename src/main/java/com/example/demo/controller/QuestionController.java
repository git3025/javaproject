package com.example.demo.controller;

import com.example.demo.dto.QuestionSaveRequest;
import com.example.demo.entity.KnowledgeSummary;
import com.example.demo.entity.Question;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;


import javax.persistence.criteria.Root;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);
    private static final String COZE_API_URL = "https://api.coze.cn/v1/workflow/run";
    private static final String AUTH_TOKEN = "Bearer pat_IGe7yBZIbmrjXA88Oh6OvYwPZZ8JSBFm5yDTuCyPp3JUCMTpI3NHUsOjW5ufr8KD";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private QuestionService questionService;

    @PostMapping("/questions/generate")
    public ResponseEntity<Map<String, Object>> generateQuestions(
            @RequestBody KnowledgeSummary request) {

        logger.info("接收到题目生成请求: knowledge={}, stage={}, subject={}, number={}",
                request.getKnowledge(), request.getStage(), request.getSubject(), request.getNumber());

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> cozeRequest = new HashMap<>();
        cozeRequest.put("workflow_id", "7521945864991588404");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("knowledge", request.getKnowledge());
        parameters.put("stage", request.getStage());
        parameters.put("subject", request.getSubject());
        parameters.put("number", String.valueOf(request.getNumber()));

        cozeRequest.put("parameters", parameters);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", AUTH_TOKEN);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(cozeRequest, headers);

        try {
            // 发送请求并获取响应
            Map<String, Object> cozeResponse = restTemplate.postForObject(
                    COZE_API_URL, entity, Map.class);

            if (cozeResponse == null) {
                response.put("error", "Coze API返回空响应");
                logger.error("Coze API返回空响应");
                throw new IllegalStateException(response.get("error").toString());
            }

            logger.debug("Coze API响应: {}", cozeResponse);
            System.out.println("========================cozeResponse======================");
            System.out.println(cozeResponse);




            // 解析data字段(JSON字符串)
            if (!cozeResponse.containsKey("data")) {
                response.put("error", "Coze响应中缺少data字段");
                logger.error("Coze响应中缺少data字段");
                throw new IllegalStateException(response.get("error").toString());
            }

            String dataStr = (String) cozeResponse.get("data");
            System.out.println("========================dataStr======================");
            System.out.println(dataStr);

            Map<String, Object> data = objectMapper.readValue(dataStr, Map.class);
            System.out.println("========================data======================");
            System.out.println(data);


            // **新增：检查data中的error字段**
            if (data.containsKey("error") && data.get("error") != "") {
                response.put("error", data.get("error"));
                logger.error("Coze API业务错误: {}", data.get("error"));
                throw new IllegalStateException(response.get("error").toString());
            }


            // 转换各类题目
            if (data.containsKey("choice")) {
                response.put("choice", convertQuestions((List<Map<String, Object>>) data.get("choice"), "选择题"));
            }
            if (data.containsKey("fill")) {
                response.put("fill", convertQuestions((List<Map<String, Object>>) data.get("fill"), "填空题"));
            }
            if (data.containsKey("judge")) {
                response.put("judge", convertQuestions((List<Map<String, Object>>) data.get("judge"), "判断题"));
            }
            if (data.containsKey("comprehensive")) {
                response.put("comprehensive", convertQuestions((List<Map<String, Object>>) data.get("comprehensive"), "综合体"));
            }

            System.out.println("=======================调用完成，开始返回===================================");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // 处理API返回的业务错误（error字段非空）
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (HttpClientErrorException e) {
            // 处理4xx客户端错误（如请求参数错误）
            response.put("error", "客户端错误: " + e.getStatusCode() + " - " + e.getMessage());
            logger.error("客户端错误: {}", e.getMessage(), e);
            return ResponseEntity.status(e.getStatusCode()).body(response);
        } catch (HttpServerErrorException e) {
            // 处理5xx服务器错误（如API服务异常）
            response.put("error", "服务器错误: " + e.getStatusCode() + " - " + e.getMessage());
            logger.error("服务器错误: {}", e.getMessage(), e);
            return ResponseEntity.status(e.getStatusCode()).body(response);
        } catch (Exception e) {
            // 处理其他未知异常（如解析JSON失败）
            response.put("error", "系统错误: " + e.getMessage());
            logger.error("生成题目时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // 辅助方法：转换题目格式，增加更安全的null检查
    private List<Map<String, Object>> convertQuestions(List<Map<String, Object>> questions, String type) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (questions == null) {
            logger.warn("接收到空的题目列表，类型: {}", type);
            return result;
        }

        for (Map<String, Object> question : questions) {
            Map<String, Object> converted = new HashMap<>();
            converted.put("type", type);

            // 安全获取难度
            converted.put("dificulty", question.get("difficulty") != null ?
                    question.get("difficulty") : "未知难度");

            // 安全获取题目
            converted.put("question", question.get("question") != null ?
                    question.get("question") : "题目内容缺失");

            // 安全获取答案
            converted.put("answer", question.get("answer") != null ?
                    question.get("answer") : "答案缺失");

            // 安全获取解析
            converted.put("analysis", question.get("analysis") != null ?
                    question.get("analysis") : "解析缺失");


            // 处理选择题选项
            if (type.equals("选择题") && question.containsKey("option")) {
                Object optionsObj = question.get("option");
                List<String> options = new ArrayList<>();

                if (optionsObj instanceof String) {
                    // 处理字符串格式的选项
                    String optionsStr = (String) optionsObj;
                    // 按选项分隔符拆分（假设使用中文分号）
                    String[] rawOptions = optionsStr.split("；|;");
                    for (String opt : rawOptions) {
                        options.add(opt.trim());
                    }
                } else if (optionsObj instanceof List) {
                    // 处理已经是列表的情况
                    List<?> rawList = (List<?>) optionsObj;
                    for (Object opt : rawList) {
                        if (opt instanceof String) {
                            // 处理可能被拆分的选项
                            String strOpt = (String) opt;
                            if (strOpt.matches("^[A-Z]\\..*")) {
                                options.add(strOpt);
                            } else if (!options.isEmpty()) {
                                // 合并到上一个选项
                                String last = options.get(options.size()-1);
                                options.set(options.size()-1, last + strOpt);
                            }
                        }
                    }
                }

                // 确保选项格式正确（A.内容）
                List<String> formattedOptions = new ArrayList<>();
                for (String opt : options) {
                    if (!opt.matches("^[A-Z]\\..*") && !formattedOptions.isEmpty()) {
                        String last = formattedOptions.get(formattedOptions.size()-1);
                        formattedOptions.set(formattedOptions.size()-1, last + opt);
                    } else {
                        formattedOptions.add(opt);
                    }
                }

                converted.put("option", formattedOptions);
            }


            result.add(converted);
        }

        return result;
    }


    @PostMapping("/save")
    public ResponseEntity<QuestionService.SaveResult> saveQuestions(@RequestBody QuestionSaveRequest request) {
        try {
            logger.info("接收到题目保存请求，知识点: {}", request.getKnowledge());

            QuestionService.SaveResult result = questionService.saveQuestions(request);
            logger.info("题目保存完成，总数: {}, 成功: {}", result.getTotalCount(), result.getSavedCount());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("保存题目时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new QuestionService.SaveResult(0, 0, "保存题目时发生错误: " + e.getMessage()));
        }
    }




//    详情页面查询数据
    @GetMapping("/detail")
    public List<Question> getQuestions(@RequestParam String knowledge, @RequestParam String subject, @RequestParam String stage) {
        return questionService.getQuestionsByKnowledgeSubjectStage(knowledge, subject, stage);
    }

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }



    @PostMapping("/update")
    public Map<String, Object> updateQuestion(@Valid @RequestBody Question updatedQuestion) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("收到更新请求: " + updatedQuestion);

            Question existQuestion = questionRepository.findById(updatedQuestion.getId())
                    .orElseThrow(() -> new RuntimeException("题目不存在"));

            // 仅当传入的值不为 null 时才更新
            if (updatedQuestion.getQuestionText() != null) {
                existQuestion.setQuestionText(updatedQuestion.getQuestionText());
            }
            if (updatedQuestion.getAnswer() != null) {
                existQuestion.setAnswer(updatedQuestion.getAnswer());
            }
            if (updatedQuestion.getAnalysis() != null) {
                existQuestion.setAnalysis(updatedQuestion.getAnalysis());
            }
            if (updatedQuestion.getDifficulty() != null) {
                existQuestion.setDifficulty(updatedQuestion.getDifficulty());
            }
            if (updatedQuestion.getType() != null) {
                existQuestion.setType(updatedQuestion.getType());
            }
            if (updatedQuestion.getOptions() != null) {
                existQuestion.setOptions(updatedQuestion.getOptions());
            }

            System.out.println("即将保存的对象: " + existQuestion);
            questionRepository.save(existQuestion);

            result.put("code", 200);
            result.put("message", "题目修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "修改失败：" + e.getMessage());
        }
        return result;
    }


    // QuestionController.java 新增接口
    @PostMapping("/create")
    public Map<String, Object> createQuestion(@Valid @RequestBody Question newQuestion) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("收到新增题目请求: " + newQuestion);

            // 设置默认值（如果未提供）
            if (newQuestion.getCreateTime() == null) {
                newQuestion.setCreateTime(LocalDateTime.now());
            }
            if (newQuestion.getUpdateTime() == null) {
                newQuestion.setUpdateTime(LocalDateTime.now());
            }

            // 处理选择题选项（前端传递的是|分隔的字符串）
            if (newQuestion.getType() != null && newQuestion.getType().equals("choice")) {
                String optionsStr = newQuestion.getOptions();
                if (optionsStr != null && !optionsStr.isEmpty()) {
                    newQuestion.setOptions(optionsStr);
                }
            }

            Question savedQuestion = questionRepository.save(newQuestion);

            result.put("code", 200);
            result.put("message", "题目新增成功");
            result.put("data", savedQuestion.getId());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "新增失败：" + e.getMessage());
        }
        return result;
    }

//    删除详情页面具体题目的功能代码
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteQuestion(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Question> questionOptional = questionRepository.findById(id);

            // Java 8 兼容写法
            if (!questionOptional.isPresent()) {  // 替代 isEmpty()
                response.put("code", 404);
                response.put("message", "题目不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            questionRepository.deleteById(id);

            response.put("code", 200);
            response.put("message", "题目删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "删除题目时出错: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    // QuestionController.java
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countQuestions(
            @RequestParam(required = false) String level1,
            @RequestParam(required = false) String level2,
            @RequestParam(required = false) String knowledge,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String stage) {

        // 使用Specification组合条件（避免重复计数）
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 知识层级条件（满足任一即可）
            List<Predicate> knowledgePredicates = new ArrayList<>();
            if (StringUtils.hasText(level1)) {
                knowledgePredicates.add(cb.equal(root.get("knowledge"), level1));
            }
            if (StringUtils.hasText(level2)) {
                knowledgePredicates.add(cb.equal(root.get("knowledge"), level2));
            }
            if (StringUtils.hasText(knowledge)) {
                knowledgePredicates.add(cb.equal(root.get("knowledge"), knowledge));
            }
            if (!knowledgePredicates.isEmpty()) {
                predicates.add(cb.or(knowledgePredicates.toArray(new Predicate[0])));
            }

            // 学科和学段条件（必须满足）
            if (StringUtils.hasText(subject)) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }
            if (StringUtils.hasText(stage)) {
                predicates.add(cb.equal(root.get("stage"), stage));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        long totalCount = questionRepository.count(spec);
        return ResponseEntity.ok(Collections.singletonMap("count", totalCount));
    }

    private long countByCondition(String knowledgeCondition, String subject, String stage) {
        // 明确指定Specification的类型参数
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 修改后（精准查询）
            predicates.add(cb.equal(root.get("knowledge"), knowledgeCondition));

            if (StringUtils.hasText(subject)) {
                predicates.add(cb.equal(root.get("subject"), subject));
            }

            if (StringUtils.hasText(stage)) {
                predicates.add(cb.equal(root.get("stage"), stage));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return questionRepository.count(spec);
    }
}