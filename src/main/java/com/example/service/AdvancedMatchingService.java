package com.example.service;


import com.example.model.JobMatch;
import com.example.model.JobPosition;
import com.example.model.MatchResult;
import com.example.model.ParsedResume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdvancedMatchingService {

    // 技能权重配置
    private static final Map<String, Double> SKILL_WEIGHTS = Map.ofEntries(
            Map.entry("Java", 1.0), Map.entry("Spring", 0.9), Map.entry("Spring Boot", 0.9),
            Map.entry("MySQL", 0.8), Map.entry("Redis", 0.7), Map.entry("Python", 0.8),
            Map.entry("JavaScript", 0.8), Map.entry("Vue", 0.7), Map.entry("React", 0.7),
            Map.entry("Docker", 0.6), Map.entry("Kubernetes", 0.6), Map.entry("Linux", 0.5)
    );

    // 技能分类树
    private static final Map<String, List<String>> SKILL_CATEGORIES = Map.of(
            "后端开发", Arrays.asList("Java", "Spring", "MySQL", "Redis", "Python"),
            "前端开发", Arrays.asList("JavaScript", "Vue", "React", "HTML", "CSS"),
            "运维开发", Arrays.asList("Docker", "Kubernetes", "Linux", "AWS")
    );

    public MatchResult advancedMatch(ParsedResume resume, String industry) {
        long startTime = System.currentTimeMillis();

        List<JobPosition> relevantJobs = getJobDatabase().stream()
                .filter(job -> industry == null || industry.equalsIgnoreCase(job.getIndustry()))
                .collect(Collectors.toList());

        List<JobMatch> matches = relevantJobs.stream()
                .map(job -> createAdvancedJobMatch(resume, job))
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());

        MatchResult result = new MatchResult();
        result.setResume(resume);
        result.setJobMatches(matches);
        result.setAlgorithmUsed("多算法集成(TF-IDF + Jaccard + 语义匹配)");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setAnalysis(generateAnalysis(matches));

        log.info("高级匹配完成，处理时间: {}ms, 匹配岗位数: {}",
                result.getProcessingTimeMs(), matches.size());

        return result;
    }

    private JobMatch createAdvancedJobMatch(ParsedResume resume, JobPosition job) {
        // 多算法计算
        double tfidfScore = calculateTFIDFSimilarity(resume, job);
        double jaccardScore = calculateWeightedJaccard(resume, job);
        double semanticScore = calculateSemanticSimilarity(resume, job);

        // 算法集成
        double finalScore = integrateScores(tfidfScore, jaccardScore, semanticScore);

        List<String> matchedSkills = findMatchedSkills(resume.getSkills(), job.getRequiredSkills());
        List<String> missingSkills = findMissingSkills(resume.getSkills(), job.getRequiredSkills());

        JobMatch match = new JobMatch();
        match.setJob(job);
        match.setMatchScore(finalScore);
        match.setMatchedSkills(matchedSkills);
        match.setMissingSkills(missingSkills);
        match.setMatchReason(generateMatchReason(finalScore, matchedSkills.size()));

        // 设置算法得分详情
        JobMatch.AlgorithmScores algorithmScores = new JobMatch.AlgorithmScores();
        algorithmScores.setTfidfScore(tfidfScore);
        algorithmScores.setJaccardScore(jaccardScore);
        algorithmScores.setSemanticScore(semanticScore);
        algorithmScores.setWeightedScore(finalScore);
        match.setAlgorithmScores(algorithmScores);

        return match;
    }

    private double calculateTFIDFSimilarity(ParsedResume resume, JobPosition job) {
        // 简化的TF-IDF实现
        String resumeText = resume.getRawText().toLowerCase();
        String jobText = job.getDescription().toLowerCase() + " " + String.join(" ", job.getRequiredSkills()).toLowerCase();

        Map<String, Double> resumeVector = computeSimpleTFIDF(resumeText);
        Map<String, Double> jobVector = computeSimpleTFIDF(jobText);

        return computeCosineSimilarity(resumeVector, jobVector);
    }

    private Map<String, Double> computeSimpleTFIDF(String text) {
        Map<String, Double> tfidf = new HashMap<>();
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // 过滤短词
                tfidf.put(word, tfidf.getOrDefault(word, 0.0) + 1.0);
            }
        }

        // 归一化
        double norm = Math.sqrt(tfidf.values().stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) {
            for (String key : tfidf.keySet()) {
                tfidf.put(key, tfidf.get(key) / norm);
            }
        }

        return tfidf;
    }

    private double computeCosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : vectorA.keySet()) {
            if (vectorB.containsKey(term)) {
                dotProduct += vectorA.get(term) * vectorB.get(term);
            }
            normA += Math.pow(vectorA.get(term), 2);
        }

        for (Double value : vectorB.values()) {
            normB += Math.pow(value, 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double calculateWeightedJaccard(ParsedResume resume, JobPosition job) {
        List<String> resumeSkills = resume.getSkills();
        List<String> jobSkills = job.getRequiredSkills();

        if (jobSkills.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(resumeSkills);
        intersection.retainAll(jobSkills);

        Set<String> union = new HashSet<>(resumeSkills);
        union.addAll(jobSkills);

        double weightedIntersection = intersection.stream()
                .mapToDouble(skill -> SKILL_WEIGHTS.getOrDefault(skill, 0.5))
                .sum();

        double weightedUnion = union.stream()
                .mapToDouble(skill -> SKILL_WEIGHTS.getOrDefault(skill, 0.5))
                .sum();

        return weightedUnion > 0 ? weightedIntersection / weightedUnion : 0.0;
    }

    private double calculateSemanticSimilarity(ParsedResume resume, JobPosition job) {
        // 基于技能分类树的语义匹配
        double categoryOverlap = calculateCategoryOverlap(resume.getSkills(), job.getRequiredSkills());
        double experienceMatch = calculateExperienceMatch(resume, job);

        return (categoryOverlap * 0.7 + experienceMatch * 0.3);
    }

    private double calculateCategoryOverlap(List<String> resumeSkills, List<String> jobSkills) {
        Set<String> resumeCategories = new HashSet<>();
        Set<String> jobCategories = new HashSet<>();

        // 将技能映射到分类
        for (String skill : resumeSkills) {
            SKILL_CATEGORIES.forEach((category, skills) -> {
                if (skills.contains(skill)) {
                    resumeCategories.add(category);
                }
            });
        }

        for (String skill : jobSkills) {
            SKILL_CATEGORIES.forEach((category, skills) -> {
                if (skills.contains(skill)) {
                    jobCategories.add(category);
                }
            });
        }

        if (jobCategories.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(resumeCategories);
        intersection.retainAll(jobCategories);

        return (double) intersection.size() / jobCategories.size();
    }

    private double calculateExperienceMatch(ParsedResume resume, JobPosition job) {
        Integer resumeExp = resume.getPersonalInfo().getYearsOfExperience();
        if (resumeExp == null) return 0.3;
        if (job.getMinExperience() == null) return 0.5;

        if (resumeExp >= job.getMinExperience()) return 1.0;
        return (double) resumeExp / job.getMinExperience();
    }

    private double integrateScores(double tfidf, double jaccard, double semantic) {
        // 动态权重调整
        return tfidf * 0.4 + jaccard * 0.35 + semantic * 0.25;
    }

    private List<String> findMatchedSkills(List<String> resumeSkills, List<String> jobSkills) {
        return resumeSkills.stream()
                .filter(resumeSkill -> jobSkills.stream()
                        .anyMatch(jobSkill -> jobSkill.equalsIgnoreCase(resumeSkill)))
                .collect(Collectors.toList());
    }

    private List<String> findMissingSkills(List<String> resumeSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(jobSkill -> resumeSkills.stream()
                        .noneMatch(resumeSkill -> resumeSkill.equalsIgnoreCase(jobSkill)))
                .collect(Collectors.toList());
    }

    private String generateMatchReason(double score, int matchedSkillCount) {
        if (score >= 0.8) return String.format("高度匹配(%.1f%%)，具备%d项核心技能", score * 100, matchedSkillCount);
        if (score >= 0.6) return String.format("良好匹配(%.1f%%)，掌握%d项主要技能", score * 100, matchedSkillCount);
        if (score >= 0.4) return String.format("一般匹配(%.1f%%)，具备%d项相关技能", score * 100, matchedSkillCount);
        return String.format("匹配度较低(%.1f%%)，建议加强技能学习", score * 100);
    }

    private String generateAnalysis(List<JobMatch> matches) {
        if (matches.isEmpty()) return "暂无匹配的岗位";

        JobMatch bestMatch = matches.get(0);
        return String.format("推荐岗位: %s, 综合匹配度: %.1f%%, 算法详情: TF-IDF(%.1f%%) Jaccard(%.1f%%) 语义(%.1f%%)",
                bestMatch.getJob().getTitle(),
                bestMatch.getMatchScore() * 100,
                bestMatch.getAlgorithmScores().getTfidfScore() * 100,
                bestMatch.getAlgorithmScores().getJaccardScore() * 100,
                bestMatch.getAlgorithmScores().getSemanticScore() * 100);
    }

    private List<JobPosition> getJobDatabase() {
        return Arrays.asList(
                createJob("Java开发工程师", "互联网",
                        Arrays.asList("Java", "Spring", "MySQL", "Redis"), 2, 15000.0),
                createJob("高级Java开发工程师", "互联网",
                        Arrays.asList("Java", "Spring Boot", "MySQL", "Redis", "Docker"), 3, 20000.0),
                createJob("前端开发工程师", "互联网",
                        Arrays.asList("JavaScript", "Vue", "React", "HTML"), 1, 12000.0),
                createJob("全栈开发工程师", "互联网",
                        Arrays.asList("Java", "Spring", "Vue", "MySQL"), 2, 18000.0),
                createJob("后端开发工程师", "互联网",
                        Arrays.asList("Java", "Spring Boot", "MySQL", "Redis"), 2, 16000.0)
        );
    }

    private JobPosition createJob(String title, String industry,
                                  List<String> skills, int exp, double salary) {
        JobPosition job = new JobPosition();
        job.setId(UUID.randomUUID().toString());
        job.setTitle(title);
        job.setCompany("示例科技公司");
        job.setIndustry(industry);
        job.setRequiredSkills(skills);
        job.setMinExperience(exp);
        job.setRequiredEducation("本科");
        job.setBaseSalary(salary);
        job.setDescription("招聘" + title + "，需要掌握" + String.join("、", skills) + "等技术，具有" + exp + "年以上相关经验");
        return job;
    }
}