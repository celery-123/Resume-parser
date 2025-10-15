package com.example.model;

import lombok.Data;
import java.util.List;

@Data
public class JobMatch {
    private JobPosition job;
    private double matchScore;
    private String matchReason;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private AlgorithmScores algorithmScores;

    @Data
    public static class AlgorithmScores {
        private double tfidfScore;
        private double jaccardScore;
        private double semanticScore;
        private double weightedScore;
    }
}