package com.example.demo.controller;


import com.example.demo.entity.KnowledgeSummary;
import com.example.demo.repository.KnowledgeSummaryRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class KnowledgeController {

    @Autowired
    private KnowledgeSummaryRepository knowledgeSummaryRepository;

    // 上传Excel文件的接口
    @PostMapping("/excel")
    public Map<String, Object> uploadExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证文件格式
            String fileName = file.getOriginalFilename();
            if (!fileName.endsWith(".xlsx") &&!fileName.endsWith(".xls")) {
                result.put("success", false);
                result.put("message", "请上传Excel格式的文件");
                return result;
            }

            // 保存文件到服务器
            String uploadDir = "uploads/excel";
            Path directory = Paths.get(uploadDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(fileName);
            file.transferTo(filePath);

            // 解析Excel文件并插入数据到数据库
            List<KnowledgeSummary> knowledgeSummaries = parseExcelFile(filePath);
            knowledgeSummaryRepository.saveAll(knowledgeSummaries);

            result.put("success", true);
            result.put("message", "文件上传成功且数据已插入数据库");
            result.put("filePath", filePath.toString());


            // 尝试删除上传的Excel文件
            try {
                Files.deleteIfExists(filePath);
                result.put("deleteMessage", "上传的Excel文件已成功删除");
            } catch (IOException deleteException) {
                result.put("deleteMessage", "删除上传的Excel文件时出现错误: " + deleteException.getMessage());
            }


        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件上传失败: " + e.getMessage());
        }

        return result;
    }

    private List<KnowledgeSummary> parseExcelFile(Path filePath) throws IOException {
        List<KnowledgeSummary> knowledgeSummaries = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(filePath))) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // 跳过表头行
                }

                String category_level1 = getCellValue(row.getCell(0));
                String category_level2 = getCellValue(row.getCell(1));
                String knowledge = getCellValue(row.getCell(2));
                String subject = getCellValue(row.getCell(3));
                String number = "0"; // 设置默认值
                String stage = getCellValue(row.getCell(4));
                KnowledgeSummary knowledgeSummary = new KnowledgeSummary(category_level1, category_level2, knowledge, subject, number, stage);
                knowledgeSummaries.add(knowledgeSummary);
            }
        }

        return knowledgeSummaries;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }



    // 下载Excel模板的接口
    @GetMapping("/template/excel")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        Resource resource = new ClassPathResource("templates/知识分类模板.xlsx");

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 文件名
        String filename = "知识分类模板.xlsx";

        // 按照 RFC 6266 标准编码文件名
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20"); // 将 + 替换为 %20，符合 RFC 规范

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // 设置符合 RFC 6266 标准的 Content-Disposition 头
        headers.setContentDispositionFormData("attachment", encodedFilename);
        // 或者使用更标准的格式
        headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }



    /**
     * 分页 + 条件查询知识分类
     */
    @GetMapping("/knowledgeSummary/page")
    public ResponseEntity<Page<KnowledgeSummary>> pageQuery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categoryLevel1,
            @RequestParam(required = false) String categoryLevel2,
            @RequestParam(required = false) String knowledge, // 原categoryLevel3改为knowledge
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String number,    // 原questionCount改为number，类型改为String
            @RequestParam(required = false) String stage) {   // 原grade改为stage

        // 创建查询条件对象
        KnowledgeSummary probe = new KnowledgeSummary();
        if (categoryLevel1 != null) probe.setCategory_level1(categoryLevel1);
        if (categoryLevel2 != null) probe.setCategory_level2(categoryLevel2);
        if (knowledge != null) probe.setKnowledge(knowledge);
        if (subject != null) probe.setSubject(subject);
        if (number != null) probe.setNumber(number);       // 注意：实体类中number是String类型
        if (stage != null) probe.setStage(stage);

        // 构建匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                // 模糊查询字段（包含匹配）
                .withMatcher("category_level1", match -> match.contains())
                .withMatcher("category_level2", match -> match.contains())
                .withMatcher("knowledge", match -> match.contains())      // 修改字段名
                // 字符串类型处理（根据需求选择模糊或精准）
                .withMatcher("number", match -> match.exact())            // 题目数量改为字符串匹配
                // 精准查询字段
                .withMatcher("subject", match -> match.exact())
                .withMatcher("stage", match -> match.exact())             // 修改字段名
                .withIgnoreNullValues(); // 忽略null值字段

        // 创建查询示例
        Example<KnowledgeSummary> example = Example.of(probe, matcher);

        // 排序和分页
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);

        // 执行查询
        Page<KnowledgeSummary> dataPage = knowledgeSummaryRepository.findAll(example, pageable);
        return ResponseEntity.ok(dataPage);
    }

    /**
     * 3. 根据 ID 删除单条数据
     * 前端表格操作列删除按钮调用
     */
    @DeleteMapping("/knowledgeSummary/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteById(@PathVariable Integer id) {
        // 保持原代码不变
        Map<String, Object> result = new HashMap<>();
        try {
            knowledgeSummaryRepository.deleteById(id);
            result.put("success", true);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 4. 批量删除（根据 ID 列表）
     * 前端若支持多选删除，可调用此接口
     * @param ids 要删除的 ID 列表，格式：[1,2,3]
     */
    @DeleteMapping("/knowledgeSummary/batchDelete")
    public ResponseEntity<Map<String, String>> batchDelete(@RequestBody List<Integer> ids) {
        // 保持原代码不变
        Map<String, String> result = new HashMap<>();
        try {
            knowledgeSummaryRepository.deleteAllById(ids);
            result.put("message", "批量删除成功，共删除 " + ids.size() + " 条数据");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", "批量删除失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 5. 根据 ID 查询单条详情（编辑时回显数据用）
     */
    @GetMapping("/knowledgeSummary/{id}")
    public ResponseEntity<KnowledgeSummary> getById(@PathVariable Integer id) {
        // 保持原代码不变
        return knowledgeSummaryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 6. 新增/修改数据（编辑后提交用，根据 id 是否为空判断新增或修改）
     * 前端表单提交时调用
     */
    @PostMapping("/knowledgeSummary/save")
    public ResponseEntity<Map<String, String>> saveOrUpdate(@RequestBody KnowledgeSummary entity) {
        // 保持原代码不变
        Map<String, String> result = new HashMap<>();
        try {
            knowledgeSummaryRepository.save(entity);
            String message = entity.getId() == null ? "新增成功" : "修改成功";
            result.put("message", message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", "操作失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }



    @PutMapping("/updateCount/{id}")
    public ResponseEntity<?> updateQuestionCount(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {

        Optional<KnowledgeSummary> optional = knowledgeSummaryRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        KnowledgeSummary summary = optional.get();
        summary.setNumber(String.valueOf(request.get("number")));  // 转换为String
        knowledgeSummaryRepository.save(summary);

        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    // 在KnowledgeController中添加
    @GetMapping("/all")
    public ResponseEntity<List<KnowledgeSummary>> getAllKnowledgeSummaries() {
        return ResponseEntity.ok(knowledgeSummaryRepository.findAll());
    }

}

