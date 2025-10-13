package com.example.service;

import com.example.model.JobMatch;
import com.example.model.JobPosition;
import com.example.model.MatchResult;
import com.example.model.ParsedResume;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobMatchingService {

    public MatchResult basicMatch(ParsedResume resume, String industry) {
        long startTime = System.currentTimeMillis();

        List<JobPosition> relevantJobs = getJobDatabase().stream()
                .filter(job -> industry == null || industry.equalsIgnoreCase(job.getIndustry()))
                .collect(Collectors.toList());

        List<JobMatch> matches = relevantJobs.stream()
                .map(job -> createBasicJobMatch(resume, job))
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());

        MatchResult result = new MatchResult();
        result.setResume(resume);
        result.setJobMatches(matches);
        result.setAlgorithmUsed("基础技能匹配");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setAnalysis(generateBasicAnalysis(matches));

        return result;
    }

    private JobMatch createBasicJobMatch(ParsedResume resume, JobPosition job) {
        double score = calculateBasicMatchScore(resume, job);
        List<String> matchedSkills = findMatchedSkills(resume.getSkills(), job.getRequiredSkills());
        List<String> missingSkills = findMissingSkills(resume.getSkills(), job.getRequiredSkills());

        JobMatch match = new JobMatch();
        match.setJob(job);
        match.setMatchScore(score);
        match.setMatchedSkills(matchedSkills);
        match.setMissingSkills(missingSkills);
        match.setMatchReason(generateBasicMatchReason(score, matchedSkills.size()));

        return match;
    }

    private double calculateBasicMatchScore(ParsedResume resume, JobPosition job) {
        double score = 0.0;

        // 技能匹配 (60%)
        score += calculateSkillsMatch(resume.getSkills(), job.getRequiredSkills()) * 0.6;

        // 经验匹配 (40%)
        score += calculateExperienceMatch(resume, job) * 0.4;

        return Math.min(score, 1.0);
    }

    private double calculateSkillsMatch(List<String> resumeSkills, List<String> jobSkills) {
        if (jobSkills.isEmpty()) return 0.0;
        if (resumeSkills.isEmpty()) return 0.0;

        long matchedCount = resumeSkills.stream()
                .filter(resumeSkill -> jobSkills.stream()
                        .anyMatch(jobSkill -> jobSkill.equalsIgnoreCase(resumeSkill)))
                .count();

        return (double) matchedCount / jobSkills.size();
    }

    private double calculateExperienceMatch(ParsedResume resume, JobPosition job) {
        Integer resumeExp = resume.getPersonalInfo().getYearsOfExperience();
        if (resumeExp == null) return 0.3;
        if (job.getMinExperience() == null) return 0.5;

        if (resumeExp >= job.getMinExperience()) return 1.0;
        return (double) resumeExp / job.getMinExperience();
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

    private String generateBasicMatchReason(double score, int matchedSkillCount) {
        if (score >= 0.7) return "基础匹配良好，具备" + matchedSkillCount + "项所需技能";
        if (score >= 0.5) return "基础匹配一般，掌握" + matchedSkillCount + "项主要技能";
        return "基础匹配度较低，具备" + matchedSkillCount + "项相关技能";
    }

    private String generateBasicAnalysis(List<JobMatch> matches) {
        if (matches.isEmpty()) return "暂无匹配的岗位";
        JobMatch bestMatch = matches.get(0);
        return String.format("最匹配岗位: %s, 基础匹配度: %.1f%%",
                bestMatch.getJob().getTitle(), bestMatch.getMatchScore() * 100);
    }

    private List<JobPosition> getJobDatabase() {
        // 复用AdvancedMatchingService中的岗位数据
        return Arrays.asList(
                createJob("Java开发工程师", "互联网",
                        Arrays.asList("Java", "Spring", "MySQL", "Redis"), 2),
                createJob("前端开发工程师", "互联网",
                        Arrays.asList("JavaScript", "Vue", "React", "HTML"), 1)
        );
    }

    private JobPosition createJob(String title, String industry,
                                  List<String> skills, int exp) {
        JobPosition job = new JobPosition();
        job.setId(UUID.randomUUID().toString());
        job.setTitle(title);
        job.setCompany("示例公司");
        job.setIndustry(industry);
        job.setRequiredSkills(skills);
        job.setMinExperience(exp);
        job.setRequiredEducation("本科");
        job.setDescription("招聘" + title);
        return job;
    }
}