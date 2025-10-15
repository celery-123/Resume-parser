package com.example.model;

import lombok.Data;

@Data
public class ExtractionResult {
    private String value;
    private double confidence; // 0.0 - 1.0
    private String method; // "pattern", "heuristic", "context", "none"
    private String explanation;

    public ExtractionResult(String value, double confidence, String method) {
        this.value = value;
        this.confidence = confidence;
        this.method = method;
        this.explanation = generateExplanation(method, confidence);
    }

    public ExtractionResult(String value, double confidence, String method, String explanation) {
        this.value = value;
        this.confidence = confidence;
        this.method = method;
        this.explanation = explanation;
    }

    private String generateExplanation(String method, double confidence) {
        switch (method) {
            case "pattern":
                return String.format("通过标准格式匹配提取，置信度 %.1f%%", confidence * 100);
            case "heuristic":
                return String.format("通过启发式规则提取，置信度 %.1f%%", confidence * 100);
            case "context":
                return String.format("通过上下文分析提取，置信度 %.1f%%", confidence * 100);
            case "none":
                return "未找到匹配的姓名信息";
            default:
                return String.format("通过%s方法提取，置信度 %.1f%%", method, confidence * 100);
        }
    }

    public boolean isValid() {
        return value != null && !value.trim().isEmpty() && confidence > 0.3;
    }
}
