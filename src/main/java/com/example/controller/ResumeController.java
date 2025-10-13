package com.example.controller;


import com.example.model.MatchResult;
import com.example.model.ParsedResume;
import com.example.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeParserService resumeParserService;
    private final OCRResumeParser ocrResumeParser;
    private final JobMatchingService jobMatchingService;
    private final AdvancedMatchingService advancedMatchingService;

    @PostMapping("/upload")
    public ResponseEntity<ParsedResume> uploadResume(@RequestParam("file") MultipartFile file) {
        log.info("收到简历上传请求，文件名: {}, 大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        ParsedResume resume = resumeParserService.parseResume(file);
        return ResponseEntity.ok(resume);
    }

    @PostMapping("/basic-match")
    public ResponseEntity<MatchResult> basicMatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "industry", required = false) String industry) {

        log.info("基础匹配请求，文件: {}, 行业: {}", file.getOriginalFilename(), industry);

        ParsedResume resume = parseResumeWithOCR(file);
        MatchResult result = jobMatchingService.basicMatch(resume, industry);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/advanced-match")
    public ResponseEntity<MatchResult> advancedMatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "industry", required = false) String industry) {

        log.info("高级匹配请求，文件: {}, 行业: {}", file.getOriginalFilename(), industry);

        ParsedResume resume = parseResumeWithOCR(file);
        MatchResult result = advancedMatchingService.advancedMatch(resume, industry);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/compare-match")
    public ResponseEntity<Map<String, Object>> compareMatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "industry", required = false) String industry) {

        log.info("对比匹配请求，文件: {}, 行业: {}", file.getOriginalFilename(), industry);

        ParsedResume resume = parseResumeWithOCR(file);

        MatchResult basicResult = jobMatchingService.basicMatch(resume, industry);
        MatchResult advancedResult = advancedMatchingService.advancedMatch(resume, industry);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("basicMatch", basicResult);
        comparison.put("advancedMatch", advancedResult);
        comparison.put("resume", resume);

        return ResponseEntity.ok(comparison);
    }

    private ParsedResume parseResumeWithOCR(MultipartFile file) {
        if (ocrResumeParser.isImageFile(file.getOriginalFilename())) {
            log.info("检测到图片简历，启动OCR解析: {}", file.getOriginalFilename());

            // 检查OCR功能是否可用
            if (!ocrResumeParser.isOCRAvailable()) {
                log.warn("OCR功能未完全配置，使用基础文本处理");
                return createBasicResumeFromImage(file);
            }

            try {
                String ocrText = ocrResumeParser.parseImageResume(file);
                log.info("OCR识别结果字符数: {}", ocrText.length());

                // 使用专门的图片简历解析方法
                return resumeParserService.parseImageResume(file, ocrText);

            } catch (Exception e) {
                log.error("OCR解析失败，回退到基础处理", e);
                return createBasicResumeFromImage(file);
            }
        } else {
            return resumeParserService.parseResume(file);
        }
    }

    /**
     * OCR不可用时的备选方案
     */
    private ParsedResume createBasicResumeFromImage(MultipartFile file) {
        ParsedResume resume = new ParsedResume();
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText("图片简历 - 需要OCR功能支持完整解析");

        // 设置基础信息
        resume.getPersonalInfo().setName("待识别");
        resume.setSkills(Arrays.asList("图片简历技能待识别"));

        return resume;
    }

    @GetMapping("/ocr-status")
    public ResponseEntity<Map<String, Object>> getOCRStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean ocrAvailable = ocrResumeParser.isOCRAvailable();
        status.put("ocrAvailable", ocrAvailable);
        status.put("tessdataPath", "./tessdata");

        // 检查语言包文件
        File tessdataDir = new File("./tessdata");
        if (tessdataDir.exists()) {
            String[] languagePacks = tessdataDir.list((dir, name) -> name.endsWith(".traineddata"));
            status.put("languagePacks", languagePacks != null ? Arrays.asList(languagePacks) : Collections.emptyList());
        } else {
            status.put("languagePacks", Collections.emptyList());
        }

        status.put("supportedImageFormats", Arrays.asList("jpg", "jpeg", "png", "bmp", "tiff", "tif"));

        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "智能简历解析与匹配系统");
        status.put("version", "1.0.0");
        status.put("environment", "JDK 17 + Spring Boot 3.2.0");
        status.put("algorithm", "多算法集成(TF-IDF + Jaccard + 语义匹配)");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/algorithms")
    public ResponseEntity<Map<String, Object>> getAlgorithmsInfo() {
        Map<String, Object> algorithms = new HashMap<>();

        algorithms.put("basic", Map.of(
                "name", "基础技能匹配",
                "description", "基于技能交集和经验的简单匹配算法",
                "features", Arrays.asList("技能匹配", "经验评估")
        ));

        algorithms.put("advanced", Map.of(
                "name", "多算法集成匹配",
                "description", "集成TF-IDF、加权Jaccard和语义相似度的智能匹配",
                "features", Arrays.asList("TF-IDF文本相似度", "加权Jaccard技能匹配",
                        "语义分类匹配", "动态权重调整")
        ));

        return ResponseEntity.ok(algorithms);
    }
}