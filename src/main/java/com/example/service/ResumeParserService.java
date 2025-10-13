package com.example.service;


import com.example.model.ParsedResume;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ResumeParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?86)?1[3-9]\\d{9}|(\\d{3,4}-)?\\d{7,8}");
    private static final Pattern NAME_PATTERN = Pattern.compile("姓名[：:]\\s*([\\u4e00-\\u9fa5a-zA-Z]{2,10})");

    public ParsedResume parseResume(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        try {
            String filename = file.getOriginalFilename();
            String content;

            if (filename.toLowerCase().endsWith(".pdf")) {
                content = parsePdf(file.getInputStream());
            } else if (filename.toLowerCase().endsWith(".docx")) {
                content = parseDocx(file.getInputStream());
            } else if (filename.toLowerCase().endsWith(".txt")) {
                content = new String(file.getBytes(), "UTF-8");
            } else {
                throw new UnsupportedOperationException("不支持的文件格式: " + filename);
            }

            ParsedResume resume = extractResumeInfo(content);
            resume.setFileName(filename);

            long endTime = System.currentTimeMillis();
            log.info("简历解析完成: {}, 耗时: {}ms", filename, endTime - startTime);

            return resume;
        } catch (Exception e) {
            log.error("解析简历失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("解析失败: " + e.getMessage());
        }
    }

    private String parsePdf(InputStream inputStream) throws Exception {
        // 替换原来的 PDDocument.load(inputStream)
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private ParsedResume extractResumeInfo(String content) {
        ParsedResume resume = new ParsedResume();
        resume.setRawText(content);

        // 提取姓名
        var nameMatcher = NAME_PATTERN.matcher(content);
        if (nameMatcher.find()) {
            resume.getPersonalInfo().setName(nameMatcher.group(1));
        }

        // 提取邮箱
        var emailMatcher = EMAIL_PATTERN.matcher(content);
        if (emailMatcher.find()) {
            resume.getPersonalInfo().setEmail(emailMatcher.group());
        }

        // 提取电话
        var phoneMatcher = PHONE_PATTERN.matcher(content);
        if (phoneMatcher.find()) {
            resume.getPersonalInfo().setPhone(phoneMatcher.group());
        }

        // 提取技能
        resume.setSkills(extractSkills(content));

        // 估算工作经验
        resume.getPersonalInfo().setYearsOfExperience(estimateExperience(content));

        return resume;
    }

    public List<String> extractSkills(String content) {
        List<String> skillKeywords = Arrays.asList(
                "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Python", "JavaScript",
                "Vue", "React", "HTML", "CSS", "Docker", "Kubernetes", "Git", "Maven",
                "Gradle", "Linux", "AWS", "微服务", "分布式", "多线程", "RESTful", "API"
        );

        List<String> foundSkills = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (String skill : skillKeywords) {
            if (lowerContent.contains(skill.toLowerCase())) {
                foundSkills.add(skill);
            }
        }
        return foundSkills;
    }

    private Integer estimateExperience(String content) {
        // 简化的经验估算逻辑
        if (content.contains("5年") || content.contains("五年") || content.contains("5+")) return 5;
        if (content.contains("3年") || content.contains("三年") || content.contains("3+")) return 3;
        if (content.contains("1年") || content.contains("一年") || content.contains("1+")) return 1;
        return 0;
    }
}