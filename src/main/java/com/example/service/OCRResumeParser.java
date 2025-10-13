package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class OCRResumeParser {

    public String parseImageResume(MultipartFile imageFile) {
        try {
            log.info("开始OCR解析图片简历: {}", imageFile.getOriginalFilename());
            // 简化实现，实际项目可集成Tess4J
            // 这里返回模拟数据用于测试
            return "模拟OCR解析结果：张三\n邮箱：zhangsan@email.com\n技能：Java, Spring, MySQL\n工作经验：3年";
        } catch (Exception e) {
            log.error("OCR解析失败", e);
            return "OCR解析失败，请使用PDF或Word格式";
        }
    }

    public boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".bmp");
    }
}